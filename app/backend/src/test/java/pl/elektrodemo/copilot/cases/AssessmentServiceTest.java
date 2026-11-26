package pl.elektrodemo.copilot.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;
import pl.elektrodemo.copilot.prescreen.ConfidenceGate;
import pl.elektrodemo.copilot.prescreen.DecisionsClient;
import pl.elektrodemo.copilot.prescreen.DecisionsResponse;
import pl.elektrodemo.copilot.prescreen.PrescreenAssessmentMapper;
import pl.elektrodemo.copilot.prescreen.PrescreenQuestionCatalog;
import pl.elektrodemo.copilot.prescreen.PrescreenService;
import pl.elektrodemo.copilot.prescreen.PrescreenUnavailableException;
import pl.elektrodemo.copilot.prescreen.ShadowComparator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.image.ImageDownscaler;
import pl.elektrodemo.copilot.image.ImageTypeDetector;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.llm.LlmTimeoutException;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.support.CaptureFixtures;
import pl.elektrodemo.copilot.web.UnsupportedImageException;
import pl.elektrodemo.copilot.web.dto.AssessmentResponse;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.llm.LlmProperties;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceTest {

    // 32 bytes, Base64: test-only key, not a secret
    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3BxcnN0dXY=";
    private static final Path EXAMPLES = Path.of("../../assets/example-images");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T09:00:00Z"), ZoneId.of("Europe/Warsaw"));
    private static final OffsetDateTime SUBMITTED = OffsetDateTime.parse("2026-11-26T10:00:00+01:00");

    @Mock
    LlmGateway llm;

    private CaseSigner signer;
    private PolicyRuleEngine engine;
    private AssessmentService service;

    @BeforeEach
    void setUp() {
        signer = new CaseSigner(new AppProperties(TEST_KEY, null, null));
        engine = new PolicyRuleEngine();
        service = new AssessmentService(new ImageTypeDetector(), new ImageDownscaler(), CLOCK, llm, engine, signer);
    }

    /** Synthetic laptop form; purchase 2026-11-21 gives 5 days before submission (rule R4). */
    private static CaseFormDto laptopReturnForm() {
        return new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "  Lenovo ThinkPad T14 Gen 4 ",
                LocalDate.of(2026, 11, 21), "  ");
    }

    @Test
    void returnAssessmentFromCaptureIsSignedLlmContextV1() throws IOException {
        when(llm.assessImage(eq(CaseType.RETURN), eq(EquipmentCategory.LAPTOP), any(byte[].class), isNull()))
                .thenReturn(CaptureFixtures.returnAssessmentOk());

        AssessmentResponse response = service.assess(laptopReturnForm(), read("laptop-2.webp"));

        CaseContextDto ctx = response.caseContext();
        assertThat(ctx.contextVersion()).isEqualTo(1);
        assertThat(ctx.caseType()).isEqualTo(CaseType.RETURN);
        assertThat(ctx.assessmentSource()).isEqualTo(AssessmentSource.LLM);
        assertThat(ctx.submittedAt()).isEqualTo(SUBMITTED);
        assertThat(ctx.assessment()).isEqualTo(CaptureFixtures.returnAssessmentOk());
        assertThat(ctx.prescreen()).isNull();
        assertThat(ctx.consistencySignals()).isNull();
        assertThat(ctx.decision()).isNull();
        // form is trimmed by CaseFormNormalizer
        assertThat(ctx.form().modelName()).isEqualTo("Lenovo ThinkPad T14 Gen 4");
        assertThat(ctx.form().reason()).isEmpty();
        // capture: signsOfUse=[WEAR] -> damaged; 5 days -> R4 -> {ESCALATED}
        assertThat(ctx.ruleFacts().appliedRule()).isEqualTo("R4");
        assertThat(ctx.ruleFacts().allowedCategories()).isEqualTo(Set.of(DecisionCategory.ESCALATED));
        assertThat(ctx.ruleFacts().daysSincePurchase()).isEqualTo(5);
        assertThat(ctx.ruleFacts().returnWindowStatus()).isEqualTo(ReturnWindowStatus.WITHIN_14_DAYS);
        assertThat(signer.verify(ctx, response.signature())).isTrue();
    }

    @Test
    void gatewayReceivesJpegWithinBoundAndNoPreliminarySignals() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), any())).thenReturn(CaptureFixtures.returnAssessmentOk());

        service.assess(laptopReturnForm(), read("laptop-2.webp"));

        ArgumentCaptor<byte[]> jpeg = ArgumentCaptor.forClass(byte[].class);
        verify(llm).assessImage(eq(CaseType.RETURN), eq(EquipmentCategory.LAPTOP), jpeg.capture(), isNull());
        byte[] bytes = jpeg.getValue();
        assertThat(bytes[0] & 0xFF).isEqualTo(0xFF);
        assertThat(bytes[1] & 0xFF).isEqualTo(0xD8);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(Math.max(img.getWidth(), img.getHeight())).isLessThanOrEqualTo(1536);
    }

    @Test
    void gifBytesAreUnsupportedAndGatewayIsNeverCalled() {
        byte[] gif = "GIF89a\0\0\0\0\0\0".getBytes(StandardCharsets.ISO_8859_1);

        assertThatThrownBy(() -> service.assess(laptopReturnForm(), gif))
                .isInstanceOf(UnsupportedImageException.class);
        verifyNoInteractions(llm);
    }

    @Test
    void gatewayTimeoutPropagatesUnchanged() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), any()))
                .thenThrow(new LlmTimeoutException("timeout"));

        assertThatThrownBy(() -> service.assess(laptopReturnForm(), read("laptop-2.webp")))
                .isInstanceOf(LlmTimeoutException.class);
    }

    // ------------------------------------------------------------ T25 modes (captures-only Decisions answers)

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private AssessmentService modeService(String mode, DecisionsClient client) {
        AppProperties props = new AppProperties(TEST_KEY, null,
                new AppProperties.Decisions(mode, "typesafe/jev-1.14", "http://localhost/decisions", .8,
                        java.time.Duration.ofSeconds(6)));
        PrescreenService prescreen = new PrescreenService(client, new PrescreenQuestionCatalog(), new ConfidenceGate(),
                props, CLOCK, MAPPER);
        PrescreenAssessmentMapper mapper = new PrescreenAssessmentMapper(props);
        return new AssessmentService(new ImageTypeDetector(), new ImageDownscaler(), CLOCK, llm, engine, signer,
                prescreen, mapper, new ShadowComparator(mapper), props);
    }

    private static DecisionsResponse capture(String name) throws IOException {
        JsonNode envelope = MAPPER.readTree(Files.readString(Path.of("src/test/resources/captures/decisions/" + name)));
        return MAPPER.treeToValue(envelope.get("body"), DecisionsResponse.class);
    }

    private DecisionsClient clientServing(String a, String b) throws IOException {
        DecisionsClient client = mock(DecisionsClient.class);
        DecisionsResponse responseA = capture(a);
        DecisionsResponse responseB = capture(b);
        lenient().when(client.decide(any(), any())).thenAnswer(invocation -> {
            java.util.Map<?, ?> questions = invocation.getArgument(0);
            return questions.containsKey("photo_usable") ? responseA : responseB;
        });
        return client;
    }

    private static CaseFormDto phoneComplaintForm() {
        return new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE, "Smartfon testowy",
                LocalDate.of(2026, 11, 21), "Pęknięty ekran po upadku z biurka");
    }

    /** b-return.json (model match 0.72) is used because the captured b-complaint.json is a confident MODEL_MISMATCH. */
    @Test
    void gatedConfidentCaptureSkipsVisionLlmAndRulesApplyToMappedAssessment() throws IOException {
        AssessmentService gated = modeService("gated", clientServing("a-complaint-phone-1.json", "b-return.json"));

        CaseContextDto ctx = gated.assess(phoneComplaintForm(), read("phone-1.jpg")).caseContext();

        verifyNoInteractions(llm);
        assertThat(ctx.assessmentSource()).isEqualTo(AssessmentSource.DECISIONS);
        assertThat(ctx.prescreen()).isNotNull();
        assertThat(ctx.consistencySignals()).isNotNull();
        assertThat(((ComplaintImageAssessment) ctx.assessment()).probableCause())
                .isEqualTo(ProbableCause.MECHANICAL_DAMAGE);
        assertThat(ctx.ruleFacts().allowedCategories()).doesNotContain(DecisionCategory.ACCEPTED);
    }

    @Test
    void gatedUncertainPassesRequestASignalsOnlyAndKeepsPrescreen() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), any(), any(Duration.class)))
                .thenReturn(CaptureFixtures.returnAssessmentOk());
        AssessmentService gated = modeService("gated", clientServing("a-return-laptop-2.json", "b-return.json"));

        CaseContextDto ctx = gated.assess(laptopReturnForm(), read("laptop-2.webp")).caseContext();

        ArgumentCaptor<String> signals = ArgumentCaptor.forClass(String.class);
        verify(llm).assessImage(eq(CaseType.RETURN), eq(EquipmentCategory.LAPTOP), any(byte[].class), signals.capture(),
                any(Duration.class));
        assertThat(signals.getValue()).startsWith("Wstępne sygnały do weryfikacji: ").contains("photo_usable=")
                .contains("(prawdopodobieństwo 0.78)").doesNotContain("typesafe").doesNotContain("Lenovo")
                .doesNotContain("model_matches_photo");
        assertThat(ctx.assessmentSource()).isEqualTo(AssessmentSource.LLM);
        assertThat(ctx.prescreen()).isNotNull();
    }

    @Test
    void gatedUnavailableFailsOpenToPlainVisionLlm() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), isNull(), any(Duration.class))).thenReturn(CaptureFixtures.returnAssessmentOk());
        DecisionsClient client = mock(DecisionsClient.class);
        when(client.decide(any(), any())).thenThrow(new PrescreenUnavailableException("HTTP_503"));

        CaseContextDto ctx = modeService("gated", client).assess(laptopReturnForm(), read("laptop-2.webp"))
                .caseContext();

        assertThat(ctx.assessmentSource()).isEqualTo(AssessmentSource.LLM);
        assertThat(ctx.prescreen()).isNull();
        assertThat(ctx.consistencySignals()).isNull();
    }

    // ------------------------------------------------------------ T25-fix-1: vision budget after pre-screening

    /** Test clock advanced by the simulated pre-screen; the service measures elapsed time through it. */
    private static final class MutableClock extends Clock {
        private Instant now = CLOCK.instant();

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public java.time.ZoneId getZone() {
            return CLOCK.getZone();
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private AssessmentService gatedWithClock(MutableClock clock, DecisionsClient client) {
        AppProperties props = new AppProperties(TEST_KEY, null,
                new AppProperties.Decisions("gated", "typesafe/jev-1.14", "http://localhost/decisions", .8,
                        Duration.ofSeconds(6)));
        PrescreenService prescreen = new PrescreenService(client, new PrescreenQuestionCatalog(), new ConfidenceGate(),
                props, CLOCK, MAPPER);
        PrescreenAssessmentMapper mapper = new PrescreenAssessmentMapper(props);
        LlmProperties llmProps = new LlmProperties("sk-test-fake", "http://localhost", "m", "v", "low", "low",
                new LlmProperties.Timeouts(Duration.ofSeconds(26), Duration.ofSeconds(34), Duration.ofSeconds(30),
                        Duration.ofSeconds(90)),
                new LlmProperties.MaxOutputTokens(4000, 8000, 4000));
        return new AssessmentService(new ImageTypeDetector(), new ImageDownscaler(), clock, llm, engine, signer,
                prescreen, mapper, new ShadowComparator(mapper), props, llmProps);
    }

    private Duration visionTimeoutAfter(Duration prescreenTime, DecisionsClient serving) throws IOException {
        MutableClock clock = new MutableClock();
        DecisionsClient slow = mock(DecisionsClient.class);
        java.util.concurrent.atomic.AtomicBoolean first = new java.util.concurrent.atomic.AtomicBoolean(true);
        lenient().when(slow.decide(any(), any())).thenAnswer(invocation -> {
            if (first.getAndSet(false)) {
                clock.advance(prescreenTime);
            }
            return serving.decide(invocation.getArgument(0), invocation.getArgument(1));
        });
        when(llm.assessImage(any(), any(), any(byte[].class), any(), any(Duration.class)))
                .thenReturn(CaptureFixtures.returnAssessmentOk());
        gatedWithClock(clock, slow).assess(laptopReturnForm(), read("laptop-2.webp"));
        ArgumentCaptor<Duration> timeout = ArgumentCaptor.forClass(Duration.class);
        verify(llm).assessImage(any(), any(), any(byte[].class), any(), timeout.capture());
        return timeout.getValue();
    }

    @Test
    void gatedUnavailableAfterSixSecondPrescreenGivesVisionTheTwentySecondFloor() throws IOException {
        DecisionsClient failing = mock(DecisionsClient.class);
        when(failing.decide(any(), any())).thenThrow(new PrescreenUnavailableException("HTTP_503"));

        assertThat(visionTimeoutAfter(Duration.ofSeconds(6), failing)).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void gatedUnavailableAfterNineSecondPrescreenStillGetsTheTwentySecondFloor() throws IOException {
        DecisionsClient failing = mock(DecisionsClient.class);
        when(failing.decide(any(), any())).thenThrow(new PrescreenUnavailableException("HTTP_503"));

        assertThat(visionTimeoutAfter(Duration.ofSeconds(9), failing)).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void gatedUncertainAfterOneSecondPrescreenGivesVisionAboutTwentyFiveSeconds() throws IOException {
        Duration timeout = visionTimeoutAfter(Duration.ofSeconds(1),
                clientServing("a-return-laptop-2.json", "b-return.json"));

        assertThat(timeout).isBetween(Duration.ofMillis(24_500), Duration.ofMillis(25_000));
    }

    @Test
    void shadowKeepsTheOriginalFourArgumentVisionCall() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), isNull())).thenReturn(CaptureFixtures.returnAssessmentOk());

        modeService("shadow", clientServing("a-return-laptop-2.json", "b-return.json")).assess(laptopReturnForm(),
                read("laptop-2.webp"));

        verify(llm).assessImage(any(), any(), any(byte[].class), isNull());
        verify(llm, never()).assessImage(any(), any(), any(byte[].class), any(), any(Duration.class));
    }

    @Test
    void shadowKeepsLlmAssessmentWithoutSignalsToTheVisionLlm() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), isNull())).thenReturn(CaptureFixtures.returnAssessmentOk());
        AssessmentService shadow = modeService("shadow", clientServing("a-return-laptop-2.json", "b-return.json"));

        CaseContextDto ctx = shadow.assess(laptopReturnForm(), read("laptop-2.webp")).caseContext();

        assertThat(ctx.assessmentSource()).isEqualTo(AssessmentSource.LLM);
        assertThat(ctx.assessment()).isEqualTo(CaptureFixtures.returnAssessmentOk());
        assertThat(ctx.prescreen()).isNull();
        assertThat(ctx.consistencySignals()).isNotNull();
        verify(llm, times(1)).assessImage(any(), any(), any(byte[].class), isNull());
    }

    @Test
    void shadowLlmFailurePropagatesAndPrescreenFailureIsIgnored() throws IOException {
        DecisionsClient client = mock(DecisionsClient.class);
        when(client.decide(any(), any())).thenThrow(new PrescreenUnavailableException("HTTP_503"));
        when(llm.assessImage(any(), any(), any(byte[].class), any())).thenThrow(new LlmTimeoutException("timeout"));

        assertThatThrownBy(() -> modeService("shadow", client).assess(laptopReturnForm(), read("laptop-2.webp")))
                .isInstanceOf(LlmTimeoutException.class);
    }

    @Test
    void llmOnlyModeNeverTouchesDecisions() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), isNull())).thenReturn(CaptureFixtures.returnAssessmentOk());
        DecisionsClient client = mock(DecisionsClient.class);

        CaseContextDto ctx = modeService("llm-only", client).assess(laptopReturnForm(), read("laptop-2.webp"))
                .caseContext();

        verify(client, never()).decide(any(), any());
        assertThat(ctx.assessmentSource()).isEqualTo(AssessmentSource.LLM);
        assertThat(ctx.prescreen()).isNull();
        assertThat(ctx.consistencySignals()).isNull();
    }

    private static byte[] read(String name) throws IOException {
        return Files.readAllBytes(EXAMPLES.resolve(name));
    }
}
