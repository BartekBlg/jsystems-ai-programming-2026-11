package pl.elektrodemo.copilot.web;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.elektrodemo.copilot.support.ProblemAssertions.assertProblem;
import static pl.elektrodemo.copilot.web.ApiTestSupport.ASSESSMENTS;
import static pl.elektrodemo.copilot.web.ApiTestSupport.DECISIONS;
import static pl.elektrodemo.copilot.web.ApiTestSupport.MAPPER;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * T22 / ADR-001 §3.3, §3.4, §5.1-5.2, §8 (TAC-001-01/03/05): the case endpoints end-to-end on a real embedded
 * server, with OpenRouter replaced by WireMock serving the T04 captures verbatim.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CaseControllerIT {

    private static final WireMockServer WM = ApiTestSupport.startWireMock();
    private static final int TEN_MB = 10 * 1024 * 1024;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("llm.base-url", WM::baseUrl);
        r.add("llm.api-key", () -> "sk-test-fake");
        r.add("llm.timeouts.decision", () -> "3s");
        r.add("app.rate-limit.cases-per-hour", () -> "1000");
    }

    @LocalServerPort
    int port;

    private final ApiTestSupport api = ApiTestSupport.client();

    @BeforeEach
    void reset() {
        WM.resetAll();
    }

    @AfterAll
    static void stop() {
        WM.stop();
    }

    private void stubLlm(String stem) {
        WM.stubFor(post(urlEqualTo("/responses")).willReturn(ApiTestSupport.capture("responses", stem)));
    }

    private HttpResponse<String> assess() throws Exception {
        stubLlm("assessment-return-ok");
        return api.postAssessment(port, api.validForm(), "laptop-1.png", ApiTestSupport.image("laptop-1.png"));
    }

    // ------------------------------------------------------------ assessments

    @Test
    void jpegNamedPngWithValidFormGivesSignedAssessment() throws Exception {
        HttpResponse<String> r = assess();

        assertThat(r.statusCode()).isEqualTo(200);
        JsonNode body = MAPPER.readTree(r.body());
        assertThat(body.path("signature").asString()).isNotBlank();
        JsonNode ctx = body.get("caseContext");
        List<String> keys = new ArrayList<>(ctx.propertyNames());
        assertThat(keys).containsExactlyInAnyOrder("contextVersion", "caseType", "form", "submittedAt", "assessment",
                "assessmentSource", "prescreen", "consistencySignals", "ruleFacts", "decision");
        assertThat(ctx.get("prescreen").isNull()).isTrue();
        assertThat(ctx.get("decision").isNull()).isTrue();
        assertThat(WM.getAllServeEvents()).hasSize(1);
    }

    @Test
    void gifNamedJpgIsUnsupportedImageAndNeverReachesTheLlm() throws Exception {
        byte[] gif = new byte[] {'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0, 0, 0, 0, 0x3B};

        HttpResponse<String> r = api.postAssessment(port, api.validForm(), "photo.jpg", gif);

        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(r.headers().firstValue("Content-Type")).hasValueSatisfying(ct -> assertThat(ct).startsWith("application/problem+json"));
        JsonNode p = assertProblem(r.body(), 400, "UNSUPPORTED_IMAGE");
        assertThat(p.get("detail").asString()).isEqualTo(ErrorCode.UNSUPPORTED_IMAGE.detail());
        assertThat(p.get("instance").asString()).isEqualTo(ASSESSMENTS);
        assertThat(WM.getAllServeEvents()).isEmpty();
    }

    @Test
    void invalidFormListsEveryPolishFieldError() throws Exception {
        String form = ApiTestSupport.formJson("COMPLAINT", "", LocalDate.now().plusDays(3), "krótko");

        HttpResponse<String> r = api.postAssessment(port, form, "laptop-1.png", ApiTestSupport.image("laptop-1.png"));

        assertThat(r.statusCode()).isEqualTo(400);
        JsonNode p = assertProblem(r.body(), 400, "VALIDATION_FAILED");
        assertThat(fieldErrors(p)).containsExactlyInAnyOrder(
                "modelName=Podaj nazwę lub model urządzenia (2–100 znaków).",
                "purchaseDate=Data zakupu nie może być z przyszłości.",
                "reason=Opisz problem (co najmniej 10 znaków).");
        assertThat(WM.getAllServeEvents()).isEmpty();
    }

    @Test
    void missingPhotoIsValidationFailedOnPhotoField() throws Exception {
        HttpResponse<String> r = api.postAssessment(port, api.validForm(), null, null);

        assertThat(r.statusCode()).isEqualTo(400);
        JsonNode p = assertProblem(r.body(), 400, "VALIDATION_FAILED");
        assertThat(fieldErrors(p)).containsExactly("photo=Dodaj zdjęcie urządzenia.");
    }

    @Test
    void photoOverTenMegabytesIsPhotoTooLarge() throws Exception {
        byte[] photo = jpegOfSize(TEN_MB + 1);

        HttpResponse<String> r = api.postAssessment(port, api.validForm(), "big.jpg", photo);

        assertThat(r.statusCode()).isEqualTo(413);
        JsonNode p = assertProblem(r.body(), 413, "PHOTO_TOO_LARGE");
        assertThat(p.get("detail").asString()).isEqualTo(ErrorCode.PHOTO_TOO_LARGE.detail());
        assertThat(WM.getAllServeEvents()).isEmpty();
    }

    @Test
    void photoOfExactlyTenMegabytesIsNotRejectedBySizeLimit() throws Exception {
        byte[] photo = jpegOfSize(TEN_MB);

        HttpResponse<String> r = api.postAssessment(port, api.validForm(), "ten.jpg", photo);

        assertThat(r.statusCode()).isNotEqualTo(413);
    }

    // -------------------------------------------------------------- decisions

    @Test
    void validSignedContextGivesDecisionEndingWithDisclaimer() throws Exception {
        String assessment = assess().body();
        stubLlm("decision-return-ok");

        HttpResponse<String> r = api.postJson(port, DECISIONS, assessment);

        assertThat(r.statusCode()).isEqualTo(200);
        JsonNode body = MAPPER.readTree(r.body());
        assertThat(body.at("/decision/message").asString())
                .contains("To wstępna, automatyczna ocena przygotowana na podstawie formularza i zdjęcia. "
                        + "Ostateczną decyzję podejmie nasz zespół serwisu po otrzymaniu i sprawdzeniu urządzenia.")
                .endsWith("Nie ogranicza to Twojego ustawowego prawa do odstąpienia od umowy.");
        assertThat(body.at("/caseContext/decision/category").asString()).isEqualTo(body.at("/decision/category").asString());
        assertThat(body.get("signature").asString()).isNotBlank();
    }

    @Test
    void changedFieldIsInvalidCaseSignature() throws Exception {
        ObjectNode tampered = (ObjectNode) MAPPER.readTree(assess().body());
        ObjectNode assessment = (ObjectNode) tampered.at("/caseContext/assessment");
        assessment.put("confidence", "LOW".equals(assessment.get("confidence").asString()) ? "HIGH" : "LOW");
        WM.resetAll();

        HttpResponse<String> r = api.postJson(port, DECISIONS, MAPPER.writeValueAsString(tampered));

        assertThat(r.statusCode()).isEqualTo(401);
        assertProblem(r.body(), 401, "INVALID_CASE_SIGNATURE");
        assertThat(WM.getAllServeEvents()).isEmpty();
    }

    @Test
    void secondDecisionOnDecidedContextIsConflict() throws Exception {
        String assessment = assess().body();
        stubLlm("decision-return-ok");
        JsonNode first = MAPPER.readTree(api.postJson(port, DECISIONS, assessment).body());
        ObjectNode again = MAPPER.createObjectNode();
        again.set("caseContext", first.get("caseContext"));
        again.set("signature", first.get("signature"));

        HttpResponse<String> r = api.postJson(port, DECISIONS, MAPPER.writeValueAsString(again));

        assertThat(r.statusCode()).isEqualTo(409);
        assertProblem(r.body(), 409, "DECISION_ALREADY_MADE");
    }

    @Test
    void blankSignatureOrNullContextIsValidationFailed() throws Exception {
        JsonNode ctx = MAPPER.readTree(assess().body()).get("caseContext");
        ObjectNode blank = MAPPER.createObjectNode();
        blank.set("caseContext", ctx);
        blank.put("signature", " ");
        ObjectNode noContext = MAPPER.createObjectNode();
        noContext.put("signature", "abc");

        JsonNode p1 = assertProblem(api.postJson(port, DECISIONS, MAPPER.writeValueAsString(blank)).body(), 400,
                "VALIDATION_FAILED");
        JsonNode p2 = assertProblem(api.postJson(port, DECISIONS, MAPPER.writeValueAsString(noContext)).body(), 400,
                "VALIDATION_FAILED");

        assertThat(fieldErrors(p1)).hasSize(1).allMatch(s -> s.startsWith("signature="));
        assertThat(fieldErrors(p2)).hasSize(1).allMatch(s -> s.startsWith("caseContext="));
    }

    @Test
    void malformedJsonIsValidationFailedWithoutFieldErrors() throws Exception {
        HttpResponse<String> r = api.postJson(port, DECISIONS, "{not json");

        assertThat(r.statusCode()).isEqualTo(400);
        JsonNode p = assertProblem(r.body(), 400, "VALIDATION_FAILED");
        assertThat(p.has("fieldErrors")).isFalse();
    }

    @Test
    void providerRejectionIsLlmFailedWithoutProviderText() throws Exception {
        String assessment = assess().body();
        stubLlm("error-401");

        HttpResponse<String> r = api.postJson(port, DECISIONS, assessment);

        assertThat(r.statusCode()).isEqualTo(502);
        assertProblem(r.body(), 502, "LLM_FAILED");
        assertThat(r.body()).doesNotContain("User not found");
    }

    @Test
    void slowProviderIsLlmTimeout() throws Exception {
        String assessment = assess().body();
        WM.stubFor(post(urlEqualTo("/responses"))
                .willReturn(ApiTestSupport.capture("responses", "decision-return-ok").withFixedDelay(6000)));

        HttpResponse<String> r = api.postJson(port, DECISIONS, assessment);

        assertThat(r.statusCode()).isEqualTo(504);
        assertProblem(r.body(), 504, "LLM_TIMEOUT");
    }

    // ---------------------------------------------------------------- helpers

    private static List<String> fieldErrors(JsonNode problem) {
        List<String> out = new ArrayList<>();
        problem.get("fieldErrors").forEach(e -> out.add(e.get("field").asString() + "=" + e.get("message").asString()));
        return out;
    }

    /** A real JPEG header (from an example image) padded with zeros to the exact size. */
    private static byte[] jpegOfSize(int size) {
        byte[] src = ApiTestSupport.image("phone-1.jpg");
        byte[] out = Arrays.copyOf(src, size);
        return out;
    }
}
