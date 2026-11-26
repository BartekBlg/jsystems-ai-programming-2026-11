package pl.elektrodemo.copilot.prescreen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.ComplaintImageAssessment;
import pl.elektrodemo.copilot.cases.Confidence;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.PolicyRuleEngine;
import pl.elektrodemo.copilot.cases.ProbableCause;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;
import pl.elektrodemo.copilot.cases.SignOfUse;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.config.AppProperties.Decisions;

class PrescreenAssessmentMapperTest {

    private final PrescreenAssessmentMapper mapper = new PrescreenAssessmentMapper(
            new AppProperties("test-only-key", null,
                    new Decisions("shadow", "typesafe/jev-1.14", "http://localhost", .8,
                            java.time.Duration.ofSeconds(6))));

    @Test
    void mapsConfidentReturnSignsConditionAndFactualPolishTemplate() {
        List<PrescreenAnswer> answers = confidentAnswers(CaseType.RETURN);
        replace(answers, noul("use_scratches", .9, .9));
        replace(answers, score("condition", 2, .95));

        ReturnImageAssessment assessment = (ReturnImageAssessment) mapper.map(CaseType.RETURN,
                confident(CaseType.RETURN, answers));

        assertThat(assessment.signsOfUse()).containsExactly(SignOfUse.SCRATCHES);
        assertThat(assessment.resellableAsNew()).isFalse();
        assertThat(assessment.visibleDescription()).contains("Laptop", "lekkie ślady użytkowania", "zarysowania");
        assertThat(assessment.confidence()).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    void mapsMechanicalComplaintToHighConfidenceAndUnknownWhenNoDamage() {
        List<PrescreenAnswer> answers = confidentAnswers(CaseType.COMPLAINT);
        for (int index = 0; index < answers.size(); index++) {
            PrescreenAnswer answer = answers.get(index);
            if ("noul".equals(answer.type())) {
                answers.set(index, new PrescreenAnswer(answer.questionId(), answer.type(), answer.value(),
                        answer.probability(), .97, answer.score(), answer.refused()));
            }
        }
        replace(answers, choice("damage_cause", "mechanical_damage", .97));
        replace(answers, choice("damage_type", "screen_crack", .97));
        replace(answers, choice("damage_location", "screen", .97));
        ComplaintImageAssessment damage = (ComplaintImageAssessment) mapper.map(CaseType.COMPLAINT,
                confident(CaseType.COMPLAINT, answers));
        assertThat(damage.probableCause()).isEqualTo(ProbableCause.MECHANICAL_DAMAGE);
        assertThat(damage.confidence()).isEqualTo(Confidence.HIGH);
        assertThat(damage.causeReasoning()).contains("Ocena automatyczna na podstawie zdjęcia");
        var ruleResult = new PolicyRuleEngine().evaluate(CaseType.COMPLAINT, LocalDate.of(2026, 5, 26),
                OffsetDateTime.parse("2026-11-26T10:00:00+01:00"), damage);
        assertThat(ruleResult.allowedCategories()).containsExactlyInAnyOrder(DecisionCategory.REJECTED,
                DecisionCategory.ESCALATED).doesNotContain(DecisionCategory.ACCEPTED);

        replace(answers, noul("visible_damage", .1, .9));
        replace(answers, choice("damage_cause", "no_damage_visible", .97));
        replace(answers, choice("damage_type", "none_visible", .97));
        replace(answers, choice("damage_location", "none", .97));
        ComplaintImageAssessment noDamage = (ComplaintImageAssessment) mapper.map(CaseType.COMPLAINT,
                confident(CaseType.COMPLAINT, answers));
        assertThat(noDamage.defectVisible()).isFalse();
        assertThat(noDamage.probableCause()).isEqualTo(ProbableCause.UNKNOWN);
    }

    @Test
    void mapsConfidentButInsufficientPhotoAndRejectsUncertainResults() {
        List<PrescreenAnswer> answers = confidentAnswers(CaseType.RETURN);
        replace(answers, noul("photo_usable", .1, .9));
        replace(answers, noul("shows_declared_category", .1, .9));
        ReturnImageAssessment assessment = (ReturnImageAssessment) mapper.map(CaseType.RETURN,
                confident(CaseType.RETURN, answers));
        assertThat(assessment.photoSufficient()).isFalse();
        assertThat(assessment.insufficiencyReason()).contains("nieostre");

        PrescreenResult uncertain = new PrescreenResult(PrescreenStatus.OK, answers, List.of(),
                GateStatus.UNCERTAIN, List.of("LOW_CONFIDENCE:condition"), "test", 10, 0, "model", null);
        assertThatThrownBy(() -> mapper.map(CaseType.RETURN, uncertain)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mapsNoSignsAndBestConditionToResellableAsNew() {
        List<PrescreenAnswer> answers = confidentAnswers(CaseType.RETURN);

        ReturnImageAssessment assessment = (ReturnImageAssessment) mapper.map(CaseType.RETURN,
                confident(CaseType.RETURN, answers));

        assertThat(assessment.resellableAsNew()).isTrue();
        assertThat(assessment.signsOfUse()).isEmpty();
    }

    private static PrescreenResult confident(CaseType type, List<PrescreenAnswer> answers) {
        return new PrescreenResult(PrescreenStatus.OK, answers, List.of(), GateStatus.CONFIDENT, List.of(), "test",
                10, 0, "model", null);
    }

    private static List<PrescreenAnswer> confidentAnswers(CaseType type) {
        List<PrescreenAnswer> values = new ArrayList<>(List.of(noul("photo_usable", .9, .9),
                noul("shows_declared_category", .9, .9), choice("device_category", "laptop", .97),
                noul("visible_damage", .1, .9), score("condition", 0, .97)));
        if (type == CaseType.RETURN) {
            values.addAll(List.of(noul("use_scratches", .1, .9), noul("use_dirt", .1, .9),
                    noul("use_wear", .1, .9), noul("missing_protective_film", .1, .9),
                    noul("missing_accessories", .1, .9)));
        } else {
            values.addAll(List.of(choice("damage_cause", "no_damage_visible", .97),
                    choice("damage_type", "none_visible", .97), choice("damage_location", "none", .97)));
        }
        return values;
    }

    private static PrescreenAnswer noul(String id, double p, double confidence) {
        return new PrescreenAnswer(id, "noul", p >= .5, p, confidence, null, false);
    }

    private static PrescreenAnswer choice(String id, String value, double confidence) {
        return new PrescreenAnswer(id, "choice", value, null, confidence, null, false);
    }

    private static PrescreenAnswer score(String id, int level, double confidence) {
        return new PrescreenAnswer(id, "score", Integer.toString(level), null, confidence, (double) level, false);
    }

    private static void replace(List<PrescreenAnswer> answers, PrescreenAnswer replacement) {
        answers.removeIf(answer -> answer.questionId().equals(replacement.questionId()));
        answers.add(replacement);
    }
}
