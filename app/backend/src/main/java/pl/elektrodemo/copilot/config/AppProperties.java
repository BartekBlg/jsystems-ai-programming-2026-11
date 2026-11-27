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
            @DefaultValue("60") int chatPerHour,
            @DefaultValue("30") int decisionsPerHour) {
    }

    public record Decisions(
            @DefaultValue("shadow") String mode,
            @DefaultValue("typesafe/jev-1.14") String model,
            @DefaultValue("https://openrouter.ai/api/alpha/decisions") String url,
            @DefaultValue("0.80") double confidenceThreshold,
            @DefaultValue("6s") Duration timeout) {
    }
}
