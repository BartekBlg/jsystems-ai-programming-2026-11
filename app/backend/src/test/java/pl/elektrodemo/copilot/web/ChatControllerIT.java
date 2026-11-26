package pl.elektrodemo.copilot.web;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.elektrodemo.copilot.support.ProblemAssertions.assertProblem;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pl.elektrodemo.copilot.cases.AssessmentSource;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.cases.ReturnWindowStatus;
import pl.elektrodemo.copilot.cases.RuleResult;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.support.CaptureFixtures;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.dto.DecisionRef;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * T23 / ADR-001 3.5, 5.3, TAC-001-04: the chat SSE endpoint end-to-end on a real embedded server, with
 * OpenRouter replaced by WireMock serving the T04 captures (and their documented derivations) verbatim.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatControllerIT {

    private static final String CHAT = "/api/chat/messages";
    private static final WireMockServer WM = ApiTestSupport.startWireMock();
    private static final Set<String> SKIPPED = Set.of(":status", "content-length", "content-encoding",
            "transfer-encoding", "connection", "date");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("llm.base-url", WM::baseUrl);
        r.add("llm.api-key", () -> "sk-test-fake");
        r.add("app.rate-limit.chat-per-hour", () -> "1000");
        r.add("app.chat.heartbeat-interval", () -> "300ms");
    }

    @LocalServerPort
    int port;

    @Autowired
    CaseSigner signer;

    @Autowired
    JsonMapper mapper;

    @Autowired
    ChatController controller;

    private final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private ListAppender<ILoggingEvent> appLog;

    @BeforeEach
    void setUp() {
        WM.resetAll();
        appLog = new ListAppender<>();
        appLog.start();
        ((Logger) LoggerFactory.getLogger("pl.elektrodemo")).addAppender(appLog);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("pl.elektrodemo")).detachAppender(appLog);
    }

    @AfterAll
    static void stop() {
        WM.stop();
    }

    // ------------------------------------------------------------------ stream

    @Test
    void plainReplyStreamsDeltasInOrderAndEndsWithExactlyOneDone() throws Exception {
        stub(sse("chat-plain.sse"));

        HttpResponse<String> response = postChat(validRequest());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/event-stream");
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-cache");
        assertThat(response.headers().firstValue("X-Accel-Buffering")).contains("no");
        Sse sse = Sse.parse(response.body());
        assertThat(sse.names()).doesNotContain("decision", "error").endsWith("done");
        assertThat(sse.names().stream().filter(n -> n.equals("done") || n.equals("error"))).hasSize(1);
        assertThat(sse.names().get(0)).isEqualTo("delta");
        assertThat(sse.deltaText()).isEqualTo(String.join("", deltas("chat-plain.sse")));
        assertThat(sse.last().data().get("category").asString()).isEqualTo("MORE_INFO_NEEDED");
        assertThat(WM.getAllServeEvents()).hasSize(1);
    }

    @Test
    void decisionUpdateStreamStartsWithTheDecisionEvent() throws Exception {
        stub(sse("chat-decision-update.sse"));

        Sse sse = Sse.parse(postChat(validRequest()).body());

        assertThat(sse.events().get(0).name()).isEqualTo("decision");
        assertThat(sse.events().get(0).data().toString()).isEqualTo("{\"category\":\"ESCALATED\"}");
        assertThat(sse.events().get(1).name()).isEqualTo("delta");
        assertThat(sse.events().get(1).data().get("text").asString()).startsWith("Zaktualizowana decyzja:");
        assertThat(sse.names().stream().filter("decision"::equals)).hasSize(1);
        assertThat(sse.deltaText()).isEqualTo(String.join("", deltas("chat-decision-update.sse")));
        assertThat(sse.last().name()).isEqualTo("done");
        assertThat(sse.last().data().toString()).isEqualTo("{\"category\":\"ESCALATED\"}");
    }

    @Test
    void forbiddenCategoryIsRegeneratedOnceAndNeverReachesTheClient() throws Exception {
        WM.stubFor(post(urlEqualTo("/responses")).inScenario("forbidden").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(sse("chat-decision-update.derived-forbidden.sse")).willSetStateTo("regenerated"));
        WM.stubFor(post(urlEqualTo("/responses")).inScenario("forbidden").whenScenarioStateIs("regenerated")
                .willReturn(sse("chat-plain.sse")));

        HttpResponse<String> response = postChat(validRequest());

        Sse sse = Sse.parse(response.body());
        assertThat(response.body()).doesNotContain("Odrzucone").doesNotContain("Zaktualizowana");
        assertThat(sse.names()).doesNotContain("decision", "error").endsWith("done");
        assertThat(sse.deltaText()).isEqualTo(String.join("", deltas("chat-plain.sse")));
        assertThat(WM.getAllServeEvents()).hasSize(2);
        List<String> bodies = WM.getAllServeEvents().stream().map(e -> e.getRequest().getBodyAsString()).toList();
        assertThat(bodies.stream().filter(b -> b.contains("niedozwolona w tej rozmowie"))).hasSize(1);
    }

    @Test
    void derivedFailedStreamEndsWithErrorEventAfterHttp200() throws Exception {
        stub(sse("chat-plain.derived-failed.sse"));

        HttpResponse<String> response = postChat(validRequest());

        assertThat(response.statusCode()).isEqualTo(200);
        Sse sse = Sse.parse(response.body());
        assertThat(sse.names()).doesNotContain("done");
        assertThat(sse.names().stream().filter("error"::equals)).hasSize(1);
        assertThat(sse.last().name()).isEqualTo("error");
        assertThat(sse.last().data().toString()).isEqualTo("{\"code\":\"LLM_FAILED\"}");
    }

    @Test
    void invalidKeyCaptureBecomesAnErrorEventNotAnHttpError() throws Exception {
        stub(ApiTestSupport.capture("responses", "chat-invalid-key"));

        HttpResponse<String> response = postChat(validRequest());

        assertThat(response.statusCode()).isEqualTo(200);
        Sse sse = Sse.parse(response.body());
        assertThat(sse.names()).containsExactly("error");
        assertThat(sse.last().data().toString()).isEqualTo("{\"code\":\"LLM_FAILED\"}");
        assertThat(response.body()).doesNotContain("User not found").doesNotContain("sk-test-fake");
    }

    @Test
    void heartbeatCommentArrivesWhileWaitingForTheFirstToken() throws Exception {
        stub(sse("chat-plain.sse").withFixedDelay(1500));

        String body = postChat(validRequest()).body();

        Sse sse = Sse.parse(body);
        assertThat(sse.comments()).isNotEmpty();
        assertThat(body.indexOf(":heartbeat")).isGreaterThanOrEqualTo(0).isLessThan(body.indexOf("event:delta"));
        assertThat(sse.last().name()).isEqualTo("done");
    }

    @Test
    void clientDisconnectMidStreamEndsTheRelayWithoutErrors() throws Exception {
        stub(sse("chat-plain.sse").withChunkedDribbleDelay(40, 12_000));
        long start = System.nanoTime();

        HttpResponse<InputStream> response = http.send(jsonPost(validRequest()),
                HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
        assertThat(reader.readLine()).isNotNull();
        assertThat(controller.activeStreams()).isEqualTo(1);
        reader.close();

        await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> assertThat(controller.activeStreams()).isZero());
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(10));
        assertThat(appLog.list).filteredOn(e -> e.getLevel().isGreaterOrEqual(Level.ERROR)).isEmpty();
    }

    // ------------------------------------------------------------ pre-stream

    @Test
    void tamperedContextIsProblemJson401AndNeverStreams() throws Exception {
        ObjectNode request = validRequest();
        ((ObjectNode) request.at("/caseContext/ruleFacts")).put("appliedRule", "R1");

        HttpResponse<String> response = postChat(request);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("application/problem+json");
        assertProblem(response.body(), 401, "INVALID_CASE_SIGNATURE");
        assertThat(WM.getAllServeEvents()).isEmpty();
    }

    @Test
    void invalidHistoryIsProblemJson400WithFieldErrors() throws Exception {
        ObjectNode endsWithAssistant = validRequest();
        ((ArrayNode) endsWithAssistant.get("messages")).addObject().put("id", "m2").put("role", "assistant")
                .put("text", "x");
        ObjectNode longUserText = validRequest();
        ((ObjectNode) longUserText.get("messages").get(1)).put("text", "x".repeat(2001));

        HttpResponse<String> last = postChat(endsWithAssistant);
        HttpResponse<String> user = postChat(longUserText);

        assertThat(last.statusCode()).isEqualTo(400);
        JsonNode lastProblem = assertProblem(last.body(), 400, "VALIDATION_FAILED");
        assertThat(lastProblem.at("/fieldErrors/0/field").asString()).isEqualTo("messages");
        assertThat(lastProblem.at("/fieldErrors/0/message").asString()).isEqualTo("Nieprawidłowa historia rozmowy.");
        assertThat(user.statusCode()).isEqualTo(400);
        JsonNode userProblem = assertProblem(user.body(), 400, "VALIDATION_FAILED");
        assertThat(userProblem.at("/fieldErrors/0/field").asString()).isEqualTo("messages[1].text");
        assertThat(userProblem.at("/fieldErrors/0/message").asString())
                .isEqualTo("Wiadomość musi mieć od 1 do 2000 znaków.");
        assertThat(WM.getAllServeEvents()).isEmpty();
    }

    @Test
    void contextWithoutDecisionIsProblemJson400() throws Exception {
        ObjectNode request = validRequest(null);

        HttpResponse<String> response = postChat(request);

        assertThat(response.statusCode()).isEqualTo(400);
        assertProblem(response.body(), 400, "VALIDATION_FAILED");
        assertThat(WM.getAllServeEvents()).isEmpty();
    }

    // ---------------------------------------------------------------- helpers

    private void stub(ResponseDefinitionBuilder response) {
        WM.stubFor(post(urlEqualTo("/responses")).willReturn(response));
    }

    private ObjectNode validRequest() {
        return validRequest(DecisionCategory.MORE_INFO_NEEDED);
    }

    /** Signed with the real signer: RETURN case from the captured assessment, REJECTED forbidden in chat. */
    private ObjectNode validRequest(DecisionCategory decision) {
        CaseContextDto ctx = new CaseContextDto(2, CaseType.RETURN,
                new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "Lenovo ThinkPad T14 Gen 4",
                        LocalDate.of(2026, 11, 21), ""),
                OffsetDateTime.parse("2026-11-26T10:00:00+01:00"), CaptureFixtures.returnAssessmentOk(),
                AssessmentSource.LLM, null, null,
                new RuleResult(5, ReturnWindowStatus.WITHIN_14_DAYS, false,
                        Set.of(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED), "",
                        Set.of(DecisionCategory.REJECTED), "", "R5"),
                decision == null ? null : new DecisionRef(decision));
        ObjectNode request = mapper.createObjectNode();
        request.set("caseContext", mapper.valueToTree(ctx));
        request.put("signature", signer.sign(ctx));
        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("id", "m0").put("role", "assistant")
                .put("text", CaptureFixtures.decisionReturnOk().message());
        messages.addObject().put("id", "m1").put("role", "user").put("text", "Urządzenie zostało dostarczone 3 dni temu.");
        return request;
    }

    private HttpRequest jsonPost(ObjectNode body) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + CHAT))
                .header("Content-Type", "application/json").header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
    }

    private HttpResponse<String> postChat(ObjectNode body) throws IOException, InterruptedException {
        return http.send(jsonPost(body), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** A recorded stream served verbatim ({@code <file>} + the headers of its source capture). */
    private static ResponseDefinitionBuilder sse(String file) {
        String stem = file.substring(0, file.length() - ".sse".length());
        String headerStem = stem.contains(".derived-") ? stem.substring(0, stem.indexOf(".derived-")) : stem;
        JsonNode headers = ApiTestSupport.MAPPER.readTree(read("captures/responses/" + headerStem + ".headers.json"));
        ResponseDefinitionBuilder builder = new ResponseDefinitionBuilder().withStatus(headers.get("status").asInt())
                .withBody(read("captures/responses/" + file));
        headers.get("headers").properties().forEach(e -> {
            if (!SKIPPED.contains(e.getKey().toLowerCase(Locale.ROOT))) {
                List<String> values = new ArrayList<>();
                e.getValue().forEach(v -> values.add(v.asString()));
                builder.withHeader(e.getKey(), values.toArray(String[]::new));
            }
        });
        return builder;
    }

    private static byte[] read(String resource) {
        return ApiTestSupport.read(resource);
    }

    private static List<String> deltas(String file) {
        List<String> out = new ArrayList<>();
        for (String line : new String(read("captures/responses/" + file), StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith("data: {")) {
                JsonNode event = ApiTestSupport.MAPPER.readTree(line.substring(6));
                if ("response.output_text.delta".equals(event.path("type").asString())) {
                    out.add(event.get("delta").asString());
                }
            }
        }
        return out;
    }

    /** Minimal SSE reader: blocks separated by a blank line; {@code :} lines are comments. */
    private record Sse(List<Event> events, List<String> comments) {

        record Event(String name, JsonNode data) {
        }

        static Sse parse(String body) {
            List<Event> events = new ArrayList<>();
            List<String> comments = new ArrayList<>();
            for (String block : body.split("\r?\n\r?\n")) {
                String name = null;
                String data = null;
                for (String line : block.split("\r?\n")) {
                    if (line.startsWith(":")) {
                        comments.add(line);
                    } else if (line.startsWith("event:")) {
                        name = line.substring(6).strip();
                    } else if (line.startsWith("data:")) {
                        data = line.substring(5).strip();
                    }
                }
                if (name != null) {
                    events.add(new Event(name, ApiTestSupport.MAPPER.readTree(data)));
                }
            }
            return new Sse(events, comments);
        }

        List<String> names() {
            return events.stream().map(Event::name).toList();
        }

        Event last() {
            return events.get(events.size() - 1);
        }

        String deltaText() {
            StringBuilder sb = new StringBuilder();
            events.stream().filter(e -> e.name().equals("delta")).forEach(e -> sb.append(e.data().get("text").asString()));
            return sb.toString();
        }
    }
}
