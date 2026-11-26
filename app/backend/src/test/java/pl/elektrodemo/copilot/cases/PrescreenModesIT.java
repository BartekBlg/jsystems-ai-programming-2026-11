package pl.elektrodemo.copilot.cases;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.notContaining;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * T25 / ADR-004 3.5-3.8, 8 (TAC-004-01..04, 06): llm-only / shadow / gated orchestration end-to-end, with both
 * OpenRouter endpoints replaced by WireMock serving the T04 captures verbatim.
 */
class PrescreenModesIT {

    private static final WireMockServer WM = ModesSupport.startWireMock();
    private static final String DEVICE_MODEL = "Lenovo ThinkPad T14 Gen 4";
    private static final String RETURN_REASON = "Klawiatura przestała działać po upadku";
    private static final String COMPLAINT_REASON = "Pęknięty ekran po upadku z biurka";
    private static final String SIGNALS_HEADER = "Wstępne sygnały do weryfikacji";

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("llm.base-url", WM::baseUrl);
        r.add("llm.api-key", () -> "sk-test-fake");
        r.add("llm.timeouts.decision", () -> "3s");
        r.add("app.decisions.url", () -> WM.baseUrl() + "/decisions");
        r.add("app.rate-limit.cases-per-hour", () -> "1000");
    }

    @AfterAll
    static void stop() {
        WM.stop();
    }

    abstract static class Base {
        @LocalServerPort
        int port;

        final ModesSupport api = ModesSupport.client();
        ListAppender<ILoggingEvent> prescreenLog;

        @BeforeEach
        void setUp() {
            WM.resetAll();
            prescreenLog = new ListAppender<>();
            prescreenLog.start();
            ((Logger) LoggerFactory.getLogger("prescreen")).addAppender(prescreenLog);
        }

        @AfterEach
        void tearDown() {
            ((Logger) LoggerFactory.getLogger("prescreen")).detachAppender(prescreenLog);
        }

        void stubVision(String stem) {
            WM.stubFor(post(urlEqualTo("/responses")).withRequestBody(containing("input_image"))
                    .willReturn(ModesSupport.capture("responses", stem)));
        }

        void stubDecisionLlm(String stem) {
            WM.stubFor(post(urlEqualTo("/responses")).withRequestBody(notContaining("input_image"))
                    .willReturn(ModesSupport.capture("responses", stem)));
        }

        void stubA(String stem) {
            WM.stubFor(post(urlEqualTo("/decisions")).withRequestBody(containing("photo_usable"))
                    .willReturn(ModesSupport.capture("decisions", stem)));
        }

        void stubB(String stem) {
            WM.stubFor(post(urlEqualTo("/decisions")).withRequestBody(containing("model_matches_photo"))
                    .willReturn(ModesSupport.capture("decisions", stem)));
        }

        List<LoggedRequest> visionRequests() {
            return WM.findAll(postRequestedFor(urlEqualTo("/responses")).withRequestBody(containing("input_image")));
        }

        List<LoggedRequest> decisionLlmRequests() {
            return WM.findAll(postRequestedFor(urlEqualTo("/responses")).withRequestBody(notContaining("input_image")));
        }

        List<LoggedRequest> decisionsRequests() {
            return WM.findAll(postRequestedFor(urlEqualTo("/decisions")));
        }

        HttpResponse<String> assessReturnLaptop(String image, String reason) throws Exception {
            String form = ModesSupport.formJson("RETURN", DEVICE_MODEL, ModesSupport.recentPurchase(),
                    reason);
            return api.postAssessment(port, form, image, ModesSupport.image(image));
        }

        HttpResponse<String> assessComplaintPhone() throws Exception {
            String form = ModesSupport.MAPPER.writeValueAsString(Map.of("caseType", "COMPLAINT", "category",
                    "SMARTPHONE", "modelName", "Smartfon testowy", "purchaseDate",
                    ModesSupport.recentPurchase().toString(), "reason", COMPLAINT_REASON));
            return api.postAssessment(port, form, "phone-1.jpg", ModesSupport.image("phone-1.jpg"));
        }

        JsonNode json(HttpResponse<String> response) {
            assertThat(response.statusCode()).isEqualTo(200);
            return ModesSupport.MAPPER.readTree(response.body());
        }

        List<String> logMessages(String prefix) {
            return prescreenLog.list.stream().map(ILoggingEvent::getFormattedMessage)
                    .filter(m -> m.startsWith(prefix)).toList();
        }

        void assertDecisionsRequestShape() {
            List<LoggedRequest> requests = decisionsRequests();
            assertThat(requests).isNotEmpty();
            for (LoggedRequest request : requests) {
                JsonNode body = ModesSupport.MAPPER.readTree(request.getBodyAsString());
                assertThat(body.get("model").asString()).isEqualTo("typesafe/jev-1.14");
                if (body.get("questions").has("photo_usable")) {
                    String text = request.getBodyAsString();
                    assertThat(text).doesNotContain(DEVICE_MODEL).doesNotContain(RETURN_REASON)
                            .doesNotContain("Smartfon testowy").doesNotContain(COMPLAINT_REASON);
                }
            }
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(properties = "app.decisions.mode=llm-only")
    class LlmOnly extends Base {

        @Test
        void neverCallsDecisionsAndUsesLlmSource() throws Exception {
            stubVision("assessment-return-ok");
            stubDecisionLlm("decision-return-ok");

            JsonNode assessment = json(assessReturnLaptop("laptop-2.webp", ""));
            JsonNode ctx = assessment.get("caseContext");

            assertThat(ctx.get("assessmentSource").asString()).isEqualTo("LLM");
            assertThat(ctx.get("prescreen").isNull()).isTrue();
            assertThat(ctx.get("consistencySignals").isNull()).isTrue();
            assertThat(decisionsRequests()).isEmpty();
            assertThat(visionRequests()).hasSize(1);
            assertThat(visionRequests().get(0).getBodyAsString()).doesNotContain(SIGNALS_HEADER);

            assertThat(api.postJson(port, ModesSupport.DECISIONS, assessment.toString()).statusCode())
                    .isEqualTo(200);
            assertThat(decisionLlmRequests()).hasSize(1);
            assertThat(decisionsRequests()).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(properties = "app.decisions.mode=shadow")
    class Shadow extends Base {

        @Test
        void runsPrescreenInParallelKeepsLlmAssessmentAndLogsOneEventWithoutText() throws Exception {
            stubVision("assessment-return-ok");
            stubA("a-return-laptop-2");
            stubB("b-return");

            JsonNode ctx = json(assessReturnLaptop("laptop-2.webp", RETURN_REASON)).get("caseContext");

            assertThat(ctx.get("assessmentSource").asString()).isEqualTo("LLM");
            assertThat(ctx.get("prescreen").isNull()).isTrue();
            assertThat(ctx.get("consistencySignals").isArray()).isTrue();
            assertThat(ctx.get("consistencySignals")).isNotEmpty();
            assertThat(decisionsRequests()).hasSize(2);
            assertThat(visionRequests()).hasSize(1);
            // independent comparison: the vision LLM never sees the pre-screen
            assertThat(visionRequests().get(0).getBodyAsString()).doesNotContain(SIGNALS_HEADER);
            assertDecisionsRequestShape();

            List<String> events = logMessages("event=prescreen.shadow ");
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).doesNotContain(DEVICE_MODEL).doesNotContain(RETURN_REASON)
                    .doesNotContain("Klawiatura").contains("agreement=").contains("wouldSkipLlm=");
        }

        @Test
        void prescreenFailureNeverAffectsTheResultAndLogsOneWarning() throws Exception {
            stubVision("assessment-return-ok");
            WM.stubFor(post(urlEqualTo("/decisions")).willReturn(ModesSupport.capture("decisions", "error-401")));

            JsonNode ctx = json(assessReturnLaptop("laptop-2.webp", "")).get("caseContext");

            assertThat(ctx.get("assessmentSource").asString()).isEqualTo("LLM");
            assertThat(ctx.get("consistencySignals").isNull()).isTrue();
            assertThat(logMessages("event=prescreen.shadow ")).isEmpty();
            assertThat(prescreenLog.list.stream().filter(e -> e.getLevel().toString().equals("WARN"))).hasSize(1);
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(properties = "app.decisions.mode=gated")
    class Gated extends Base {

        @Test
        void confidentCaptureSkipsVisionLlmAndMapsComplaint() throws Exception {
            stubA("a-complaint-phone-1");
            stubB("b-return"); // the captured b-complaint is a confident MODEL_MISMATCH, which makes the gate UNCERTAIN
            stubDecisionLlm("decision-complaint-ok");

            JsonNode assessment = json(assessComplaintPhone());
            JsonNode ctx = assessment.get("caseContext");

            assertThat(visionRequests()).isEmpty();
            assertThat(ctx.get("assessmentSource").asString()).isEqualTo("DECISIONS");
            assertThat(ctx.get("prescreen").isArray()).isTrue();
            assertThat(ctx.get("prescreen")).isNotEmpty();
            assertThat(ctx.get("assessment").get("probableCause").asString()).isEqualTo("MECHANICAL_DAMAGE");
            // TAC-004-06: mechanical cause never leaves ACCEPTED available (rule engine runs on the mapped assessment)
            List<String> allowed = ctx.at("/ruleFacts/allowedCategories").valueStream().map(JsonNode::asString)
                    .toList();
            assertThat(allowed).isNotEmpty().doesNotContain("ACCEPTED");
            assertDecisionsRequestShape();
            List<String> gatedLog = logMessages("event=prescreen.gated");
            assertThat(gatedLog).hasSize(1);
            assertThat(gatedLog.get(0)).contains("visionLlmSkipped=true");

            HttpResponse<String> decision = api.postJson(port, ModesSupport.DECISIONS, assessment.toString());
            assertThat(decision.statusCode()).isEqualTo(200);
            assertThat(decisionLlmRequests()).hasSize(1);
            assertThat(visionRequests()).isEmpty();
            JsonNode decided = ModesSupport.MAPPER.readTree(decision.body());
            assertThat(allowed).contains(decided.at("/decision/category").asString());
        }

        @Test
        void uncertainCaptureSendsOnlyRequestASignalsToVisionLlm() throws Exception {
            stubA("a-return-laptop-2");
            stubB("b-return");
            stubVision("assessment-return-ok");

            JsonNode ctx = json(assessReturnLaptop("laptop-2.webp", RETURN_REASON)).get("caseContext");

            assertThat(ctx.get("assessmentSource").asString()).isEqualTo("LLM");
            assertThat(ctx.get("prescreen").isArray()).isTrue();
            assertThat(visionRequests()).hasSize(1);
            String vision = visionRequests().get(0).getBodyAsString();
            assertThat(vision).contains(SIGNALS_HEADER).contains("photo_usable=")
                    .doesNotContain(DEVICE_MODEL).doesNotContain(RETURN_REASON).doesNotContain("typesafe")
                    .doesNotContain("model_matches_photo").doesNotContain("reason_contains_instructions");
            List<String> gatedLog = logMessages("event=prescreen.gated");
            assertThat(gatedLog).hasSize(1);
            assertThat(gatedLog.get(0)).contains("visionLlmSkipped=false");
            assertDecisionsRequestShape();
        }

        @Test
        void decisionsErrorFailsOpenToPlainVisionLlm() throws Exception {
            WM.stubFor(post(urlEqualTo("/decisions")).willReturn(ModesSupport.capture("decisions", "error-401")));
            stubVision("assessment-return-ok");

            JsonNode ctx = json(assessReturnLaptop("laptop-2.webp", "")).get("caseContext");

            assertThat(ctx.get("assessmentSource").asString()).isEqualTo("LLM");
            assertThat(ctx.get("prescreen").isNull()).isTrue();
            assertThat(visionRequests()).hasSize(1);
            assertThat(visionRequests().get(0).getBodyAsString()).doesNotContain(SIGNALS_HEADER);
            List<String> gatedLog = logMessages("event=prescreen.gated");
            assertThat(gatedLog).hasSize(1);
            assertThat(gatedLog.get(0)).contains("visionLlmSkipped=false").contains("HTTP_401");
        }

        @Test
        void tamperedPrescreenConfidenceIsInvalidSignature() throws Exception {
            stubA("a-complaint-phone-1");
            stubB("b-return"); // the captured b-complaint is a confident MODEL_MISMATCH, which makes the gate UNCERTAIN
            ObjectNode tampered = (ObjectNode) json(assessComplaintPhone());
            ObjectNode first = (ObjectNode) tampered.at("/caseContext/prescreen/0");
            first.put("confidence", 0.01);
            WM.resetAll();

            HttpResponse<String> response = api.postJson(port, ModesSupport.DECISIONS, tampered.toString());

            assertThat(response.statusCode()).isEqualTo(401);
            assertThat(response.body()).contains("INVALID_CASE_SIGNATURE");
            assertThat(WM.getAllServeEvents()).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @TestPropertySource(properties = {"app.decisions.mode=gated", "app.decisions.timeout=1s"})
    class GatedSlowDecisions extends Base {

        @Test
        void decisionsDelayBeyondTimeoutFailsOpenToVisionLlm() throws Exception {
            WM.stubFor(post(urlEqualTo("/decisions")).willReturn(
                    ModesSupport.capture("decisions", "a-return-laptop-2").withFixedDelay(2000)));
            stubVision("assessment-return-ok");

            JsonNode ctx = json(assessReturnLaptop("laptop-2.webp", "")).get("caseContext");

            assertThat(ctx.get("assessmentSource").asString()).isEqualTo("LLM");
            assertThat(ctx.get("prescreen").isNull()).isTrue();
            assertThat(visionRequests()).hasSize(1);
            assertThat(visionRequests().get(0).getBodyAsString()).doesNotContain(SIGNALS_HEADER);
        }
    }
}


/** Copy of the web-package T22 helpers (package-private there): WireMock serving captures verbatim, multipart client. */
final class ModesSupport {

    static final JsonMapper MAPPER = JsonMapper.builder().build();
    static final String ASSESSMENTS = "/api/cases/assessments";
    static final String DECISIONS = "/api/cases/decisions";
    static final Path EXAMPLE_IMAGES = Path.of("../../assets/example-images");
    private static final Set<String> SKIPPED = Set.of(":status", "content-length", "content-encoding",
            "transfer-encoding", "connection", "date");

    private final HttpClient http = HttpClient.newHttpClient();

    private ModesSupport() {
    }

    static ModesSupport client() {
        return new ModesSupport();
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
        try (InputStream in = ModesSupport.class.getClassLoader().getResourceAsStream(resource)) {
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
