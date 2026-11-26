package pl.elektrodemo.copilot.prescreen;

import java.util.Comparator;
import java.util.Map;

/** A normalized answer returned by an OpenRouter Decisions question. */
public record PrescreenAnswer(
        String questionId,
        String type,
        Object value,
        Double probability,
        double confidence,
        Double score,
        boolean refused) {

    public static PrescreenAnswer from(String questionId, DecisionsResponse.DecisionAnswer answer) {
        if (answer == null || answer.type() == null) {
            return refused(questionId, "missing");
        }
        return switch (answer.type()) {
            case "noul" -> normalizeNoul(questionId, answer);
            case "choice" -> normalizeChoice(questionId, answer);
            case "score" -> normalizeScore(questionId, answer);
            case "refusal" -> refused(questionId, "refusal");
            default -> refused(questionId, answer.type());
        };
    }

    private static PrescreenAnswer normalizeNoul(String questionId, DecisionsResponse.DecisionAnswer answer) {
        Double p = answer.noul();
        if (!probability(p)) {
            return refused(questionId, "noul");
        }
        double confidence = answer.confidence() == null ? Math.max(p, 1 - p) : answer.confidence();
        if (!probability(confidence)) {
            return refused(questionId, "noul");
        }
        return new PrescreenAnswer(questionId, "noul", p >= 0.5, p, confidence, null, false);
    }

    private static PrescreenAnswer normalizeChoice(String questionId, DecisionsResponse.DecisionAnswer answer) {
        if (answer.choice() == null || answer.choice().isBlank() || !probability(answer.confidence())) {
            return refused(questionId, "choice");
        }
        return new PrescreenAnswer(questionId, "choice", answer.choice(), null, answer.confidence(), null, false);
    }

    private static PrescreenAnswer normalizeScore(String questionId, DecisionsResponse.DecisionAnswer answer) {
        Map<String, Double> probabilities = answer.probabilities();
        if (answer.score() == null || !Double.isFinite(answer.score()) || probabilities == null
                || probabilities.isEmpty() || !probability(answer.confidence())
                || probabilities.values().stream().anyMatch(value -> !probability(value))) {
            return refused(questionId, "score");
        }
        String level = probabilities.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue)
                        .thenComparing(entry -> -parseLevel(entry.getKey())))
                .map(Map.Entry::getKey).orElse(null);
        if (level == null) {
            return refused(questionId, "score");
        }
        return new PrescreenAnswer(questionId, "score", level, null, answer.confidence(), answer.score(), false);
    }

    private static int parseLevel(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private static boolean probability(Double value) {
        return value != null && Double.isFinite(value) && value >= 0 && value <= 1;
    }

    private static PrescreenAnswer refused(String questionId, String type) {
        return new PrescreenAnswer(questionId, type, null, null, 0, null, true);
    }
}
