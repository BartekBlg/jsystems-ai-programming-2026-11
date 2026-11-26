package pl.elektrodemo.copilot.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * RFC 9457 problem+json with a stable {@code code} and a Polish {@code detail} (ADR-001 §6.3).
 * T03 covers the ADR-005 D3 404; T22 adds the remaining error codes.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Nie znaleziono zasobu.");
        problem.setTitle("Not Found");
        problem.setProperty("code", "NOT_FOUND");
        return handleExceptionInternal(ex, problem, headers, HttpStatus.NOT_FOUND, request);
    }
}
