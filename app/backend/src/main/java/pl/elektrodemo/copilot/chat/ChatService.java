package pl.elektrodemo.copilot.chat;

import java.util.List;
import org.springframework.stereotype.Service;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.RuleResult;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome;
import pl.elektrodemo.copilot.llm.ChatPrompt;
import pl.elektrodemo.copilot.llm.ChatSink;
import pl.elektrodemo.copilot.llm.DecisionPrompt;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.llm.TokenUsage;
import pl.elektrodemo.copilot.policy.PolicyRepository;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.web.ApiException;
import pl.elektrodemo.copilot.web.ErrorCode;
import pl.elektrodemo.copilot.web.InvalidCaseSignatureException;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.ChatMessageDto;
import pl.elektrodemo.copilot.web.dto.ChatRequest;

/**
 * ADR-001 3.5 / 5.3, ADR-002 3.7, ADR-005 D1: verifies the signed context, derives the current category,
 * relays one chat reply through the {@link DecisionGate} and emits exactly one terminal event.
 * Nothing of a reply whose announced category is forbidden in chat ever reaches {@link ChatEvents}.
 */
@Service
public class ChatService {

    private final LlmGateway llmGateway;
    private final PolicyRepository policyRepository;
    private final CaseSigner caseSigner;

    public ChatService(LlmGateway llmGateway, PolicyRepository policyRepository, CaseSigner caseSigner) {
        this.llmGateway = llmGateway;
        this.policyRepository = policyRepository;
        this.caseSigner = caseSigner;
    }

    /** Everything derived from a verified request; created before a stream starts. */
    public record PreparedChat(CaseContextDto context, DecisionCategory currentCategory,
            List<ChatMessageDto> history) {
    }

    /** The signed context carries no decision yet, so there is nothing to chat about (400 VALIDATION_FAILED). */
    public static final class MissingDecisionException extends ApiException {

        public MissingDecisionException() {
            super(ErrorCode.VALIDATION_FAILED);
        }
    }

    /** Pre-stream step: signature (401), decision present (400), D1 current category. Never calls the LLM. */
    public PreparedChat prepare(ChatRequest request) {
        CaseContextDto ctx = request.caseContext();
        if (ctx == null || !caseSigner.verify(ctx, request.signature())) {
            throw new InvalidCaseSignatureException();
        }
        if (ctx.decision() == null || ctx.decision().category() == null) {
            throw new MissingDecisionException();
        }
        List<ChatMessageDto> history = request.messages() == null ? List.of() : List.copyOf(request.messages());
        return new PreparedChat(ctx, currentCategory(ctx, history), history);
    }

    public void stream(ChatRequest request, ChatEvents out) {
        stream(prepare(request), out);
    }

    public void stream(PreparedChat chat, ChatEvents out) {
        Outcome outcome = attempt(chat, null, out);
        if (outcome instanceof Outcome.Forbidden first) {
            outcome = attempt(chat, correctiveNote(first.category()), out);
        }
        switch (outcome) {
            case Outcome.Completed c -> out.done(c.updated() != null ? c.updated() : chat.currentCategory());
            case Outcome.Failed f -> out.error(f.code());
            case Outcome.Forbidden ignored -> out.error(ErrorCode.LLM_FAILED);
            case Outcome.Abandoned ignored -> {
                // The client is gone or the thread was cancelled: nobody is left to receive a terminal event.
            }
        }
    }

    static String correctiveNote(DecisionCategory category) {
        return "Kategoria " + category.polishLabel() + " jest niedozwolona w tej rozmowie. "
                + "Nie zmieniaj decyzji na tę kategorię.";
    }

    // ------------------------------------------------------------ D1

