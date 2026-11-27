package pl.elektrodemo.copilot.llm;

import pl.elektrodemo.copilot.web.ErrorCode;

public interface ChatSink {

    void onDelta(String text);

    void onCompleted(TokenUsage usage);

    /** code is LLM_FAILED or LLM_TIMEOUT. */
    void onFailed(ErrorCode code);
}
