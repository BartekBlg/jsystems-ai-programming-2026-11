package pl.elektrodemo.copilot.prescreen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.ComplaintImageAssessment;
import pl.elektrodemo.copilot.cases.ImageAssessment;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;

@Component
public class ShadowComparator {

    private static final Logger LOGGER = LoggerFactory.getLogger("prescreen");
    private static final Pattern SAFE_MODEL = Pattern.compile("[A-Za-z0-9._/-]{1,100}");
    private final PrescreenAssessmentMapper mapper;

    public ShadowComparator(PrescreenAssessmentMapper mapper) {
        this.mapper = mapper;
    }

    public void compare(CaseType caseType, PrescreenResult result, ImageAssessment llmAssessment,
            long visionLlmLatencyMs) {
        if (llmAssessment == null) {
            LOGGER.warn("event=prescreen.shadow.skipped reason=LLM_ASSESSMENT_FAILED");
            return;
        }
        ImageAssessment decisionsAssessment = null;
        if (result != null && result.status() == PrescreenStatus.OK && result.gate() == GateStatus.CONFIDENT) {
            decisionsAssessment = mapper.map(caseType, result);
        }
        Map<String, Boolean> agreement = new LinkedHashMap<>();
        if (decisionsAssessment != null && llmAssessment != null) {
            agreement.put("photoSufficient", decisionsAssessment.photoSufficient() == llmAssessment.photoSufficient());
            agreement.put("confidenceBucket", decisionsAssessment.confidence() == llmAssessment.confidence());
            if (decisionsAssessment instanceof ReturnImageAssessment decisions
                    && llmAssessment instanceof ReturnImageAssessment llm) {
                agreement.put("damageVisible", decisions.damageVisible() == llm.damageVisible());
                agreement.put("signsOfUse", java.util.Set.copyOf(decisions.signsOfUse())
                        .equals(java.util.Set.copyOf(llm.signsOfUse())));
                agreement.put("resellableAsNew", decisions.resellableAsNew() == llm.resellableAsNew());
            }
            if (decisionsAssessment instanceof ComplaintImageAssessment decisions
                    && llmAssessment instanceof ComplaintImageAssessment llm) {
                agreement.put("damageVisible", decisions.defectVisible() == llm.defectVisible());
                agreement.put("probableCause", decisions.probableCause() == llm.probableCause());
            }
        }
        LOGGER.info("event=prescreen.shadow catalogVersion={} caseType={} gate={} reasons={} confidences={} agreement={} "
                        + "wouldSkipLlm={} decisionsLatencyMs={} visionLlmLatencyMs={} cost={} model={}",
                result == null ? PrescreenQuestionCatalog.CATALOG_VERSION : result.catalogVersion(), caseType,
                result == null ? GateStatus.UNCERTAIN : result.gate(),
                result == null ? java.util.List.of() : result.gateReasons(), confidences(result), agreement,
                result != null && result.status() == PrescreenStatus.OK && result.gate() == GateStatus.CONFIDENT,
                result == null ? 0 : result.latencyMs(), visionLlmLatencyMs, result == null ? 0 : result.cost(),
                safeModel(result == null ? null : result.model()));
    }

    public void gated(GateResult result, boolean visionLlmSkipped) {
        LOGGER.info("event=prescreen.gated gate={} reasons={} visionLlmSkipped={}",
                result == null ? GateStatus.UNCERTAIN : result.status(),
                result == null ? java.util.List.of() : result.reasons(), visionLlmSkipped);
    }

    private static Map<String, Double> confidences(PrescreenResult result) {
        Map<String, Double> values = new LinkedHashMap<>();
        if (result != null) {
            for (PrescreenAnswer answer : result.photoAnswers()) {
                values.put(answer.questionId(), answer.confidence());
            }
        }
        return values;
    }

    private static String safeModel(String model) {
        return model != null && SAFE_MODEL.matcher(model).matches() ? model : "unknown";
    }
}
