package pl.elektrodemo.copilot.cases;

public record ComplaintImageAssessment(
        boolean photoSufficient,
        String insufficiencyReason,
        boolean defectVisible,
        String defectType,
        String defectLocation,
        ProbableCause probableCause,
        String causeReasoning,
        String visibleDescription,
        Confidence confidence) implements ImageAssessment {
}
