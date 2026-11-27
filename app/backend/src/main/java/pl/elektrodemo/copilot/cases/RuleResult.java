package pl.elektrodemo.copilot.cases;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** Serialized as {@code ruleFacts}. Category sets are EnumSet-backed, so they serialize in enum order. */
public record RuleResult(
        int daysSincePurchase,
        ReturnWindowStatus returnWindowStatus,
        boolean liabilityExpired,
        Set<DecisionCategory> allowedCategories,
        String requiredReasonText,
        Set<DecisionCategory> chatForbiddenCategories,
        String rejectionRequiresCitation,
        String appliedRule) {

    public RuleResult {
        allowedCategories = enumSet(allowedCategories);
        chatForbiddenCategories = enumSet(chatForbiddenCategories);
    }

    private static Set<DecisionCategory> enumSet(Collection<DecisionCategory> source) {
        EnumSet<DecisionCategory> set = EnumSet.noneOf(DecisionCategory.class);
        if (source != null) {
            set.addAll(source);
        }
        return set;
    }
}
