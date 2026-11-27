package pl.elektrodemo.copilot.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorCodeTest {

    @Test
    void statusPerCode() {
        assertThat(ErrorCode.VALIDATION_FAILED.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.UNSUPPORTED_IMAGE.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ErrorCode.PHOTO_TOO_LARGE.status()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(ErrorCode.INVALID_CASE_SIGNATURE.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ErrorCode.DECISION_ALREADY_MADE.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ErrorCode.NOT_FOUND.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ErrorCode.RATE_LIMITED.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(ErrorCode.LLM_FAILED.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(ErrorCode.LLM_TIMEOUT.status()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(ErrorCode.INTERNAL.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void polishDetails() {
        assertThat(ErrorCode.NOT_FOUND.detail()).isEqualTo("Nie znaleziono zasobu.");
        assertThat(ErrorCode.VALIDATION_FAILED.detail()).isEqualTo("Formularz zawiera błędy.");
        assertThat(ErrorCode.RATE_LIMITED.detail()).isEqualTo(ErrorCode.RATE_LIMITED_CASES_DETAIL);
        assertThat(ErrorCode.RATE_LIMITED_CHAT_DETAIL).isEqualTo("Wysłano zbyt wiele wiadomości. Spróbuj ponownie później.");
        assertThat(ErrorCode.INTERNAL.detail()).isEqualTo(ErrorCode.LLM_FAILED.detail());
        assertThat(ErrorCode.LLM_TIMEOUT.detail()).isEqualTo("Nie udało się przygotować decyzji. Spróbuj ponownie za chwilę.");
    }
}
