package pl.elektrodemo.copilot.security;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import pl.elektrodemo.copilot.cases.AssessmentSource;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.Confidence;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;
import pl.elektrodemo.copilot.cases.ReturnWindowStatus;
import pl.elektrodemo.copilot.cases.RuleResult;
import pl.elektrodemo.copilot.cases.SignOfUse;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.dto.DecisionRef;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/** Synthetic case context used by the security unit tests. No personal data. */
final class CaseContextFixtures {

    private CaseContextFixtures() {
    }

    static CaseContextDto returnCase() {
        return new CaseContextDto(
                1,
                CaseType.RETURN,
                new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "Model Testowy X1",
                        LocalDate.of(2026, 11, 1), ""),
                OffsetDateTime.parse("2026-11-10T10:15:30+01:00"),
                new ReturnImageAssessment(true, "", false, List.of(SignOfUse.SCRATCHES), true,
                        "Zarysowania na obudowie.", Confidence.HIGH),
                AssessmentSource.LLM,
                null,
                null,
                new RuleResult(9, ReturnWindowStatus.WITHIN_14_DAYS, false,
                        Set.of(DecisionCategory.ACCEPTED, DecisionCategory.ESCALATED),
                        null, Set.of(), null, "R6"),
                new DecisionRef(DecisionCategory.ACCEPTED));
    }

    /** Same content with a non-null prescreen tree, so nested JsonNode ordering is exercised too. */
    static CaseContextDto returnCaseWithPrescreen() {
        CaseContextDto base = returnCase();
        ObjectNode prescreen = JsonNodeFactory.instance.objectNode();
        prescreen.put("zeta", 1);
        prescreen.put("alpha", "a");
        prescreen.putObject("nested").put("b", true).put("a", false);
        return new CaseContextDto(base.contextVersion(), base.caseType(), base.form(), base.submittedAt(),
                base.assessment(), base.assessmentSource(), prescreen, null, base.ruleFacts(), base.decision());
    }

    /** Spring-like mapper used by the application for HTTP bodies (Jackson 3, auto-detected modules). */
    static JsonMapper springLikeMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}
