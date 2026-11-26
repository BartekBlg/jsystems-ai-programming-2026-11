package pl.elektrodemo.copilot.llm;

/** Internal reason only; never shown to the customer. */
public class LlmFailedException extends RuntimeException {

    public LlmFailedException(String reason) {
        super(reason);
    }

    public LlmFailedException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
