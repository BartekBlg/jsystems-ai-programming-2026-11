package pl.elektrodemo.copilot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static pl.elektrodemo.copilot.support.ProblemAssertions.assertProblem;
import static pl.elektrodemo.copilot.web.ApiTestSupport.ASSESSMENTS;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import pl.elektrodemo.copilot.cases.AssessmentService;
import tools.jackson.databind.JsonNode;

/** T22 / AC-43, TAC-05: an unexpected exception becomes a generic 500 INTERNAL with nothing leaked. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiExceptionHandlerIT {

    @MockitoBean
    AssessmentService assessmentService;

    @LocalServerPort
    int port;

    @Test
    void unexpectedExceptionIsInternalWithoutMessageLeak() throws Exception {
        when(assessmentService.assess(any(), any()))
                .thenThrow(new IllegalStateException("secret internal state sk-or-v1-abc at pl.elektrodemo.Foo"));
        ApiTestSupport api = ApiTestSupport.client();

        HttpResponse<String> r = api.postAssessment(port, api.validForm(), "laptop-1.png",
                ApiTestSupport.image("laptop-1.png"));

        assertThat(r.statusCode()).isEqualTo(500);
        assertThat(r.headers().firstValue("Content-Type")).hasValueSatisfying(ct -> assertThat(ct).startsWith("application/problem+json"));
        JsonNode p = assertProblem(r.body(), 500, "INTERNAL");
        assertThat(p.get("detail").asString()).isEqualTo(ErrorCode.INTERNAL.detail());
        assertThat(p.get("instance").asString()).isEqualTo(ASSESSMENTS);
        assertThat(r.body()).doesNotContain("secret").doesNotContain("IllegalState");
    }
}
