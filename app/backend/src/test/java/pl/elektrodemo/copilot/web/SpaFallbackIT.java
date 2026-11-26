package pl.elektrodemo.copilot.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * ADR-001 §3.6 / TAC-001-06 / ADR-005 D3: GET of a non-/api, non-/actuator path without a file
 * extension serves index.html; unknown /api paths are problem+json 404 with code NOT_FOUND.
 * Runs against a real embedded server with a test index.html on the classpath.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpaFallbackIT {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void rootServesIndexHtml() throws Exception {
        HttpResponse<String> r = get("/");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("<app-root>");
    }

    @Test
    void spaRouteWithoutExtensionForwardsToIndexHtml() throws Exception {
        HttpResponse<String> r = get("/czat/123");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("<app-root>");
    }

    @Test
    void missingFileWithExtensionIsNotIndexHtml() throws Exception {
        HttpResponse<String> r = get("/missing-file.js");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.body()).doesNotContain("<app-root>");
    }

    @Test
    void unknownApiPathIsProblemJson404() throws Exception {
        HttpResponse<String> r = get("/api/unknown");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.headers().firstValue("Content-Type")).hasValueSatisfying(
                ct -> assertThat(ct).startsWith("application/problem+json"));
        assertThat(r.body()).contains("\"code\":\"NOT_FOUND\"").contains("Nie znaleziono zasobu.")
                .doesNotContain("<app-root>").doesNotContain("Exception");
    }

    @Test
    void healthIsUp() throws Exception {
        HttpResponse<String> r = get("/actuator/health");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("\"status\":\"UP\"");
    }
}
