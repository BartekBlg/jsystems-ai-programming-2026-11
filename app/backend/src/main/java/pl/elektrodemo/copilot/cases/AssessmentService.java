package pl.elektrodemo.copilot.cases;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.prescreen.GateResult;
import pl.elektrodemo.copilot.prescreen.GateStatus;
import pl.elektrodemo.copilot.prescreen.PrescreenAnswer;
import pl.elektrodemo.copilot.prescreen.PrescreenAssessmentMapper;
import pl.elektrodemo.copilot.prescreen.PrescreenContextNodes;
import pl.elektrodemo.copilot.prescreen.PrescreenResult;
import pl.elektrodemo.copilot.prescreen.PrescreenService;
import pl.elektrodemo.copilot.prescreen.PrescreenStatus;
import pl.elektrodemo.copilot.prescreen.ShadowComparator;
import tools.jackson.databind.JsonNode;
import pl.elektrodemo.copilot.image.ImageDownscaler;
import pl.elektrodemo.copilot.image.ImageType;
import pl.elektrodemo.copilot.image.ImageTypeDetector;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.llm.LlmProperties;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.web.UnsupportedImageException;
import pl.elektrodemo.copilot.web.dto.AssessmentResponse;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.validation.CaseFormNormalizer;

/** ADR-001 5.1, ADR-002 3.5, ADR-004 3.5, ADR-005 D3: step 1 (image assessment) with llm-only / shadow / gated modes. */
@Service
public class AssessmentService {

    private static final int SIGNATURE_CONTEXT_VERSION = 1;
    private static final Duration DEFAULT_VISION_BUDGET = Duration.ofSeconds(26);
    private static final Duration MIN_VISION_BUDGET = Duration.ofSeconds(20);
    private static final Logger LOGGER = LoggerFactory.getLogger("prescreen");

    private final ImageTypeDetector typeDetector;
    private final ImageDownscaler downscaler;
    private final Clock clock;
    private final LlmGateway llmGateway;
    private final PolicyRuleEngine policyRuleEngine;
    private final CaseSigner caseSigner;
    private final PrescreenService prescreenService;
    private final PrescreenAssessmentMapper assessmentMapper;
    private final ShadowComparator shadowComparator;
    private final String mode;
    private final Duration visionBudget;

    /** llm-only assembly (no pre-screen collaborators). */
    public AssessmentService(ImageTypeDetector typeDetector, ImageDownscaler downscaler, Clock clock,
            LlmGateway llmGateway, PolicyRuleEngine policyRuleEngine, CaseSigner caseSigner) {
        this(typeDetector, downscaler, clock, llmGateway, policyRuleEngine, caseSigner, null, null, null, null);
    }

    public AssessmentService(ImageTypeDetector typeDetector, ImageDownscaler downscaler, Clock clock,
            LlmGateway llmGateway, PolicyRuleEngine policyRuleEngine, CaseSigner caseSigner,
            PrescreenService prescreenService, PrescreenAssessmentMapper assessmentMapper,
            ShadowComparator shadowComparator, AppProperties properties) {
        this(typeDetector, downscaler, clock, llmGateway, policyRuleEngine, caseSigner, prescreenService,
                assessmentMapper, shadowComparator, properties, null);
    }

    @Autowired
    public AssessmentService(ImageTypeDetector typeDetector, ImageDownscaler downscaler, Clock clock,
            LlmGateway llmGateway, PolicyRuleEngine policyRuleEngine, CaseSigner caseSigner,
            PrescreenService prescreenService, PrescreenAssessmentMapper assessmentMapper,
            ShadowComparator shadowComparator, AppProperties properties, LlmProperties llmProperties) {
        this.visionBudget = llmProperties == null || llmProperties.timeouts() == null ? DEFAULT_VISION_BUDGET
                : llmProperties.timeouts().vision();
        this.typeDetector = typeDetector;
        this.downscaler = downscaler;
        this.clock = clock;
        this.llmGateway = llmGateway;
        this.policyRuleEngine = policyRuleEngine;
        this.caseSigner = caseSigner;
        this.prescreenService = prescreenService;
        this.assessmentMapper = assessmentMapper;
        this.shadowComparator = shadowComparator;
        String configured = properties == null || properties.decisions() == null ? null : properties.decisions().mode();
        this.mode = prescreenService == null || configured == null ? "llm-only" : configured.toLowerCase(Locale.ROOT);
    }

    public AssessmentResponse assess(CaseFormDto rawForm, byte[] photo) {
        CaseFormDto form = CaseFormNormalizer.trim(rawForm);
        if (detectHeader(photo).isEmpty()) {
            throw new UnsupportedImageException();
        }
        byte[] jpeg = downscaler.toAnalysisJpeg(photo);
        OffsetDateTime submittedAt = OffsetDateTime.now(clock);

        Outcome outcome = switch (mode) {
            case "shadow" -> assessShadow(form, jpeg);
            case "gated" -> assessGated(form, jpeg);
            default -> new Outcome(assessWithLlm(form, jpeg, null), AssessmentSource.LLM, null, null);
        };

        RuleResult ruleFacts = policyRuleEngine.evaluate(form.caseType(), form.purchaseDate(), submittedAt,
                outcome.assessment());
        CaseContextDto ctx = new CaseContextDto(SIGNATURE_CONTEXT_VERSION, form.caseType(), form, submittedAt,
                outcome.assessment(), outcome.source(), outcome.prescreen(), outcome.consistencySignals(), ruleFacts,
                null);
        return new AssessmentResponse(ctx, caseSigner.sign(ctx));
    }

    private record Outcome(ImageAssessment assessment, AssessmentSource source, JsonNode prescreen,
            JsonNode consistencySignals) {
    }

