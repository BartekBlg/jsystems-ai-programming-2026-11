package pl.elektrodemo.copilot.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * T20 / TAC-002-07: startup capability check is non-fatal. No capture exists for
 * GET /models/{model}/endpoints, so only status-only stubs (empty bodies) are used.
 */
class LlmCapabilityCheckTest {

    private WireMockServer wm;
    private ListAppender<ILoggingEvent> log;
    private Logger logger;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wm.start();
        log = new ListAppender<>();
        log.start();
        logger = (Logger) LoggerFactory.getLogger(LlmCapabilityCheck.class);
        logger.addAppender(log);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(log);
        wm.stop();
    }

    private LlmCapabilityCheck check(String vision, String model, boolean allowMissingKey) {
        LlmProperties props = new LlmProperties("sk-test-fake", wm.baseUrl(), model, vision, "medium", "low",
                new LlmProperties.Timeouts(Duration.ofSeconds(26), Duration.ofSeconds(34), Duration.ofSeconds(30),
                        Duration.ofSeconds(90)),
                new LlmProperties.MaxOutputTokens(4000, 8000, 4000));
        return new LlmCapabilityCheck(props, allowMissingKey);
    }

    private List<ILoggingEvent> warnings() {
        return log.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
    }

    @Test
    void status404IsNonFatalSingleWarnPerModel() {
        wm.stubFor(get(urlEqualTo("/models/vendor/vision/endpoints")).willReturn(aResponse().withStatus(404)));
        wm.stubFor(get(urlEqualTo("/models/vendor/decide/endpoints")).willReturn(aResponse().withStatus(404)));
        assertThatCode(() -> check("vendor/vision", "vendor/decide", false).run()).doesNotThrowAnyException();
        assertThat(wm.getAllServeEvents()).hasSize(2);
        assertThat(warnings()).hasSize(2);
    }

    @Test
    void status500IsNonFatal() {
        wm.stubFor(get(urlEqualTo("/models/vendor/same/endpoints")).willReturn(aResponse().withStatus(500)));
        assertThatCode(() -> check("vendor/same", "vendor/same", false).run()).doesNotThrowAnyException();
        // same model configured twice: queried once
        assertThat(wm.getAllServeEvents()).hasSize(1);
        assertThat(warnings()).hasSize(1);
    }

    @Test
    void emptyOkBodyIsParseFailureWithSingleWarn() {
        wm.stubFor(get(urlEqualTo("/models/vendor/same/endpoints")).willReturn(aResponse().withStatus(200)));
        assertThatCode(() -> check("vendor/same", "vendor/same", false).run()).doesNotThrowAnyException();
        assertThat(warnings()).hasSize(1);
    }

    @Test
    void connectionFailureIsNonFatal() {
        LlmCapabilityCheck check = check("vendor/same", "vendor/same", false);
        wm.stop();
        assertThatCode(check::run).doesNotThrowAnyException();
        assertThat(warnings()).hasSize(1);
    }

    @Test
    void skippedWhenMissingKeyEscapeIsSet() {
        check("vendor/vision", "vendor/decide", true).run();
        assertThat(wm.getAllServeEvents()).isEmpty();
    }
}
