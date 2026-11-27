package pl.elektrodemo.copilot.cases;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test helper: loads recorded Responses API bodies (T04 captures) and extracts the structured
 * output {@code category}/{@code message} from the {@code output_text} content.
 */
final class DecisionCaptures {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private DecisionCaptures() {
    }

    static DecisionDraft load(String name) {
        JsonNode body = read("captures/responses/" + name + ".json");
        List<String> texts = new ArrayList<>();
        collectOutputText(body, texts);
        if (texts.size() != 1) {
            throw new IllegalStateException("Expected exactly one output_text in " + name + ", got " + texts.size());
        }
        JsonNode draft = MAPPER.readTree(texts.get(0));
        return new DecisionDraft(DecisionCategory.valueOf(draft.get("category").asString()),
                draft.get("message").asString());
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
        try (InputStream in = resource(resource)) {
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static InputStream resource(String path) {
        InputStream in = DecisionCaptures.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Missing test resource " + path);
        }
        return in;
    }
}
