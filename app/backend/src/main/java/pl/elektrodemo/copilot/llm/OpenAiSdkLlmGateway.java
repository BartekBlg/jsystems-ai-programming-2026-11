package pl.elektrodemo.copilot.llm;

import com.openai.client.OpenAIClient;
import com.openai.core.RequestOptions;
import com.openai.core.Timeout;
import com.openai.core.http.StreamResponse;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.ReasoningEffort;
import com.openai.models.Reasoning;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseInputContent;
import com.openai.models.responses.ResponseInputImage;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseInputText;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.ResponseStreamEvent;
import com.openai.models.responses.ResponseUsage;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.ComplaintImageAssessment;
import pl.elektrodemo.copilot.cases.DecisionDraft;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.cases.ImageAssessment;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;
import pl.elektrodemo.copilot.prompts.PromptRepository;
import pl.elektrodemo.copilot.web.ErrorCode;
import pl.elektrodemo.copilot.web.dto.ChatMessageDto;

/**
 * ADR-002 §3: the only class that builds SDK requests/reads SDK responses. Steps 1-2 use strict structured
 * output, step 3 streams. Stateless (store=false, no previous_response_id). Retry policy (§3.5): at most one
 * retry, only for 429/5xx/connection errors, before any output, and only with at least 10 s of the step budget
 * left; timeouts are never retried. Logs numbers and ids only (logger {@code llm.usage}).
 *
 * <p>Cancellation of {@link #streamChat}: the caller interrupts the worker thread; the stream is closed and
 * the method returns silently (no sink callback).
 */
