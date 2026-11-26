package pl.elektrodemo.copilot.cases;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Deterministic policy rules for return and complaint decisions (ADR-002 §3.3, ADR-005 D5).
 *
 * <p>Mirrors the policy documents. A policy edit must trigger a review of this engine and its tests:
 * <ul>
 *   <li>return-policy.md §2 (right to return), §3 (deadline and delivery date), §4 (item condition),
 *       §5 (exclusions, cited on rejection);</li>
 *   <li>complaint-policy.md §2 (liability period), §3 (what the complaint covers), §4 (exclusions).</li>
 * </ul>
 */
@Component
public class PolicyRuleEngine {

    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    static final String RETURN_CITATION = "§5";
    static final String LIABILITY_EXPIRED_REASON = "upłynął 2-letni okres odpowiedzialności sprzedawcy";
    private static final int RETURN_DAYS = 30;
    private static final int WITHIN_FOURTEEN_DAYS = 14;

    public RuleResult evaluate(CaseType type, LocalDate purchaseDate, OffsetDateTime submittedAt,
            ImageAssessment assessment) {
        LocalDate submittedDate = submittedAt.atZoneSameInstant(WARSAW).toLocalDate();
        int days = (int) ChronoUnit.DAYS.between(purchaseDate, submittedDate);
        if (type == CaseType.RETURN) {
            if (!(assessment instanceof ReturnImageAssessment ret)) {
                throw new IllegalArgumentException("RETURN requires ReturnImageAssessment");
            }
            return evaluateReturn(days, ret);
        }
        if (!(assessment instanceof ComplaintImageAssessment complaint)) {
            throw new IllegalArgumentException("COMPLAINT requires ComplaintImageAssessment");
        }
        return evaluateComplaint(days, purchaseDate, submittedDate, complaint);
    }

    private static RuleResult evaluateReturn(int days, ReturnImageAssessment a) {
        boolean signs = a.signsOfUse() != null && !a.signsOfUse().isEmpty();
        boolean damaged = a.damageVisible() || signs || !a.resellableAsNew();
        boolean lowConfidence = a.confidence() == null || a.confidence() == Confidence.LOW;

        ReturnWindowStatus window = days > RETURN_DAYS ? ReturnWindowStatus.EXPIRED
                : days > WITHIN_FOURTEEN_DAYS ? ReturnWindowStatus.DELIVERY_DATE_NEEDED
                : ReturnWindowStatus.WITHIN_14_DAYS;
        Set<DecisionCategory> chatForbidden = damaged && days <= RETURN_DAYS
                ? EnumSet.of(DecisionCategory.REJECTED)
                : EnumSet.noneOf(DecisionCategory.class);

        if (days > RETURN_DAYS) {
            return new RuleResult(days, window, false, EnumSet.of(DecisionCategory.REJECTED), "",
                    chatForbidden, "", "R1");
        }
        if (!a.photoSufficient()) {
            return new RuleResult(days, window, false, EnumSet.of(DecisionCategory.MORE_INFO_NEEDED), "",
                    chatForbidden, "", "R2");
        }
        if (days >= 15) {
            return new RuleResult(days, window, false, EnumSet.of(DecisionCategory.MORE_INFO_NEEDED), "",
                    chatForbidden, "", "R3");
        }
        if (damaged) {
            return new RuleResult(days, window, false, EnumSet.of(DecisionCategory.ESCALATED), "",
                    chatForbidden, "", "R4");
        }
        if (lowConfidence) {
            return new RuleResult(days, window, false,
                    EnumSet.of(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED), "",
                    chatForbidden, "", "R5");
        }
        return new RuleResult(days, window, false, EnumSet.allOf(DecisionCategory.class), "",
                chatForbidden, RETURN_CITATION, "R6");
    }

    private static RuleResult evaluateComplaint(int days, LocalDate purchaseDate, LocalDate submittedDate,
            ComplaintImageAssessment a) {
        boolean damageCause = a.probableCause() == ProbableCause.MECHANICAL_DAMAGE
                || a.probableCause() == ProbableCause.LIQUID_DAMAGE;
        boolean highConfidence = a.confidence() == Confidence.HIGH;
        boolean lowConfidence = a.confidence() == null || a.confidence() == Confidence.LOW;
        boolean unknownCause = a.probableCause() == null || a.probableCause() == ProbableCause.UNKNOWN;
        Set<DecisionCategory> chatForbidden = damageCause
                ? EnumSet.of(DecisionCategory.ACCEPTED)
                : EnumSet.noneOf(DecisionCategory.class);
        ReturnWindowStatus window = ReturnWindowStatus.NOT_APPLICABLE;

        if (submittedDate.isAfter(purchaseDate.plusYears(2))) {
            // complaint-policy.md section 2.3: expiry depends on the purchase date only, so no chat message
            // (for example a later delivery date) can change the outcome away from REJECTED.
            return new RuleResult(days, window, true, EnumSet.of(DecisionCategory.REJECTED), LIABILITY_EXPIRED_REASON,
                    EnumSet.of(DecisionCategory.ACCEPTED, DecisionCategory.ESCALATED,
                            DecisionCategory.MORE_INFO_NEEDED), "", "C1");
        }
        if (!a.photoSufficient()) {
            return new RuleResult(days, window, false, EnumSet.of(DecisionCategory.MORE_INFO_NEEDED), "",
                    chatForbidden, "", "C2");
        }
        if (damageCause && highConfidence) {
            return new RuleResult(days, window, false,
                    EnumSet.of(DecisionCategory.REJECTED, DecisionCategory.ESCALATED), "",
                    chatForbidden, "", "C3");
        }
        if (damageCause) {
            return new RuleResult(days, window, false, EnumSet.of(DecisionCategory.ESCALATED), "",
                    chatForbidden, "", "C4");
        }
        if (unknownCause || lowConfidence) {
            return new RuleResult(days, window, false,
                    EnumSet.of(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED), "",
                    chatForbidden, "", "C5");
        }
        return new RuleResult(days, window, false, EnumSet.allOf(DecisionCategory.class), "",
                chatForbidden, "", "C6");
    }
}
