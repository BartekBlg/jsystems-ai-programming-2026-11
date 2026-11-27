package pl.elektrodemo.copilot.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.web.dto.ChatMessageDto;
import pl.elektrodemo.copilot.web.dto.FieldErrorDto;

/** ADR-001 4 (ChatRequest) boundaries; texts here are synthetic filler, not LLM replies. */
class ChatHistoryValidatorTest {

    private static final String HISTORY = "Nieprawidłowa historia rozmowy.";
    private static final String USER_TEXT = "Wiadomość musi mieć od 1 do 2000 znaków.";

    private final ChatHistoryValidator validator = new ChatHistoryValidator();

    private static ChatMessageDto assistant(int i, String text) {
        return new ChatMessageDto("m" + i, "assistant", text);
    }

    private static ChatMessageDto user(int i, String text) {
        return new ChatMessageDto("m" + i, "user", text);
    }

    /** assistant, user, assistant, user, ... ending with a user message; size must be even. */
    private static List<ChatMessageDto> alternating(int size) {
        List<ChatMessageDto> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(i % 2 == 0 ? assistant(i, "a") : user(i, "u"));
        }
        return list;
    }

    private static List<ChatMessageDto> single(ChatMessageDto userMessage) {
        return List.of(assistant(0, "decyzja"), userMessage);
    }

    @Test
    void minimalValidHistoryPasses() {
        assertThat(validator.validate(alternating(2))).isEmpty();
    }

    @Test
    void oneMessageOnlyIsRejectedBecauseLastMustBeUserAndFirstAssistant() {
        assertThat(validator.validate(List.of(user(0, "u")))).extracting(FieldErrorDto::field).contains("messages");
    }

    @Test
    void emptyAndNullHistoryAreRejected() {
        assertThat(validator.validate(List.of())).containsExactly(new FieldErrorDto("messages", HISTORY));
        assertThat(validator.validate(null)).containsExactly(new FieldErrorDto("messages", HISTORY));
    }

    @Test
    void hundredMessagesPassAndHundredAndOneAreRejected() {
        assertThat(validator.validate(alternating(100))).isEmpty();
        List<ChatMessageDto> tooMany = alternating(101);
        assertThat(validator.validate(tooMany)).containsExactly(new FieldErrorDto("messages", HISTORY));
    }

    @Test
    void userTextBoundariesAreOneToTwoThousandAfterTrimming() {
        assertThat(validator.validate(single(user(1, "x")))).isEmpty();
        assertThat(validator.validate(single(user(1, "x".repeat(2000))))).isEmpty();
        assertThat(validator.validate(single(user(1, "  " + "x".repeat(2000) + "  ")))).isEmpty();
        assertThat(validator.validate(single(user(1, "x".repeat(2001)))))
                .containsExactly(new FieldErrorDto("messages[1].text", USER_TEXT));
        assertThat(validator.validate(single(user(1, ""))))
                .containsExactly(new FieldErrorDto("messages[1].text", USER_TEXT));
        assertThat(validator.validate(single(user(1, "   \n\t "))))
                .containsExactly(new FieldErrorDto("messages[1].text", USER_TEXT));
        assertThat(validator.validate(single(user(1, null))))
                .containsExactly(new FieldErrorDto("messages[1].text", USER_TEXT));
    }

    @Test
    void firstMessageMustBeFromTheAssistant() {
        List<ChatMessageDto> messages = List.of(user(0, "u"), assistant(1, "a"), user(2, "u"));
        assertThat(validator.validate(messages)).containsExactly(new FieldErrorDto("messages", HISTORY));
    }

    @Test
    void lastMessageMustBeFromTheUser() {
        List<ChatMessageDto> messages = List.of(assistant(0, "a"), user(1, "u"), assistant(2, "a"));
        assertThat(validator.validate(messages)).containsExactly(new FieldErrorDto("messages", HISTORY));
    }

    @Test
    void assistantTextBoundaryIsTwentyThousandChars() {
        assertThat(validator.validate(List.of(assistant(0, "a".repeat(20_000)), user(1, "u")))).isEmpty();
        assertThat(validator.validate(List.of(assistant(0, "a".repeat(20_001)), user(1, "u"))))
                .containsExactly(new FieldErrorDto("messages[0].text", HISTORY));
    }

    @Test
    void unknownRoleAndBlankIdAreRejected() {
        List<ChatMessageDto> badRole = List.of(assistant(0, "a"), new ChatMessageDto("m1", "system", "x"), user(2, "u"));
        List<ChatMessageDto> blankId = List.of(assistant(0, "a"), new ChatMessageDto(" ", "user", "u"));
        List<ChatMessageDto> nullId = List.of(assistant(0, "a"), new ChatMessageDto(null, "user", "u"));
        assertThat(validator.validate(badRole)).containsExactly(new FieldErrorDto("messages", HISTORY));
        assertThat(validator.validate(blankId)).containsExactly(new FieldErrorDto("messages", HISTORY));
        assertThat(validator.validate(nullId)).containsExactly(new FieldErrorDto("messages", HISTORY));
    }

    @Test
    void nullElementIsRejectedWithoutThrowing() {
        List<ChatMessageDto> messages = new ArrayList<>(alternating(2));
        messages.add(1, null);
        assertThat(validator.validate(messages)).containsExactly(new FieldErrorDto("messages", HISTORY));
    }
}
