package pl.elektrodemo.copilot.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.cases.*;
import tools.jackson.databind.json.JsonMapper;

class CaseContextDtoJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    private static RuleResult rules(String rule) {
        return new RuleResult(3, ReturnWindowStatus.WITHIN_14_DAYS, false,
                EnumSet.of(DecisionCategory.ESCALATED, DecisionCategory.ACCEPTED), "",
                EnumSet.of(DecisionCategory.REJECTED), "", rule);
    }

    private static CaseContextDto returnContext() {
        return new CaseContextDto(1, CaseType.RETURN,
                new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "Model X", LocalDate.of(2026, 11, 20), "Nie pasuje"),
                OffsetDateTime.parse("2026-11-26T10:15:30+01:00"),
                new ReturnImageAssessment(true, "", false, List.of(SignOfUse.DIRT), true, "Czysty laptop", Confidence.HIGH),
                AssessmentSource.LLM, null, null, rules("R3"), null);
    }

    private static CaseContextDto complaintContext() {
        return new CaseContextDto(1, CaseType.COMPLAINT,
                new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE, "Model Y", LocalDate.of(2026, 11, 20), "Pęknięty ekran"),
                OffsetDateTime.parse("2026-11-26T10:15:30+01:00"),
                new ComplaintImageAssessment(true, "", true, "pęknięcie", "ekran", ProbableCause.MECHANICAL_DAMAGE,
                        "Ślady uderzenia", "Pęknięty ekran", Confidence.MEDIUM),
                AssessmentSource.DECISIONS, null, null, rules("C1"),
                new DecisionRef(DecisionCategory.ESCALATED));
    }

    @Test
    void returnRoundTrip() throws Exception {
        CaseContextDto ctx = returnContext();
        CaseContextDto back = mapper.readValue(mapper.writeValueAsString(ctx), CaseContextDto.class);
        assertThat(back).isEqualTo(ctx);
        assertThat(back.assessment()).isInstanceOf(ReturnImageAssessment.class);
    }

    @Test
    void complaintRoundTrip() throws Exception {
        CaseContextDto ctx = complaintContext();
        CaseContextDto back = mapper.readValue(mapper.writeValueAsString(ctx), CaseContextDto.class);
        assertThat(back).isEqualTo(ctx);
        assertThat(back.assessment()).isInstanceOf(ComplaintImageAssessment.class);
    }

    @Test
    void nullableFieldsSerializedAsNull() throws Exception {
        String json = mapper.writeValueAsString(returnContext());
        assertThat(json).contains("\"prescreen\":null", "\"consistencySignals\":null", "\"decision\":null");
    }

    @Test
    void noSourceKeyAndRuleFactsName() throws Exception {
        String json = mapper.writeValueAsString(complaintContext());
        assertThat(json).doesNotContain("\"source\"");
        assertThat(json).contains("\"assessmentSource\":\"DECISIONS\"", "\"ruleFacts\":");
        // allowedCategories serialized in enum order
        assertThat(json).contains("\"allowedCategories\":[\"ACCEPTED\",\"ESCALATED\"]");
    }

    @Test
    void dates() throws Exception {
        String json = mapper.writeValueAsString(returnContext());
        assertThat(json).contains("\"purchaseDate\":\"2026-11-20\"");
        assertThat(json).contains("\"submittedAt\":\"2026-11-26T10:15:30+01:00\"");
    }
}
