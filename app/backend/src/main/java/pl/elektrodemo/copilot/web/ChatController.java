package pl.elektrodemo.copilot.web;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.chat.ChatEvents;
import pl.elektrodemo.copilot.chat.ChatHistoryValidator;
import pl.elektrodemo.copilot.chat.ChatService;
import pl.elektrodemo.copilot.web.dto.ChatRequest;
import pl.elektrodemo.copilot.web.dto.FieldErrorDto;

/**
 * ADR-001 3.5 / 5.3: {@code POST /api/chat/messages} relays one chat reply as server-sent events.
 * Validation and signature verification happen before the emitter exists, so 400/401 are ordinary
 * problem+json responses; everything after the first byte is an event (HTTP status stays 200).
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final long EMITTER_TIMEOUT_MS = 90_000L;
    private static final String HISTORY_INVALID = "Nieprawidłowa historia rozmowy.";
    private static final ScheduledExecutorService HEARTBEATS = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "chat-heartbeat");
        t.setDaemon(true);
        return t;
    });

    private final ChatService chatService;
    private final ChatHistoryValidator historyValidator;
    private final Duration heartbeatInterval;
    private final AtomicInteger activeStreams = new AtomicInteger();

    public ChatController(ChatService chatService, ChatHistoryValidator historyValidator,
            @Value("${app.chat.heartbeat-interval:15s}") Duration heartbeatInterval) {
        this.chatService = chatService;
        this.historyValidator = historyValidator;
        this.heartbeatInterval = heartbeatInterval;
    }

    /** Number of relay threads that have not finished yet (observability for disconnect handling). */
    public int activeStreams() {
        return activeStreams.get();
    }

    @PostMapping(path = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> send(@RequestBody ChatRequest request) {
        List<FieldErrorDto> errors = new ArrayList<>();
        if (request.caseContext() == null) {
            errors.add(new FieldErrorDto("caseContext", "Brak danych zgłoszenia."));
        }
        if (request.signature() == null || request.signature().isBlank()) {
            errors.add(new FieldErrorDto("signature", "Brak podpisu zgłoszenia."));
        }
        errors.addAll(historyValidator.validate(request.messages()));
        if (!errors.isEmpty()) {
            throw new CaseController.RequestValidationException(errors);
        }
        ChatService.PreparedChat prepared;
        try {
            prepared = chatService.prepare(request);
        } catch (ChatService.MissingDecisionException e) {
            throw new CaseController.RequestValidationException(List.of(new FieldErrorDto("messages", HISTORY_INVALID)));
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        Relay relay = new Relay(emitter);
        emitter.onCompletion(relay::clientGoneOrFinished);
        emitter.onError(t -> relay.cancel());
        emitter.onTimeout(relay::cancel);
        relay.start(() -> chatService.stream(prepared, relay));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    /** Thrown by the outbound side when the client has gone; unwinds the gateway's sink callback. */
    private static final class ClientGoneException extends RuntimeException {
        ClientGoneException(Throwable cause) {
            super("client gone", cause, false, false);
        }
    }

    /** One relay: virtual worker thread, event writer, heartbeat. */
    private final class Relay implements ChatEvents {

        private final SseEmitter emitter;
        private final AtomicBoolean firstEventSent = new AtomicBoolean();
        private final AtomicBoolean terminalSent = new AtomicBoolean();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile Thread worker;
        private volatile ScheduledFuture<?> heartbeat;

        Relay(SseEmitter emitter) {
            this.emitter = emitter;
        }

        void start(Runnable work) {
            activeStreams.incrementAndGet();
            heartbeat = HEARTBEATS.scheduleWithFixedDelay(this::beat, heartbeatInterval.toMillis(),
                    heartbeatInterval.toMillis(), TimeUnit.MILLISECONDS);
            worker = Thread.ofVirtual().name("chat-stream").start(() -> run(work));
        }

        private void run(Runnable work) {
            try {
                work.run();
            } catch (ClientGoneException gone) {
                log.debug("chat client disconnected");
            } catch (RuntimeException e) {
                log.error("Chat stream failed", e);
                if (!finished.get() && !terminalSent.get()) {
                    try {
                        error(ErrorCode.LLM_FAILED);
                    } catch (ClientGoneException ignored) {
                        // nobody to tell
                    }
                }
            } finally {
                finish();
                activeStreams.decrementAndGet();
            }
        }

        private void finish() {
            if (finished.compareAndSet(false, true)) {
                stopHeartbeat();
                emitter.complete();
            }
        }

        private void stopHeartbeat() {
            ScheduledFuture<?> h = heartbeat;
            if (h != null) {
                h.cancel(false);
            }
        }

        /** Emitter completed (by us or by the container after a disconnect). */
        void clientGoneOrFinished() {
            if (!finished.get()) {
                cancel();
            }
        }

        /** Disconnect, error or timeout: stop the heartbeat and interrupt the worker so the gateway closes upstream. */
        void cancel() {
            stopHeartbeat();
            Thread t = worker;
            if (t != null && !finished.get()) {
                t.interrupt();
            }
        }

        private void beat() {
            if (firstEventSent.get() || finished.get()) {
                stopHeartbeat();
                return;
            }
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                cancel();
            }
        }

        // ---- ChatEvents

        @Override
        public void decision(DecisionCategory category) {
            send("decision", Map.of("category", category.name()));
        }

        @Override
        public void delta(String text) {
            send("delta", Map.of("text", text));
        }

        @Override
        public void done(DecisionCategory category) {
            terminalSent.set(true);
            send("done", Map.of("category", category.name()));
        }

        @Override
        public void error(ErrorCode code) {
            terminalSent.set(true);
            send("error", Map.of("code", code.name()));
        }

        private void send(String name, Map<String, String> data) {
            firstEventSent.set(true);
            try {
                emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                cancel();
                throw new ClientGoneException(e);
            }
        }
    }
}
