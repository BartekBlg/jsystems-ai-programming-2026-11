package pl.elektrodemo.copilot.web;

public class RateLimitedException extends ApiException {

    private final String detail;

    public RateLimitedException(String detail) {
        super(ErrorCode.RATE_LIMITED);
        this.detail = detail;
    }

    public String detail() {
        return detail;
    }
}
