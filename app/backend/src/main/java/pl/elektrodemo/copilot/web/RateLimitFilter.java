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
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.security.RateLimiter;
import tools.jackson.databind.json.JsonMapper;

/** Per-IP hourly limit on the two expensive POST endpoints (ADR-001 §8, ADR-000 Decision 8.6). Decisions are not limited. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    static final String ASSESSMENTS_PATH = "/api/cases/assessments";
    static final String CHAT_PATH = "/api/chat/messages";

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
        boolean cases = ASSESSMENTS_PATH.equals(path(request));
        int limit = cases ? limits.casesPerHour() : limits.chatPerHour();
        if (rateLimiter.tryAcquire(bucket(request), ClientIp.of(request), limit)) {
            chain.doFilter(request, response);
            return;
        }
        String detail = cases ? ErrorCode.RATE_LIMITED_CASES_DETAIL : ErrorCode.RATE_LIMITED_CHAT_DETAIL;
        Map<String, Object> body = ProblemJson.body(ErrorCode.RATE_LIMITED, detail, path(request), null);
        response.setStatus(ErrorCode.RATE_LIMITED.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(mapper.writeValueAsBytes(body));
    }

    private static String bucket(HttpServletRequest request) {
        return switch (path(request)) {
            case ASSESSMENTS_PATH -> "cases";
            case CHAT_PATH -> "chat";
            default -> null;
        };
    }

    private static String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context.isEmpty() ? uri : uri.substring(context.length());
    }
}
