package pl.elektrodemo.copilot.prescreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.ComplaintImageAssessment;
import pl.elektrodemo.copilot.cases.Confidence;
import pl.elektrodemo.copilot.cases.ImageAssessment;
import pl.elektrodemo.copilot.cases.ProbableCause;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;
import pl.elektrodemo.copilot.cases.SignOfUse;
import pl.elektrodemo.copilot.config.AppProperties;

@Component
public class PrescreenAssessmentMapper {

    private final double threshold;
    private final ResourceBundle templates = ResourceBundle.getBundle("prescreen.templates_pl",
            Locale.forLanguageTag("pl"));

    public PrescreenAssessmentMapper(AppProperties properties) {
        this.threshold = properties.decisions().confidenceThreshold();
    }

    public ImageAssessment map(CaseType caseType, PrescreenResult result) {
        if (result == null || result.status() != PrescreenStatus.OK || result.gate() != GateStatus.CONFIDENT) {
            throw new IllegalStateException("Only a CONFIDENT pre-screen result can be mapped");
        }
        return caseType == CaseType.RETURN ? mapReturn(result.photoAnswers()) : mapComplaint(result.photoAnswers());
    }

    private ReturnImageAssessment mapReturn(List<PrescreenAnswer> answers) {
        boolean photoSufficient = bool(answers, "photo_usable") && bool(answers, "shows_declared_category");
        String insufficiency = photoSufficient ? null : insufficiency(answers);
        boolean damageVisible = bool(answers, "visible_damage");
        List<SignOfUse> signs = new ArrayList<>();
        addSign(signs, answers, "use_scratches", SignOfUse.SCRATCHES);
        addSign(signs, answers, "use_dirt", SignOfUse.DIRT);
        addSign(signs, answers, "use_wear", SignOfUse.WEAR);
        addSign(signs, answers, "missing_protective_film", SignOfUse.MISSING_PROTECTIVE_FILM);
        addSign(signs, answers, "missing_accessories", SignOfUse.MISSING_ACCESSORIES);
        int condition = conditionLevel(answers);
        boolean resellableAsNew = condition <= 1 && signs.isEmpty() && !damageVisible;
        String visible = text("description.return", text("category." + categoryCode(value(answers, "device_category")),
                "urządzenie"), text("condition." + condition, "nieokreślony stan"),
                damageVisible ? text("damage.visible", "widoczne uszkodzenie")
                        : text("damage.none", "brak widocznych uszkodzeń"), signsText(signs));
        return new ReturnImageAssessment(photoSufficient, insufficiency, damageVisible, List.copyOf(signs),
                resellableAsNew, visible, confidence(answers));
    }

    private ComplaintImageAssessment mapComplaint(List<PrescreenAnswer> answers) {
        boolean photoSufficient = bool(answers, "photo_usable") && bool(answers, "shows_declared_category");
        String insufficiency = photoSufficient ? null : insufficiency(answers);
        boolean defectVisible = bool(answers, "visible_damage");
        String causeKey = string(answers, "damage_cause");
        ProbableCause cause = switch (causeKey) {
            case "manufacturing_defect" -> ProbableCause.MANUFACTURING_DEFECT;
            case "normal_wear" -> ProbableCause.NORMAL_WEAR;
            case "mechanical_damage" -> ProbableCause.MECHANICAL_DAMAGE;
            case "liquid_damage" -> ProbableCause.LIQUID_DAMAGE;
            default -> ProbableCause.UNKNOWN;
        };
        String typeKey = string(answers, "damage_type");
        String locationKey = string(answers, "damage_location");
        int condition = conditionLevel(answers);
        String causeLabel = text("cause." + cause.name(), "nieznana przyczyna");
        String defectLabel = text("damageType." + typeKey, typeKey);
        String locationLabel = text("damageLocation." + locationKey, locationKey);
        String visualDamage = defectVisible
                ? text("description.complaint.damage", defectLabel, locationLabel)
                : text("damage.none", "brak widocznych uszkodzeń");
        String visible = text("description.complaint", text("category." + categoryCode(value(answers, "device_category")),
                "urządzenie"), text("condition." + condition, "nieokreślony stan"), visualDamage);
        return new ComplaintImageAssessment(photoSufficient, insufficiency, defectVisible, defectLabel, locationLabel,
                cause, text("cause.reasoning", causeLabel), visible, confidence(answers));
    }

    private String insufficiency(List<PrescreenAnswer> answers) {
        if (!bool(answers, "photo_usable")) {
            return text("photo.insufficient.quality");
        }
        return text("photo.insufficient.category");
    }

    private static void addSign(List<SignOfUse> signs, List<PrescreenAnswer> answers, String id, SignOfUse sign) {
        if (bool(answers, id)) {
            signs.add(sign);
        }
    }

    private static int conditionLevel(List<PrescreenAnswer> answers) {
        try {
            return Integer.parseInt(string(answers, "condition"));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Confident result has no condition level", exception);
        }
    }

    private static boolean bool(List<PrescreenAnswer> answers, String id) {
        Object value = value(answers, id);
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalStateException("Confident result has no Boolean answer for " + id);
    }

    private static String string(List<PrescreenAnswer> answers, String id) {
        Object value = value(answers, id);
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalStateException("Confident result has no value for " + id);
    }

    private static Object value(List<PrescreenAnswer> answers, String id) {
        if (answers != null) {
            for (PrescreenAnswer answer : answers) {
                if (id.equals(answer.questionId()) && !answer.refused()) {
                    return answer.value();
                }
            }
        }
        throw new IllegalStateException("Confident result is missing answer " + id);
    }

    private Confidence confidence(List<PrescreenAnswer> answers) {
        double min = answers.stream().mapToDouble(PrescreenAnswer::confidence).min()
                .orElseThrow(() -> new IllegalStateException("Confident result has no photo answers"));
        if (min >= 0.95) {
            return Confidence.HIGH;
        }
        if (min >= threshold) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    private String signsText(List<SignOfUse> signs) {
        if (signs.isEmpty()) {
            return text("sign.none");
        }
        return signs.stream().map(sign -> text("sign." + sign.name())).collect(java.util.stream.Collectors.joining(", "));
    }

    private String text(String key, Object... values) {
        String value = templates.getString(key);
        for (int index = 0; index < values.length; index++) {
            value = value.replace("{" + index + "}", String.valueOf(values[index]));
        }
        return value;
    }

    private static String categoryCode(Object value) {
        return value instanceof String category ? category.toUpperCase(Locale.ROOT) : "OTHER_DEVICE";
    }
}
