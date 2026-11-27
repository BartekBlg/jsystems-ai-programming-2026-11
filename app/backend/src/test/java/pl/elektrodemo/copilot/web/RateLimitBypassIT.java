package pl.elektrodemo.copilot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.elektrodemo.copilot.support.ProblemAssertions.assertProblem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * T50c-fix H1/M3: the limit must key on the normalized path (no path parameters, percent-decoded), so alternate
 * spellings that Spring still routes to the same handler cannot bypass it. Real embedded server and raw HTTP client
 * (MockMvc would normalize). Non-image JSON bodies: counted by the filter, never reach the LLM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitBypassIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.rate-limit.cases-per-hour", () -> "1");
        r.add("app.rate-limit.chat-per-hour", () -> "1");
        r.add("app.rate-limit.decisions-per-hour", () -> "1");
    }

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> post(String rawPath) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + rawPath))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void assessmentsLimitCannotBeBypassedWithPathParametersOrPercentEncoding() throws Exception {
        assertThat(post("/api/cases/assessments").statusCode()).isNotEqualTo(429);

        assertThat(post("/api/cases/assessments;x=1").statusCode()).isEqualTo(429);
        assertThat(post("/api/cases/%61ssessments").statusCode()).isEqualTo(429);
        assertThat(post("/api/cases/assessments/;y=2").statusCode()).isNotEqualTo(200);
    }

    @Test
    void chatLimitCannotBeBypassedWithPathParametersOrPercentEncoding() throws Exception {
        assertThat(post("/api/chat/messages").statusCode()).isNotEqualTo(429);

        assertThat(post("/api/chat/messages;x=1").statusCode()).isEqualTo(429);
        assertThat(post("/api/chat/%6Dessages").statusCode()).isEqualTo(429);
    }

    @Test
    void decisionsAreLimitedWithTheCasesTextAndNotBypassable() throws Exception {
        assertThat(post("/api/cases/decisions").statusCode()).isNotEqualTo(429);

        HttpResponse<String> limited = post("/api/cases/decisions");
        assertThat(limited.statusCode()).isEqualTo(429);
        JsonNode p = assertProblem(limited.body(), 429, "RATE_LIMITED");
        assertThat(p.get("detail").asString()).isEqualTo(ErrorCode.RATE_LIMITED_CASES_DETAIL);

        assertThat(post("/api/cases/decisions;x=1").statusCode()).isEqualTo(429);
        assertThat(post("/api/cases/%64ecisions").statusCode()).isEqualTo(429);
    }
}
