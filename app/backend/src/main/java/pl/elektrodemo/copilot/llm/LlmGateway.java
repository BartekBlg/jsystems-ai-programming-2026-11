package pl.elektrodemo.copilot.llm;

import java.time.Duration;
import pl.elektrodemo.copilot.cases.*;

/** ADR-002 §5. */
public interface LlmGateway {

    /** preliminarySignals is nullable; used for gated-UNCERTAIN only. */
    ImageAssessment assessImage(CaseType caseType, EquipmentCategory category, byte[] jpeg, String preliminarySignals);

    /**
     * Same as above with an explicit step budget (ADR-004 3.7: the vision step after pre-screening). The default
     * ignores the budget.
     */
    default ImageAssessment assessImage(CaseType caseType, EquipmentCategory category, byte[] jpeg,
            String preliminarySignals, Duration timeout) {
        return assessImage(caseType, category, jpeg, preliminarySignals);
    }

    DecisionDraft decide(DecisionPrompt prompt);

    void streamChat(ChatPrompt prompt, ChatSink sink);
}
