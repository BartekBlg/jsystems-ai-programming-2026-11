package pl.elektrodemo.copilot.chat;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.web.dto.ChatMessageDto;
import pl.elektrodemo.copilot.web.dto.FieldErrorDto;

/**
 * ADR-001 4 (ChatRequest): 1-100 messages, first from the assistant (the decision message), last from the
 * user, user text 1-2000 chars after trimming, assistant text up to 20,000 chars, non-blank ids.
 * Returns the field errors; the controller turns a non-empty list into 400 VALIDATION_FAILED.
 */
@Component
public class ChatHistoryValidator {

    static final int MAX_MESSAGES = 100;
    static final int MAX_USER_TEXT = 2000;
    static final int MAX_ASSISTANT_TEXT = 20_000;
    static final String USER = "user";
    static final String ASSISTANT = "assistant";
    static final String HISTORY_INVALID = "Nieprawidłowa historia rozmowy.";
    static final String USER_TEXT_INVALID = "Wiadomość musi mieć od 1 do 2000 znaków.";

    public List<FieldErrorDto> validate(List<ChatMessageDto> messages) {
        List<FieldErrorDto> errors = new ArrayList<>();
        if (messages == null || messages.isEmpty() || messages.size() > MAX_MESSAGES) {
            errors.add(new FieldErrorDto("messages", HISTORY_INVALID));
            return errors;
        }
        boolean structureBroken = false;
        for (int i = 0; i < messages.size(); i++) {
            ChatMessageDto m = messages.get(i);
            if (m == null || m.id() == null || m.id().isBlank() || !isKnownRole(m.role())) {
                structureBroken = true;
                continue;
            }
            String field = "messages[" + i + "].text";
            String text = m.text();
            if (USER.equals(m.role())) {
                int length = text == null ? 0 : text.strip().length();
                if (length < 1 || length > MAX_USER_TEXT) {
                    errors.add(new FieldErrorDto(field, USER_TEXT_INVALID));
                }
            } else if (text == null || text.length() > MAX_ASSISTANT_TEXT) {
                errors.add(new FieldErrorDto(field, HISTORY_INVALID));
            }
        }
        ChatMessageDto first = messages.get(0);
        ChatMessageDto last = messages.get(messages.size() - 1);
        if (structureBroken || first == null || !ASSISTANT.equals(first.role()) || last == null
                || !USER.equals(last.role())) {
            errors.add(0, new FieldErrorDto("messages", HISTORY_INVALID));
        }
        return errors;
    }

    private static boolean isKnownRole(String role) {
        return USER.equals(role) || ASSISTANT.equals(role);
    }
}
