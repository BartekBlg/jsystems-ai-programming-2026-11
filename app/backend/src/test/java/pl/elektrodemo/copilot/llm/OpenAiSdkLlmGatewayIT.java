package pl.elektrodemo.copilot.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import pl.elektrodemo.copilot.cases.*;
import pl.elektrodemo.copilot.prompts.PromptRepository;
import pl.elektrodemo.copilot.web.ErrorCode;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.dto.ChatMessageDto;
import tools.jackson.databind.JsonNode;

/**
 * T20 / ADR-002 §3.1-§3.8: the SDK gateway against WireMock serving the T04 captures verbatim.
 * No real OpenRouter call is made; keys are fake.
 */
class OpenAiSdkLlmGatewayIT {

    private static final String VISION_MODEL = "test/vision-model";
    private static final String DECISION_MODEL = "test/decision-model";
    private static final String DEVICE_MODEL = "Lenovo ThinkPad T14 Gen 4";
    private static final String REASON = "Ekran miga po kilku tygodniach";
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    private WireMockServer wm;
    private PromptRepository prompts;
    private ListAppender<ILoggingEvent> usageLog;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        prompts = new PromptRepository(new DefaultResourceLoader());
        usageLog = new ListAppender<>();
        usageLog.start();
        ((Logger) LoggerFactory.getLogger("llm.usage")).addAppender(usageLog);
        ((Logger) LoggerFactory.getLogger("llm.usage")).setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("llm.usage")).detachAppender(usageLog);
        wm.stop();
    }

    // ---------------------------------------------------------------- helpers

    private LlmProperties props(Duration vision, Duration decision, Duration chatFirst, Duration chatTotal) {
        return new LlmProperties("sk-test-fake", wm.baseUrl(), DECISION_MODEL, VISION_MODEL, "medium", "low",
                new LlmProperties.Timeouts(vision, decision, chatFirst, chatTotal),
                new LlmProperties.MaxOutputTokens(4000, 8000, 4000));
    }

    private LlmProperties defaultProps() {
        return props(Duration.ofSeconds(26), Duration.ofSeconds(34), Duration.ofSeconds(30), Duration.ofSeconds(90));
    }

    private OpenAiSdkLlmGateway gateway(LlmProperties props) {
        return new OpenAiSdkLlmGateway(new OpenRouterClientConfig().openAiClient(props, false), props, prompts);
    }

    private void stubResponses(ResponseDefinitionBuilder response) {
        wm.stubFor(post(urlEqualTo("/responses")).willReturn(response));
    }

    private List<JsonNode> requestBodies() {
        return wm.getAllServeEvents().reversed().stream()
                .map(e -> Captures.MAPPER.readTree(e.getRequest().getBodyAsString())).toList();
    }

    private JsonNode onlyRequest() {
        List<JsonNode> bodies = requestBodies();
        assertThat(bodies).hasSize(1);
        return bodies.get(0);
    }

    private static ReturnImageAssessment returnAssessment() {
        return Captures.MAPPER.readValue(Captures.outputText("assessment-return-ok"), ReturnImageAssessment.class);
    }

    private static DecisionPrompt returnPrompt(String deviceModel, String reason, LocalDate purchase, String note) {
        RuleResult rules = new RuleResult(5, ReturnWindowStatus.WITHIN_14_DAYS, false,
                EnumSet.of(DecisionCategory.ESCALATED), null, EnumSet.of(DecisionCategory.REJECTED), null, "R3");
        return new DecisionPrompt(CaseType.RETURN,
                new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, deviceModel, purchase, reason),
                OffsetDateTime.of(2026, 11, 26, 10, 0, 0, 0, ZoneOffset.UTC), returnAssessment(), AssessmentSource.LLM,
                rules, "POLICY-RETURN-TEXT", null, null, note);
    }

    private static DecisionPrompt capturePrompt() {
        return returnPrompt(DEVICE_MODEL, "", LocalDate.of(2026, 11, 21), null);
    }

    private static List<String> texts(JsonNode message) {
        List<String> out = new ArrayList<>();
        message.get("content").forEach(c -> {
            if ("input_text".equals(c.get("type").asString())) {
                out.add(c.get("text").asString());
            }
        });
        return out;
    }

    private static final class RecordingSink implements ChatSink {
        final StringBuilder text = new StringBuilder();
        final List<TokenUsage> completed = new CopyOnWriteArrayList<>();
        final List<ErrorCode> failed = new CopyOnWriteArrayList<>();

        @Override
        public void onDelta(String t) {
            text.append(t);
        }

        @Override
        public void onCompleted(TokenUsage usage) {
            completed.add(usage);
        }

        @Override
        public void onFailed(ErrorCode code) {
            failed.add(code);
        }
    }

    private ChatPrompt chatPrompt(String stem) {
        JsonNode request = Captures.MAPPER.readTree(
                getClass().getClassLoader().getResourceAsStream("captures/responses/" + stem + ".request.json"));
        List<ChatMessageDto> history = new ArrayList<>();
        JsonNode input = request.get("input");
        for (int i = 1; i < input.size(); i++) {
            JsonNode item = input.get(i);
            history.add(new ChatMessageDto("m" + i, item.get("role").asString(),
                    item.get("content").get(0).get("text").asString()));
        }
        DecisionCategory current = stem.equals("chat-plain") ? DecisionCategory.ESCALATED
                : DecisionCategory.MORE_INFO_NEEDED;
        return new ChatPrompt(capturePrompt(), current, history, null);
    }

    // ---------------------------------------------------------------- assessImage

    @Test
    void assessImageReturnParsesCapturedStructuredOutput() {
        stubResponses(Captures.json("assessment-return-ok"));
        ImageAssessment result = gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null);
        assertThat(result).isEqualTo(returnAssessment());
        ReturnImageAssessment r = (ReturnImageAssessment) result;
        assertThat(r.photoSufficient()).isTrue();
        assertThat(r.damageVisible()).isFalse();
        assertThat(r.signsOfUse()).containsExactly(SignOfUse.WEAR);
        assertThat(r.resellableAsNew()).isFalse();
        assertThat(r.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    void assessImageComplaintParsesCapturedStructuredOutput() {
        stubResponses(Captures.json("assessment-complaint-ok"));
        ImageAssessment result = gateway(defaultProps()).assessImage(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE, JPEG, null);
        assertThat(result).isInstanceOf(ComplaintImageAssessment.class);
        ComplaintImageAssessment c = (ComplaintImageAssessment) result;
        assertThat(c.defectVisible()).isTrue();
        assertThat(c.probableCause()).isEqualTo(ProbableCause.MECHANICAL_DAMAGE);
        assertThat(c.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    void assessImageInsufficientPhoto() {
        stubResponses(Captures.json("assessment-insufficient"));
        ImageAssessment result = gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null);
        assertThat(result.photoSufficient()).isFalse();
        assertThat(result.insufficiencyReason()).isEqualTo("Na zdjęciu widać telefon, a nie laptop.");
    }

    @Test
    void assessImageRequestLayout() {
        stubResponses(Captures.json("assessment-return-ok"));
        gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null);

        JsonNode body = onlyRequest();
        assertThat(body.get("model").asString()).isEqualTo(VISION_MODEL);
        assertThat(body.get("instructions").asString()).isEqualTo(prompts.imageAssessment(CaseType.RETURN));
        assertThat(body.get("reasoning").get("effort").asString()).isEqualTo("low");
        assertThat(body.get("max_output_tokens").asInt()).isEqualTo(4000);
        assertThat(body.get("store").asBoolean()).isFalse();
        assertThat(body.has("previous_response_id")).isFalse();
        assertThat(body.path("stream").asBoolean(false)).isFalse();
        JsonNode format = body.get("text").get("format");
        assertThat(format.get("type").asString()).isEqualTo("json_schema");
        assertThat(format.get("strict").asBoolean()).isTrue();
        assertThat(format.get("schema").get("properties").propertyNames())
                .contains("photoSufficient", "signsOfUse", "resellableAsNew", "confidence");

        JsonNode content = body.get("input").get(0).get("content");
        assertThat(body.get("input")).hasSize(1);
        assertThat(body.get("input").get(0).get("role").asString()).isEqualTo("user");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).get("type").asString()).isEqualTo("input_text");
        assertThat(content.get(0).get("text").asString()).isEqualTo("Oceń zdjęcie zgodnie z instrukcją. Kategoria sprzętu: Laptop.");
        assertThat(content.get(1).get("type").asString()).isEqualTo("input_image");
        assertThat(content.get(1).get("image_url").asString()).startsWith("data:image/jpeg;base64,/9j/");
    }

    @Test
    void assessImageComplaintUsesComplaintInstructionsAndSignals() {
        stubResponses(Captures.json("assessment-complaint-ok"));
        gateway(defaultProps()).assessImage(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE, JPEG, "photo_usable=0.41");
        JsonNode body = onlyRequest();
        assertThat(body.get("instructions").asString()).isEqualTo(prompts.imageAssessment(CaseType.COMPLAINT));
        List<String> texts = texts(body.get("input").get(0));
        assertThat(texts.get(0)).isEqualTo("Oceń zdjęcie zgodnie z instrukcją. Kategoria sprzętu: Smartfon.");
        assertThat(texts.get(1)).isEqualTo("Wstępne sygnały do weryfikacji: photo_usable=0.41");
    }

    @Test
    void visionBodyContainsNeitherModelNameNorReason() {
        stubResponses(Captures.json("assessment-return-ok"));
        gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null);
        String raw = wm.getAllServeEvents().get(0).getRequest().getBodyAsString();
        assertThat(raw).doesNotContain(DEVICE_MODEL).doesNotContain(REASON).doesNotContain("model_urzadzenia")
                .doesNotContain("opis_klienta");
    }

    // ---------------------------------------------------------------- decide

    @Test
    void decideParsesCapturedDecisionDraft() {
        stubResponses(Captures.json("decision-return-ok"));
        DecisionDraft draft = gateway(defaultProps()).decide(capturePrompt());
        DecisionDraft expected = Captures.MAPPER.readValue(Captures.outputText("decision-return-ok"), DecisionDraft.class);
        assertThat(draft).isEqualTo(expected);
        assertThat(draft.category()).isEqualTo(DecisionCategory.ESCALATED);
        assertThat(draft.message()).contains("## Decyzja");
    }

    @Test
    void decideParsesOtherCapturedDrafts() {
        for (String stem : List.of("decision-return-more-info", "decision-return-insufficient", "decision-complaint-ok")) {
            wm.resetAll();
            stubResponses(Captures.json(stem));
            DecisionDraft draft = gateway(defaultProps()).decide(capturePrompt());
            assertThat(draft).as(stem).isEqualTo(Captures.MAPPER.readValue(Captures.outputText(stem), DecisionDraft.class));
        }
    }

    @Test
    void decideRequestLayoutMatchesCapture() {
        stubResponses(Captures.json("decision-return-ok"));
        gateway(defaultProps()).decide(capturePrompt());

        JsonNode body = onlyRequest();
        assertThat(body.get("model").asString()).isEqualTo(DECISION_MODEL);
        assertThat(body.get("instructions").asString())
                .isEqualTo(prompts.decision(CaseType.RETURN) + "\n\n# Regulamin\n\nPOLICY-RETURN-TEXT");
        assertThat(body.get("reasoning").get("effort").asString()).isEqualTo("medium");
        assertThat(body.get("max_output_tokens").asInt()).isEqualTo(8000);
        assertThat(body.get("store").asBoolean()).isFalse();
        assertThat(body.has("previous_response_id")).isFalse();
        assertThat(body.get("text").get("format").get("schema").get("properties").propertyNames())
                .containsExactlyInAnyOrder("category", "message");
        assertThat(body.get("input")).hasSize(1);
        assertThat(texts(body.get("input").get(0))).hasSize(1);

        // Same block as the captured request, plus the "Źródło oceny" line required by the T20 brief.
        String capture = Captures.requestUserText("decision-return-ok");
        int at = capture.indexOf("\n\n## Fakty wyliczone przez system");
        String expected = capture.substring(0, at) + "\nŹródło oceny: LLM" + capture.substring(at);
        assertThat(texts(body.get("input").get(0)).get(0)).isEqualTo(expected);
    }

    @Test
    void decideComplaintBlockMatchesCapture() {
        stubResponses(Captures.json("decision-complaint-ok"));
        ComplaintImageAssessment assessment = Captures.MAPPER.readValue(
                Captures.outputText("assessment-complaint-ok"), ComplaintImageAssessment.class);
        RuleResult rules = new RuleResult(184, ReturnWindowStatus.NOT_APPLICABLE, false,
                EnumSet.of(DecisionCategory.ESCALATED), null, EnumSet.noneOf(DecisionCategory.class), null, "C4");
        DecisionPrompt prompt = new DecisionPrompt(CaseType.COMPLAINT,
                new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE, "Samsung Galaxy S24",
                        LocalDate.of(2026, 5, 26),
                        "Ekran ma uszkodzenie, problem pojawił się po kilku tygodniach używania."),
                OffsetDateTime.of(2026, 11, 26, 10, 0, 0, 0, ZoneOffset.UTC), assessment, AssessmentSource.LLM, rules,
                "POLICY-COMPLAINT", null, null, null);
        gateway(defaultProps()).decide(prompt);
        JsonNode body = onlyRequest();
        assertThat(body.get("instructions").asString()).startsWith(prompts.decision(CaseType.COMPLAINT));
        String capture = Captures.requestUserText("decision-complaint-ok");
        int at = capture.indexOf("\n\n## Fakty wyliczone przez system");
        assertThat(texts(body.get("input").get(0)).get(0))
                .isEqualTo(capture.substring(0, at) + "\nŹródło oceny: LLM" + capture.substring(at));
    }

    @Test
    void decideAddsRuleTextSignalsAndCorrectiveNote() {
        stubResponses(Captures.json("decision-return-ok"));
        RuleResult rules = new RuleResult(20, ReturnWindowStatus.EXPIRED, false,
                EnumSet.of(DecisionCategory.REJECTED, DecisionCategory.ESCALATED), "Termin zwrotu upłynął.",
                EnumSet.noneOf(DecisionCategory.class), null, "R2");
        DecisionPrompt base = capturePrompt();
        DecisionPrompt prompt = new DecisionPrompt(base.caseType(), base.form(), base.submittedAt(), base.assessment(),
                AssessmentSource.DECISIONS, rules, base.policyText(),
                Captures.MAPPER.readTree("{\"photo_usable\":0.91}"), Captures.MAPPER.readTree("{\"shows_declared_category\":0.55}"),
                "Dozwolone kategorie: ESCALATED.");
        gateway(defaultProps()).decide(prompt);

        List<String> texts = texts(onlyRequest().get("input").get(0));
        assertThat(texts).hasSize(2);
        assertThat(texts.get(0)).contains("Źródło oceny: DECISIONS", "Dni od zakupu: 20",
                "Dozwolone kategorie: REJECTED, ESCALATED", "Wymagany tekst uzasadnienia: Termin zwrotu upłynął.",
                "## Sygnały wstępnej analizy (probabilistyczne, do weryfikacji)", "photo_usable", "shows_declared_category");
        assertThat(texts.get(1)).isEqualTo("Dozwolone kategorie: ESCALATED.");
    }

    @Test
    void instructionsAreByteIdenticalForDifferentCasesOfTheSameType() {
        stubResponses(Captures.json("decision-return-ok"));
        OpenAiSdkLlmGateway gateway = gateway(defaultProps());
        gateway.decide(returnPrompt("Model A", "powód A", LocalDate.of(2026, 11, 1), null));
        gateway.decide(returnPrompt("Model B", "inny powód", LocalDate.of(2026, 11, 20), null));
        List<JsonNode> bodies = requestBodies();
        assertThat(bodies).hasSize(2);
        assertThat(bodies.get(0).get("instructions").asString()).isEqualTo(bodies.get(1).get("instructions").asString());
        assertThat(bodies.get(0).get("input")).isNotEqualTo(bodies.get(1).get("input"));
    }

    // ---------------------------------------------------------------- streaming

    @Test
    void streamChatPlainDeliversCapturedDeltasAndUsage() {
        stubResponses(Captures.sse("chat-plain"));
        RecordingSink sink = new RecordingSink();
        gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), sink);

        assertThat(sink.text.toString()).isEqualTo(Captures.sseDeltas("chat-plain")).isNotEmpty();
        assertThat(sink.failed).isEmpty();
        JsonNode usage = Captures.sseUsage("chat-plain");
        assertThat(sink.completed).containsExactly(
                new TokenUsage(usage.get("input_tokens").asLong(), usage.get("output_tokens").asLong()));
    }

    @Test
    void streamChatRequestLayout() {
        stubResponses(Captures.sse("chat-plain"));
        gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), new RecordingSink());

        JsonNode body = onlyRequest();
        assertThat(body.get("model").asString()).isEqualTo(DECISION_MODEL);
        assertThat(body.get("stream").asBoolean()).isTrue();
        assertThat(body.get("store").asBoolean()).isFalse();
        assertThat(body.has("previous_response_id")).isFalse();
        assertThat(body.get("reasoning").get("effort").asString()).isEqualTo("low");
        assertThat(body.get("max_output_tokens").asInt()).isEqualTo(4000);
        assertThat(body.get("instructions").asString()).isEqualTo(prompts.chat() + "\n\n# Regulamin\n\nPOLICY-RETURN-TEXT");
        assertThat(body.has("text")).isFalse();

        JsonNode input = body.get("input");
        assertThat(input).hasSize(3);
        String capture = Captures.requestUserText("chat-plain");
        int at = capture.indexOf("\n\n## Fakty wyliczone przez system");
        assertThat(texts(input.get(0)).get(0)).isEqualTo(capture.substring(0, at) + "\nŹródło oceny: LLM"
                + capture.substring(at));
        JsonNode assistant = input.get(1);
        assertThat(assistant.get("type").asString()).isEqualTo("message");
        assertThat(assistant.get("role").asString()).isEqualTo("assistant");
        assertThat(assistant.get("id").asString()).isEqualTo("msg_0");
        assertThat(assistant.get("status").asString()).isEqualTo("completed");
        assertThat(assistant.get("content").get(0).get("type").asString()).isEqualTo("output_text");
        assertThat(assistant.get("content").get(0).get("text").asString()).startsWith("Dzień dobry, dziękujemy");
        assertThat(input.get(2).get("role").asString()).isEqualTo("user");
        assertThat(input.get(2).get("content").get(0).get("type").asString()).isEqualTo("input_text");
        assertThat(input.get(2).get("content").get(0).get("text").asString()).isEqualTo("Jak mam zapakować urządzenie?");
    }

    @Test
    void streamChatDecisionUpdateIgnoresKeepAliveComments() {
        stubResponses(Captures.sse("chat-decision-update"));
        RecordingSink sink = new RecordingSink();
        gateway(defaultProps()).streamChat(chatPrompt("chat-decision-update"), sink);
        assertThat(sink.text.toString()).isEqualTo(Captures.sseDeltas("chat-decision-update"))
                .startsWith("Zaktualizowana decyzja:");
        assertThat(sink.failed).isEmpty();
        assertThat(sink.completed).hasSize(1);
    }

    @Test
    void streamChatAppendsCorrectiveNote() {
        stubResponses(Captures.sse("chat-plain"));
        ChatPrompt base = chatPrompt("chat-plain");
        gateway(defaultProps()).streamChat(
                new ChatPrompt(base.decisionPrompt(), base.currentCategory(), base.history(), "Nie wybieraj: Odrzucone."),
                new RecordingSink());
        JsonNode input = onlyRequest().get("input");
        assertThat(input).hasSize(4);
        assertThat(texts(input.get(3))).containsExactly("Nie wybieraj: Odrzucone.");
    }

    @Test
    void streamTruncatedBeforeCompletedFails() {
        stubResponses(Captures.sse("chat-plain.derived-truncated"));
        RecordingSink sink = new RecordingSink();
        gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), sink);
        assertThat(sink.failed).containsExactly(ErrorCode.LLM_FAILED);
        assertThat(sink.completed).isEmpty();
    }

    @Test
    void streamResponseFailedEventFails() {
        stubResponses(Captures.sse("chat-plain.derived-failed"));
        RecordingSink sink = new RecordingSink();
        gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), sink);
        assertThat(sink.failed).containsExactly(ErrorCode.LLM_FAILED);
        assertThat(sink.completed).isEmpty();
    }

    @Test
    void streamPreStreamAuthErrorReportsFailedWithoutThrowing() {
        stubResponses(Captures.json("chat-invalid-key"));
        RecordingSink sink = new RecordingSink();
        gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), sink);
        assertThat(sink.failed).containsExactly(ErrorCode.LLM_FAILED);
        assertThat(sink.text.toString()).isEmpty();
        assertThat(wm.getAllServeEvents()).hasSize(1);
    }

    @Test
    void streamTimeoutReportsLlmTimeout() {
        stubResponses(Captures.sse("chat-plain").withFixedDelay(2500));
        LlmProperties props = props(Duration.ofSeconds(26), Duration.ofSeconds(34), Duration.ofSeconds(1), Duration.ofSeconds(2));
        RecordingSink sink = new RecordingSink();
        gateway(props).streamChat(chatPrompt("chat-plain"), sink);
        assertThat(sink.failed).containsExactly(ErrorCode.LLM_TIMEOUT);
        assertThat(sink.completed).isEmpty();
    }

    @Test
    void streamRetriesOnceOn503BeforeAnyOutput() {
        wm.stubFor(post(urlEqualTo("/responses")).inScenario("s").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503)).willSetStateTo("second"));
        wm.stubFor(post(urlEqualTo("/responses")).inScenario("s").whenScenarioStateIs("second")
                .willReturn(Captures.sse("chat-plain")));
        RecordingSink sink = new RecordingSink();
        gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), sink);
        assertThat(wm.getAllServeEvents()).hasSize(2);
        assertThat(sink.text.toString()).isEqualTo(Captures.sseDeltas("chat-plain"));
        assertThat(sink.completed).hasSize(1);
        assertThat(sink.failed).isEmpty();
    }

    @Test
    void streamStopsWhenWorkerThreadIsInterrupted() throws Exception {
        stubResponses(Captures.sse("chat-plain").withChunkedDribbleDelay(40, 4000));
        RecordingSink sink = new RecordingSink();
        AtomicBoolean finished = new AtomicBoolean();
        Thread worker = new Thread(() -> {
            gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), sink);
            finished.set(true);
        });
        worker.start();
        Thread.sleep(1000);
        worker.interrupt();
        worker.join(5000);
        assertThat(finished).isTrue();
        assertThat(sink.completed).isEmpty();
    }

    // ---------------------------------------------------------------- errors, retries, timeouts

    @Test
    void unauthorizedCaptureIsLlmFailedWithoutRetry() {
        stubResponses(Captures.json("error-401"));
        assertThatThrownBy(() -> gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null))
                .isInstanceOf(LlmFailedException.class);
        assertThat(wm.getAllServeEvents()).hasSize(1);
    }

    @Test
    void badRequestCaptureIsLlmFailedWithoutRetry() {
        stubResponses(Captures.json("error-400"));
        assertThatThrownBy(() -> gateway(defaultProps()).decide(capturePrompt())).isInstanceOf(LlmFailedException.class);
        assertThat(wm.getAllServeEvents()).hasSize(1);
    }

    @Test
    void retriesExactlyOnceOn503ThenSucceeds() {
        // Derived: status-only 503 stub (empty body), then the real 200 capture.
        wm.stubFor(post(urlEqualTo("/responses")).inScenario("r").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503)).willSetStateTo("ok"));
        wm.stubFor(post(urlEqualTo("/responses")).inScenario("r").whenScenarioStateIs("ok")
                .willReturn(Captures.json("assessment-return-ok")));
        ImageAssessment result = gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null);
        assertThat(result).isEqualTo(returnAssessment());
        assertThat(wm.getAllServeEvents()).hasSize(2);
    }

    @Test
    void twoConsecutive503sFailWithTwoRequests() {
        stubResponses(aResponse().withStatus(503));
        assertThatThrownBy(() -> gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null))
                .isInstanceOf(LlmFailedException.class);
        assertThat(wm.getAllServeEvents()).hasSize(2);
    }

    @Test
    void noRetryWhenLessThanTenSecondsOfBudgetRemain() {
        stubResponses(aResponse().withStatus(503));
        LlmProperties props = props(Duration.ofSeconds(5), Duration.ofSeconds(34), Duration.ofSeconds(30), Duration.ofSeconds(90));
        assertThatThrownBy(() -> gateway(props).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null))
                .isInstanceOf(LlmFailedException.class);
        assertThat(wm.getAllServeEvents()).hasSize(1);
    }

    @Test
    void stepTimeoutIsLlmTimeoutException() {
        stubResponses(Captures.json("assessment-return-ok").withFixedDelay(2500));
        LlmProperties props = props(Duration.ofSeconds(1), Duration.ofSeconds(34), Duration.ofSeconds(30), Duration.ofSeconds(90));
        assertThatThrownBy(() -> gateway(props).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null))
                .isInstanceOf(LlmTimeoutException.class);
        assertThat(wm.getAllServeEvents()).hasSize(1);
    }

    @Test
    void unparseableOutputIsLlmFailed() {
        // decision capture served for an assessment call: the output JSON does not fit the assessment record.
        stubResponses(Captures.json("decision-return-ok"));
        assertThatThrownBy(() -> gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null))
                .isInstanceOf(LlmFailedException.class);
    }

    // ---------------------------------------------------------------- usage log

    @Test
    void oneUsageLogLinePerCallWithNumbersAndIdsOnly() {
        stubResponses(Captures.json("decision-return-ok"));
        gateway(defaultProps()).decide(returnPrompt(DEVICE_MODEL, REASON, LocalDate.of(2026, 11, 21), null));

        List<String> lines = usageLog.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(lines).hasSize(1);
        String line = lines.get(0);
        assertThat(line).contains("step=decision", "model=" + DECISION_MODEL, "latencyMs=", "inputTokens=", "outputTokens=",
                "requestId=gen-");
        assertThat(line).doesNotContain(DEVICE_MODEL).doesNotContain(REASON).doesNotContain("Decyzja")
                .doesNotContain("Przekazane");
        assertThat(line).matches(".*inputTokens=\\d+ outputTokens=\\d+.*");
    }

    @Test
    void usageLogForVisionAndStreaming() {
        stubResponses(Captures.json("assessment-return-ok"));
        gateway(defaultProps()).assessImage(CaseType.RETURN, EquipmentCategory.LAPTOP, JPEG, null);
        assertThat(usageLog.list).hasSize(1);
        assertThat(usageLog.list.get(0).getFormattedMessage()).contains("step=vision", "model=" + VISION_MODEL);

        wm.resetAll();
        usageLog.list.clear();
        stubResponses(Captures.sse("chat-plain"));
        gateway(defaultProps()).streamChat(chatPrompt("chat-plain"), new RecordingSink());
        assertThat(usageLog.list).hasSize(1);
        String line = usageLog.list.get(0).getFormattedMessage();
        assertThat(line).contains("step=chat", "inputTokens=2836", "outputTokens=111", "requestId=gen-");
        assertThat(line).doesNotContain("Zapakuj");
    }
}
