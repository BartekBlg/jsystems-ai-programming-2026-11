package pl.elektrodemo.copilot.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Reports all violations of a {@link CaseFormDto} at once.
 * Lengths are measured in Java chars (UTF-16 units), the same as the browser's maxLength/length.
 */
public class CaseFormValidator implements ConstraintValidator<ValidCaseForm, CaseFormDto> {

    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    static final int MODEL_MIN = 2;
    static final int MODEL_MAX = 100;
    static final int REASON_MAX = 2000;
    static final int COMPLAINT_REASON_MIN = 10;

    private final Clock clock;

    public CaseFormValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(CaseFormDto form, ConstraintValidatorContext context) {
        if (form == null) {
            return true;
        }
        List<Violation> violations = new ArrayList<>();
        check(violations, form);
        if (violations.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        for (Violation v : violations) {
            context.buildConstraintViolationWithTemplate(v.message())
                    .addPropertyNode(v.property())
                    .addConstraintViolation();
        }
        return false;
    }

    private void check(List<Violation> out, CaseFormDto form) {
        if (form.caseType() == null) {
            out.add(new Violation("caseType", "Wybierz rodzaj zgłoszenia."));
        }
        if (form.category() == null) {
            out.add(new Violation("category", "Wybierz kategorię sprzętu."));
        }
        String model = form.modelName() == null ? null : form.modelName().strip();
        if (model == null || model.length() < MODEL_MIN || model.length() > MODEL_MAX) {
            out.add(new Violation("modelName", "Podaj nazwę lub model urządzenia (2–100 znaków)."));
        }
        if (form.purchaseDate() == null) {
            out.add(new Violation("purchaseDate", "Podaj datę zakupu."));
        } else if (form.purchaseDate().isAfter(today())) {
            out.add(new Violation("purchaseDate", "Data zakupu nie może być z przyszłości."));
        }
        String reason = form.reason() == null ? "" : form.reason();
        if (reason.length() > REASON_MAX) {
            out.add(new Violation("reason", "Powód może mieć najwyżej 2000 znaków."));
        }
        if (form.caseType() == CaseType.COMPLAINT && reason.strip().length() < COMPLAINT_REASON_MIN) {
            out.add(new Violation("reason", "Opisz problem (co najmniej 10 znaków)."));
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(WARSAW));
    }

    private record Violation(String property, String message) {
    }
}
