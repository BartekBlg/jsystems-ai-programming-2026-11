package pl.elektrodemo.copilot.cases;

import org.springframework.stereotype.Service;
import pl.elektrodemo.copilot.llm.DecisionPrompt;
import pl.elektrodemo.copilot.llm.LlmFailedException;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.policy.PolicyRepository;
import pl.elektrodemo.copilot.security.CaseSigner;
import pl.elektrodemo.copilot.web.DecisionAlreadyMadeException;
import pl.elektrodemo.copilot.web.InvalidCaseSignatureException;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.DecisionBody;
import pl.elektrodemo.copilot.web.dto.DecisionRef;
import pl.elektrodemo.copilot.web.dto.DecisionRequest;
import pl.elektrodemo.copilot.web.dto.DecisionResponse;

/** ADR-001 5.2, ADR-002 3.5-3.6 and 6.4, ADR-005 D3/D5: step 2 (decision) of a case. */
@Service
public class DecisionService {

    static final String CORRECTIVE_PREFIX = "Poprzednia odpowiedź była niezgodna z wymaganiami: ";

    private final LlmGateway llmGateway;
    private final DecisionMessageValidator validator;
    private final PolicyRepository policyRepository;
    private final CaseSigner caseSigner;

    public DecisionService(LlmGateway llmGateway, DecisionMessageValidator validator,
            PolicyRepository policyRepository, CaseSigner caseSigner) {
        this.llmGateway = llmGateway;
        this.validator = validator;
        this.policyRepository = policyRepository;
        this.caseSigner = caseSigner;
    }

    public DecisionResponse decide(DecisionRequest req) {
        CaseContextDto ctx = req.caseContext();
        if (!caseSigner.verify(ctx, req.signature())) {
            throw new InvalidCaseSignatureException();
        }
        if (ctx.decision() != null) {
            throw new DecisionAlreadyMadeException();
        }
        RuleResult ruleFacts = ctx.ruleFacts();

        DecisionDraft draft = llmGateway.decide(prompt(ctx, null));
        DecisionMessageValidator.ValidationResult result = validator.validate(draft, ruleFacts, ctx.caseType());
        if (!result.valid()) {
            // Exactly one corrective retry; transport retries belong to the gateway.
            draft = llmGateway.decide(prompt(ctx, CORRECTIVE_PREFIX + String.join("; ", result.problems())));
            result = validator.validate(draft, ruleFacts, ctx.caseType());
            if (!result.valid()) {
                throw new LlmFailedException("decision validation failed");
            }
        }

        CaseContextDto next = new CaseContextDto(ctx.contextVersion() + 1, ctx.caseType(), ctx.form(),
                ctx.submittedAt(), ctx.assessment(), ctx.assessmentSource(), ctx.prescreen(),
                ctx.consistencySignals(), ruleFacts, new DecisionRef(draft.category()));
        String signature = caseSigner.sign(next);
        return new DecisionResponse(new DecisionBody(draft.category(), result.normalizedMessage()), next, signature);
    }

    private DecisionPrompt prompt(CaseContextDto ctx, String correctiveNote) {
        return new DecisionPrompt(ctx.caseType(), ctx.form(), ctx.submittedAt(), ctx.assessment(),
                ctx.assessmentSource(), ctx.ruleFacts(), policyRepository.policyFor(ctx.caseType()),
                ctx.prescreen(), ctx.consistencySignals(), correctiveNote);
    }
}
