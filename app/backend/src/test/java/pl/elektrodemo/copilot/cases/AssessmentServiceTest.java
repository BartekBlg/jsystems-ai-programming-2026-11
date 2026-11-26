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
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Set;
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

    private static byte[] read(String name) throws IOException {
        return Files.readAllBytes(EXAMPLES.resolve(name));
    }
}
