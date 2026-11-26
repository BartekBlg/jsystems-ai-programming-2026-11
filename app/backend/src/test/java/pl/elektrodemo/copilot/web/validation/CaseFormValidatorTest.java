package pl.elektrodemo.copilot.web.validation;

import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CaseFormValidatorTest {

    private static final String CASE_TYPE_MSG = "Wybierz rodzaj zgłoszenia.";
    private static final String CATEGORY_MSG = "Wybierz kategorię sprzętu.";
    private static final String MODEL_MSG = "Podaj nazwę lub model urządzenia (2–100 znaków).";
    private static final String PURCHASE_MISSING_MSG = "Podaj datę zakupu.";
    private static final String PURCHASE_FUTURE_MSG = "Data zakupu nie może być z przyszłości.";
    private static final String REASON_MAX_MSG = "Powód może mieć najwyżej 2000 znaków.";
    private static final String REASON_MIN_MSG = "Opisz problem (co najmniej 10 znaków).";

    // 2026-11-26T23:30Z is 2026-11-27 00:30 in Europe/Warsaw
    private static final Instant NOW = Instant.parse("2026-11-26T23:30:00Z");

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = validatorFor(Clock.fixed(NOW, ZoneId.of("Europe/Warsaw")));
    }

    @Test
    void validReturnFormHasNoViolations() {
        assertThat(problems(validReturn())).isEmpty();
    }

    @Test
    void validComplaintFormHasNoViolations() {
        CaseFormDto form = new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE,
                "Galaxy S24", LocalDate.of(2026, 11, 1), "Ekran przestał działać po upadku");
        assertThat(problems(form)).isEmpty();
    }

    @Test
    void emptyFormReportsAllRequiredViolationsAtOnce() {
        CaseFormDto form = new CaseFormDto(null, null, null, null, null);

        assertThat(problems(form)).containsExactlyInAnyOrder(
                "caseType: " + CASE_TYPE_MSG,
                "category: " + CATEGORY_MSG,
                "modelName: " + MODEL_MSG,
                "purchaseDate: " + PURCHASE_MISSING_MSG);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 101})
    void modelNameOutsideLengthBoundsIsInvalid(int length) {
        CaseFormDto form = withModel("x".repeat(length));
        assertThat(problems(form)).containsExactly("modelName: " + MODEL_MSG);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 100})
    void modelNameAtLengthBoundsIsValid(int length) {
        assertThat(problems(withModel("x".repeat(length)))).isEmpty();
    }

    @Test
    void modelNameIsMeasuredAfterTrimming() {
        assertThat(problems(withModel("   a   "))).containsExactly("modelName: " + MODEL_MSG);
        assertThat(problems(withModel("  ab  "))).isEmpty();
    }

    @Test
    void blankModelNameIsInvalid() {
        assertThat(problems(withModel("     "))).containsExactly("modelName: " + MODEL_MSG);
    }

    @Test
    void nullModelNameIsInvalid() {
        assertThat(problems(withModel(null))).containsExactly("modelName: " + MODEL_MSG);
    }

    @Test
    void nullPurchaseDateIsInvalid() {
        CaseFormDto form = new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "MacBook", null, null);
        assertThat(problems(form)).containsExactly("purchaseDate: " + PURCHASE_MISSING_MSG);
    }

    @Test
    void purchaseDateTodayInWarsawIsValid() {
        // Warsaw date is 2026-11-27 for the fixed instant
        assertThat(problems(withPurchase(LocalDate.of(2026, 11, 27)))).isEmpty();
    }

    @Test
    void purchaseDateTomorrowInWarsawIsInvalid() {
        assertThat(problems(withPurchase(LocalDate.of(2026, 11, 28))))
                .containsExactly("purchaseDate: " + PURCHASE_FUTURE_MSG);
    }

    @Test
    void todayIsDeterminedInWarsawEvenWhenClockHasUtcZone() {
        Validator utcValidator = validatorFor(Clock.fixed(NOW, ZoneId.of("UTC")));
        CaseFormDto form = withPurchase(LocalDate.of(2026, 11, 27));
        assertThat(problems(utcValidator, form)).isEmpty();
    }

    @Test
    void reasonOf2000CharactersIsValid() {
        assertThat(problems(withReason(CaseType.RETURN, "x".repeat(2000)))).isEmpty();
    }

    @Test
    void reasonOf2001CharactersIsInvalid() {
        assertThat(problems(withReason(CaseType.RETURN, "x".repeat(2001))))
                .containsExactly("reason: " + REASON_MAX_MSG);
    }

    @Test
    void returnReasonMayBeBlankOrNull() {
        assertThat(problems(withReason(CaseType.RETURN, null))).isEmpty();
        assertThat(problems(withReason(CaseType.RETURN, "          "))).isEmpty();
    }

    @Test
    void complaintReasonOf9CharactersIsInvalid() {
        assertThat(problems(withReason(CaseType.COMPLAINT, "x".repeat(9))))
                .containsExactly("reason: " + REASON_MIN_MSG);
    }

    @Test
    void complaintReasonOf10CharactersIsValid() {
        assertThat(problems(withReason(CaseType.COMPLAINT, "x".repeat(10)))).isEmpty();
    }

    @Test
    void complaintMinimumIsMeasuredAfterTrimming() {
        assertThat(problems(withReason(CaseType.COMPLAINT, "  123456789  ")))
                .containsExactly("reason: " + REASON_MIN_MSG);
        assertThat(problems(withReason(CaseType.COMPLAINT, "  1234567890  "))).isEmpty();
    }

    @Test
    void complaintWithWhitespaceOnlyReasonIsInvalid() {
        assertThat(problems(withReason(CaseType.COMPLAINT, "                    ")))
                .containsExactly("reason: " + REASON_MIN_MSG);
    }

    @Test
    void complaintWithNullReasonIsInvalid() {
        assertThat(problems(withReason(CaseType.COMPLAINT, null)))
                .containsExactly("reason: " + REASON_MIN_MSG);
    }

    @Test
    void complaintReportsMultipleViolationsTogether() {
        CaseFormDto form = new CaseFormDto(CaseType.COMPLAINT, null, "a", null, "short");
        assertThat(problems(form)).containsExactlyInAnyOrder(
                "category: " + CATEGORY_MSG,
                "modelName: " + MODEL_MSG,
                "purchaseDate: " + PURCHASE_MISSING_MSG,
                "reason: " + REASON_MIN_MSG);
    }

    private static CaseFormDto validReturn() {
        return new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "MacBook Air",
                LocalDate.of(2026, 11, 1), null);
    }

    private static CaseFormDto withModel(String modelName) {
        return new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, modelName,
                LocalDate.of(2026, 11, 1), null);
    }

    private static CaseFormDto withPurchase(LocalDate purchaseDate) {
        return new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP, "MacBook Air",
                purchaseDate, null);
    }

    private static CaseFormDto withReason(CaseType type, String reason) {
        return new CaseFormDto(type, EquipmentCategory.LAPTOP, "MacBook Air",
                LocalDate.of(2026, 11, 1), reason);
    }

    private Set<String> problems(CaseFormDto form) {
        return problems(validator, form);
    }

    private static Set<String> problems(Validator validator, CaseFormDto form) {
        Set<ConstraintViolation<CaseFormDto>> violations = validator.validate(form);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
                .collect(Collectors.toSet());
    }

    /**
     * Builds a Hibernate Validator instance that applies {@link ValidCaseForm} to
     * {@link CaseFormDto} through an XML mapping (the DTO itself is a read-only T05 contract),
     * and creates {@link CaseFormValidator} with the given clock.
     */
    private static Validator validatorFor(Clock clock) {
        String mapping = """
                <?xml version="1.0" encoding="UTF-8"?>
                <constraint-mappings xmlns="https://jakarta.ee/xml/ns/validation/mapping"
                                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                     xsi:schemaLocation="https://jakarta.ee/xml/ns/validation/mapping https://jakarta.ee/xml/ns/validation/mapping/validation-mapping-3.0.xsd"
                                     version="3.0">
                  <bean class="pl.elektrodemo.copilot.web.dto.CaseFormDto" ignore-annotations="true">
                    <class ignore-annotations="true">
                      <constraint annotation="pl.elektrodemo.copilot.web.validation.ValidCaseForm"/>
                    </class>
                  </bean>
                </constraint-mappings>
                """;
        ConstraintValidatorFactory factory = new ConstraintValidatorFactory() {
            @Override
            public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                if (key == CaseFormValidator.class) {
                    return key.cast(new CaseFormValidator(clock));
                }
                throw new IllegalArgumentException("Unexpected validator " + key);
            }

            @Override
            public void releaseInstance(ConstraintValidator<?, ?> instance) {
                // nothing to release
            }
        };
        return Validation.byProvider(HibernateValidator.class)
                .configure()
                .constraintValidatorFactory(factory)
                .addMapping(new ByteArrayInputStream(mapping.getBytes(StandardCharsets.UTF_8)))
                .buildValidatorFactory()
                .getValidator();
    }
}
