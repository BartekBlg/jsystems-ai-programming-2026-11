package pl.elektrodemo.copilot.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import pl.elektrodemo.copilot.cases.ComplaintImageAssessment;
import pl.elektrodemo.copilot.cases.DecisionDraft;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loads recorded Responses API bodies from classpath:captures/responses/ (T04 captures) and returns the
 * structured output parsed from the single {@code output_text} content. Mocked LlmGateway results must come
 * from here, never from hand-written objects.
 */
public final class CaptureFixtures {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private CaptureFixtures() {
    }

    public static ReturnImageAssessment returnAssessmentOk() {
        return parse("assessment-return-ok", ReturnImageAssessment.class);
    }

    public static ComplaintImageAssessment complaintAssessmentOk() {
        return parse("assessment-complaint-ok", ComplaintImageAssessment.class);
    }

    public static ReturnImageAssessment insufficientAssessment() {
        return parse("assessment-insufficient", ReturnImageAssessment.class);
    }

    public static DecisionDraft decisionReturnOk() {
        return parse("decision-return-ok", DecisionDraft.class);
    }

    public static DecisionDraft decisionReturnMoreInfo() {
        return parse("decision-return-more-info", DecisionDraft.class);
    }

    public static DecisionDraft decisionReturnInsufficient() {
        return parse("decision-return-insufficient", DecisionDraft.class);
    }

    public static DecisionDraft decisionComplaintOk() {
        return parse("decision-complaint-ok", DecisionDraft.class);
    }

    private static <T> T parse(String name, Class<T> type) {
        JsonNode body = read("captures/responses/" + name + ".json");
        List<String> texts = new ArrayList<>();
        collectOutputText(body, texts);
        if (texts.size() != 1) {
            throw new IllegalStateException("Expected exactly one output_text in " + name + ", got " + texts.size());
        }
        return MAPPER.readValue(texts.get(0), type);
    }

    private static void collectOutputText(JsonNode node, List<String> out) {
        if (node.isObject()) {
            if ("output_text".equals(node.path("type").asString()) && node.has("text")) {
                out.add(node.get("text").asString());
            }
            node.properties().forEach(e -> collectOutputText(e.getValue(), out));
        } else if (node.isArray()) {
            node.forEach(child -> collectOutputText(child, out));
        }
    }

    private static JsonNode read(String resource) {
        try (InputStream in = CaptureFixtures.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + resource);
            }
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
