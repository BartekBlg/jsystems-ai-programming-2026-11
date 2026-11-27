package pl.elektrodemo.copilot.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import pl.elektrodemo.copilot.web.dto.FieldErrorDto;

/** The single place that names the members of an RFC 9457 error body (ADR-001 §6.3); used by the advice and the filter. */
final class ProblemJson {

    private ProblemJson() {
    }

    static Map<String, Object> body(ErrorCode code, String detail, String instance, List<FieldErrorDto> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", code.status().getReasonPhrase());
        body.put("status", code.status().value());
        body.put("detail", detail);
        body.put("code", code.name());
        body.put("instance", instance);
        if (fieldErrors != null) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }
}
