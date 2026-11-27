package pl.elektrodemo.copilot.web;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared helpers of the T22 web ITs: WireMock serving T04 captures verbatim (status, headers, body),
 * and a hand-rolled multipart client request (MockMvc does not enforce the multipart limits).
 */
final class ApiTestSupport {

    static final JsonMapper MAPPER = JsonMapper.builder().build();
    static final String ASSESSMENTS = "/api/cases/assessments";
    static final String DECISIONS = "/api/cases/decisions";
    static final Path EXAMPLE_IMAGES = Path.of("../../assets/example-images");
    private static final Set<String> SKIPPED = Set.of(":status", "content-length", "content-encoding",
            "transfer-encoding", "connection", "date");

    private final HttpClient http = HttpClient.newHttpClient();

    private ApiTestSupport() {
    }

    static ApiTestSupport client() {
        return new ApiTestSupport();
    }

    static WireMockServer startWireMock() {
        WireMockServer wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        return wm;
    }

    /** Non-streaming T04 capture {@code captures/<dir>/<stem>.json} ({status, headers, body}). */
    static ResponseDefinitionBuilder capture(String dir, String stem) {
        JsonNode capture = MAPPER.readTree(read("captures/" + dir + "/" + stem + ".json"));
        ResponseDefinitionBuilder builder = new ResponseDefinitionBuilder().withStatus(capture.get("status").asInt())
                .withBody(MAPPER.writeValueAsString(capture.get("body")).getBytes(StandardCharsets.UTF_8));
        capture.get("headers").properties().forEach(e -> {
            if (!SKIPPED.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                List<String> values = new ArrayList<>();
                e.getValue().forEach(v -> values.add(v.asString()));
                builder.withHeader(e.getKey(), values.toArray(String[]::new));
            }
        });
        return builder;
    }

    static byte[] read(String resource) {
        try (InputStream in = ApiTestSupport.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + resource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static byte[] image(String name) {
        try {
            return Files.readAllBytes(EXAMPLE_IMAGES.resolve(name));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static String formJson(String caseType, String modelName, LocalDate purchaseDate, String reason) {
        return MAPPER.writeValueAsString(java.util.Map.of("caseType", caseType, "category", "LAPTOP",
                "modelName", modelName, "purchaseDate", purchaseDate.toString(), "reason", reason));
    }

    static LocalDate recentPurchase() {
        return LocalDate.now(ZoneId.of("Europe/Warsaw")).minusDays(5);
    }

    String validForm() {
        return formJson("RETURN", "Lenovo ThinkPad T14 Gen 4", recentPurchase(), "");
    }

    HttpResponse<String> postAssessment(int port, String formJson, String fileName, byte[] photo) throws Exception {
        String boundary = "----T22" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        if (formJson != null) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"form\"\r\n"
                    + "Content-Type: application/json\r\n\r\n" + formJson + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        if (photo != null) {
            body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"photo\"; filename=\""
                    + fileName + "\"\r\nContent-Type: application/octet-stream\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            body.writeBytes(photo);
            body.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + ASSESSMENTS))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build());
    }

    HttpResponse<String> postJson(int port, String path, String json) throws Exception {
        return send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build());
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
