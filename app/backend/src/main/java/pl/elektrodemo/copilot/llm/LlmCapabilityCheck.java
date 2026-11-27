package pl.elektrodemo.copilot.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * TAC-002-07: one non-fatal, asynchronous {@code GET {baseUrl}/models/{model}/endpoints} per configured model
 * at startup. Warns when {@code image} input (vision model) or {@code structured_outputs}/{@code reasoning}
 * support is not advertised. The response shape was never captured, so the check only looks for the
 * capability tokens in the raw body; any failure yields a single WARN. Skipped when
 * {@code llm.allow-missing-key=true}. Never logs the key or any body text.
 */
@Component
public class LlmCapabilityCheck {

    private static final Logger LOG = LoggerFactory.getLogger(LlmCapabilityCheck.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final LlmProperties props;
    private final boolean allowMissingKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public LlmCapabilityCheck(LlmProperties props, @Value("${llm.allow-missing-key:false}") boolean allowMissingKey) {
        this.props = props;
        this.allowMissingKey = allowMissingKey;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        if (!allowMissingKey) {
            Thread.ofVirtual().name("llm-capability-check").start(this::run);
        }
    }

    /** Synchronous body of the check; never throws. */
    void run() {
        if (allowMissingKey) {
            return;
        }
        Set<String> models = new LinkedHashSet<>();
        models.add(props.visionModel());
        models.add(props.model());
        for (String model : models) {
            try {
                checkModel(model, model.equals(props.visionModel()));
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOG.warn("LLM capability check failed for model={}: {}", model, e.getClass().getSimpleName());
            }
        }
    }

    private void checkModel(String model, boolean vision) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(props.baseUrl() + "/models/" + model + "/endpoints"))
                .timeout(TIMEOUT).header("Authorization", "Bearer " + props.apiKey()).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            LOG.warn("LLM capability check inconclusive for model={}: HTTP {}", model, response.statusCode());
            return;
        }
        String body = response.body();
        List<String> missing = new ArrayList<>();
        if (vision && !body.contains("\"image\"")) {
            missing.add("image input");
        }
        if (!body.contains("structured_outputs")) {
            missing.add("structured_outputs");
        }
        if (!body.contains("reasoning")) {
            missing.add("reasoning");
        }
        if (missing.isEmpty()) {
            LOG.info("LLM capability check OK for model={}", model);
        } else {
            LOG.warn("LLM model={} does not advertise: {}", model, String.join(", ", missing));
        }
    }
}
