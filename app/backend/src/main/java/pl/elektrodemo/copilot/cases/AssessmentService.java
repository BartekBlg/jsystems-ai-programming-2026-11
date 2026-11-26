package pl.elektrodemo.copilot.cases;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import pl.elektrodemo.copilot.image.ImageDownscaler;
import pl.elektrodemo.copilot.image.ImageType;
import pl.elektrodemo.copilot.image.ImageTypeDetector;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.web.UnsupportedImageException;
import pl.elektrodemo.copilot.web.dto.AssessmentResponse;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.validation.CaseFormNormalizer;

/** ADR-001 5.1, ADR-002 3.5, ADR-005 D3: step 1 (image assessment) of a case. llm-only path in this phase. */
public class AssessmentService {

    private static final int SIGNATURE_CONTEXT_VERSION = 1;

    private final ImageTypeDetector typeDetector;
    private final ImageDownscaler downscaler;
    private final Clock clock;
    private final LlmGateway llmGateway;
    private final PolicyRuleEngine policyRuleEngine;
    private final CaseSigner caseSigner;

    public AssessmentService(ImageTypeDetector typeDetector, ImageDownscaler downscaler, Clock clock,
            LlmGateway llmGateway, PolicyRuleEngine policyRuleEngine, CaseSigner caseSigner) {
        this.typeDetector = typeDetector;
        this.downscaler = downscaler;
        this.clock = clock;
        this.llmGateway = llmGateway;
        this.policyRuleEngine = policyRuleEngine;
        this.caseSigner = caseSigner;
    }

    public AssessmentResponse assess(CaseFormDto rawForm, byte[] photo) {
        CaseFormDto form = CaseFormNormalizer.trim(rawForm);
        if (detectHeader(photo).isEmpty()) {
            throw new UnsupportedImageException();
        }
        byte[] jpeg = downscaler.toAnalysisJpeg(photo);
        OffsetDateTime submittedAt = OffsetDateTime.now(clock);

        ImageAssessment assessment = assessWithLlm(form, jpeg);

        RuleResult ruleFacts = policyRuleEngine.evaluate(form.caseType(), form.purchaseDate(), submittedAt,
                assessment);
        CaseContextDto ctx = new CaseContextDto(SIGNATURE_CONTEXT_VERSION, form.caseType(), form, submittedAt,
                assessment, AssessmentSource.LLM, null, null, ruleFacts, null);
        return new AssessmentResponse(ctx, caseSigner.sign(ctx));
    }

    /** Mode switch (shadow/gated, T25) is inserted here, before the gateway call. */
    private ImageAssessment assessWithLlm(CaseFormDto form, byte[] jpeg) {
        return llmGateway.assessImage(form.caseType(), form.category(), jpeg, null);
    }

    private Optional<ImageType> detectHeader(byte[] photo) {
        if (photo == null) {
            return Optional.empty();
        }
        return typeDetector.detect(Arrays.copyOf(photo, Math.min(12, photo.length)));
    }
}
