package pl.elektrodemo.copilot.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pl.elektrodemo.copilot.llm.LlmFailedException;
import pl.elektrodemo.copilot.llm.LlmTimeoutException;
import pl.elektrodemo.copilot.web.dto.FieldErrorDto;

/**
 * RFC 9457 problem+json with a stable {@code code} and a Polish {@code detail} (ADR-001 §6.3).
 * Bodies never carry exception messages, class names, provider text or stack traces (AC-43, TAC-05);
 * unexpected errors are logged server-side under an error id only.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String PHOTO_MISSING = "Dodaj zdjęcie urządzenia.";
    private static final String FORM_MISSING = "Wypełnij formularz zgłoszenia.";

    // ------------------------------------------------------------ our exceptions

    @ExceptionHandler(CaseController.RequestValidationException.class)
    ResponseEntity<Object> handleRequestValidation(CaseController.RequestValidationException ex, HttpServletRequest req) {
        return problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), req.getRequestURI(),
                ex.fieldErrors());
    }

    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<Object> handleRateLimited(RateLimitedException ex, HttpServletRequest req) {
        return problem(ex.code(), ex.detail(), req.getRequestURI(), null);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApiException(ApiException ex, HttpServletRequest req) {
        return problem(ex.code(), ex.code().detail(), req.getRequestURI(), null);
    }

    @ExceptionHandler(LlmFailedException.class)
    ResponseEntity<Object> handleLlmFailed(LlmFailedException ex, HttpServletRequest req) {
        logWithId(ex);
        return problem(ErrorCode.LLM_FAILED, ErrorCode.LLM_FAILED.detail(), req.getRequestURI(), null);
    }

    @ExceptionHandler(LlmTimeoutException.class)
    ResponseEntity<Object> handleLlmTimeout(LlmTimeoutException ex, HttpServletRequest req) {
        logWithId(ex);
        return problem(ErrorCode.LLM_TIMEOUT, ErrorCode.LLM_TIMEOUT.detail(), req.getRequestURI(), null);
    }

    /** Multipart parsing failures; a size violation Tomcat raised may arrive as a plain MultipartException. */
    @ExceptionHandler(MultipartException.class)
    ResponseEntity<Object> handleMultipart(MultipartException ex, HttpServletRequest req) {
        if (isSizeLimit(ex)) {
            return problem(ErrorCode.PHOTO_TOO_LARGE, ErrorCode.PHOTO_TOO_LARGE.detail(), req.getRequestURI(), null);
        }
        return problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), req.getRequestURI(), null);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<Object> handleBind(BindException ex, HttpServletRequest req) {
        return problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), req.getRequestURI(),
                fieldErrors(ex));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest req) {
        logWithId(ex);
        return problem(ErrorCode.INTERNAL, ErrorCode.INTERNAL.detail(), req.getRequestURI(), null);
    }

    // --------------------------------------------- framework exceptions (base class)

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), path(request), fieldErrors(ex));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDto> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                String field = error instanceof FieldError fe ? fe.getField() : parameter;
                errors.add(new FieldErrorDto(field, error.getDefaultMessage()));
            }
        });
        return problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), path(request), errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), path(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String part = ex.getRequestPartName();
        String message = "photo".equals(part) ? PHOTO_MISSING : FORM_MISSING;
        return problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(), path(request),
                List.of(new FieldErrorDto(part, message)));
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return problem(ErrorCode.PHOTO_TOO_LARGE, ErrorCode.PHOTO_TOO_LARGE.detail(), path(request), null);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        return problem(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.detail(), path(request), null);
    }

    /** Every remaining framework error (405, 415, ...): keep its status, drop its text. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null || status.is5xxServerError()) {
            logWithId(ex);
            return problem(ErrorCode.INTERNAL, ErrorCode.INTERNAL.detail(), path(request), null);
        }
        ResponseEntity<Object> response = problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.detail(),
                path(request), null);
        Map<String, Object> map = mapBody(response);
        map.put("status", status.value());
        map.put("title", status.getReasonPhrase());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status).headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (ex instanceof ErrorResponse er) {
            builder.headers(er.getHeaders());
        }
        return builder.body(map);
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapBody(ResponseEntity<Object> response) {
        return (Map<String, Object>) response.getBody();
    }

    private static ResponseEntity<Object> problem(ErrorCode code, String detail, String instance,
            List<FieldErrorDto> fieldErrors) {
        return ResponseEntity.status(code.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemJson.body(code, detail, instance, fieldErrors));
    }

    private static String path(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }

    private static List<FieldErrorDto> fieldErrors(BindException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDto(fe.getField(), fe.getDefaultMessage())).toList();
    }

    private static boolean isSizeLimit(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            String name = t.getClass().getName();
            if (t instanceof MaxUploadSizeExceededException || name.contains("SizeLimitExceeded")
                    || name.contains("SizeException")) {
                return true;
            }
        }
        return false;
    }

    private static void logWithId(Exception ex) {
        log.error("Request failed, errorId={}", UUID.randomUUID(), ex);
    }
}
