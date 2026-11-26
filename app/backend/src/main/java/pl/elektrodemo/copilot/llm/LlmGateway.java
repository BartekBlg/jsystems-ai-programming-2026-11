package pl.elektrodemo.copilot.llm;

import pl.elektrodemo.copilot.cases.*;

/** ADR-002 §5. */
public interface LlmGateway {

    /** preliminarySignals is nullable; used for gated-UNCERTAIN only. */
    ImageAssessment assessImage(CaseType caseType, EquipmentCategory category, byte[] jpeg, String preliminarySignals);

    DecisionDraft decide(DecisionPrompt prompt);

    void streamChat(ChatPrompt prompt, ChatSink sink);
}
