package pl.elektrodemo.copilot.prescreen;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.ImageAssessment;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.config.AppProperties.Decisions;
import pl.elektrodemo.copilot.support.CaptureFixtures;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ShadowComparatorTest {

    @Test
    void emitsExactlyOneShadowEventAndDoesNotLogUserText() {
        Logger logger = (Logger) LoggerFactory.getLogger("prescreen");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            PrescreenAssessmentMapper mapper = new PrescreenAssessmentMapper(
                    new AppProperties("test-key", null,
                            new Decisions("shadow", "typesafe/jev-1.14", "http://localhost", .8,
                                    java.time.Duration.ofSeconds(6))));
            ShadowComparator comparator = new ShadowComparator(mapper);
            PrescreenResult result = new PrescreenResult(PrescreenStatus.UNAVAILABLE, java.util.List.of(),
                    java.util.List.of(), GateStatus.UNCERTAIN, java.util.List.of("HTTP_401"), "catalog-v1", 12, 0,
                    "typesafe/jev-1.14-20261014", "HTTP_401");

            comparator.compare(CaseType.RETURN, result, CaptureFixtures.returnAssessmentOk(), 23);

            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage()).contains("event=prescreen.shadow",
                    "gate=UNCERTAIN", "model=typesafe/jev-1.14-20261014")
                    .doesNotContain("PRIVATE-MODEL", "PRIVATE-REASON", "klient napisał");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void comparesConfidentCapturedAnswersAgainstTheVisionAssessment() throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger("prescreen");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            PrescreenAssessmentMapper assessmentMapper = new PrescreenAssessmentMapper(
                    new AppProperties("test-key", null,
                            new Decisions("shadow", "typesafe/jev-1.14", "http://localhost", .8,
                                    java.time.Duration.ofSeconds(6))));
            ShadowComparator comparator = new ShadowComparator(assessmentMapper);
            JsonMapper jsonMapper = JsonMapper.builder().build();
            JsonNode envelope = jsonMapper.readTree(Files.readString(
                    Path.of("src/test/resources/captures/decisions/a-complaint-phone-1.json")));
            DecisionsResponse response = jsonMapper.treeToValue(envelope.get("body"), DecisionsResponse.class);
            PrescreenResult result = new PrescreenResult(PrescreenStatus.OK,
                    response.answers().keySet().stream().map(response::normalize).toList(), java.util.List.of(),
                    GateStatus.CONFIDENT, java.util.List.of(), PrescreenQuestionCatalog.CATALOG_VERSION, 125, .00016,
                    response.model(), null);
            ImageAssessment mapped = assessmentMapper.map(CaseType.COMPLAINT, result);

            comparator.compare(CaseType.COMPLAINT, result, mapped, 230);

            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getFormattedMessage()).contains("agreement={photoSufficient=true",
                    "confidenceBucket=true", "damageVisible=true", "probableCause=true")
                    .doesNotContain("Samsung", "Pęknięty ekran");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void omitsComparisonEventWhenTheVisionAssessmentFailed() {
        Logger logger = (Logger) LoggerFactory.getLogger("prescreen");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            PrescreenAssessmentMapper mapper = new PrescreenAssessmentMapper(
                    new AppProperties("test-key", null,
                            new Decisions("shadow", "typesafe/jev-1.14", "http://localhost", .8,
                                    java.time.Duration.ofSeconds(6))));
            ShadowComparator comparator = new ShadowComparator(mapper);

            comparator.compare(CaseType.RETURN, null, null, 0);

            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.getFirst().getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
            assertThat(appender.list.getFirst().getFormattedMessage()).contains("LLM_ASSESSMENT_FAILED")
                    .doesNotContain("event=prescreen.shadow ");
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
