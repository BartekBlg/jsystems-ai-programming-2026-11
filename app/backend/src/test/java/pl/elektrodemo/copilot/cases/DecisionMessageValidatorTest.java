package pl.elektrodemo.copilot.cases;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import pl.elektrodemo.copilot.cases.DecisionMessageValidator.ValidationResult;

class DecisionMessageValidatorTest {

    private static final String DISCLAIMER = "To wstępna, automatyczna ocena przygotowana na podstawie formularza i zdjęcia. "
            + "Ostateczną decyzję podejmie nasz zespół serwisu po otrzymaniu i sprawdzeniu urządzenia.";
    private static final String RETURN_EXTRA = "Nie ogranicza to Twojego ustawowego prawa do odstąpienia od umowy.";

    private final DecisionMessageValidator validator = new DecisionMessageValidator();

    private static final String GREETING = "Dzień dobry, dziękujemy za zgłoszenie.";
    private static final String DECISION_SECTION = "## Decyzja\nPrzyjęte. Zwrot zostaje przyjęty.";
    private static final String REASON_SECTION = "## Uzasadnienie\nZgodnie z §2 ust. 1 regulaminu.";
    private static final String SEE_SECTION = "## Co widać na zdjęciu\nUrządzenie jest czyste.";
    private static final String STEPS_SECTION = "## Kolejne kroki\nOdeślij urządzenie.";

    private static String validMessage() {
        return String.join("\n\n", GREETING, DECISION_SECTION, REASON_SECTION, SEE_SECTION, STEPS_SECTION);
    }

    private static RuleResult rules(Set<DecisionCategory> allowed, String requiredReason, String citation,
            Set<DecisionCategory> forbidden) {
        return new RuleResult(5, ReturnWindowStatus.WITHIN_14_DAYS, false, allowed, requiredReason, forbidden,
                citation, "TEST");
    }

    private static RuleResult c1Rules() {
        return new RuleResult(900, ReturnWindowStatus.NOT_APPLICABLE, true, EnumSet.of(DecisionCategory.REJECTED),
                "upłynął 2-letni okres odpowiedzialności sprzedawcy", Set.of(), "", "C1");
    }

    private static RuleResult accepted() {
        return rules(EnumSet.of(DecisionCategory.ACCEPTED), null, null, Set.of());
    }

    private static RuleResult acceptedRejectable() {
        return rules(EnumSet.of(DecisionCategory.ACCEPTED, DecisionCategory.REJECTED), null, "§5", Set.of());
    }

    @Nested
    class Categories {

        @Test
        void validMinimalMessageIsAccepted() {
            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, validMessage()),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isTrue();
            assertThat(result.problems()).isEmpty();
        }

