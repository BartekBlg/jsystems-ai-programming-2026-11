package pl.elektrodemo.copilot.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app")
public record AppProperties(
        String caseSigningKey,
        @DefaultValue RateLimit rateLimit,
        @DefaultValue Decisions decisions) {

    public record RateLimit(
            @DefaultValue("10") int casesPerHour,
            @DefaultValue("60") int chatPerHour) {
    }

    public record Decisions(
            @DefaultValue("shadow") String mode,
            @DefaultValue("openai/gpt-6-luna-decisions") String model,
            @DefaultValue("https://openrouter.ai/api/alpha/decisions") String url,
            @DefaultValue("0.80") double confidenceThreshold,
            @DefaultValue("6s") Duration timeout) {
    }
}
