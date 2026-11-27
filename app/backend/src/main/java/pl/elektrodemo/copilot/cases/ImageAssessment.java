package pl.elektrodemo.copilot.cases;

/** Concrete type is chosen by CaseContextDto.caseType, not by a JSON type property (ADR-005 D2). */
public sealed interface ImageAssessment permits ReturnImageAssessment, ComplaintImageAssessment {
    boolean photoSufficient();

    String insufficiencyReason();

    String visibleDescription();

    Confidence confidence();
}
