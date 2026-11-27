package pl.elektrodemo.copilot.web;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.elektrodemo.copilot.support.ProblemAssertions.assertProblem;
import static pl.elektrodemo.copilot.web.ApiTestSupport.ASSESSMENTS;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

/**
 * T41-fix-1 / ADR-001 §3.4, §5.1 (AC-10 server side): the multipart size limits on a real embedded Tomcat, with a
 * browser-like body (the form part carries a filename, as Playwright/browser clients send it, and the photo is
 * sent as image/jpeg). Whatever limit is hit, the answer is 413 PHOTO_TOO_LARGE and the LLM is never reached.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartLimitIT {

    private static final WireMockServer WM = ApiTestSupport.startWireMock();
    private static final int TEN_MB = 10 * 1024 * 1024;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("llm.base-url", WM::baseUrl);
        r.add("llm.api-key", () -> "sk-test-fake");
        r.add("app.rate-limit.cases-per-hour", () -> "1000");
    }

    @LocalServerPort
    int port;

    @BeforeEach
    void resetWireMock() {
        WM.resetRequests();
    }

    @AfterAll
    static void stop() {
        WM.stop();
    }

    @Test
    void elevenMegabyteBrowserLikeUploadIsPhotoTooLarge() throws Exception {
        HttpResponse<String> r = post(repeatedImage(TEN_MB + 1));

        assertThat(r.statusCode()).isEqualTo(413);
        JsonNode p = assertProblem(r.body(), 413, "PHOTO_TOO_LARGE");
        assertThat(p.get("detail").asString()).isEqualTo(ErrorCode.PHOTO_TOO_LARGE.detail());
        assertThat(WM.findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void wholeRequestOverTheRequestLimitIsPhotoTooLarge() throws Exception {
        HttpResponse<String> r = post(repeatedImage(13 * 1024 * 1024));

        assertThat(r.statusCode()).isEqualTo(413);
        assertProblem(r.body(), 413, "PHOTO_TOO_LARGE");
        assertThat(WM.findAll(anyRequestedFor(anyUrl()))).isEmpty();
    }

    @Test
    void exactlyTenMegabytesBrowserLikeUploadIsNotRejectedAsTooLarge() throws Exception {
        HttpResponse<String> r = post(repeatedImage(TEN_MB));

        assertThat(r.statusCode()).isNotEqualTo(413);
    }

    private static byte[] repeatedImage(int size) {
        byte[] png = ApiTestSupport.image("laptop-1.png");
        byte[] out = new byte[size];
        for (int off = 0; off < size; off += png.length) {
            System.arraycopy(png, 0, out, off, Math.min(png.length, size - off));
        }
        return out;
    }

    private HttpResponse<String> post(byte[] photo) throws Exception {
        String boundary = "----T41fix" + UUID.randomUUID();
        ApiTestSupport api = ApiTestSupport.client();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"form\"; filename=\"form.json\"\r\n"
                + "Content-Type: application/json\r\n\r\n" + api.validForm() + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"photo\"; filename=\"photo.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.writeBytes(photo);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + ASSESSMENTS))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
