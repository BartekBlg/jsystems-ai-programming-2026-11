package pl.elektrodemo.copilot.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards GET requests for client-side paths to the Angular index.html (ADR-001 §3.6).
 * Only paths whose segments contain no dot (no file extension) match, and the first segment
 * must not be {@code api} or {@code actuator}; static files and API paths are handled elsewhere.
 * The app has no router (ADR-003 §6.2), so three levels are enough for any shared link.
 */
@Controller
public class SpaFallbackController {

    private static final String FIRST = "{s1:(?!api$|actuator$)[^.]+}";

    @GetMapping({
        "/" + FIRST,
        "/" + FIRST + "/{s2:[^.]+}",
        "/" + FIRST + "/{s2:[^.]+}/{s3:[^.]+}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
