package pl.elektrodemo.copilot.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import pl.elektrodemo.copilot.cases.*;
import java.time.OffsetDateTime;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

/** ADR-005 D2. Nullable fields are serialized as JSON null, never omitted. */
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonDeserialize(using = CaseContextDto.Deserializer.class)
public record CaseContextDto(
        int contextVersion,
        CaseType caseType,
        CaseFormDto form,
        @JsonFormat(without = JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) OffsetDateTime submittedAt,
        ImageAssessment assessment,
        AssessmentSource assessmentSource,
        JsonNode prescreen,
        JsonNode consistencySignals,
        RuleResult ruleFacts,
        DecisionRef decision) {

    /** Same shape, but with the assessment kept as a tree until caseType is known. */
    record Raw(
            int contextVersion,
            CaseType caseType,
            CaseFormDto form,
            @JsonFormat(without = JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE) OffsetDateTime submittedAt,
            JsonNode assessment,
            AssessmentSource assessmentSource,
            JsonNode prescreen,
            JsonNode consistencySignals,
            RuleResult ruleFacts,
            DecisionRef decision) {
    }

    /** Picks the ImageAssessment record by caseType. */
    static final class Deserializer extends ValueDeserializer<CaseContextDto> {
        @Override
        public CaseContextDto deserialize(JsonParser p, DeserializationContext ctxt) {
            JsonNode tree = ctxt.readTree(p);
            Raw raw = ctxt.readTreeAsValue(tree, Raw.class);
            ImageAssessment assessment = null;
            if (raw.assessment() != null && !raw.assessment().isNull()) {
                Class<? extends ImageAssessment> type = raw.caseType() == CaseType.COMPLAINT
                        ? ComplaintImageAssessment.class
                        : ReturnImageAssessment.class;
                assessment = ctxt.readTreeAsValue(raw.assessment(), type);
            }
            return new CaseContextDto(raw.contextVersion(), raw.caseType(), raw.form(), raw.submittedAt(),
                    assessment, raw.assessmentSource(), nullIfNull(raw.prescreen()),
                    nullIfNull(raw.consistencySignals()), raw.ruleFacts(), raw.decision());
        }

        private static JsonNode nullIfNull(JsonNode n) {
            return n == null || n.isNull() ? null : n;
        }
    }
}
