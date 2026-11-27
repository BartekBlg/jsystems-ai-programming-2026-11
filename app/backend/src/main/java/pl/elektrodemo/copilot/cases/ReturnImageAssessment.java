package pl.elektrodemo.copilot.cases;

import java.util.List;

public record ReturnImageAssessment(
        boolean photoSufficient,
        String insufficiencyReason,
        boolean damageVisible,
        List<SignOfUse> signsOfUse,
        boolean resellableAsNew,
        String visibleDescription,
        Confidence confidence) implements ImageAssessment {
}
