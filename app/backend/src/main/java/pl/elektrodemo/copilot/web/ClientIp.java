package pl.elektrodemo.copilot.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The socket address of the caller. Forwarded headers are never read here: with
 * {@code server.forward-headers-strategy=native} the container has already applied them (ADR-000 Decision 8.6).
 */
final class ClientIp {

    private ClientIp() {
    }

    static String of(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
