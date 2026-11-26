package pl.elektrodemo.copilot.chat;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.RuleResult;

/**
 * Per-stream gate for the chat (ADR-002 3.6-3.7). The chat may announce an updated decision in its first
 * line ("Zaktualizowana decyzja: &lt;label&gt;"). The gate buffers until the first newline or 120 Java chars
 * (never ending the window inside a surrogate pair), decides once, and blocks any category that the rule
 * engine forbids for chat. Callers forward {@link #drainBuffered()} after a PASS_THROUGH or DECISION outcome
 * and must not forward anything after FORBIDDEN (they receive SUPPRESSED instead).
 */
public final class DecisionGate {

    private static final Logger log = LoggerFactory.getLogger(DecisionGate.class);
    static final String UPDATE_PREFIX = "Zaktualizowana decyzja:";
    static final int WINDOW = 120;

    private final RuleResult rules;
    private final StringBuilder buffer = new StringBuilder();
    private boolean decided;
    private boolean forbidden;

    public DecisionGate(RuleResult rules) {
        this.rules = rules;
    }

    public GateOutcome push(String delta) {
        if (decided) {
            return forbidden ? new GateOutcome.Suppressed() : new GateOutcome.Forward(delta);
        }
        buffer.append(delta);
        int lead = firstNonWhitespace();
        if (lead < 0) {
            // Only blank text so far: keep buffering, but never indefinitely.
            return buffer.length() > WINDOW ? decide("") : new GateOutcome.Buffering();
        }
        int newline = buffer.indexOf("\n", lead);
        if (newline >= 0) {
            return decide(buffer.substring(lead, newline));
        }
        int available = buffer.length() - lead;
        if (available > WINDOW) {
            return decide(buffer.substring(lead, lead + safeCut(lead, WINDOW)));
        }
        if (available == WINDOW && !Character.isHighSurrogate(buffer.charAt(buffer.length() - 1))) {
            return decide(buffer.substring(lead));
        }
        return new GateOutcome.Buffering();
    }

    private int firstNonWhitespace() {
        for (int i = 0; i < buffer.length(); i++) {
            if (!Character.isWhitespace(buffer.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Ends the stream: decides on whatever is buffered if no decision was taken yet. */
    public GateOutcome finish() {
        if (decided) {
            return forbidden ? new GateOutcome.Suppressed() : new GateOutcome.Forward("");
        }
        return decide(buffer.toString().stripLeading());
    }

    /** Returns and clears the text buffered before the decision, so the caller can forward it. */
    public String drainBuffered() {
        String text = buffer.toString();
        buffer.setLength(0);
        return text;
    }

    private int safeCut(int lead, int length) {
        return Character.isHighSurrogate(buffer.charAt(lead + length - 1)) ? length - 1 : length;
    }

    private GateOutcome decide(String head) {
        decided = true;
        if (!head.startsWith(UPDATE_PREFIX)) {
            return new GateOutcome.PassThrough();
        }
        String label = head.substring(UPDATE_PREFIX.length()).strip().replaceAll("\\p{Punct}+$", "").strip();
        Optional<DecisionCategory> category = DecisionCategory.fromPolishLabel(label);
        if (category.isEmpty()) {
            log.warn("GATE_UNKNOWN_LABEL");
            return new GateOutcome.PassThrough();
        }
        if (rules.chatForbiddenCategories().contains(category.get())) {
            forbidden = true;
            return new GateOutcome.Forbidden(category.get());
        }
        return new GateOutcome.Decision(category.get());
    }

    public sealed interface GateOutcome {

        /** Not decided yet; keep pushing. */
        record Buffering() implements GateOutcome {
        }

        /** Decided that the buffered text is ordinary content; forward drainBuffered(). */
        record PassThrough() implements GateOutcome {
        }

        /** Decided a permitted category; forward drainBuffered(), then every later delta. */
        record Decision(DecisionCategory category) implements GateOutcome {
        }

        /** Decided a category forbidden in chat; nothing may be forwarded. */
        record Forbidden(DecisionCategory category) implements GateOutcome {
        }

        /** Already decided (pass-through or decision): forward this text. */
        record Forward(String text) implements GateOutcome {
        }

        /** Already decided as forbidden: drop this text. */
        record Suppressed() implements GateOutcome {
        }
    }
}
