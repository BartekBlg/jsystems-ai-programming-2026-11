package pl.elektrodemo.copilot.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.image.ImageDownscaler;
import pl.elektrodemo.copilot.image.ImageTypeDetector;
import pl.elektrodemo.copilot.llm.DecisionPrompt;
import pl.elektrodemo.copilot.llm.LlmFailedException;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.llm.LlmTimeoutException;
import pl.elektrodemo.copilot.policy.PolicyRepository;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.support.CaptureFixtures;
import pl.elektrodemo.copilot.web.DecisionAlreadyMadeException;
import pl.elektrodemo.copilot.web.InvalidCaseSignatureException;
import pl.elektrodemo.copilot.web.dto.AssessmentResponse;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.dto.DecisionRef;
import pl.elektrodemo.copilot.web.dto.DecisionRequest;
import pl.elektrodemo.copilot.web.dto.DecisionResponse;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    // 32 bytes, Base64: test-only key, not a secret
    private static final String TEST_KEY = "MDEyMzQ1Njc4OWFiY2RlZmdoaWprbG1ub3BxcnN0dXY=";
    private static final Path EXAMPLES = Path.of("../../assets/example-images");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-26T09:00:00Z"), ZoneId.of("Europe/Warsaw"));
    // Literal copies of the canonical texts (ADR-002 3.7), not read from the production constants.
    private static final String DISCLAIMER = "To wstępna, automatyczna ocena przygotowana na podstawie formularza "
            + "i zdjęcia. Ostateczną decyzję podejmie nasz zespół serwisu po otrzymaniu i sprawdzeniu urządzenia.";
    private static final String RETURN_EXTRA = "Nie ogranicza to Twojego ustawowego prawa do odstąpienia od umowy.";
    private static final String CORRECTIVE_PREFIX = "Poprzednia odpowiedź była niezgodna z wymaganiami: ";

    @Mock
    LlmGateway llm;

    private CaseSigner signer;
    private PolicyRepository policies;
    private AssessmentService assessment;
    private DecisionService service;

    @BeforeEach
    void setUp() {
        signer = new CaseSigner(new AppProperties(TEST_KEY, null, null));
        policies = new PolicyRepository(new DefaultResourceLoader());
        assessment = new AssessmentService(new ImageTypeDetector(), new ImageDownscaler(), CLOCK, llm,
                new PolicyRuleEngine(), signer);
        service = new DecisionService(llm, new DecisionMessageValidator(), policies, signer);
    }

    /** Real pipeline step 1 with the captured return assessment; purchase 2026-11-21 -> R4 -> {ESCALATED}. */
    private AssessmentResponse returnContext() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), isNull()))
                .thenReturn(CaptureFixtures.returnAssessmentOk());
        CaseFormDto form = new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "Lenovo ThinkPad T14 Gen 4",
                LocalDate.of(2026, 11, 21), "");
        return assessment.assess(form, Files.readAllBytes(EXAMPLES.resolve("laptop-2.webp")));
    }

    /** Complaint context from the captured complaint assessment: MECHANICAL_DAMAGE, MEDIUM -> C4 -> {ESCALATED}. */
    private AssessmentResponse complaintContext() throws IOException {
        when(llm.assessImage(any(), any(), any(byte[].class), isNull()))
                .thenReturn(CaptureFixtures.complaintAssessmentOk());
        CaseFormDto form = new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE, "Smartfon testowy",
                LocalDate.of(2026, 11, 21), "Pęknięty ekran");
        return assessment.assess(form, Files.readAllBytes(EXAMPLES.resolve("phone-2.jpeg")));
    }

    private DecisionResponse decide(CaseContextDto ctx, String signature) {
        return service.decide(new DecisionRequest(ctx, signature));
    }

    @Test
    void validReturnDecisionIsNormalizedSignedAndCalledOnce() throws IOException {
        AssessmentResponse first = returnContext();
        when(llm.decide(any(DecisionPrompt.class))).thenReturn(CaptureFixtures.decisionReturnOk());

        DecisionResponse response = decide(first.caseContext(), first.signature());

        assertThat(response.decision().category()).isEqualTo(DecisionCategory.ESCALATED);
        assertThat(response.decision().message()).endsWith(DISCLAIMER + " " + RETURN_EXTRA);
        assertThat(response.decision().message()).contains("## Decyzja");
        CaseContextDto ctx = response.caseContext();
        assertThat(ctx.decision()).isEqualTo(new DecisionRef(DecisionCategory.ESCALATED));
        assertThat(ctx.contextVersion()).isEqualTo(2);
        assertThat(ctx.assessment()).isEqualTo(first.caseContext().assessment());
        assertThat(ctx.ruleFacts()).isEqualTo(first.caseContext().ruleFacts());
        assertThat(signer.verify(ctx, response.signature())).isTrue();
        verify(llm, times(1)).decide(any(DecisionPrompt.class));
    }

    @Test
    void tamperedContextIsRejectedBeforeGatewayIsCalled() throws IOException {
        AssessmentResponse first = returnContext();
        CaseContextDto original = first.caseContext();
        var tamperedAssessment = (pl.elektrodemo.copilot.cases.ReturnImageAssessment) original.assessment();
        // Mutation: confidence MEDIUM -> HIGH while keeping the old signature.
        var mutated = new pl.elektrodemo.copilot.cases.ReturnImageAssessment(tamperedAssessment.photoSufficient(),
                tamperedAssessment.insufficiencyReason(), tamperedAssessment.damageVisible(),
                tamperedAssessment.signsOfUse(), tamperedAssessment.resellableAsNew(),
                tamperedAssessment.visibleDescription(), Confidence.HIGH);
        CaseContextDto tampered = new CaseContextDto(original.contextVersion(), original.caseType(), original.form(),
                original.submittedAt(), mutated, original.assessmentSource(), null, null, original.ruleFacts(), null);

        assertThatThrownBy(() -> decide(tampered, first.signature())).isInstanceOf(InvalidCaseSignatureException.class);
        verify(llm, never()).decide(any());
    }

    @Test
    void alreadyDecidedContextIsRejected() throws IOException {
        AssessmentResponse first = returnContext();
        CaseContextDto c = first.caseContext();
        CaseContextDto decided = new CaseContextDto(c.contextVersion(), c.caseType(), c.form(), c.submittedAt(),
                c.assessment(), c.assessmentSource(), null, null, c.ruleFacts(),
                new DecisionRef(DecisionCategory.ESCALATED));
        String signature = signer.sign(decided);

        assertThatThrownBy(() -> decide(decided, signature)).isInstanceOf(DecisionAlreadyMadeException.class);
        verify(llm, never()).decide(any());
    }

    @Test
    void disallowedCategoryThenValidGivesExactlyTwoCallsWithCorrectiveNote() throws IOException {
        AssessmentResponse first = returnContext();
        // Mutation: capture decision-return-ok with category REJECTED (not in allowed set {ESCALATED}).
        DecisionDraft valid = CaptureFixtures.decisionReturnOk();
        DecisionDraft disallowed = new DecisionDraft(DecisionCategory.REJECTED, valid.message());
        when(llm.decide(any(DecisionPrompt.class))).thenReturn(disallowed, valid);

        DecisionResponse response = decide(first.caseContext(), first.signature());

        assertThat(response.decision().category()).isEqualTo(DecisionCategory.ESCALATED);
        ArgumentCaptor<DecisionPrompt> prompts = ArgumentCaptor.forClass(DecisionPrompt.class);
        verify(llm, times(2)).decide(prompts.capture());
        assertThat(prompts.getAllValues().get(0).correctiveNote()).isNull();
        String note = prompts.getAllValues().get(1).correctiveNote();
        assertThat(note).startsWith(CORRECTIVE_PREFIX).contains("Dozwolone kategorie:");
    }

    @Test
    void invalidTwiceFailsWithLlmFailedExceptionAfterTwoCalls() throws IOException {
        AssessmentResponse first = returnContext();
        DecisionDraft valid = CaptureFixtures.decisionReturnOk();
        DecisionDraft disallowed = new DecisionDraft(DecisionCategory.ACCEPTED, valid.message());
        when(llm.decide(any(DecisionPrompt.class))).thenReturn(disallowed, disallowed);

        assertThatThrownBy(() -> decide(first.caseContext(), first.signature()))
                .isInstanceOf(LlmFailedException.class)
                .hasMessage("decision validation failed");
        verify(llm, times(2)).decide(any(DecisionPrompt.class));
    }

    @Test
    void missingParagraphSignTriggersOneCorrectiveRetry() throws IOException {
        AssessmentResponse first = returnContext();
        DecisionDraft valid = CaptureFixtures.decisionReturnOk();
        // Mutation: every "§" replaced by "paragraf", so the citation check fails.
        DecisionDraft noSign = new DecisionDraft(valid.category(), valid.message().replace("§", "paragraf"));
        when(llm.decide(any(DecisionPrompt.class))).thenReturn(noSign, valid);

        DecisionResponse response = decide(first.caseContext(), first.signature());

        assertThat(response.decision().category()).isEqualTo(DecisionCategory.ESCALATED);
        ArgumentCaptor<DecisionPrompt> prompts = ArgumentCaptor.forClass(DecisionPrompt.class);
        verify(llm, times(2)).decide(prompts.capture());
        assertThat(prompts.getAllValues().get(1).correctiveNote()).contains("missing paragraph citation");
    }

    @Test
    void promptUsesSignedRuleFactsAndReturnPolicy() throws IOException {
        AssessmentResponse first = returnContext();
        CaseContextDto c = first.caseContext();
        // A correctly signed context whose ruleFacts differ from what the engine would compute now:
        // the service must pass them through unchanged instead of recomputing.
        RuleResult signedRules = new RuleResult(c.ruleFacts().daysSincePurchase(), c.ruleFacts().returnWindowStatus(),
                false, Set.of(DecisionCategory.ESCALATED, DecisionCategory.ACCEPTED), "", Set.of(), "", "R4");
        CaseContextDto signed = new CaseContextDto(c.contextVersion(), c.caseType(), c.form(), c.submittedAt(),
                c.assessment(), c.assessmentSource(), null, null, signedRules, null);
        when(llm.decide(any(DecisionPrompt.class))).thenReturn(CaptureFixtures.decisionReturnOk());

        decide(signed, signer.sign(signed));

        ArgumentCaptor<DecisionPrompt> prompt = ArgumentCaptor.forClass(DecisionPrompt.class);
        verify(llm).decide(prompt.capture());
        assertThat(prompt.getValue().ruleResult()).isEqualTo(signedRules);
        assertThat(prompt.getValue().policyText()).isEqualTo(policies.policyFor(CaseType.RETURN));
        assertThat(prompt.getValue().correctiveNote()).isNull();
    }

    @Test
    void complaintUsesComplaintPolicy() throws IOException {
        AssessmentResponse first = complaintContext();
        when(llm.decide(any(DecisionPrompt.class))).thenReturn(CaptureFixtures.decisionComplaintOk());

        DecisionResponse response = decide(first.caseContext(), first.signature());

        assertThat(response.decision().category()).isEqualTo(DecisionCategory.ESCALATED);
        ArgumentCaptor<DecisionPrompt> prompt = ArgumentCaptor.forClass(DecisionPrompt.class);
        verify(llm).decide(prompt.capture());
        assertThat(prompt.getValue().policyText()).isEqualTo(policies.policyFor(CaseType.COMPLAINT));
        assertThat(prompt.getValue().policyText()).isNotEqualTo(policies.policyFor(CaseType.RETURN));
    }

    @Test
    void gatewayTimeoutPropagatesWithoutExtraRetry() throws IOException {
        AssessmentResponse first = returnContext();
        when(llm.decide(any(DecisionPrompt.class))).thenThrow(new LlmTimeoutException("timeout"));

        assertThatThrownBy(() -> decide(first.caseContext(), first.signature()))
                .isInstanceOf(LlmTimeoutException.class);
        verify(llm, times(1)).decide(any(DecisionPrompt.class));
    }
}
