package pl.elektrodemo.copilot.prescreen;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DecisionsResponse(
        String id,
        String model,
        String provider,
        Map<String, DecisionAnswer> answers,
        Usage usage) {

    public PrescreenAnswer normalize(String questionId) {
        return PrescreenAnswer.from(questionId, answers == null ? null : answers.get(questionId));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DecisionAnswer(
            String type,
            Double noul,
            String choice,
            Double score,
            Map<String, Double> probabilities,
            Double confidence,
            Map<String, String> legend) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            Double cost,
            @JsonProperty("cost_details")
            CostDetails costDetails) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CostDetails(
            @JsonProperty("upstream_inference_cost")
            Double upstreamInferenceCost) {
    }
}
