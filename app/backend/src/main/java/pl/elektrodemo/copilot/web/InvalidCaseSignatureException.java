package pl.elektrodemo.copilot.web;

public class InvalidCaseSignatureException extends ApiException {

    public InvalidCaseSignatureException() {
        super(ErrorCode.INVALID_CASE_SIGNATURE);
    }
}
