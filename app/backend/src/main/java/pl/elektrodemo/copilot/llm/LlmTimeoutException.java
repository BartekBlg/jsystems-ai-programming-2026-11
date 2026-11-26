package pl.elektrodemo.copilot.llm;

/** Internal reason only; never shown to the customer. */
public class LlmTimeoutException extends RuntimeException {

    public LlmTimeoutException(String reason) {
        super(reason);
    }

    public LlmTimeoutException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