@Component
public class OpenAiSdkLlmGateway implements LlmGateway {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiSdkLlmGateway.class);
    private static final Logger USAGE = LoggerFactory.getLogger("llm.usage");
    private static final Duration MIN_RETRY_BUDGET = Duration.ofSeconds(10);
    private static final String POLICY_HEADING = "\n\n# Regulamin\n\n";

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "llm-first-token-watchdog");
        t.setDaemon(true);
        return t;
    });

    private enum Failure { TIMEOUT, RETRYABLE, FATAL }

    private final OpenAIClient client;
    private final LlmProperties props;
    private final PromptRepository prompts;

    public OpenAiSdkLlmGateway(OpenAIClient client, LlmProperties props, PromptRepository prompts) {
        this.client = client;
        this.props = props;
        this.prompts = prompts;
    }

    // ------------------------------------------------------------------ step 1

    @Override
    public ImageAssessment assessImage(CaseType caseType, EquipmentCategory category, byte[] jpeg,
            String preliminarySignals) {
        return assessImage(caseType, category, jpeg, preliminarySignals, props.timeouts().vision());
    }

    /** {@code timeout} is the whole vision step budget; the retry rule (>= 10 s left) applies to it. */
    @Override
    public ImageAssessment assessImage(CaseType caseType, EquipmentCategory category, byte[] jpeg,
            String preliminarySignals, Duration timeout) {
        List<ResponseInputContent> content = new ArrayList<>();
        content.add(text("Oceń zdjęcie zgodnie z instrukcją. Kategoria sprzętu: " + category.polishLabel() + "."));
        if (preliminarySignals != null) {
            content.add(text("Wstępne sygnały do weryfikacji: " + preliminarySignals));
        }
        content.add(ResponseInputContent.ofInputImage(ResponseInputImage.builder()
                .imageUrl("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg))
                .detail(ResponseInputImage.Detail.AUTO).build()));
        Class<? extends ImageAssessment> type = caseType == CaseType.RETURN ? ReturnImageAssessment.class
                : ComplaintImageAssessment.class;
        return structured("vision", props.visionModel(), timeout, type,
                prompts.imageAssessment(caseType), List.of(userMessage(content)), "low", props.maxOutputTokens().vision());
    }

    // ------------------------------------------------------------------ step 2

    @Override
    public DecisionDraft decide(DecisionPrompt prompt) {
        List<ResponseInputContent> content = new ArrayList<>();
        content.add(text(CaseBlockFormatter.decisionBlock(prompt)));
        if (prompt.correctiveNote() != null) {
            content.add(text(prompt.correctiveNote()));
        }
        return structured("decision", props.model(), props.timeouts().decision(), DecisionDraft.class,
                prompts.decision(prompt.caseType()) + POLICY_HEADING + prompt.policyText(), List.of(userMessage(content)),
                props.decisionReasoningEffort(), props.maxOutputTokens().decision());
    }

    private <T> T structured(String step, String model, Duration budget, Class<T> type, String instructions,
            List<ResponseInputItem> input, String effort, int maxOutputTokens) {
        StructuredResponseCreateParams<T> params;
        try {
            params = ResponseCreateParams.builder().model(model).instructions(instructions).inputOfResponse(input)
                    .reasoning(Reasoning.builder().effort(ReasoningEffort.of(effort)).build())
                    .maxOutputTokens(maxOutputTokens).store(false).text(type).build();
        } catch (RuntimeException e) {
            throw new LlmFailedException("request build failed: " + step, e);
        }
        long t0 = System.nanoTime();
        return withRetry(step, model, budget, remaining -> {
            StructuredResponse<T> response = client.responses().create(params, options(remaining));
            if (response.status().filter(s -> !s.equals(ResponseStatus.COMPLETED)).isPresent()) {
                throw new LlmFailedException("response status " + response.status().get().asString());
            }
            T value = response.output().stream().flatMap(i -> i.message().stream())
                    .flatMap(m -> m.content().stream()).flatMap(c -> c.outputText().stream()).findFirst()
                    .orElseThrow(() -> new LlmFailedException("no structured output"));
            ResponseUsageNumbers usage = response.usage().map(ResponseUsageNumbers::of).orElse(ResponseUsageNumbers.NONE);
            logUsage(step, model, usage, response.id(), t0);
            return value;
        });
    }

    private <R> R withRetry(String step, String model, Duration budget, Function<Duration, R> call) {
        long start = System.nanoTime();
        for (int attempt = 1;; attempt++) {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
            Duration remaining = budget.minus(elapsed);
            if (remaining.isNegative() || remaining.isZero()) {
                throw new LlmTimeoutException("budget exhausted: " + step);
            }
            try {
                return call.apply(remaining);
            } catch (LlmFailedException | LlmTimeoutException e) {
                logFailure(step, model, start, e);
                throw e;
            } catch (RuntimeException e) {
                Failure failure = classify(e);
                Duration left = budget.minus(Duration.ofNanos(System.nanoTime() - start));
                if (failure == Failure.RETRYABLE && attempt == 1 && left.compareTo(MIN_RETRY_BUDGET) >= 0) {
                    LOG.info("llm retry step={} model={} reason={}", step, model, describe(e));
                    continue;
                }
                logFailure(step, model, start, e);
                if (failure == Failure.TIMEOUT) {
                    throw new LlmTimeoutException("timeout in step " + step, e);
                }
                throw new LlmFailedException("LLM call failed in step " + step, e);
            }
        }
    }

    // ------------------------------------------------------------------ step 3

    @Override
    public void streamChat(ChatPrompt prompt, ChatSink sink) {
        String model = props.model();
        long start = System.nanoTime();
        ResponseCreateParams params;
        try {
            params = chatParams(prompt, model);
        } catch (RuntimeException e) {
            LOG.warn("llm chat request build failed: {}", e.getClass().getSimpleName());
            sink.onFailed(ErrorCode.LLM_FAILED);
            return;
        }
        boolean gotDelta = false;
        for (int attempt = 1;; attempt++) {
            Duration remaining = props.timeouts().chatTotal().minus(Duration.ofNanos(System.nanoTime() - start));
            if (remaining.isNegative() || remaining.isZero()) {
                sink.onFailed(ErrorCode.LLM_TIMEOUT);
                return;
            }
            StreamState state = new StreamState();
            ScheduledFuture<?> watchdog = null;
            try {
                Timeout timeout = Timeout.builder().connect(min(remaining, Duration.ofSeconds(10)))
                        .read(min(remaining, props.timeouts().chatFirstToken())).write(remaining).request(remaining).build();
                RequestOptions options = RequestOptions.builder().timeout(timeout).build();
                try (StreamResponse<ResponseStreamEvent> stream = client.responses().createStreaming(params, options)) {
                    long firstTokenMs = Math.max(1, props.timeouts().chatFirstToken().toMillis()
                            - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
                    watchdog = WATCHDOG.schedule(() -> {
                        if (!state.gotDelta.get()) {
                            state.firstTokenTimedOut.set(true);
                            stream.close();
                        }
                    }, firstTokenMs, TimeUnit.MILLISECONDS);
                    var events = stream.stream().iterator();
                    while (events.hasNext()) {
                        if (Thread.currentThread().isInterrupted()) {
                            return; // cancelled by the caller
                        }
                        ResponseStreamEvent event = events.next();
                        Optional<String> delta = event.outputTextDelta().map(d -> d.delta());
                        if (delta.isPresent()) {
                            state.gotDelta.set(true);
                            gotDelta = true;
                            watchdog.cancel(false);
                            try {
                                sink.onDelta(delta.get());
                            } catch (RuntimeException sinkFailure) {
                                LOG.debug("llm chat sink rejected a delta; stream closed");
                                return;
                            }
                        } else if (event.completed().isPresent()) {
                            var response = event.completed().get().response();
                            ResponseUsageNumbers usage = response.usage().map(ResponseUsageNumbers::of)
                                    .orElse(ResponseUsageNumbers.NONE);
                            logUsage("chat", model, usage, response.id(), start);
                            sink.onCompleted(new TokenUsage(Math.max(usage.input(), 0), Math.max(usage.output(), 0)));
                            return;
                        } else if (event.failed().isPresent() || event.incomplete().isPresent()
                                || event.error().isPresent()) {
                            LOG.warn("llm chat step=chat model={} terminal event failed/incomplete/error", model);
                            sink.onFailed(ErrorCode.LLM_FAILED);
                            return;
                        }
                    }
                }
                if (state.firstTokenTimedOut.get()) {
                    sink.onFailed(ErrorCode.LLM_TIMEOUT);
                } else {
                    LOG.warn("llm chat step=chat model={} stream ended without terminal event", model);
                    sink.onFailed(ErrorCode.LLM_FAILED);
                }
                return;
            } catch (RuntimeException e) {
                if (cancelled(e)) {
                    Thread.currentThread().interrupt();
                    return;
                }
                Failure failure = state.firstTokenTimedOut.get() ? Failure.TIMEOUT : classify(e);
                Duration left = props.timeouts().chatTotal().minus(Duration.ofNanos(System.nanoTime() - start));
                if (!gotDelta && failure == Failure.RETRYABLE && attempt == 1 && left.compareTo(MIN_RETRY_BUDGET) >= 0) {
                    LOG.info("llm retry step=chat model={} reason={}", model, describe(e));
                    continue;
                }
                logFailure("chat", model, start, e);
                sink.onFailed(failure == Failure.TIMEOUT ? ErrorCode.LLM_TIMEOUT : ErrorCode.LLM_FAILED);
                return;
            } finally {
                if (watchdog != null) {
                    watchdog.cancel(false);
                }
            }
        }
    }

    private static final class StreamState {
        final AtomicBoolean gotDelta = new AtomicBoolean();
        final AtomicBoolean firstTokenTimedOut = new AtomicBoolean();
    }

    private ResponseCreateParams chatParams(ChatPrompt chat, String model) {
        DecisionPrompt dp = chat.decisionPrompt();
        List<ResponseInputItem> input = new ArrayList<>();
        input.add(userMessage(List.of(text(CaseBlockFormatter.chatBlock(chat)))));
        List<ChatMessageDto> history = chat.history();
        for (int i = 0; i < history.size(); i++) {
            ChatMessageDto m = history.get(i);
            if ("assistant".equalsIgnoreCase(m.role())) {
                input.add(ResponseInputItem.ofResponseOutputMessage(ResponseOutputMessage.builder().id("msg_" + i)
                        .status(ResponseOutputMessage.Status.COMPLETED)
                        .addContent(ResponseOutputText.builder().text(m.text()).annotations(List.of()).build()).build()));
            } else {
                input.add(userMessage(List.of(text(m.text()))));
            }
        }
        if (chat.correctiveNote() != null) {
            input.add(userMessage(List.of(text(chat.correctiveNote()))));
        }
        return ResponseCreateParams.builder().model(model)
                .instructions(prompts.chat() + POLICY_HEADING + dp.policyText()).inputOfResponse(input)
                .reasoning(Reasoning.builder().effort(ReasoningEffort.of(props.chatReasoningEffort())).build())
                .maxOutputTokens(props.maxOutputTokens().chat()).store(false).build();
    }

    // ------------------------------------------------------------------ helpers

    private static ResponseInputContent text(String t) {
        return ResponseInputContent.ofInputText(ResponseInputText.builder().text(t).build());
    }

    private static ResponseInputItem userMessage(List<ResponseInputContent> content) {
        return ResponseInputItem.ofMessage(
                ResponseInputItem.Message.builder().role(ResponseInputItem.Message.Role.USER).content(content).build());
    }

    /**
     * OkHttp truncates call timeouts to whole milliseconds and treats 0 as "no timeout", so a sub-millisecond
     * remainder of a step budget would silently disable the limit. Round up to a whole millisecond (min 1 ms).
     */
    private static RequestOptions options(Duration timeout) {
        long nanos = Math.max(1, timeout.toNanos());
        Duration wholeMillis = Duration.ofMillis((nanos + 999_999) / 1_000_000);
        return RequestOptions.builder().timeout(wholeMillis).build();
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static Failure classify(Throwable e) {
        if (e instanceof OpenAIServiceException se) {
            return se.statusCode() == 429 || se.statusCode() >= 500 ? Failure.RETRYABLE : Failure.FATAL;
        }
        if (e instanceof OpenAIIoException) {
            return isTimeout(e) ? Failure.TIMEOUT : Failure.RETRYABLE;
        }
        if (isTimeout(e)) {
            return Failure.TIMEOUT;
        }
        return Failure.FATAL;
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException) {
                return true;
            }
            if (t instanceof InterruptedIOException && message(t).matches(".*(timeout|timed out).*")) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    /** True when the worker thread was interrupted (caller cancelled) rather than a network timeout. */
    private static boolean cancelled(Throwable e) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        }
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof InterruptedException
                    || (t instanceof InterruptedIOException && !isTimeout(t) && message(t).contains("interrupt"))) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private static String message(Throwable t) {
        return t.getMessage() == null ? "" : t.getMessage().toLowerCase(java.util.Locale.ROOT);
    }

    /** Class name and HTTP status only: SDK messages can echo response bodies. */
    private static String describe(Throwable e) {
        if (e instanceof OpenAIServiceException se) {
            return e.getClass().getSimpleName() + " status=" + se.statusCode() + " requestId=" + se.requestId().orElse("-");
        }
        return e.getClass().getSimpleName();
    }

    private void logFailure(String step, String model, long startNanos, Throwable e) {
        LOG.warn("llm failure step={} model={} latencyMs={} cause={}", step, model,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos), describe(e));
    }

    private void logUsage(String step, String model, ResponseUsageNumbers usage, String requestId, long startNanos) {
        USAGE.info("step={} model={} latencyMs={} inputTokens={} outputTokens={} cachedTokens={} requestId={}", step, model,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos), usage.input(), usage.output(),
                usage.cached(), requestId);
    }

    private record ResponseUsageNumbers(long input, long output, long cached) {

        static final ResponseUsageNumbers NONE = new ResponseUsageNumbers(-1, -1, -1);

        static ResponseUsageNumbers of(ResponseUsage usage) {
            try {
                long cached = -1;
                try {
                    cached = usage.inputTokensDetails().cachedTokens();
                } catch (RuntimeException ignored) {
                    // OpenRouter omits details for some models
                }
                return new ResponseUsageNumbers(usage.inputTokens(), usage.outputTokens(), cached);
            } catch (RuntimeException e) {
                return NONE;
            }
        }
    }
}
