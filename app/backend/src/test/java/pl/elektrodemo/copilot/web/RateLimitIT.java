package pl.elektrodemo.copilot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.elektrodemo.copilot.support.ProblemAssertions.assertProblem;
import static pl.elektrodemo.copilot.web.ApiTestSupport.ASSESSMENTS;
import static pl.elektrodemo.copilot.web.ApiTestSupport.DECISIONS;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * T22 / ADR-001 §8 (Rate limit), ADR-000 Decision 8.6: per-IP limit on assessments, none on decisions.
 * Requests carry a GIF so they are counted by the filter but never reach the LLM (no OpenRouter involved).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitIT {

    private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0, 0, 0, 0, 0x3B};

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.rate-limit.cases-per-hour", () -> "2");
        r.add("app.rate-limit.chat-per-hour", () -> "1");
    }

    @LocalServerPort
    int port;

    private final ApiTestSupport api = ApiTestSupport.client();

    @Test
    void thirdAssessmentFromTheSameIpIsRateLimitedAndDecisionsAreNot() throws Exception {
        for (int i = 0; i < 2; i++) {
            assertThat(api.postAssessment(port, api.validForm(), "p.jpg", GIF).statusCode()).isEqualTo(400);
        }

        HttpResponse<String> limited = api.postAssessment(port, api.validForm(), "p.jpg", GIF);

        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.headers().firstValue("Content-Type")).hasValueSatisfying(ct -> assertThat(ct).startsWith("application/problem+json"));
        JsonNode p = assertProblem(limited.body(), 429, "RATE_LIMITED");
        assertThat(p.get("detail").asString()).isEqualTo(ErrorCode.RATE_LIMITED_CASES_DETAIL);
        assertThat(p.get("instance").asString()).isEqualTo(ASSESSMENTS);

        for (int i = 0; i < 5; i++) {
            assertThat(api.postJson(port, DECISIONS, "{}").statusCode()).isEqualTo(400);
        }
    }

    @Test
    void chatPathIsLimitedByItsOwnBucketBeforeTheControllerExists() throws Exception {
        // First call reaches routing (no controller until T23: 404 or 405 problem), the second is over the limit of 1.
        HttpResponse<String> first = api.postJson(port, "/api/chat/messages", "{}");
        assertThat(first.statusCode()).isNotEqualTo(429);

        HttpResponse<String> limited = api.postJson(port, "/api/chat/messages", "{}");

        assertThat(limited.statusCode()).isEqualTo(429);
        JsonNode p = assertProblem(limited.body(), 429, "RATE_LIMITED");
        assertThat(p.get("detail").asString()).isEqualTo(ErrorCode.RATE_LIMITED_CHAT_DETAIL);
    }
}
