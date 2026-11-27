package pl.elektrodemo.copilot.web;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Formularz zawiera błędy."),
    UNSUPPORTED_IMAGE(HttpStatus.BAD_REQUEST, "Nieobsługiwany format. Dodaj zdjęcie JPEG, PNG lub WebP."),
    PHOTO_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Zdjęcie przekracza limit 10 MB."),
    INVALID_CASE_SIGNATURE(HttpStatus.UNAUTHORIZED, "Nie udało się odczytać zgłoszenia. Rozpocznij nowe zgłoszenie."),
    DECISION_ALREADY_MADE(HttpStatus.CONFLICT, "Decyzja dla tego zgłoszenia została już przygotowana."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Nie znaleziono zasobu."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED_CASES_DETAIL),
    LLM_FAILED(HttpStatus.BAD_GATEWAY, ErrorCode.DECISION_FAILED_DETAIL),
    LLM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, ErrorCode.DECISION_FAILED_DETAIL),
    INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.DECISION_FAILED_DETAIL);

    public static final String RATE_LIMITED_CASES_DETAIL = "Wysłano zbyt wiele zgłoszeń. Spróbuj ponownie później.";
    public static final String RATE_LIMITED_CHAT_DETAIL = "Wysłano zbyt wiele wiadomości. Spróbuj ponownie później.";
    static final String DECISION_FAILED_DETAIL = "Nie udało się przygotować decyzji. Spróbuj ponownie za chwilę.";

    private final HttpStatus status;
    private final String detail;

    ErrorCode(HttpStatus status, String detail) {
        this.status = status;
        this.detail = detail;
    }

    public HttpStatus status() {
        return status;
    }

    public String detail() {
        return detail;
    }
}
