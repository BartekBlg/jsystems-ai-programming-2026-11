package pl.elektrodemo.copilot.web.dto;

import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import java.time.LocalDate;

// T13: add @ValidCaseForm
public record CaseFormDto(
        CaseType caseType,
        EquipmentCategory category,
        String modelName,
        LocalDate purchaseDate,
        String reason) {
}
