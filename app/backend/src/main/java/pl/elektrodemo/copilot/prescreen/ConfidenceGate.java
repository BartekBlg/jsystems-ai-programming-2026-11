package pl.elektrodemo.copilot.prescreen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;

@Component
public class ConfidenceGate {

    private static final List<String> RETURN_QUESTIONS = List.of("photo_usable", "shows_declared_category",
            "device_category", "visible_damage", "use_scratches", "use_dirt", "use_wear",
            "missing_protective_film", "missing_accessories", "condition");
    private static final List<String> COMPLAINT_QUESTIONS = List.of("photo_usable", "shows_declared_category",
            "device_category", "visible_damage", "condition", "damage_cause", "damage_type", "damage_location");

    public GateResult evaluate(CaseType caseType, List<PrescreenAnswer> photoAnswers,
            List<PrescreenAnswer> consistencySignals, double threshold) {
        return evaluate(caseType, null, photoAnswers, consistencySignals, threshold);
    }

    public GateResult evaluate(CaseType caseType, EquipmentCategory declaredCategory,
            List<PrescreenAnswer> photoAnswers, List<PrescreenAnswer> consistencySignals, double threshold) {
        List<String> reasons = new ArrayList<>();
        Map<String, PrescreenAnswer> byId = index(photoAnswers);
        List<String> required = caseType == CaseType.RETURN ? RETURN_QUESTIONS : COMPLAINT_QUESTIONS;

        for (String id : required) {
            PrescreenAnswer answer = byId.get(id);
            if (answer == null) {
                reasons.add("MISSING:" + id);
            } else if (answer.refused()) {
                reasons.add("REFUSAL:" + id);
            } else if (answer.confidence() < threshold) {
                reasons.add("LOW_CONFIDENCE:" + id);
            }
        }

        checkPhotoConsistency(byId, declaredCategory, reasons);
        if (caseType == CaseType.COMPLAINT) {
            checkDamageConsistency(byId, reasons);
        }
        if (hasConfidentModelMismatch(consistencySignals, threshold)) {
            reasons.add("MODEL_MISMATCH");
        }
        return new GateResult(reasons.isEmpty() ? GateStatus.CONFIDENT : GateStatus.UNCERTAIN, reasons);
    }

    private static Map<String, PrescreenAnswer> index(List<PrescreenAnswer> answers) {
        Map<String, PrescreenAnswer> result = new LinkedHashMap<>();
        if (answers != null) {
            for (PrescreenAnswer answer : answers) {
                if (answer != null && answer.questionId() != null) {
                    result.putIfAbsent(answer.questionId(), answer);
                }
            }
        }
        return result;
    }

    private static void checkPhotoConsistency(Map<String, PrescreenAnswer> answers,
            EquipmentCategory declaredCategory, List<String> reasons) {
        PrescreenAnswer usable = answers.get("photo_usable");
        PrescreenAnswer declared = answers.get("shows_declared_category");
        PrescreenAnswer device = answers.get("device_category");
        if (usable == null || declared == null || device == null || usable.refused() || declared.refused()
                || device.refused() || !(usable.value() instanceof Boolean usableValue)
                || !(declared.value() instanceof Boolean declaredValue) || !(device.value() instanceof String deviceValue)) {
            return;
        }
        boolean inconsistent = usableValue != declaredValue;
        String expectedCategory = categoryKey(declaredCategory);
        if (expectedCategory == null) {
            inconsistent |= declaredValue == "no_device".equals(deviceValue);
        } else {
            inconsistent |= declaredValue != expectedCategory.equals(deviceValue);
        }
        addOnce(reasons, inconsistent, "INCONSISTENT:category");
    }

    private static void checkDamageConsistency(Map<String, PrescreenAnswer> answers, List<String> reasons) {
        PrescreenAnswer visibleDamage = answers.get("visible_damage");
        PrescreenAnswer cause = answers.get("damage_cause");
        PrescreenAnswer type = answers.get("damage_type");
        if (visibleDamage == null || cause == null || type == null || visibleDamage.refused() || cause.refused()
                || type.refused() || !(visibleDamage.value() instanceof Boolean damageValue)
                || !(cause.value() instanceof String causeValue) || !(type.value() instanceof String typeValue)) {
            return;
        }
        boolean causeSaysNone = "no_damage_visible".equals(causeValue);
        boolean typeSaysNone = "none_visible".equals(typeValue);
        boolean inconsistent = causeSaysNone == damageValue || typeSaysNone != causeSaysNone;
        addOnce(reasons, inconsistent, "INCONSISTENT:damage");
    }

    private static boolean hasConfidentModelMismatch(List<PrescreenAnswer> signals, double threshold) {
        if (signals == null) {
            return false;
        }
        for (PrescreenAnswer signal : signals) {
            if (signal != null && "model_matches_photo".equals(signal.questionId()) && !signal.refused()
                    && signal.probability() != null && signal.probability() <= 0.20
                    && signal.confidence() >= threshold) {
                return true;
            }
        }
        return false;
    }

    private static void addOnce(List<String> reasons, boolean condition, String reason) {
        // Each consistency rule is evaluated exactly once per case, so reason codes are unique by construction.
        if (condition) {
            reasons.add(reason);
        }
    }

    private static String categoryKey(EquipmentCategory category) {
        if (category == null) {
            return null;
        }
        return switch (category) {
            case GAME_CONSOLE -> "game_console";
            case OTHER -> "other_device";
            default -> category.name().toLowerCase(java.util.Locale.ROOT);
        };
    }
}
