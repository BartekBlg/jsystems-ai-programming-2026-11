package pl.elektrodemo.copilot.prescreen;

public class PrescreenUnavailableException extends RuntimeException {

    public PrescreenUnavailableException(String reason) {
        super(reason);
    }

    public PrescreenUnavailableException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
