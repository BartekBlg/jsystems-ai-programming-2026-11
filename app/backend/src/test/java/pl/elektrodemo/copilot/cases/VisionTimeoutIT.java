package pl.elektrodemo.copilot.cases;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import pl.elektrodemo.copilot.llm.LlmProperties;

/** ADR-005 D10: llm.timeouts.vision is enforced in the running app (real HTTP stack, WireMock capture, 1 ms budget). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = { "llm.timeouts.vision=1ms", "app.decisions.mode=llm-only" })
class VisionTimeoutIT {

    private static final WireMockServer WM = ModesSupport.startWireMock();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("llm.base-url", WM::baseUrl);
        r.add("llm.api-key", () -> "sk-test-fake");
        r.add("app.rate-limit.cases-per-hour", () -> "1000");
    }

    @AfterAll
    static void stop() {
        WM.stop();
    }

    @LocalServerPort
    int port;

    @Autowired
    LlmProperties llm;

    @BeforeEach
    void reset() {
        WM.resetAll();
    }

    @Test
    void visionTimeoutBindsFromProperties() {
        assertThat(llm.timeouts().vision()).isEqualTo(Duration.ofMillis(1));
    }

    @Test
    void assessmentReturns504LlmTimeoutWhenVisionExceedsItsBudget() throws Exception {
        WM.stubFor(post(urlEqualTo("/responses"))
                .willReturn(ModesSupport.capture("responses", "assessment-return-ok").withFixedDelay(500)));
        String form = ModesSupport.formJson("RETURN", "Lenovo ThinkPad T14 Gen 4", ModesSupport.recentPurchase(), "");

        HttpResponse<String> response = ModesSupport.client().postAssessment(port, form, "laptop-2.webp",
                ModesSupport.image("laptop-2.webp"));

        assertThat(response.statusCode()).isEqualTo(504);
        assertThat(response.body()).contains("LLM_TIMEOUT");
    }
}
