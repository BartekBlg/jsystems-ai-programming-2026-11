package pl.elektrodemo.copilot.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** T50c-fix: the SSE emitter must outlive the chat total budget so the terminal event is never lost. */
class ChatEmitterTimeoutTest {

    @Test
    void emitterTimeoutExceedsTheChatTotalBudgetByFiveSeconds() {
        assertThat(ChatController.emitterTimeoutMs(Duration.ofSeconds(90))).isEqualTo(95_000L);
        assertThat(ChatController.emitterTimeoutMs(Duration.ofSeconds(10))).isEqualTo(15_000L);
    }
}
