package pl.elektrodemo.copilot.llm;

import pl.elektrodemo.copilot.cases.*;
import java.time.OffsetDateTime;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import tools.jackson.databind.JsonNode;

/** correctiveNote is nullable. */
public record DecisionPrompt(
        CaseType caseType,
        CaseFormDto form,
        OffsetDateTime submittedAt,
        ImageAssessment assessment,
        AssessmentSource assessmentSource,
        RuleResult ruleResult,
        String policyText,
        JsonNode prescreen,
        JsonNode consistencySignals,
        String correctiveNote) {
}
