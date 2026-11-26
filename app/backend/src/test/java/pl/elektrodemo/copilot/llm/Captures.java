package pl.elektrodemo.copilot.llm;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test helper: serves the T04 captures from {@code classpath:captures/responses/} through WireMock
 * with the captured status, headers and body. Hop-by-hop / framing headers are dropped because
 * WireMock frames the body itself; the {@code .json} captures store the body as parsed JSON, so it is
 * re-serialised compactly (same content, not the original whitespace).
 */
final class Captures {

    static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Set<String> SKIPPED = Set.of(":status", "content-length", "content-encoding",
            "transfer-encoding", "connection", "date");

    private Captures() {
    }

    /** A non-streaming capture: {@code <stem>.json} holds {status, headers, body}. */
    static ResponseDefinitionBuilder json(String stem) {
        JsonNode capture = readJson("captures/responses/" + stem + ".json");
        return build(capture.get("status").asInt(), capture.get("headers"),
                MAPPER.writeValueAsString(capture.get("body")).getBytes(StandardCharsets.UTF_8));
    }

    /** A streaming capture: {@code <stem>.sse} raw body + {@code <stem>.headers.json}; derived stems reuse the source headers. */
    static ResponseDefinitionBuilder sse(String stem) {
        String headerStem = stem.contains(".derived-") ? stem.substring(0, stem.indexOf(".derived-")) : stem;
        JsonNode headers = readJson("captures/responses/" + headerStem + ".headers.json");
        return build(headers.get("status").asInt(), headers.get("headers"), rawSse(stem));
    }

    static byte[] rawSse(String stem) {
        return readBytes("captures/responses/" + stem + ".sse");
    }

    /** Concatenated {@code response.output_text.delta} texts of an SSE capture. */
    static String sseDeltas(String stem) {
        StringBuilder out = new StringBuilder();
        for (String line : new String(rawSse(stem), StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith("data: {")) {
                JsonNode event = MAPPER.readTree(line.substring(6));
                if ("response.output_text.delta".equals(event.path("type").asString())) {
                    out.append(event.get("delta").asString());
                }
            }
        }
        return out.toString();
    }

    /** {@code usage} object of the {@code response.completed} event of an SSE capture. */
    static JsonNode sseUsage(String stem) {
        for (String line : new String(rawSse(stem), StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith("data: {")) {
                JsonNode event = MAPPER.readTree(line.substring(6));
                if ("response.completed".equals(event.path("type").asString())) {
                    return event.get("response").get("usage");
                }
            }
        }
        throw new IllegalStateException("no response.completed in " + stem);
    }

    /** The single {@code output_text} text of a non-streaming capture (the structured-output JSON). */
    static String outputText(String stem) {
        List<String> texts = new ArrayList<>();
        collect(readJson("captures/responses/" + stem + ".json").get("body"), texts);
        if (texts.size() != 1) {
            throw new IllegalStateException(stem + ": expected one output_text, got " + texts.size());
        }
        return texts.get(0);
    }

    /** The user "Dane zgłoszenia" text of a captured request. */
    static String requestUserText(String stem) {
        return readJson("captures/responses/" + stem + ".request.json").get("input").get(0).get("content").get(0)
                .get("text").asString();
    }

    private static void collect(JsonNode node, List<String> out) {
        if (node.isObject()) {
            if ("output_text".equals(node.path("type").asString()) && node.has("text")) {
                out.add(node.get("text").asString());
            }
            node.properties().forEach(e -> collect(e.getValue(), out));
        } else if (node.isArray()) {
            node.forEach(c -> collect(c, out));
        }
    }

    private static ResponseDefinitionBuilder build(int status, JsonNode headers, byte[] body) {
        ResponseDefinitionBuilder builder = new ResponseDefinitionBuilder().withStatus(status).withBody(body);
        headers.properties().forEach(e -> {
            if (!SKIPPED.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                List<String> values = new ArrayList<>();
                e.getValue().forEach(v -> values.add(v.asString()));
                builder.withHeader(e.getKey(), values.toArray(String[]::new));
            }
        });
        return builder;
    }

    private static JsonNode readJson(String resource) {
        return MAPPER.readTree(readBytes(resource));
    }

    private static byte[] readBytes(String resource) {
        try (InputStream in = Captures.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + resource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
