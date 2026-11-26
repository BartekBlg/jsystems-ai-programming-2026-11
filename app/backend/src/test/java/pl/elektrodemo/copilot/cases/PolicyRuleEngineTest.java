package pl.elektrodemo.copilot.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PolicyRuleEngineTest {

    private static final LocalDate PURCHASE = LocalDate.of(2026, 10, 1);
    private static final Set<DecisionCategory> ALL = Set.of(DecisionCategory.ACCEPTED, DecisionCategory.REJECTED,
            DecisionCategory.MORE_INFO_NEEDED, DecisionCategory.ESCALATED);
    private static final String REASON = "upłynął 2-letni okres odpowiedzialności sprzedawcy";

    private final PolicyRuleEngine engine = new PolicyRuleEngine();

    /** Noon UTC keeps the Warsaw calendar date equal to the UTC date for ordinary cases. */
    private static OffsetDateTime submittedOn(LocalDate date) {
        return OffsetDateTime.of(date, LocalTime.NOON, ZoneOffset.UTC);
    }

    private static OffsetDateTime daysAfterPurchase(long days) {
        return submittedOn(PURCHASE.plusDays(days));
    }

    private static ReturnImageAssessment ret(boolean photoOk, boolean damage, List<SignOfUse> signs,
            boolean resellable, Confidence confidence) {
        return new ReturnImageAssessment(photoOk, photoOk ? null : "za ciemno", damage, signs, resellable,
                "opis", confidence);
    }

    private static ReturnImageAssessment cleanReturn(Confidence confidence) {
        return ret(true, false, List.of(), true, confidence);
    }

    private static ComplaintImageAssessment complaint(boolean photoOk, ProbableCause cause, Confidence confidence) {
        return new ComplaintImageAssessment(photoOk, photoOk ? null : "za ciemno", true, "defekt", "obudowa",
                cause, "uzasadnienie", "opis", confidence);
    }

    // ---- RETURN: R1..R6 and window ----

    @ParameterizedTest
    @ValueSource(longs = {0, 14})
    void returnWithinFourteenDaysIsR6WithinWindow(long days) {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(days),
                cleanReturn(Confidence.HIGH));

        assertThat(r.daysSincePurchase()).isEqualTo((int) days);
        assertThat(r.appliedRule()).isEqualTo("R6");
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.WITHIN_14_DAYS);
        assertThat(r.allowedCategories()).isEqualTo(ALL);
        assertThat(r.rejectionRequiresCitation()).isEqualTo("§5");
        assertThat(r.liabilityExpired()).isFalse();
        assertThat(r.requiredReasonText()).isEqualTo("");
        assertThat(r.chatForbiddenCategories()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {15, 30})
    void returnBetweenFifteenAndThirtyDaysIsR3(long days) {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(days),
                cleanReturn(Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R3");
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.DELIVERY_DATE_NEEDED);
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.MORE_INFO_NEEDED);
        assertThat(r.rejectionRequiresCitation()).isEqualTo("");
        assertThat(r.chatForbiddenCategories()).isEmpty();
    }

    @Test
    void returnAfterThirtyDaysIsR1RejectedAndExpired() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(31),
                cleanReturn(Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R1");
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.EXPIRED);
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.REJECTED);
        assertThat(r.chatForbiddenCategories()).isEmpty();
    }

    @Test
    void r1WinsOverDamageAndChatForbidsNothingAfterThirtyDays() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(45),
                ret(true, true, List.of(SignOfUse.SCRATCHES), false, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R1");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.REJECTED);
        assertThat(r.chatForbiddenCategories()).isEmpty();
    }

    @Test
    void r2InsufficientPhotoWithinWindow() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(5),
                ret(false, false, List.of(), true, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R2");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.MORE_INFO_NEEDED);
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.WITHIN_14_DAYS);
        assertThat(r.rejectionRequiresCitation()).isEqualTo("");
    }

    @Test
    void r2InsufficientPhotoInDeliveryWindowKeepsDeliveryDateStatus() {
        // Window is derived from days alone (R1/R3); R2 only decides the category set.
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(20),
                ret(false, false, List.of(), true, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R2");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.MORE_INFO_NEEDED);
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.DELIVERY_DATE_NEEDED);
    }

    @Test
    void r2WinsOverR4WhenPhotoIsInsufficient() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(5),
                ret(false, true, List.of(), false, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R2");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.MORE_INFO_NEEDED);
        // R4 condition still holds within 30 days, so chat must not produce an acceptance-like answer.
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.REJECTED);
    }

    @Test
    void r4DamageVisibleEscalatesAndForbidsChatRejectionWithinThirtyDays() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(10),
                ret(true, true, List.of(), true, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R4");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.ESCALATED);
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.WITHIN_14_DAYS);
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.REJECTED);
        assertThat(r.rejectionRequiresCitation()).isEqualTo("");
    }

    @Test
    void r4SignsOfUseEscalates() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(3),
                ret(true, false, List.of(SignOfUse.DIRT), true, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R4");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.ESCALATED);
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.REJECTED);
    }

    @Test
    void r4NotResellableAsNewEscalates() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(3),
                ret(true, false, List.of(), false, Confidence.MEDIUM));

        assertThat(r.appliedRule()).isEqualTo("R4");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.ESCALATED);
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.REJECTED);
    }

    @Test
    void r3PrecedesR4InDeliveryWindowButChatStillForbidsRejection() {
        // R3 is earlier in rule order than R4, so it decides the categories at day 30 even with visible damage.
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(30),
                ret(true, true, List.of(), true, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("R3");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.MORE_INFO_NEEDED);
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.DELIVERY_DATE_NEEDED);
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.REJECTED);
    }

    @Test
    void r5LowConfidenceEscalatesOrAsksForMoreInfo() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(3),
                cleanReturn(Confidence.LOW));

        assertThat(r.appliedRule()).isEqualTo("R5");
        assertThat(r.allowedCategories())
                .containsExactlyInAnyOrder(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED);
        assertThat(r.rejectionRequiresCitation()).isEqualTo("");
        assertThat(r.chatForbiddenCategories()).isEmpty();
    }

    @Test
    void r4WinsOverR5WhenDamageAndLowConfidence() {
        RuleResult r = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(3),
                ret(true, true, List.of(), true, Confidence.LOW));

        assertThat(r.appliedRule()).isEqualTo("R4");
    }

    @Test
    void nullSignsOfUseAndNullConfidenceAreTreatedAsNoSignsAndLow() {
        RuleResult noSigns = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(3),
                ret(true, false, null, true, Confidence.HIGH));
        assertThat(noSigns.appliedRule()).isEqualTo("R6");

        RuleResult nullConfidence = engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(3),
                ret(true, false, List.of(), true, null));
        assertThat(nullConfidence.appliedRule()).isEqualTo("R5");
    }

    @Test
    void returnRejectsComplaintAssessment() {
        assertThatThrownBy(() -> engine.evaluate(CaseType.RETURN, PURCHASE, daysAfterPurchase(3),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void complaintRejectsReturnAssessment() {
        assertThatThrownBy(() -> engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(3),
                cleanReturn(Confidence.HIGH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- RETURN: Warsaw calendar date ----

    @Test
    void submissionDateIsConvertedToWarsawBeforeCountingDays() {
        // 2026-11-26T23:30Z is 2026-11-27 00:30 in Europe/Warsaw (UTC+1).
        OffsetDateTime submitted = OffsetDateTime.parse("2026-11-26T23:30:00Z");

        // Purchased 2026-10-27: Warsaw day count is 31 (R1), UTC day count would be 30 (R3).
        RuleResult r = engine.evaluate(CaseType.RETURN, LocalDate.of(2026, 10, 27), submitted,
                cleanReturn(Confidence.HIGH));

        assertThat(r.daysSincePurchase()).isEqualTo(31);
        assertThat(r.appliedRule()).isEqualTo("R1");
    }

    // ---- COMPLAINT: C1..C6 ----

    @Test
    void complaintLiabilityExpiredIsC1() {
        LocalDate purchase = LocalDate.of(2024, 6, 15);
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, purchase, submittedOn(LocalDate.of(2026, 6, 16)),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C1");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.REJECTED);
        assertThat(r.liabilityExpired()).isTrue();
        assertThat(r.requiredReasonText()).isEqualTo(REASON);
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.NOT_APPLICABLE);
        assertThat(r.rejectionRequiresCitation()).isEqualTo("");
    }

    @Test
    void complaintExactlyTwoYearsIsNotExpired() {
        LocalDate purchase = LocalDate.of(2024, 6, 15);
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, purchase, submittedOn(LocalDate.of(2026, 6, 15)),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C6");
        assertThat(r.liabilityExpired()).isFalse();
        assertThat(r.requiredReasonText()).isEqualTo("");
    }

    @Test
    void complaintTwoYearsAndOneDayIsC1() {
        LocalDate purchase = LocalDate.of(2024, 6, 15);
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, purchase, submittedOn(LocalDate.of(2026, 6, 16)),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C1");
    }

    @Test
    void complaintLeapDayPurchaseUsesPlusYearsClampSemantics() {
        // LocalDate.plusYears(2) from 2024-02-29 is 2026-02-28 (clamped), so 2026-02-28 is still in liability.
        LocalDate purchase = LocalDate.of(2024, 2, 29);

        RuleResult onClampedDay = engine.evaluate(CaseType.COMPLAINT, purchase,
                submittedOn(LocalDate.of(2026, 2, 28)),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH));
        assertThat(onClampedDay.appliedRule()).isEqualTo("C6");

        RuleResult dayAfter = engine.evaluate(CaseType.COMPLAINT, purchase,
                submittedOn(LocalDate.of(2026, 3, 1)),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH));
        assertThat(dayAfter.appliedRule()).isEqualTo("C1");
    }

    @Test
    void complaintWarsawDateDecidesLiabilityExpiry() {
        // Purchase 2024-11-26; submitted 2026-11-26T23:30Z is 2026-11-27 in Warsaw, after plusYears(2).
        OffsetDateTime submitted = OffsetDateTime.parse("2026-11-26T23:30:00Z");
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, LocalDate.of(2024, 11, 26), submitted,
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C1");
    }

    @Test
    void c1WinsOverDamageCauseAndStillForbidsAcceptanceInChat() {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, LocalDate.of(2020, 1, 1), submittedOn(LocalDate.of(2026, 1, 1)),
                complaint(true, ProbableCause.LIQUID_DAMAGE, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C1");
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.ACCEPTED);
    }

    @Test
    void complaintInsufficientPhotoIsC2() {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(false, ProbableCause.MANUFACTURING_DEFECT, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C2");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.MORE_INFO_NEEDED);
        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.NOT_APPLICABLE);
        assertThat(r.chatForbiddenCategories()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = ProbableCause.class, names = {"MECHANICAL_DAMAGE", "LIQUID_DAMAGE"})
    void damageCauseWithHighConfidenceIsC3(ProbableCause cause) {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, cause, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C3");
        assertThat(r.allowedCategories())
                .containsExactlyInAnyOrder(DecisionCategory.REJECTED, DecisionCategory.ESCALATED);
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.ACCEPTED);
        assertThat(r.rejectionRequiresCitation()).isEqualTo("");
    }

    @ParameterizedTest
    @EnumSource(value = Confidence.class, names = {"MEDIUM", "LOW"})
    void damageCauseWithMediumOrLowConfidenceIsC4(Confidence confidence) {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, ProbableCause.MECHANICAL_DAMAGE, confidence));

        assertThat(r.appliedRule()).isEqualTo("C4");
        assertThat(r.allowedCategories()).containsExactly(DecisionCategory.ESCALATED);
        assertThat(r.chatForbiddenCategories()).containsExactly(DecisionCategory.ACCEPTED);
    }

    @Test
    void unknownCauseIsC5() {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, ProbableCause.UNKNOWN, Confidence.HIGH));

        assertThat(r.appliedRule()).isEqualTo("C5");
        assertThat(r.allowedCategories())
                .containsExactlyInAnyOrder(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED);
        assertThat(r.chatForbiddenCategories()).isEmpty();
    }

    @Test
    void lowConfidenceNonDamageCauseIsC5() {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, Confidence.LOW));

        assertThat(r.appliedRule()).isEqualTo("C5");
        assertThat(r.allowedCategories())
                .containsExactlyInAnyOrder(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED);
    }

    @Test
    void nullCauseIsTreatedAsUnknownAndNullConfidenceAsLow() {
        RuleResult nullCause = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, null, Confidence.HIGH));
        assertThat(nullCause.appliedRule()).isEqualTo("C5");

        RuleResult nullConfidence = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, ProbableCause.MANUFACTURING_DEFECT, null));
        assertThat(nullConfidence.appliedRule()).isEqualTo("C5");
    }

    @ParameterizedTest
    @EnumSource(value = ProbableCause.class, names = {"MANUFACTURING_DEFECT", "NORMAL_WEAR"})
    void otherCausesWithGoodConfidenceAreC6WithAllCategories(ProbableCause cause) {
        RuleResult high = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, cause, Confidence.HIGH));
        RuleResult medium = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, cause, Confidence.MEDIUM));

        for (RuleResult r : List.of(high, medium)) {
            assertThat(r.appliedRule()).isEqualTo("C6");
            assertThat(r.allowedCategories()).isEqualTo(ALL);
            assertThat(r.chatForbiddenCategories()).isEmpty();
            assertThat(r.liabilityExpired()).isFalse();
            assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.NOT_APPLICABLE);
        }
    }

    @Test
    void c6DoesNotCarryCitation() {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(40),
                complaint(true, ProbableCause.NORMAL_WEAR, Confidence.HIGH));

        assertThat(r.rejectionRequiresCitation()).isEqualTo("");
    }

    @Test
    void complaintWindowIsNotApplicableEvenAfterThirtyDays() {
        RuleResult r = engine.evaluate(CaseType.COMPLAINT, PURCHASE, daysAfterPurchase(90),
                complaint(true, ProbableCause.NORMAL_WEAR, Confidence.HIGH));

        assertThat(r.returnWindowStatus()).isEqualTo(ReturnWindowStatus.NOT_APPLICABLE);
        assertThat(r.daysSincePurchase()).isEqualTo(90);
    }
}
