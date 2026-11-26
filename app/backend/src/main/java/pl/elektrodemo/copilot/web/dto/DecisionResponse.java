package pl.elektrodemo.copilot.web.dto;

public record DecisionResponse(DecisionBody decision, CaseContextDto caseContext, String signature) {
}
