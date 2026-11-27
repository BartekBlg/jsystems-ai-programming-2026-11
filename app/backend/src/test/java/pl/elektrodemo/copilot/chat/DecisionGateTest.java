package pl.elektrodemo.copilot.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.ReturnWindowStatus;
import pl.elektrodemo.copilot.cases.RuleResult;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome.Buffering;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome.Decision;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome.Forbidden;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome.Forward;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome.PassThrough;
import pl.elektrodemo.copilot.chat.DecisionGate.GateOutcome.Suppressed;

class DecisionGateTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String UPDATE_LINE = "Zaktualizowana decyzja: Przekazane do specjalisty\n";

    private static RuleResult rulesForbidding(DecisionCategory... forbidden) {
        return new RuleResult(5, ReturnWindowStatus.WITHIN_14_DAYS, false,
                EnumSet.of(DecisionCategory.ESCALATED), null,
                forbidden.length == 0 ? Set.of() : EnumSet.copyOf(List.of(forbidden)), "§5", "TEST");
    }

    private static DecisionGate gate() {
        return new DecisionGate(rulesForbidding(DecisionCategory.REJECTED));
    }

    @Test
    void wholeLineInOneDeltaDecidesImmediately() {
        DecisionGate gate = gate();

        GateOutcome outcome = gate.push(UPDATE_LINE);

        assertThat(outcome).isEqualTo(new Decision(DecisionCategory.ESCALATED));
        assertThat(gate.drainBuffered()).isEqualTo(UPDATE_LINE);
    }

    @Test
    void splitAtEveryCharBoundaryDecidesOnlyAfterNewline() {
        for (int i = 0; i <= UPDATE_LINE.length(); i++) {
            DecisionGate gate = gate();
            String head = UPDATE_LINE.substring(0, i);
            String tail = UPDATE_LINE.substring(i);

            GateOutcome first = gate.push(head);
            if (head.contains("\n")) {
                assertThat(first).as("split %d", i).isEqualTo(new Decision(DecisionCategory.ESCALATED));
            } else {
                assertThat(first).as("split %d", i).isEqualTo(new Buffering());
                assertThat(gate.push(tail)).as("split %d", i).isEqualTo(new Decision(DecisionCategory.ESCALATED));
            }
            assertThat(gate.drainBuffered()).as("split %d", i).isEqualTo(UPDATE_LINE);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"\nZaktualizowana decyzja: Przyjęte\nTreść.", "  \r\n\nZaktualizowana decyzja: Przyjęte\nTreść.",
            "   Zaktualizowana decyzja: Przyjęte\nTreść."})
    void leadingBlankLinesDoNotBypassTheGate(String text) {
        for (int i = 0; i <= text.length(); i++) {
            DecisionGate gate = new DecisionGate(rulesForbidding(DecisionCategory.ACCEPTED));
            GateOutcome outcome = new Buffering();
            for (String part : new String[] {text.substring(0, i), text.substring(i)}) {
                if (part.isEmpty()) {
                    continue;
                }
                if (outcome instanceof Buffering) {
                    outcome = gate.push(part);
                } else {
                    assertThat(gate.push(part)).isEqualTo(new Suppressed());
                }
            }
            if (outcome instanceof Buffering) {
                outcome = gate.finish();
            }
            assertThat(outcome).as("split at %d", i).isEqualTo(new Forbidden(DecisionCategory.ACCEPTED));
        }
    }

    @Test
    void leadingBlankLinesAreStillForwardedForAnAllowedUpdate() {
        DecisionGate gate = gate();

        GateOutcome outcome = gate.push("\n \n" + UPDATE_LINE);

        assertThat(outcome).isEqualTo(new Decision(DecisionCategory.ESCALATED));
        assertThat(gate.drainBuffered()).isEqualTo("\n \n" + UPDATE_LINE);
    }

    @Test
    void windowCountsFromTheFirstNonWhitespaceChar() {
        DecisionGate gate = gate();

        assertThat(gate.push("\n\n   ")).isEqualTo(new Buffering());
        assertThat(gate.push("x".repeat(DecisionGate.WINDOW - 1))).isEqualTo(new Buffering());
        assertThat(gate.push("x")).isEqualTo(new PassThrough());
    }

    @Test
    void endlessWhitespaceEventuallyPassesThrough() {
        DecisionGate gate = gate();

        assertThat(gate.push(" ".repeat(DecisionGate.WINDOW + 1))).isEqualTo(new PassThrough());
    }

    @Test
    void forbiddenLabelIsReportedAndLaterDeltasAreSuppressed() {
        DecisionGate gate = gate();

        assertThat(gate.push("Zaktualizowana decyzja: Odrzucone\n")).isEqualTo(new Forbidden(DecisionCategory.REJECTED));
        assertThat(gate.push("Dalsza treść")).isEqualTo(new Suppressed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"**Zaktualizowana decyzja: Odrzucone**\n", "Zaktualizowana decyzja: Odrzucone (bo dostawa minęła)\n",
            "Zaktualizowana decyzja: *Odrzucone*.\n", "Zaktualizowana decyzja: \"Odrzucone\" - z powodu X\n",
            "__Zaktualizowana decyzja:__ `odrzucone`\n", "Zaktualizowana decyzja: ODRZUCONE, ponieważ...\n"})
    void decoratedForbiddenLabelsAreStillForbidden(String line) {
        for (int i = 0; i <= line.length(); i++) {
            DecisionGate gate = gate();
            GateOutcome outcome = new Buffering();
            for (String part : new String[] {line.substring(0, i), line.substring(i)}) {
                if (part.isEmpty()) {
                    continue;
                }
                outcome = outcome instanceof Buffering ? gate.push(part) : outcome;
            }
            if (outcome instanceof Buffering) {
                outcome = gate.finish();
            }
            assertThat(outcome).as("split at %d of %s", i, line).isEqualTo(new Forbidden(DecisionCategory.REJECTED));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"**Zaktualizowana decyzja: Przyjęte**\n", "Zaktualizowana decyzja: Przyjęte (bo dostawa…)\n",
            "Zaktualizowana decyzja: *Przekazane do specjalisty*.\n"})
    void decoratedAllowedLabelsAreDecisions(String line) {
        DecisionCategory expected = line.contains("specjalisty") ? DecisionCategory.ESCALATED : DecisionCategory.ACCEPTED;

        assertThat(gate().push(line)).isEqualTo(new Decision(expected));
    }

    @Test
    void unknownDecoratedLabelStillPassesThrough() {
        assertThat(gate().push("**Zaktualizowana decyzja: Coś innego**\n")).isEqualTo(new PassThrough());
    }

    @Test
    void trailingPunctuationIsStripped() {
        DecisionGate gate = gate();

        assertThat(gate.push("Zaktualizowana decyzja: Przyjęte.\n")).isEqualTo(new Decision(DecisionCategory.ACCEPTED));
    }

    @Test
    void unknownLabelPassesThroughWithoutDecision() {
        DecisionGate gate = gate();
        String line = "Zaktualizowana decyzja: Nieznana kategoria\n";

        assertThat(gate.push(line)).isEqualTo(new PassThrough());
        assertThat(gate.drainBuffered()).isEqualTo(line);
        assertThat(gate.push("Dalej")).isEqualTo(new Forward("Dalej"));
    }

    @Test
    void textWithoutUpdatePrefixPassesThrough() {
        DecisionGate gate = gate();

        assertThat(gate.push("Dzień dobry\n")).isEqualTo(new PassThrough());
        assertThat(gate.drainBuffered()).isEqualTo("Dzień dobry\n");
    }

    @Test
    void textWithoutNewlineDecidesAtOneHundredTwentyChars() {
        DecisionGate gate = gate();
        String text = "x".repeat(130);

        assertThat(gate.push(text.substring(0, 119))).isEqualTo(new Buffering());
        assertThat(gate.push(text.substring(119))).isEqualTo(new PassThrough());
        assertThat(gate.drainBuffered()).isEqualTo(text);
    }

    @Test
    void emojiStraddlingTheBoundaryIsNotSplit() {
        DecisionGate gate = gate();
        String emoji = "😀";

        assertThat(gate.push("a".repeat(119))).isEqualTo(new Buffering());
        assertThat(gate.push(emoji.substring(0, 1))).isEqualTo(new Buffering());
        assertThat(gate.push(emoji.substring(1))).isEqualTo(new PassThrough());
        assertThat(gate.drainBuffered()).isEqualTo("a".repeat(119) + emoji);
    }

    @Test
    void emojiOnOneDeltaAcrossBoundaryDecidesWithWholePair() {
        DecisionGate gate = gate();

        assertThat(gate.push("a".repeat(119))).isEqualTo(new Buffering());
        assertThat(gate.push("😀")).isEqualTo(new PassThrough());
        assertThat(gate.drainBuffered()).isEqualTo("a".repeat(119) + "😀");
    }

    @Test
    void finishDecidesOnBufferedTextWithoutNewline() {
        DecisionGate gate = gate();

        assertThat(gate.push("Zaktualizowana decyzja: Odrzucone")).isEqualTo(new Buffering());
        assertThat(gate.finish()).isEqualTo(new Forbidden(DecisionCategory.REJECTED));
    }

    @Test
    void finishOnEmptyStreamPassesThrough() {
        DecisionGate gate = gate();

        assertThat(gate.finish()).isEqualTo(new PassThrough());
        assertThat(gate.drainBuffered()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Zaktualizowana decyzja: Przekazane do specjalisty", "Zaktualizowana decyzja:Przyjęte"})
    void finishDecidesValidLabelWithoutNewline(String text) {
        DecisionGate gate = gate();

        assertThat(gate.push(text)).isEqualTo(new Buffering());
        assertThat(gate.finish()).isInstanceOf(Decision.class);
    }

    @Test
    void recordedChatCaptureIsDecidedOnFirstLine() {
        List<String> deltas = chatDeltas("chat-decision-update");
        DecisionGate gate = gate();
        StringBuilder forwarded = new StringBuilder();
        GateOutcome decided = null;

        for (String delta : deltas) {
            GateOutcome outcome = gate.push(delta);
            if (decided == null && !(outcome instanceof Buffering)) {
                decided = outcome;
                forwarded.append(gate.drainBuffered());
            } else if (decided != null) {
                forwarded.append(((Forward) outcome).text());
            }
        }

        assertThat(decided).isEqualTo(new Decision(DecisionCategory.ESCALATED));
        assertThat(forwarded.toString()).isEqualTo(String.join("", deltas));
    }

    private static List<String> chatDeltas(String name) {
        List<String> deltas = new ArrayList<>();
        try (InputStream in = DecisionGateTest.class.getClassLoader()
                .getResourceAsStream("captures/responses/" + name + ".sse")) {
            assertThat(in).as("capture %s", name).isNotNull();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String payload = line.startsWith("data:") ? line.substring("data:".length()).trim() : "";
                if (!payload.isEmpty() && !"[DONE]".equals(payload)) {
                    JsonNode event = JSON.readTree(payload);
                    if ("response.output_text.delta".equals(event.path("type").asString())) {
                        deltas.add(event.get("delta").asString());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return deltas;
    }
}
