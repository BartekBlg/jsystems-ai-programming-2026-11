package pl.elektrodemo.copilot.web;

public class UnsupportedImageException extends ApiException {

    public UnsupportedImageException() {
        super(ErrorCode.UNSUPPORTED_IMAGE);
    }
}
