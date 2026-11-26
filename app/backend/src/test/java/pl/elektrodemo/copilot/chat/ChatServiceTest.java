package pl.elektrodemo.copilot.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.core.io.DefaultResourceLoader;
import pl.elektrodemo.copilot.cases.AssessmentSource;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.cases.ReturnWindowStatus;
import pl.elektrodemo.copilot.cases.RuleResult;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.llm.ChatPrompt;
import pl.elektrodemo.copilot.llm.ChatSink;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.llm.TokenUsage;
import pl.elektrodemo.copilot.policy.PolicyRepository;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.support.CaptureFixtures;
import pl.elektrodemo.copilot.web.ErrorCode;
import pl.elektrodemo.copilot.web.InvalidCaseSignatureException;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.dto.ChatMessageDto;
import pl.elektrodemo.copilot.web.dto.ChatRequest;
import pl.elektrodemo.copilot.web.dto.DecisionRef;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * T23 / ADR-002 3.7, ADR-005 D1: ChatService with a mocked gateway that replays the delta sequences of the
 * recorded streams (T04 captures; the forbidden stream is the derived capture documented next to it).
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    // 32 bytes, Base64: test-only key, not a secret
    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3BxcnN0dXY=";
    private static final String FORBIDDEN_NOTE = "Kategoria Odrzucone jest niedozwolona w tej rozmowie. "
            + "Nie zmieniaj decyzji na tę kategorię.";
    private static final Set<DecisionCategory> REJECTED_FORBIDDEN = Set.of(DecisionCategory.REJECTED);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Mock
    LlmGateway llm;

    private CaseSigner signer;
    private ChatService service;
    private List<String> plain;
    private List<String> update;
    private List<String> forbidden;

    @BeforeEach
    void setUp() {
        signer = new CaseSigner(new AppProperties(TEST_KEY, null, null));
        service = new ChatService(llm, new PolicyRepository(new DefaultResourceLoader()), signer);
        plain = deltas("chat-plain.sse");
        update = deltas("chat-decision-update.sse");
        forbidden = deltas("chat-decision-update.derived-forbidden.sse");
    }

    // ------------------------------------------------------------------ behavior

    @Test
    void plainReplyIsForwardedInOrderAndDoneKeepsTheSignedCategory() {
        replayInOrder(plain);
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.kinds()).startsWith("delta").endsWith("done").doesNotContain("decision", "error");
        assertThat(out.deltaText()).isEqualTo(String.join("", plain));
        assertThat(out.last()).isEqualTo("done:MORE_INFO_NEEDED");
        assertThat(out.terminalCount()).isEqualTo(1);
        assertThat(out.emptyDeltas).isZero();
        ChatPrompt prompt = prompts(1).get(0);
        assertThat(prompt.correctiveNote()).isNull();
        assertThat(prompt.currentCategory()).isEqualTo(DecisionCategory.MORE_INFO_NEEDED);
        assertThat(prompt.history()).isEqualTo(history());
        assertThat(prompt.decisionPrompt().policyText()).isNotBlank();
        assertThat(prompt.decisionPrompt().ruleResult()).isNotNull();
    }

    @Test
    void decisionUpdateAnnouncesTheCategoryBeforeTheFirstDeltaAndStartsTheReplyWithTheUpdateLine() {
        replayInOrder(update);
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.events.get(0)).isEqualTo("decision:ESCALATED");
        assertThat(out.events.get(1)).startsWith("delta:Zaktualizowana decyzja:");
        assertThat(out.kinds().stream().filter("decision"::equals)).hasSize(1);
        assertThat(out.deltaText()).isEqualTo(String.join("", update));
        assertThat(out.last()).isEqualTo("done:ESCALATED");
        assertThat(out.terminalCount()).isEqualTo(1);
        assertThat(out.emptyDeltas).isZero();
    }

    @Test
    void forbiddenCategoryIsNeverEmittedAndOneRegenerationFollowsWithTheCorrectiveNote() {
        replayInOrder(forbidden, plain);
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.deltaText()).isEqualTo(String.join("", plain));
        assertThat(out.deltaText()).doesNotContain("Odrzucone").doesNotContain("Zaktualizowana");
        assertThat(out.kinds()).doesNotContain("decision", "error");
        assertThat(out.last()).isEqualTo("done:MORE_INFO_NEEDED");
        assertThat(out.terminalCount()).isEqualTo(1);
        List<ChatPrompt> prompts = prompts(2);
        assertThat(prompts.get(0).correctiveNote()).isNull();
        assertThat(prompts.get(1).correctiveNote()).isEqualTo(FORBIDDEN_NOTE);
        assertThat(prompts.get(1).currentCategory()).isEqualTo(DecisionCategory.MORE_INFO_NEEDED);
    }

    @Test
    void regenerationMayAnnounceAnAllowedUpdate() {
        replayInOrder(forbidden, update);
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.events.get(0)).isEqualTo("decision:ESCALATED");
        assertThat(out.deltaText()).isEqualTo(String.join("", update));
        assertThat(out.last()).isEqualTo("done:ESCALATED");
        assertThat(out.terminalCount()).isEqualTo(1);
    }

    @Test
    void forbiddenTwiceEndsWithTheSingleErrorAndNothingElse() {
        replayInOrder(forbidden, forbidden);
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.events).containsExactly("error:LLM_FAILED");
        prompts(2);
    }

    @Test
    void forbiddenCategoryOnShortReplyIsCaughtAtCompletion() {
        // The reply ends before the gate window closes: the decision happens in finish().
        replayInOrder(forbidden.subList(0, 14), plain);
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.deltaText()).isEqualTo(String.join("", plain));
        assertThat(out.last()).isEqualTo("done:MORE_INFO_NEEDED");
        assertThat(prompts(2).get(1).correctiveNote()).isEqualTo(FORBIDDEN_NOTE);
    }

    @Test
    void shortPlainReplyIsFlushedAtCompletion() {
        replayInOrder(plain.subList(0, 3));
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.deltaText()).isEqualTo(String.join("", plain.subList(0, 3)));
        assertThat(out.last()).isEqualTo("done:MORE_INFO_NEEDED");
    }

    @Test
    void gatewayFailureBeforeAnyOutputIsASingleError() {
        doAnswer(inv -> {
            ((ChatSink) inv.getArgument(1)).onFailed(ErrorCode.LLM_TIMEOUT);
            return null;
        }).when(llm).streamChat(any(), any());
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.events).containsExactly("error:LLM_TIMEOUT");
    }

    @Test
    void gatewayFailureAfterDeltasKeepsThemAndEndsWithOneError() {
        List<String> head = plain.subList(0, 40);
        doAnswer(inv -> {
            ChatSink sink = inv.getArgument(1);
            head.forEach(sink::onDelta);
            sink.onFailed(ErrorCode.LLM_FAILED);
            return null;
        }).when(llm).streamChat(any(), any());
        Recorder out = new Recorder();

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out);

        assertThat(out.kinds()).startsWith("delta");
        assertThat(out.deltaText()).isEqualTo(String.join("", head));
        assertThat(out.last()).isEqualTo("error:LLM_FAILED");
        assertThat(out.terminalCount()).isEqualTo(1);
        verify(llm, times(1)).streamChat(any(), any());
    }

    @Test
    void clientGoneWhileForwardingStopsWithoutRegenerationOrErrorEvent() {
        replayInOrder(plain, plain);
        Recorder out = new Recorder();
        out.failOnDelta = true;

        assertThatThrownBy(() -> service.stream(
                request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history()), out))
                .isInstanceOf(IllegalStateException.class);

        assertThat(out.events).isEmpty();
        verify(llm, times(1)).streamChat(any(), any());
    }

    // ---------------------------------------------------- D1 current category

    @Test
    void lastAllowedUpdateInTheHistoryBecomesTheCurrentCategory() {
        replayInOrder(plain);
        Recorder out = new Recorder();
        List<ChatMessageDto> history = List.of(assistantMessage(0, decisionMessage()), user(1, "Dostarczono 3 dni temu."),
                assistantMessage(2, String.join("", update)), user(3, "Dziękuję, a co dalej?"));

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history), out);

        assertThat(prompts(1).get(0).currentCategory()).isEqualTo(DecisionCategory.ESCALATED);
        assertThat(out.last()).isEqualTo("done:ESCALATED");
    }

    @Test
    void lastUpdateForbiddenFallsBackToTheEarlierValidUpdate() {
        replayInOrder(plain);
        Recorder out = new Recorder();
        List<ChatMessageDto> history = List.of(assistantMessage(0, decisionMessage()), user(1, "Dostarczono 3 dni temu."),
                assistantMessage(2, String.join("", update)), user(3, "A jeśli odrzucicie?"),
                assistantMessage(4, String.join("", forbidden)), user(5, "Jeszcze raz."));

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history), out);

        assertThat(prompts(1).get(0).currentCategory()).isEqualTo(DecisionCategory.ESCALATED);
        assertThat(out.last()).isEqualTo("done:ESCALATED");
    }

    @Test
    void onlyForbiddenUpdatesFallBackToTheSignedDecision() {
        replayInOrder(plain);
        Recorder out = new Recorder();
        List<ChatMessageDto> history = List.of(assistantMessage(0, decisionMessage()), user(1, "Dostarczono 3 dni temu."),
                assistantMessage(2, String.join("", forbidden)), user(3, "Jeszcze raz."));

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history), out);

        assertThat(prompts(1).get(0).currentCategory()).isEqualTo(DecisionCategory.MORE_INFO_NEEDED);
        assertThat(out.last()).isEqualTo("done:MORE_INFO_NEEDED");
    }

    @Test
    void updateLineInTheMiddleOfAMessageIsNotAnUpdate() {
        replayInOrder(plain);
        Recorder out = new Recorder();
        List<ChatMessageDto> history = List.of(assistantMessage(0, decisionMessage()), user(1, "Dostarczono 3 dni temu."),
                assistantMessage(2, "Wstęp\n" + String.join("", update)), user(3, "Dalej?"));

        service.stream(request(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), history), out);

        assertThat(prompts(1).get(0).currentCategory()).isEqualTo(DecisionCategory.MORE_INFO_NEEDED);
    }

    // ------------------------------------------------------------- pre-stream

    @Test
    void tamperedContextIsRejectedBeforeTheGatewayIsCalled() {
        CaseContextDto signed = signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN);
        String signature = signer.sign(signed);
        CaseContextDto tampered = withDecision(signed, DecisionCategory.ACCEPTED);
        Recorder out = new Recorder();

        assertThatThrownBy(() -> service.stream(new ChatRequest(tampered, signature, history()), out))
                .isInstanceOf(InvalidCaseSignatureException.class);

        verify(llm, never()).streamChat(any(), any());
        assertThat(out.events).isEmpty();
    }

    @Test
    void missingSignatureIsRejectedBeforeTheGatewayIsCalled() {
        CaseContextDto signed = signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN);

        assertThatThrownBy(() -> service.stream(new ChatRequest(signed, " ", history()), new Recorder()))
                .isInstanceOf(InvalidCaseSignatureException.class);

        verify(llm, never()).streamChat(any(), any());
    }

    @Test
    void signedContextWithoutDecisionIsRejectedBeforeTheGatewayIsCalled() {
        CaseContextDto undecided = withDecision(signed(DecisionCategory.MORE_INFO_NEEDED, REJECTED_FORBIDDEN), null);

        assertThatThrownBy(() -> service.stream(request(undecided, history()), new Recorder()))
                .isInstanceOf(ChatService.MissingDecisionException.class);

        verify(llm, never()).streamChat(any(), any());
    }

    // ---------------------------------------------------------------- helpers

    @SafeVarargs
    private void replayInOrder(List<String>... streams) {
        org.mockito.stubbing.Stubber stubber = null;
        for (List<String> deltas : streams) {
            Answer<Void> answer = replay(deltas);
            stubber = stubber == null ? doAnswer(answer) : stubber.doAnswer(answer);
        }
        stubber.when(llm).streamChat(any(), any());
    }

    /** Mimics the real gateway: stops silently when the sink rejects a delta by throwing. */
    private static Answer<Void> replay(List<String> deltas) {
        return invocation -> {
            ChatSink sink = invocation.getArgument(1);
            try {
                deltas.forEach(sink::onDelta);
                sink.onCompleted(new TokenUsage(1, 1));
            } catch (RuntimeException sinkRejected) {
                // upstream closed
            }
            return null;
        };
    }

    private List<ChatPrompt> prompts(int expectedCalls) {
        ArgumentCaptor<ChatPrompt> captor = ArgumentCaptor.forClass(ChatPrompt.class);
        verify(llm, times(expectedCalls)).streamChat(captor.capture(), any());
        return captor.getAllValues();
    }

    private CaseContextDto signed(DecisionCategory decision, Set<DecisionCategory> chatForbidden) {
        return new CaseContextDto(2, CaseType.RETURN,
                new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "Lenovo ThinkPad T14 Gen 4",
                        LocalDate.of(2026, 11, 21), ""),
                OffsetDateTime.parse("2026-11-26T10:00:00+01:00"), CaptureFixtures.returnAssessmentOk(),
                AssessmentSource.LLM, null, null,
                new RuleResult(5, ReturnWindowStatus.WITHIN_14_DAYS, false,
                        Set.of(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED), "", chatForbidden, "",
                        "R5"),
                new DecisionRef(decision));
    }

    private static CaseContextDto withDecision(CaseContextDto c, DecisionCategory category) {
        return new CaseContextDto(c.contextVersion(), c.caseType(), c.form(), c.submittedAt(), c.assessment(),
                c.assessmentSource(), c.prescreen(), c.consistencySignals(), c.ruleFacts(),
                category == null ? null : new DecisionRef(category));
    }

    private ChatRequest request(CaseContextDto ctx, List<ChatMessageDto> messages) {
        return new ChatRequest(ctx, signer.sign(ctx), messages);
    }

    private static List<ChatMessageDto> history() {
        return List.of(assistantMessage(0, decisionMessage()), user(1, "Urządzenie zostało dostarczone 3 dni temu."));
    }

    private static String decisionMessage() {
        return CaptureFixtures.decisionReturnOk().message();
    }

    private static ChatMessageDto assistantMessage(int i, String text) {
        return new ChatMessageDto("m" + i, "assistant", text);
    }

    private static ChatMessageDto user(int i, String text) {
        return new ChatMessageDto("m" + i, "user", text);
    }

    /** The {@code response.output_text.delta} texts of a recorded stream, in order. */
    private static List<String> deltas(String file) {
        List<String> out = new ArrayList<>();
        try (InputStream in = ChatServiceTest.class.getClassLoader().getResourceAsStream("captures/responses/" + file)) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (line.startsWith("data: {")) {
                    JsonNode event = MAPPER.readTree(line.substring(6));
                    if ("response.output_text.delta".equals(event.path("type").asString())) {
                        out.add(event.get("delta").asString());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    private static final class Recorder implements ChatEvents {
        final List<String> events = new ArrayList<>();
        boolean failOnDelta;
        int emptyDeltas;

        @Override
        public void decision(DecisionCategory category) {
            events.add("decision:" + category);
        }

        @Override
        public void delta(String text) {
            if (failOnDelta) {
                throw new IllegalStateException("client gone");
            }
            if (text.isEmpty()) {
                emptyDeltas++;
            }
            events.add("delta:" + text);
        }

        @Override
        public void done(DecisionCategory category) {
            events.add("done:" + category);
        }

        @Override
        public void error(ErrorCode code) {
            events.add("error:" + code);
        }

        List<String> kinds() {
            return events.stream().map(e -> e.substring(0, e.indexOf(':'))).distinct().toList();
        }

        String deltaText() {
            StringBuilder sb = new StringBuilder();
            events.stream().filter(e -> e.startsWith("delta:")).forEach(e -> sb.append(e.substring(6)));
            return sb.toString();
        }

        String last() {
            return events.get(events.size() - 1);
        }

        long terminalCount() {
            return events.stream().filter(e -> e.startsWith("done:") || e.startsWith("error:")).count();
        }
    }
}
