package pl.elektrodemo.copilot.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.security.RateLimiter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Per-IP hourly limit on the expensive POST endpoints (ADR-001 §8, ADR-000 Decision 8.6). The path is matched in its
 * normalized form (percent-decoded, path parameters removed, duplicate slashes collapsed), so spellings such as
 * {@code /api/cases/assessments;x=1} or {@code /api/cases/%61ssessments} hit the same bucket as the canonical path.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final String ASSESSMENTS_PATH = "/api/cases/assessments";
    static final String DECISIONS_PATH = "/api/cases/decisions";
    static final String CHAT_PATH = "/api/chat/messages";

    private static final UrlPathHelper PATHS = new UrlPathHelper();

    static {
        PATHS.setRemoveSemicolonContent(true);
        PATHS.setUrlDecode(true);
    }

    private final RateLimiter rateLimiter;
    private final AppProperties.RateLimit limits;
    private final JsonMapper mapper = JsonMapper.builder().build();

    public RateLimitFilter(RateLimiter rateLimiter, AppProperties properties) {
        this.rateLimiter = rateLimiter;
        this.limits = properties.rateLimit();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || bucket(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String bucket = bucket(request);
        boolean chat = "chat".equals(bucket);
        int limit = switch (bucket) {
            case "cases" -> limits.casesPerHour();
            case "decisions" -> limits.decisionsPerHour();
            default -> limits.chatPerHour();
        };
        if (rateLimiter.tryAcquire(bucket, ClientIp.of(request), limit)) {
            chain.doFilter(request, response);
            return;
        }
        String detail = chat ? ErrorCode.RATE_LIMITED_CHAT_DETAIL : ErrorCode.RATE_LIMITED_CASES_DETAIL;
        Map<String, Object> body = ProblemJson.body(ErrorCode.RATE_LIMITED, detail, path(request), null);
        response.setStatus(ErrorCode.RATE_LIMITED.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(mapper.writeValueAsBytes(body));
    }

    private static String bucket(HttpServletRequest request) {
        return switch (path(request)) {
            case ASSESSMENTS_PATH -> "cases";
            case DECISIONS_PATH -> "decisions";
            case CHAT_PATH -> "chat";
            default -> null;
        };
    }

    private static String path(HttpServletRequest request) {
        String path = PATHS.getPathWithinApplication(request);
        String normalized = path.replaceAll("/{2,}", "/");
        return normalized.length() > 1 && normalized.endsWith("/")
                ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
