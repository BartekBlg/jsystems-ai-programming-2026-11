package pl.elektrodemo.copilot.web;

/** Base for errors mapped to problem+json; carries no message so nothing internal leaks. */
public class ApiException extends RuntimeException {

    private final ErrorCode code;

    public ApiException(ErrorCode code) {
        super(code.name(), null, false, false);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
