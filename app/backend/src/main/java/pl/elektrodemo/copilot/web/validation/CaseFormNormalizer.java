package pl.elektrodemo.copilot.web.validation;

import pl.elektrodemo.copilot.web.dto.CaseFormDto;

public final class CaseFormNormalizer {

    private CaseFormNormalizer() {
    }

    /** Returns a copy with trimmed modelName and reason; a null reason becomes an empty string. */
    public static CaseFormDto trim(CaseFormDto form) {
        String model = form.modelName() == null ? null : form.modelName().strip();
        String reason = form.reason() == null ? "" : form.reason().strip();
        return new CaseFormDto(form.caseType(), form.category(), model, form.purchaseDate(), reason);
    }
}
