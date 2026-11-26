package pl.elektrodemo.copilot.llm;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("llm")
public record LlmProperties(
        String apiKey,
        @DefaultValue("https://openrouter.ai/api/v1") String baseUrl,
        @DefaultValue("openai/gpt-6-luna") String model,
        @DefaultValue("openai/gpt-6-luna") String visionModel,
        @DefaultValue("medium") String decisionReasoningEffort,
        @DefaultValue("low") String chatReasoningEffort,
        @DefaultValue Timeouts timeouts,
        @DefaultValue MaxOutputTokens maxOutputTokens) {

    public record Timeouts(
            @DefaultValue("26s") Duration vision,
            @DefaultValue("34s") Duration decision,
            @DefaultValue("30s") Duration chatFirstToken,
            @DefaultValue("90s") Duration chatTotal) {
    }

    public record MaxOutputTokens(
            @DefaultValue("4000") int vision,
            @DefaultValue("8000") int decision,
            @DefaultValue("4000") int chat) {
    }
}
