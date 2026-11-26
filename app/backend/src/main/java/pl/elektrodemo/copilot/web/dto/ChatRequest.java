package pl.elektrodemo.copilot.web.dto;

import java.util.List;

public record ChatRequest(CaseContextDto caseContext, String signature, List<ChatMessageDto> messages) {
}
