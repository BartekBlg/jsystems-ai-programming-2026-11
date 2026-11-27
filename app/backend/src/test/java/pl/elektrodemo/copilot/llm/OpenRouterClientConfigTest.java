package pl.elektrodemo.copilot.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** T20: client bean wiring (ADR-002 §3.1) - key policy, auth header, attribution headers, no SDK retries. */
class OpenRouterClientConfigTest {

    private static LlmProperties props(String key, String baseUrl) {
        return new LlmProperties(key, baseUrl, "m", "v", "medium", "low",
                new LlmProperties.Timeouts(Duration.ofSeconds(26), Duration.ofSeconds(34), Duration.ofSeconds(30),
                        Duration.ofSeconds(90)),
                new LlmProperties.MaxOutputTokens(4000, 8000, 4000));
    }

    @Test
    void blankKeyFailsStartupWithClearMessage() {
        assertThatThrownBy(() -> new OpenRouterClientConfig().openAiClient(props("  ", "http://localhost:1"), false))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("OPENROUTER_API_KEY");
        assertThatThrownBy(() -> new OpenRouterClientConfig().openAiClient(props(null, "http://localhost:1"), false))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankKeyAllowedOnlyWithTestEscape() {
        assertThat(new OpenRouterClientConfig().openAiClient(props("", "http://localhost:1"), true)).isNotNull();
    }

    @Test
    void sendsBearerKeyAttributionHeadersAndDoesNotRetry() {
        WireMockServer wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        try {
            wm.stubFor(post(urlEqualTo("/responses")).willReturn(aResponse().withStatus(503)));
            OpenAIClient client = new OpenRouterClientConfig().openAiClient(props("sk-test-fake", wm.baseUrl()), false);
            assertThatThrownBy(() -> client.responses().create(ResponseCreateParams.builder().model("m").input("x").build()))
                    .isInstanceOf(RuntimeException.class);
            assertThat(wm.getAllServeEvents()).hasSize(1);
            wm.verify(postRequestedFor(urlEqualTo("/responses"))
                    .withHeader("Authorization", equalTo("Bearer sk-test-fake"))
                    .withHeader("HTTP-Referer", equalTo("http://localhost:8080"))
                    .withHeader("X-OpenRouter-Title", equalTo("ElektroDemo Service Copilot PoC")));
        } finally {
            wm.stop();
        }
    }
}
