package pl.elektrodemo.copilot.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Shape and no-leak checks for problem+json bodies (ADR-001 §6.3, AC-43, TAC-05). */
public final class ProblemAssertions {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final List<String> FORBIDDEN = List.of("Exception", "at pl.", "openrouter", "OpenRouter", "gpt",
            "jev", "sk-or", "stack");

    private ProblemAssertions() {
    }

    /** Checks status, code, required members and that nothing internal leaks; returns the parsed body. */
    public static JsonNode assertProblem(String json, int status, String code) {
        for (String word : FORBIDDEN) {
            assertThat(json).as("body must not contain '%s'", word).doesNotContain(word);
        }
        JsonNode body = MAPPER.readTree(json);
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asString()).isEqualTo(code);
        assertThat(body.path("type").asString()).isNotBlank();
        assertThat(body.path("title").asString()).isNotBlank();
        assertThat(body.path("detail").asString()).isNotBlank();
        assertThat(body.path("instance").asString()).startsWith("/");
        return body;
    }
}