    /** ADR-005 D1: last allowed "Zaktualizowana decyzja" assistant message, else the signed decision. */
    private static DecisionCategory currentCategory(CaseContextDto ctx, List<ChatMessageDto> history) {
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessageDto message = history.get(i);
            if (message != null && "assistant".equals(message.role()) && message.text() != null) {
                DecisionCategory announced = allowedAnnouncement(ctx.ruleFacts(), message.text());
                if (announced != null) {
                    return announced;
                }
            }
        }
        return ctx.decision().category();
    }

    /** Parses the first line exactly like the gate does; null if absent, unknown or forbidden in chat. */
    private static DecisionCategory allowedAnnouncement(RuleResult rules, String text) {
        DecisionGate gate = new DecisionGate(rules);
        GateOutcome outcome = gate.push(text);
        if (outcome instanceof GateOutcome.Buffering) {
            outcome = gate.finish();
        }
        return outcome instanceof GateOutcome.Decision d ? d.category() : null;
    }

    // ------------------------------------------------------------ one upstream attempt

    private sealed interface Outcome {
        record Completed(DecisionCategory updated) implements Outcome {
        }

        record Failed(ErrorCode code) implements Outcome {
        }

        record Forbidden(DecisionCategory category) implements Outcome {
        }

        record Abandoned() implements Outcome {
        }
    }

    private Outcome attempt(PreparedChat chat, String note, ChatEvents out) {
        CaseContextDto ctx = chat.context();
        DecisionPrompt decisionPrompt = new DecisionPrompt(ctx.caseType(), ctx.form(), ctx.submittedAt(),
                ctx.assessment(), ctx.assessmentSource(), ctx.ruleFacts(), policyRepository.policyFor(ctx.caseType()),
                ctx.prescreen(), ctx.consistencySignals(), null);
        ChatPrompt prompt = new ChatPrompt(decisionPrompt, chat.currentCategory(), chat.history(), note);
        GatedSink sink = new GatedSink(new DecisionGate(ctx.ruleFacts()), out);
        try {
            llmGateway.streamChat(prompt, sink);
        } catch (AbortUpstream expected) {
            // thrown by the sink to make the gateway close the upstream stream
        }
        if (sink.clientGone) {
            throw sink.failure;
        }
        if (sink.result == null && Thread.currentThread().isInterrupted()) {
            return new Outcome.Abandoned();
        }
        return sink.result != null ? sink.result : new Outcome.Failed(ErrorCode.LLM_FAILED);
    }

    /** Thrown out of {@code onDelta} after a forbidden category: the gateway swallows it and closes upstream. */
    private static final class AbortUpstream extends RuntimeException {
        AbortUpstream() {
            super("forbidden category", null, false, false);
        }
    }

    private static final class GatedSink implements ChatSink {

        private final DecisionGate gate;
        private final ChatEvents out;
        private DecisionCategory updated;
        Outcome result;
        boolean clientGone;
        RuntimeException failure;

        GatedSink(DecisionGate gate, ChatEvents out) {
            this.gate = gate;
            this.out = out;
        }

        @Override
        public void onDelta(String text) {
            if (result != null) {
                throw new AbortUpstream();
            }
            guarded(() -> handle(gate.push(text)));
        }

        @Override
        public void onCompleted(TokenUsage usage) {
            if (result != null) {
                return;
            }
            guarded(() -> {
                handle(gate.finish());
                if (result == null) {
                    result = new Outcome.Completed(updated);
                }
            });
        }

        @Override
        public void onFailed(ErrorCode code) {
            if (result == null) {
                result = new Outcome.Failed(code);
            }
        }

        private void handle(GateOutcome outcome) {
            switch (outcome) {
                case GateOutcome.Buffering ignored -> {
                }
                case GateOutcome.Suppressed ignored -> {
                }
                case GateOutcome.PassThrough ignored -> forward(gate.drainBuffered());
                case GateOutcome.Decision d -> {
                    updated = d.category();
                    out.decision(d.category());
                    forward(gate.drainBuffered());
                }
                case GateOutcome.Forward f -> forward(f.text());
                case GateOutcome.Forbidden f -> {
                    result = new Outcome.Forbidden(f.category());
                    throw new AbortUpstream();
                }
            }
        }

        private void forward(String text) {
            if (!text.isEmpty()) {
                out.delta(text);
            }
        }

        /** Remembers a failure of the outbound side (client gone) so the service can stop quietly. */
        private void guarded(Runnable action) {
            try {
                action.run();
            } catch (AbortUpstream e) {
                throw e;
            } catch (RuntimeException e) {
                clientGone = true;
                failure = e;
                throw e;
            }
        }
    }
}
