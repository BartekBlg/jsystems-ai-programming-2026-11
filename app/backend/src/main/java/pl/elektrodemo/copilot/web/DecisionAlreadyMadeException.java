package pl.elektrodemo.copilot.web;

public class DecisionAlreadyMadeException extends ApiException {

    public DecisionAlreadyMadeException() {
        super(ErrorCode.DECISION_ALREADY_MADE);
    }
}
