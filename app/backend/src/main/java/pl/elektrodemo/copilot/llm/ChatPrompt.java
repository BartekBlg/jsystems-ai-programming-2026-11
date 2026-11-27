package pl.elektrodemo.copilot.llm;

import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.web.dto.ChatMessageDto;
import java.util.List;

/** correctiveNote is nullable. */
public record ChatPrompt(
        DecisionPrompt decisionPrompt,
        DecisionCategory currentCategory,
        List<ChatMessageDto> history,
        String correctiveNote) {
}
