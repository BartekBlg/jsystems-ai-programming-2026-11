package pl.elektrodemo.copilot.prescreen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PrescreenAnswerTest {

    private static final Path CAPTURES = Path.of("src/test/resources/captures/decisions");
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void normalizesNoulConfidenceFallbackChoiceAndScoreArgmax() {
        assertThat(PrescreenAnswer.from("p92", noul(0.92, null))).satisfies(answer -> {
            assertThat(answer.value()).isEqualTo(true);
            assertThat(answer.probability()).isEqualTo(0.92);
            assertThat(answer.confidence()).isEqualTo(0.92);
        });
        assertThat(PrescreenAnswer.from("p10", noul(0.10, null)).confidence()).isEqualTo(0.90);
        assertThat(PrescreenAnswer.from("p55", noul(0.55, null)).confidence()).isEqualTo(0.55);
        assertThat(PrescreenAnswer.from("p40", noul(0.40, 0.73)).confidence()).isEqualTo(0.73);

        DecisionsResponse.DecisionAnswer choice = new DecisionsResponse.DecisionAnswer("choice", null, "screen",
                null, null, 0.70, null);
        assertThat(PrescreenAnswer.from("choice", choice).confidence()).isEqualTo(0.70);

        DecisionsResponse.DecisionAnswer score = new DecisionsResponse.DecisionAnswer("score", null, null, 2.4,
                java.util.Map.of("0", 0.1, "1", 0.2, "2", 0.7), 0.8, null);
        assertThat(PrescreenAnswer.from("score", score)).satisfies(answer -> {
            assertThat(answer.value()).isEqualTo("2");
            assertThat(answer.score()).isEqualTo(2.4);
        });
    }

    @Test
    void parsesAllRealDecisionCapturesAndIgnoresProviderSpecificUnknownFields() throws IOException {
        for (String file : new String[] {"a-complaint-laptop-1.json", "a-complaint-laptop-2.json",
                "a-complaint-phone-1.json", "a-complaint-phone-2.json", "a-complaint-phone-3.json",
                "a-return-laptop-1.json", "a-return-laptop-2.json", "a-return-phone-1.json",
                "a-return-phone-2.json", "a-return-phone-3.json", "b-return.json", "b-complaint.json"}) {
            JsonNode envelope = mapper.readTree(Files.readString(CAPTURES.resolve(file)));
            DecisionsResponse response = mapper.treeToValue(envelope.get("body"), DecisionsResponse.class);
            assertThat(response.model()).startsWith("typesafe/jev-1.14-");
            assertThat(response.answers()).isNotEmpty();
            assertThat(response.usage().cost()).isZero();
            assertThat(response.usage().costDetails().upstreamInferenceCost()).isPositive();
        }

        DecisionsResponse response = parse("a-return-laptop-2.json");
        assertThat(PrescreenAnswer.from("photo_usable", response.answers().get("photo_usable")).confidence())
                .isEqualTo(0.78);
        assertThat(PrescreenAnswer.from("condition", response.answers().get("condition")).value()).isEqualTo("2");
    }

    @Test
    void refusalMissingAndUnexpectedAnswersFailClosed() throws IOException {
        PrescreenAnswer refusal = PrescreenAnswer.from("refused",
                new DecisionsResponse.DecisionAnswer("refusal", null, null, null, null, null, null));
        PrescreenAnswer unexpected = PrescreenAnswer.from("unexpected",
                new DecisionsResponse.DecisionAnswer("box", null, null, null, null, 1.0, null));

        assertThat(refusal).satisfies(answer -> {
            assertThat(answer.refused()).isTrue();
            assertThat(answer.confidence()).isZero();
        });
        assertThat(unexpected).satisfies(answer -> {
            assertThat(answer.refused()).isTrue();
            assertThat(answer.confidence()).isZero();
        });
        assertThat(parse("a-return-laptop-2.json").normalize("does_not_exist")).satisfies(answer -> {
            assertThat(answer.refused()).isTrue();
            assertThat(answer.confidence()).isZero();
        });
    }

    private DecisionsResponse parse(String file) throws IOException {
        JsonNode envelope = mapper.readTree(Files.readString(CAPTURES.resolve(file)));
        return mapper.treeToValue(envelope.get("body"), DecisionsResponse.class);
    }

    private static DecisionsResponse.DecisionAnswer noul(double value, Double confidence) {
        return new DecisionsResponse.DecisionAnswer("noul", value, null, null, null, confidence, null);
    }
}
