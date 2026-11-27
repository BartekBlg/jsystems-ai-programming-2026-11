package pl.elektrodemo.copilot.prescreen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.config.AppProperties.Decisions;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PrescreenServiceTest {

    private static final Path CAPTURES = Path.of("src/test/resources/captures/decisions");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T09:00:00Z"),
            ZoneId.of("Europe/Warsaw"));
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void runsPhotoAndConsistencyRequestsConcurrentlyAndProducesCompactSignalNodes() throws Exception {
        DecisionsClient client = mock(DecisionsClient.class);
        CountDownLatch bothStarted = new CountDownLatch(2);
        AtomicBoolean virtualThreads = new AtomicBoolean(true);
        when(client.decide(any(), any())).thenAnswer(invocation -> {
            bothStarted.countDown();
            virtualThreads.compareAndSet(true, Thread.currentThread().isVirtual());
            assertThat(bothStarted.await(2, TimeUnit.SECONDS)).isTrue();
            Map<String, PrescreenQuestionCatalog.Question> questions = invocation.getArgument(0);
            return capture(questions.containsKey("photo_usable") ? "a-complaint-phone-1.json" : "b-complaint.json");
        });
        PrescreenService service = service(client);

        PrescreenResult result = service.prescreen(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE,
                new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE, "Samsung S24",
                        LocalDate.of(2026, 5, 26), "Ekran pękł"), jpeg());

        assertThat(virtualThreads.get()).isTrue();
        assertThat(result.status()).isEqualTo(PrescreenStatus.OK);
        assertThat(result.gate()).isEqualTo(GateStatus.UNCERTAIN);
        assertThat(result.photoAnswers()).hasSize(8);
        assertThat(result.consistencySignals()).hasSize(3);
        assertThat(result.model()).isEqualTo("typesafe/jev-1.14-20261014");
        assertThat(result.cost()).isPositive();

        PrescreenContextNodes nodes = service.contextNodes(result);
        JsonNode answers = nodes.prescreen();
        JsonNode signals = nodes.consistencySignals();
        assertThat(answers.get(0).has("id")).isTrue();
        assertThat(answers.get(0).has("value")).isTrue();
        assertThat(answers.get(0).has("confidence")).isTrue();
        assertThat(signals.get(0).get("probability").doubleValue()).isEqualTo(.01);
        assertThat(signals.get(0).size()).isEqualTo(3);
    }

    @Test
    void requestBFailureDoesNotDiscardSuccessfulPhotoAnswers() throws IOException {
        DecisionsClient client = mock(DecisionsClient.class);
        when(client.decide(any(), any())).thenAnswer(invocation -> {
            Map<String, PrescreenQuestionCatalog.Question> questions = invocation.getArgument(0);
            if (questions.containsKey("photo_usable")) {
                return capture("a-return-laptop-2.json");
            }
            throw new PrescreenUnavailableException("synthetic B failure");
        });

        PrescreenResult result = service(client).prescreen(CaseType.RETURN, EquipmentCategory.LAPTOP,
                new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "ThinkPad", LocalDate.of(2026, 11, 21), ""),
                jpeg());

        assertThat(result.status()).isEqualTo(PrescreenStatus.OK);
        assertThat(result.photoAnswers()).isNotEmpty();
        assertThat(result.consistencySignals()).isEmpty();
    }

    @Test
    void requestAFailureReturnsUnavailableInsteadOfThrowing() {
        DecisionsClient client = mock(DecisionsClient.class);
        when(client.decide(any(), any())).thenThrow(new PrescreenUnavailableException("http error"));

        PrescreenResult result = service(client).prescreen(CaseType.RETURN, EquipmentCategory.LAPTOP,
                new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "ThinkPad", LocalDate.of(2026, 11, 21), ""),
                new byte[] {1, 2, 3});

        assertThat(result.status()).isEqualTo(PrescreenStatus.UNAVAILABLE);
        assertThat(result.gate()).isEqualTo(GateStatus.UNCERTAIN);
        assertThat(result.gateReasons()).contains("http error");
    }

    @Test
    void contextSignalsRoundProbabilitiesToTwoDecimalPlaces() {
        PrescreenService service = service(mock(DecisionsClient.class));
        PrescreenResult result = new PrescreenResult(PrescreenStatus.OK, List.of(),
                List.of(new PrescreenAnswer("reason_contains_instructions", "noul", false, .126, .874, null, false)),
                GateStatus.CONFIDENT, List.of(), "catalog-v1", 1, 0, "model", null);

        PrescreenContextNodes nodes = service.contextNodes(result);

        assertThat(nodes.consistencySignals().get(0).get("probability").doubleValue()).isEqualTo(.13);
        assertThat(nodes.consistencySignals().get(0).get("confidence").doubleValue()).isEqualTo(.874);
    }

    private PrescreenService service(DecisionsClient client) {
        return new PrescreenService(client, new PrescreenQuestionCatalog(), new ConfidenceGate(),
                new AppProperties("test-signing-key", null,
                        new Decisions("shadow", "typesafe/jev-1.14", "http://localhost/decisions", .8,
                                java.time.Duration.ofSeconds(6))), CLOCK, mapper);
    }

    private DecisionsResponse capture(String filename) throws IOException {
        JsonNode envelope = mapper.readTree(Files.readString(CAPTURES.resolve(filename)));
        return mapper.treeToValue(envelope.get("body"), DecisionsResponse.class);
    }

    private static byte[] jpeg() throws IOException {
        BufferedImage image = new BufferedImage(280, 280, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", output);
        return output.toByteArray();
    }
}
