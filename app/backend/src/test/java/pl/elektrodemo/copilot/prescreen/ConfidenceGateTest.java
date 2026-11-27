package pl.elektrodemo.copilot.prescreen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConfidenceGateTest {

    private final ConfidenceGate gate = new ConfidenceGate();

    @Test
    void allQuestionsAtThresholdAreConfidentAndConfidentUnusablePhotoIsValid() {
        List<PrescreenAnswer> thresholdAnswers = answers(CaseType.RETURN);
        for (int index = 0; index < thresholdAnswers.size(); index++) {
            PrescreenAnswer answer = thresholdAnswers.get(index);
            thresholdAnswers.set(index, new PrescreenAnswer(answer.questionId(), answer.type(), answer.value(),
                    answer.probability(), 0.80, answer.score(), answer.refused()));
        }
        GateResult confident = gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, thresholdAnswers,
                List.of(), 0.80);
        assertThat(confident.status()).isEqualTo(GateStatus.CONFIDENT);
        assertThat(confident.reasons()).isEmpty();

        List<PrescreenAnswer> notUsable = answers(CaseType.RETURN);
        replace(notUsable, noul("photo_usable", 0.10, 0.90));
        replace(notUsable, noul("shows_declared_category", 0.10, 0.90));
        replace(notUsable, choice("device_category", "smartphone", 0.95));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, notUsable, List.of(), 0.80).status())
                .isEqualTo(GateStatus.CONFIDENT);
    }

    @Test
    void reportsLowConfidenceRefusalMissingAndUnexpectedAnswers() {
        List<PrescreenAnswer> low = answers(CaseType.RETURN);
        replace(low, noul("visible_damage", 0.79, 0.79));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, low, List.of(), 0.80).reasons())
                .containsExactly("LOW_CONFIDENCE:visible_damage");

        List<PrescreenAnswer> refusal = answers(CaseType.RETURN);
        replace(refusal, refused("condition"));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, refusal, List.of(), 0.80).reasons())
                .contains("REFUSAL:condition");

        List<PrescreenAnswer> missing = answers(CaseType.RETURN);
        missing.removeIf(answer -> answer.questionId().equals("condition"));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, missing, List.of(), 0.80).reasons())
                .contains("MISSING:condition");

        List<PrescreenAnswer> unexpected = answers(CaseType.RETURN);
        replace(unexpected, refused("condition"));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, unexpected, List.of(), 0.80).reasons())
                .contains("REFUSAL:condition");
    }

    @Test
    void reportsUsabilityCategoryAndComplaintDamageInconsistency() {
        List<PrescreenAnswer> contradictoryPhoto = answers(CaseType.RETURN);
        replace(contradictoryPhoto, noul("photo_usable", 0.90, 0.90));
        replace(contradictoryPhoto, noul("shows_declared_category", 0.10, 0.90));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, contradictoryPhoto, List.of(), 0.80).reasons())
                .contains("INCONSISTENT:category");

        List<PrescreenAnswer> wrongCategory = answers(CaseType.RETURN);
        replace(wrongCategory, choice("device_category", "smartphone", 0.95));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, wrongCategory, List.of(), 0.80).reasons())
                .contains("INCONSISTENT:category");

        List<PrescreenAnswer> complaint = answers(CaseType.COMPLAINT);
        replace(complaint, choice("damage_cause", "no_damage_visible", 0.95));
        replace(complaint, choice("damage_type", "screen_crack", 0.95));
        assertThat(gate.evaluate(CaseType.COMPLAINT, EquipmentCategory.LAPTOP, complaint, List.of(), 0.80).reasons())
                .contains("INCONSISTENT:damage");
    }

    @Test
    void confidentModelMismatchForcesUncertainButOtherBSignalsDoNotAffectGate() {
        List<PrescreenAnswer> signals = List.of(noul("model_matches_photo", 0.20, 0.80),
                noul("description_matches_photo", 0.01, 0.99));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, answers(CaseType.RETURN), signals, 0.80).reasons())
                .containsExactly("MODEL_MISMATCH");

        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, answers(CaseType.RETURN),
                List.of(noul("model_matches_photo", 0.19, 0.79)), 0.80).status())
                .isEqualTo(GateStatus.CONFIDENT);
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, answers(CaseType.RETURN),
                List.of(noul("reason_contains_instructions", 1.0, 1.0)), 0.80).status())
                .isEqualTo(GateStatus.CONFIDENT);
    }

    @Test
    void realJevCapturesMatchDocumentedConfidentAndUncertainOutcomes() throws IOException {
        JsonMapper mapper = JsonMapper.builder().build();
        DecisionsResponse confidentCapture = response(mapper, "a-complaint-phone-1.json");
        DecisionsResponse uncertainCapture = response(mapper, "a-return-laptop-2.json");

        GateResult confident = gate.evaluate(CaseType.COMPLAINT, EquipmentCategory.SMARTPHONE,
                normalize(confidentCapture, CaseType.COMPLAINT), List.of(), .80);
        GateResult uncertain = gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP,
                normalize(uncertainCapture, CaseType.RETURN), List.of(), .80);

        assertThat(confident.status()).isEqualTo(GateStatus.CONFIDENT);
        assertThat(uncertain.status()).isEqualTo(GateStatus.UNCERTAIN);
        assertThat(uncertain.reasons()).containsExactly("LOW_CONFIDENCE:photo_usable", "LOW_CONFIDENCE:use_scratches",
                "LOW_CONFIDENCE:use_dirt", "LOW_CONFIDENCE:use_wear", "LOW_CONFIDENCE:missing_protective_film",
                "LOW_CONFIDENCE:missing_accessories", "LOW_CONFIDENCE:condition");
    }

    @Test
    void nullMalformedAndRefusedAnswersFailClosedAndExerciseMissingBranches() {
        assertThat(gate.evaluate(CaseType.RETURN, (List<PrescreenAnswer>) null, null, .8).reasons())
                .contains("MISSING:photo_usable", "MISSING:condition");
        List<PrescreenAnswer> withNulls = new ArrayList<>(Arrays.asList(null,
                new PrescreenAnswer(null, "noul", true, 1.0, 1.0, null, false)));
        assertThat(gate.evaluate(CaseType.RETURN, withNulls, null, .8).status()).isEqualTo(GateStatus.UNCERTAIN);

        for (String id : List.of("photo_usable", "shows_declared_category", "device_category")) {
            List<PrescreenAnswer> missing = answers(CaseType.RETURN);
            missing.removeIf(answer -> answer.questionId().equals(id));
            gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, missing, List.of(), .8);

            List<PrescreenAnswer> refused = answers(CaseType.RETURN);
            replace(refused, refused(id));
            gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, refused, List.of(), .8);

            List<PrescreenAnswer> malformed = answers(CaseType.RETURN);
            replace(malformed, malformed(id));
            gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, malformed, List.of(), .8);
        }
    }

    @Test
    void complaintDamageRuleFailsClosedForMissingRefusedAndUnexpectedValues() {
        for (String id : List.of("visible_damage", "damage_cause", "damage_type")) {
            List<PrescreenAnswer> missing = answers(CaseType.COMPLAINT);
            missing.removeIf(answer -> answer.questionId().equals(id));
            gate.evaluate(CaseType.COMPLAINT, EquipmentCategory.LAPTOP, missing, List.of(), .8);

            List<PrescreenAnswer> refused = answers(CaseType.COMPLAINT);
            replace(refused, refused(id));
            gate.evaluate(CaseType.COMPLAINT, EquipmentCategory.LAPTOP, refused, List.of(), .8);

            List<PrescreenAnswer> malformed = answers(CaseType.COMPLAINT);
            replace(malformed, malformed(id));
            gate.evaluate(CaseType.COMPLAINT, EquipmentCategory.LAPTOP, malformed, List.of(), .8);
        }

        List<PrescreenAnswer> causeMismatch = answers(CaseType.COMPLAINT);
        replace(causeMismatch, noul("visible_damage", .9, .9));
        replace(causeMismatch, choice("damage_cause", "no_damage_visible", .95));
        replace(causeMismatch, choice("damage_type", "none_visible", .95));
        assertThat(gate.evaluate(CaseType.COMPLAINT, EquipmentCategory.LAPTOP, causeMismatch, List.of(), .8).reasons())
                .contains("INCONSISTENT:damage");
    }

    @Test
    void modelMismatchSignalRequiresEveryConditionAndSupportsMissingDeclaredCategoryFallback() {
        List<PrescreenAnswer> answers = answers(CaseType.RETURN);
        assertThat(gate.evaluate(CaseType.RETURN, answers, null, .8).status()).isEqualTo(GateStatus.CONFIDENT);
        List<PrescreenAnswer> fallback = answers(CaseType.RETURN);
        replace(fallback, choice("device_category", "no_device", .95));
        assertThat(gate.evaluate(CaseType.RETURN, fallback, List.of(), .8).reasons())
                .contains("INCONSISTENT:category");

        List<PrescreenAnswer> signals = new ArrayList<>(Arrays.asList(null,
                noul("other_signal", .01, .99), refused("model_matches_photo"),
                new PrescreenAnswer("model_matches_photo", "noul", false, null, .99, null, false),
                noul("model_matches_photo", .21, .99), noul("model_matches_photo", .20, .79)));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, answers, signals, .8).status())
                .isEqualTo(GateStatus.CONFIDENT);
        signals.add(noul("model_matches_photo", .20, .80));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.LAPTOP, answers, signals, .8).reasons())
                .contains("MODEL_MISMATCH");
    }

    @Test
    void deviceCategoryMappingCoversOtherAndGameConsoleCategories() {
        List<PrescreenAnswer> console = answers(CaseType.RETURN);
        replace(console, choice("device_category", "game_console", .95));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.GAME_CONSOLE, console, List.of(), .8).status())
                .isEqualTo(GateStatus.CONFIDENT);
        List<PrescreenAnswer> other = answers(CaseType.RETURN);
        replace(other, choice("device_category", "other_device", .95));
        assertThat(gate.evaluate(CaseType.RETURN, EquipmentCategory.OTHER, other, List.of(), .8).status())
                .isEqualTo(GateStatus.CONFIDENT);
    }

    private static List<PrescreenAnswer> answers(CaseType type) {
        List<PrescreenAnswer> values = new ArrayList<>(List.of(noul("photo_usable", .9, .9),
                noul("shows_declared_category", .9, .9), choice("device_category", "laptop", .95),
                noul("visible_damage", .1, .9), score("condition", 2, .95)));
        if (type == CaseType.RETURN) {
            values.addAll(List.of(noul("use_scratches", .1, .9), noul("use_dirt", .1, .9),
                    noul("use_wear", .1, .9), noul("missing_protective_film", .1, .9),
                    noul("missing_accessories", .1, .9)));
        } else {
            values.addAll(List.of(choice("damage_cause", "no_damage_visible", .95),
                    choice("damage_type", "none_visible", .95), choice("damage_location", "none", .95)));
        }
        return values;
    }

    private static PrescreenAnswer noul(String id, double probability, double confidence) {
        return new PrescreenAnswer(id, "noul", probability >= .5, probability, confidence, null, false);
    }

    private static PrescreenAnswer choice(String id, String value, double confidence) {
        return new PrescreenAnswer(id, "choice", value, null, confidence, null, false);
    }

    private static PrescreenAnswer score(String id, int level, double confidence) {
        return new PrescreenAnswer(id, "score", Integer.toString(level), null, confidence, (double) level, false);
    }

    private static PrescreenAnswer refused(String id) {
        return new PrescreenAnswer(id, "refusal", null, null, 0, null, true);
    }

    private static PrescreenAnswer malformed(String id) {
        boolean noul = "photo_usable".equals(id) || "shows_declared_category".equals(id)
                || "visible_damage".equals(id);
        return new PrescreenAnswer(id, noul ? "noul" : "choice", 123, .95, .95, null, false);
    }

    private static void replace(List<PrescreenAnswer> answers, PrescreenAnswer replacement) {
        answers.removeIf(answer -> answer.questionId().equals(replacement.questionId()));
        answers.add(replacement);
    }

    private static DecisionsResponse response(JsonMapper mapper, String name) throws IOException {
        JsonNode envelope = mapper.readTree(Files.readString(Path.of("src/test/resources/captures/decisions", name)));
        return mapper.treeToValue(envelope.get("body"), DecisionsResponse.class);
    }

    private static List<PrescreenAnswer> normalize(DecisionsResponse response, CaseType type) {
        List<PrescreenAnswer> answers = new ArrayList<>();
        List<String> ids = type == CaseType.RETURN
                ? List.of("photo_usable", "shows_declared_category", "device_category", "visible_damage", "use_scratches",
                        "use_dirt", "use_wear", "missing_protective_film", "missing_accessories", "condition")
                : List.of("photo_usable", "shows_declared_category", "device_category", "visible_damage", "condition",
                        "damage_cause", "damage_type", "damage_location");
        ids.stream().filter(response.answers()::containsKey).map(response::normalize).forEach(answers::add);
        return answers;
    }
}
