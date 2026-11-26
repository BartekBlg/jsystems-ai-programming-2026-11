package pl.elektrodemo.copilot.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The single OpenAI SDK client pointed at OpenRouter (ADR-002 §3.1, §3.5). */
@Configuration
public class OpenRouterClientConfig {

    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(34);
    static final String REFERER = "http://localhost:8080";
    static final String TITLE = "ElektroDemo Service Copilot PoC";

    @Bean
    public OpenAIClient openAiClient(LlmProperties llm, @Value("${llm.allow-missing-key:false}") boolean allowMissingKey) {
        String key = llm.apiKey();
        if (key == null || key.isBlank()) {
            if (!allowMissingKey) {
                throw new IllegalStateException(
                        "OPENROUTER_API_KEY (llm.api-key) is not set: the application cannot call the LLM. "
                                + "Set the environment variable and restart.");
            }
            key = "missing-key-never-used";
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(key)
                .baseUrl(llm.baseUrl())
                .maxRetries(0)
                .timeout(DEFAULT_TIMEOUT)
                .putHeader("HTTP-Referer", REFERER)
                .putHeader("X-OpenRouter-Title", TITLE)
                .build();
    }
}
