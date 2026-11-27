package pl.elektrodemo.copilot.chat;

import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.web.ErrorCode;

/**
 * Outbound chat events (ADR-001 5.3), independent of the transport. A stream ends with exactly one of
 * {@link #done} or {@link #error}; {@link #decision} comes at most once, before the first {@link #delta}.
 * Implementations may throw an unchecked exception when the client is gone; the service lets it propagate.
 */
public interface ChatEvents {

    void decision(DecisionCategory category);

    void delta(String text);

    void done(DecisionCategory category);

    /** code is LLM_FAILED or LLM_TIMEOUT. */
    void error(ErrorCode code);
}