        @Test
        void categoryOutsideAllowedSetIsRejectedWithPolishList() {
            RuleResult rules = rules(EnumSet.of(DecisionCategory.ESCALATED, DecisionCategory.MORE_INFO_NEEDED),
                    null, null, Set.of());

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, validMessage()),
                    rules, CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).contains(
                    "Dozwolone kategorie: Potrzebne dodatkowe informacje, Przekazane do specjalisty");
        }

        @Test
        void nullCategoryIsInvalid() {
            ValidationResult result = validator.validate(new DecisionDraft(null, validMessage()), accepted(),
                    CaseType.RETURN);

            assertThat(result.valid()).isFalse();
        }

        @Test
        void wrongPolishLabelInDecisionSectionIsRejected() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.contains("Przyjęte"));
        }

        @Test
        void labelOutsideDecisionSectionDoesNotCount() {
            String message = validMessage().replace("Przyjęte. Zwrot zostaje przyjęty.", "Zwrot zostaje przyjęty.")
                    .replace("Odeślij urządzenie.", "Odeślij urządzenie. Przyjęte.");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
        }
    }

    @Nested
    class Headings {

        @ParameterizedTest
        @ValueSource(strings = {"## Decyzja", "## Uzasadnienie", "## Co widać na zdjęciu", "## Kolejne kroki"})
        void missingHeadingIsRejected(String heading) {
            String message = validMessage().replace(heading, "### Inne");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.contains(heading));
        }

        @Test
        void swappedSectionsAreOutOfOrder() {
            String message = String.join("\n\n", GREETING, DECISION_SECTION, SEE_SECTION, REASON_SECTION,
                    STEPS_SECTION);

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.toLowerCase().contains("order"));
        }

        @Test
        void headingWithoutSpaceAfterHashesIsAccepted() {
            String message = validMessage().replace("## Decyzja", "##Decyzja");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isTrue();
        }
    }

    @Nested
    class Citations {

        @Test
        void messageWithoutParagraphSignIsRejected() {
            String message = validMessage().replace("§2 ust. 1", "punkt 2 ust. 1");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"§1", "§ 12"})
        void anyParagraphNumberIsAccepted(String citation) {
            String message = validMessage().replace("§2 ust. 1", citation);

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isTrue();
        }

        @Test
        void rejectedUnderRule6WithoutSection5IsRejected() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.").replace("§2 ust. 1", "§2 ust. 1");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, message),
                    acceptedRejectable(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.contains("§5"));
        }

        @Test
        void rejectedWithSection5IsAccepted() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.").replace("§2 ust. 1", "§5");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, message),
                    acceptedRejectable(), CaseType.RETURN);

            assertThat(result.valid()).isTrue();
        }

        @Test
        void section50DoesNotSatisfySection5() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.").replace("§2 ust. 1", "§50");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, message),
                    acceptedRejectable(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
        }

        @Test
        void citationIsNotRequiredWhenRuleDoesNotSetIt() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.");
            RuleResult rules = rules(EnumSet.of(DecisionCategory.REJECTED), null, null, Set.of());

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, message),
                    rules, CaseType.RETURN);

            assertThat(result.valid()).isTrue();
        }
    }

    @Nested
    class RequiredReason {

        private static final String REASON = "upłynął 2-letni okres odpowiedzialności sprzedawcy";

        private RuleResult c1() {
            return rules(EnumSet.of(DecisionCategory.REJECTED), REASON, null, Set.of());
        }

        @Test
        void missingRequiredReasonIsRejected() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, message),
                    c1(), CaseType.COMPLAINT);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.contains(REASON));
        }

        @Test
        void requiredReasonPresentIsAccepted() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.")
                    .replace("Zgodnie z §2 ust. 1 regulaminu.", "Zgodnie z §2: " + REASON + ".");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, message),
                    c1(), CaseType.COMPLAINT);

            assertThat(result.valid()).isTrue();
        }
    }

    @Nested
    class Normalization {

        @Test
        void appendsCanonicalDisclaimerToReturn() {
            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, validMessage()),
                    accepted(), CaseType.RETURN);

            assertThat(result.normalizedMessage()).isEqualTo(
                    validMessage() + "\n\n" + DISCLAIMER + " " + RETURN_EXTRA);
        }

        @Test
        void appendsCanonicalDisclaimerWithoutExtraSentenceToComplaint() {
            RuleResult rules = rules(EnumSet.of(DecisionCategory.ACCEPTED), null, null, Set.of());

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, validMessage()),
                    rules, CaseType.COMPLAINT);

            assertThat(result.normalizedMessage()).isEqualTo(validMessage() + "\n\n" + DISCLAIMER);
            assertThat(result.normalizedMessage()).doesNotContain(RETURN_EXTRA);
        }

        @Test
        void modelWrittenDisclaimerIsReplacedByCanonicalOne() {
            String modelDisclaimer = "Pamiętaj, że to tylko wstępna ocena, a ostateczną decyzję podejmiemy po zwrocie.";
            String message = validMessage() + "\n\n" + modelDisclaimer;

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.COMPLAINT);

            assertThat(result.normalizedMessage()).doesNotContain(modelDisclaimer);
            assertThat(result.normalizedMessage()).isEqualTo(validMessage() + "\n\n" + DISCLAIMER);
        }

        @Test
        void modelWrittenRightDisclaimerIsReplacedByCanonicalOneWithExtraSentence() {
            String message = validMessage() + "\n\nTo wstępna ocena. Nie ogranicza ustawowego prawa.";

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.normalizedMessage()).isEqualTo(
                    validMessage() + "\n\n" + DISCLAIMER + " " + RETURN_EXTRA);
        }

        @Test
        void normalizationIsAppliedEvenWhenInvalid() {
            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, "Krótko."),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.normalizedMessage()).endsWith(DISCLAIMER + " " + RETURN_EXTRA);
        }

        @ParameterizedTest
        @EnumSource(CaseType.class)
        void normalizedMessageIsIdempotent(CaseType type) {
            RuleResult rules = accepted();
            String once = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, validMessage()), rules, type)
                    .normalizedMessage();

            String twice = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, once), rules, type)
                    .normalizedMessage();

            assertThat(twice).isEqualTo(once);
        }
    }

    @Nested
    class CapturesAndMutations {

        private static String emptyToNull(String value) {
            return value.isEmpty() ? null : value;
        }

        @ParameterizedTest
        @CsvSource({
                "decision-return-ok, ESCALATED, 5, ESCALATED, '', REJECTED, '', RETURN",
                "decision-return-more-info, MORE_INFO_NEEDED, 20, MORE_INFO_NEEDED, '', '', '', RETURN",
                "decision-return-insufficient, MORE_INFO_NEEDED, 5, MORE_INFO_NEEDED, '', '', '', RETURN",
                "decision-complaint-ok, ESCALATED, 184, ESCALATED, '', ACCEPTED, '', COMPLAINT"
        })
        void recordedDecisionsAreValid(String capture, String expectedCategory, int days, String allowed,
                String requiredReason, String forbidden, String citation, CaseType type) {
            DecisionDraft draft = DecisionCaptures.load(capture);
            RuleResult rules = new RuleResult(days, ReturnWindowStatus.WITHIN_14_DAYS, false,
                    EnumSet.of(DecisionCategory.valueOf(allowed)), emptyToNull(requiredReason),
                    forbidden.isEmpty() ? Set.of() : EnumSet.of(DecisionCategory.valueOf(forbidden)),
                    emptyToNull(citation), "CAPTURE");

            assertThat(draft.category()).isEqualTo(DecisionCategory.valueOf(expectedCategory));
            ValidationResult result = validator.validate(draft, rules, type);

            assertThat(result.problems()).isEmpty();
            assertThat(result.valid()).isTrue();
            assertThat(result.normalizedMessage()).endsWith(
                    type == CaseType.RETURN ? DISCLAIMER + " " + RETURN_EXTRA : DISCLAIMER);
        }

        @Test
        void mutatedRecordedMessageIsRejectedForMissingHeading() {
            DecisionDraft draft = DecisionCaptures.load("decision-return-ok");
            String mutated = draft.message().replace("## Kolejne kroki", "## Dalej");

            ValidationResult result = validator.validate(new DecisionDraft(draft.category(), mutated),
                    rules(EnumSet.of(DecisionCategory.ESCALATED), null, null, Set.of(DecisionCategory.REJECTED)),
                    CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.contains("Kolejne kroki"));
        }

        @Test
        void mutatedRecordedMessageIsRejectedWhenParagraphSignRemoved() {
            DecisionDraft draft = DecisionCaptures.load("decision-complaint-ok");
            String mutated = draft.message().replace("§", "paragraf ");

            ValidationResult result = validator.validate(new DecisionDraft(draft.category(), mutated),
                    rules(EnumSet.of(DecisionCategory.ESCALATED), null, null, Set.of()), CaseType.COMPLAINT);

            assertThat(result.valid()).isFalse();
        }
    }

    @Nested
    class CitationInJustification {

        @Test
        void citationOnlyInNextStepsIsRejected() {
            String message = validMessage().replace("Zgodnie z §2 ust. 1 regulaminu.", "Zgodnie z regulaminem.")
                    .replace("Odeślij urządzenie.", "Odeślij urządzenie (§6).");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, message),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.contains("§N"));
        }

        @Test
        void citationInJustificationIsAccepted() {
            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, validMessage()),
                    accepted(), CaseType.RETURN);

            assertThat(result.valid()).isTrue();
        }

        @Test
        void section5OnlyInPhotoSectionDoesNotSatisfyRejection() {
            String message = validMessage().replace("Przyjęte.", "Odrzucone.")
                    .replace("Zgodnie z §2 ust. 1 regulaminu.", "Zgodnie z §2 ust. 1 regulaminu.")
                    .replace("Urządzenie jest czyste.", "Urządzenie jest czyste (§5).");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, message),
                    acceptedRejectable(), CaseType.RETURN);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).anyMatch(p -> p.contains("§5"));
        }
    }

    @Nested
    class C1ManufacturerWarranty {

        private static final String REASON = "upłynął 2-letni okres odpowiedzialności sprzedawcy";

        private String c1Message(String steps) {
            return String.join("\n\n", GREETING, "## Decyzja\nOdrzucone. Reklamacja jest po terminie.",
                    "## Uzasadnienie\nZgodnie z §2 ust. 3: " + REASON + ".", SEE_SECTION, "## Kolejne kroki\n" + steps);
        }

        @Test
        void c1WithoutWarrantySuggestionIsRejected() {
            ValidationResult result = validator.validate(
                    new DecisionDraft(DecisionCategory.REJECTED, c1Message("Dziękujemy za zgłoszenie.")), c1Rules(),
                    CaseType.COMPLAINT);

            assertThat(result.valid()).isFalse();
            assertThat(result.problems()).contains("Brak wskazania gwarancji producenta");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Skontaktuj się z serwisem gwarancyjnym producenta.",
                "Sprawdź, czy obowiązuje gwarancja producenta.", "Możesz skorzystać z gwarancji producenta.",
                "Skontaktuj się z PRODUCENTEM w sprawie GWARANCJI.",
                "Zwróć się do producenta, który może udzielić pomocy w ramach serwisu gwarancyjnym."})
        void c1WithWarrantySuggestionIsAccepted(String steps) {
            ValidationResult result = validator.validate(
                    new DecisionDraft(DecisionCategory.REJECTED, c1Message(steps)), c1Rules(), CaseType.COMPLAINT);

            assertThat(result.problems()).isEmpty();
        }

        @Test
        void warrantyAloneWithoutManufacturerIsRejected() {
            ValidationResult result = validator.validate(
                    new DecisionDraft(DecisionCategory.REJECTED, c1Message("Sprawdź gwarancję.")), c1Rules(),
                    CaseType.COMPLAINT);

            assertThat(result.problems()).contains("Brak wskazania gwarancji producenta");
        }

        @Test
        void mutatedCapturedComplaintDecisionAsC1MissingWarrantyIsRejected() {
            DecisionDraft draft = DecisionCaptures.load("decision-complaint-ok");
            String mutated = draft.message().replace("## Decyzja\nPrzekazane do specjalisty.", "## Decyzja\nOdrzucone.")
                    .replace("Zgodnie z §4 ust. 2", "Zgodnie z §2 ust. 3: " + REASON + ", a także z §4 ust. 2");

            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.REJECTED, mutated),
                    c1Rules(), CaseType.COMPLAINT);

            assertThat(result.problems()).containsExactly("Brak wskazania gwarancji producenta");
        }

        @Test
        void nonC1RulesDoNotRequireWarranty() {
            ValidationResult result = validator.validate(new DecisionDraft(DecisionCategory.ACCEPTED, validMessage()),
                    accepted(), CaseType.COMPLAINT);

            assertThat(result.valid()).isTrue();
        }
    }
}
