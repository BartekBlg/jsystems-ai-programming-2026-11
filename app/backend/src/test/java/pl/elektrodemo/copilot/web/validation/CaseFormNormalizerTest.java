package pl.elektrodemo.copilot.web.validation;

import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CaseFormNormalizerTest {

    @Test
    void trimsModelNameAndReasonAndKeepsOtherFields() {
        CaseFormDto form = new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.TABLET,
                "  iPad Air  ", LocalDate.of(2026, 10, 3), "  Pęknięty ekran  ");

        CaseFormDto trimmed = CaseFormNormalizer.trim(form);

        assertThat(trimmed).isEqualTo(new CaseFormDto(CaseType.COMPLAINT, EquipmentCategory.TABLET,
                "iPad Air", LocalDate.of(2026, 10, 3), "Pęknięty ekran"));
    }

    @Test
    void nullReasonBecomesEmptyString() {
        CaseFormDto form = new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP,
                "MacBook", LocalDate.of(2026, 11, 1), null);

        assertThat(CaseFormNormalizer.trim(form).reason()).isEqualTo("");
    }

    @Test
    void nullModelNameStaysNull() {
        CaseFormDto form = new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP,
                null, LocalDate.of(2026, 11, 1), "");

        assertThat(CaseFormNormalizer.trim(form).modelName()).isNull();
    }

    @Test
    void whitespaceOnlyReasonBecomesEmpty() {
        CaseFormDto form = new CaseFormDto(CaseType.RETURN, EquipmentCategory.LAPTOP,
                "MacBook", LocalDate.of(2026, 11, 1), "   \n  ");

        assertThat(CaseFormNormalizer.trim(form).reason()).isEmpty();
    }
}