    private ImageAssessment assessWithLlm(CaseFormDto form, byte[] jpeg, String preliminarySignals) {
        return llmGateway.assessImage(form.caseType(), form.category(), jpeg, preliminarySignals);
    }

    /** ADR-004 3.7 / AC-30: the vision step gets max(20 s, vision budget minus time spent in pre-screening). */
    private ImageAssessment assessAfterPrescreen(CaseFormDto form, byte[] jpeg, String preliminarySignals,
            Instant prescreenStart) {
        Duration remaining = visionBudget.minus(Duration.between(prescreenStart, clock.instant()));
        Duration timeout = remaining.compareTo(MIN_VISION_BUDGET) < 0 ? MIN_VISION_BUDGET : remaining;
        return llmGateway.assessImage(form.caseType(), form.category(), jpeg, preliminarySignals, timeout);
    }

    /** ADR-004 3.5 shadow: pre-screen runs in parallel, the vision LLM stays independent and authoritative. */
    private Outcome assessShadow(CaseFormDto form, byte[] jpeg) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<PrescreenResult> prescreen = executor
                    .submit(() -> prescreenService.prescreen(form.caseType(), form.category(), form, jpeg));
            ImageAssessment assessment;
            long started = System.nanoTime();
            try {
                assessment = assessWithLlm(form, jpeg, null);
            } catch (RuntimeException exception) {
                prescreen.cancel(true);
                shadowComparator.compare(form.caseType(), null, null, 0);
                throw exception;
            }
            long visionMs = Math.max(0, (System.nanoTime() - started) / 1_000_000);
            PrescreenResult result = await(prescreen);
            JsonNode signals = null;
            if (result == null || result.status() != PrescreenStatus.OK) {
                LOGGER.warn("event=prescreen.shadow.skipped reason={}",
                        result == null ? "PRESCREEN_FAILED" : result.unavailableReason());
            } else {
                signals = signalsOf(result);
                try {
                    shadowComparator.compare(form.caseType(), result, assessment, visionMs);
                } catch (RuntimeException exception) {
                    LOGGER.warn("event=prescreen.shadow.skipped reason=COMPARISON_FAILED");
                }
            }
            return new Outcome(assessment, AssessmentSource.LLM, null, signals);
        }
    }

    /** ADR-004 3.5 gated: CONFIDENT skips the vision LLM; UNCERTAIN and UNAVAILABLE fail open to it. */
    private Outcome assessGated(CaseFormDto form, byte[] jpeg) {
        Instant prescreenStart = clock.instant();
        PrescreenResult result = prescreenService.prescreen(form.caseType(), form.category(), form, jpeg);
        if (result.status() != PrescreenStatus.OK) {
            shadowComparator.gated(new GateResult(GateStatus.UNCERTAIN, result.gateReasons()), false);
            return new Outcome(assessAfterPrescreen(form, jpeg, null, prescreenStart), AssessmentSource.LLM, null, null);
        }
        GateResult gate = new GateResult(result.gate(), result.gateReasons());
        PrescreenContextNodes nodes = prescreenService.contextNodes(result);
        JsonNode signals = emptyToNull(nodes.consistencySignals());
        if (result.gate() == GateStatus.CONFIDENT) {
            ImageAssessment mapped;
            try {
                mapped = assessmentMapper.map(form.caseType(), result);
            } catch (RuntimeException exception) {
                shadowComparator.gated(new GateResult(GateStatus.UNCERTAIN, List.of("MAPPING_FAILED")), false);
                return new Outcome(assessAfterPrescreen(form, jpeg, null, prescreenStart), AssessmentSource.LLM, null,
                        signals);
            }
            shadowComparator.gated(gate, true);
            return new Outcome(mapped, AssessmentSource.DECISIONS, nodes.prescreen(), signals);
        }
        shadowComparator.gated(gate, false);
        return new Outcome(assessAfterPrescreen(form, jpeg, preliminarySignals(result.photoAnswers()), prescreenStart),
                AssessmentSource.LLM, nodes.prescreen(), signals);
    }

    private JsonNode signalsOf(PrescreenResult result) {
        return emptyToNull(prescreenService.contextNodes(result).consistencySignals());
    }

    private static JsonNode emptyToNull(JsonNode node) {
        return node == null || node.isNull() || node.isEmpty() ? null : node;
    }

    private static PrescreenResult await(Future<PrescreenResult> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | RuntimeException exception) {
            return null;
        }
    }

    /** Request-A answers only (never form text, model name or reasons), ADR-004 4. */
    static String preliminarySignals(List<PrescreenAnswer> answers) {
        StringBuilder text = new StringBuilder("Wstępne sygnały do weryfikacji: ");
        boolean first = true;
        for (PrescreenAnswer answer : answers) {
            if (answer.refused()) {
                continue;
            }
            if (!first) {
                text.append("; ");
            }
            first = false;
            text.append(answer.questionId()).append('=');
            if (answer.probability() != null) {
                text.append(answer.value() instanceof Boolean yes && yes ? "tak" : "nie").append(" (prawdopodobieństwo ")
                        .append(String.format(Locale.ROOT, "%.2f", answer.probability())).append(')');
            } else {
                text.append(answer.value()).append(" (pewność ")
                        .append(String.format(Locale.ROOT, "%.2f", answer.confidence())).append(')');
            }
        }
        return text.append('.').toString();
    }

    private Optional<ImageType> detectHeader(byte[] photo) {
        if (photo == null) {
            return Optional.empty();
        }
        return typeDetector.detect(Arrays.copyOf(photo, Math.min(12, photo.length)));
    }
}
