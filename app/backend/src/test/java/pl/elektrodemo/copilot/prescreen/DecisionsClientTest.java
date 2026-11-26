package pl.elektrodemo.copilot.prescreen;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.config.AppProperties.Decisions;
import pl.elektrodemo.copilot.llm.LlmProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class DecisionsClientTest {

    private static final Path CAPTURES = Path.of("src/test/resources/captures/decisions");
    private static final String KEY = "synthetic-test-key";
    private static final String ENDPOINT = "/api/alpha/decisions";
    private final JsonMapper mapper = JsonMapper.builder().build();
    private WireMockServer wireMock;
    private DecisionsClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
        client = new DecisionsClient(new AppProperties("test-signing-key", null,
                new Decisions("shadow", "typesafe/jev-1.14", wireMock.baseUrl() + ENDPOINT, .8,
                        Duration.ofSeconds(6))), new LlmProperties(KEY, null, null, null, null, null, null, null));
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void postsCapturedSuccessWithBearerKeyAndPinnedModel() throws IOException {
        wireMock.stubFor(post(urlEqualTo(ENDPOINT)).withHeader("Authorization", equalTo("Bearer " + KEY))
                .willReturn(capturedResponse("a-complaint-phone-1.json")));

        DecisionsResponse response = client.decide(sampleQuestions(), List.of("synthetic state"));

        assertThat(response.model()).isEqualTo("typesafe/jev-1.14-20261014");
        assertThat(response.provider()).isEqualTo("TypeSafe");
        assertThat(wireMock.getAllServeEvents()).hasSize(1);
        JsonNode sent = mapper.readTree(wireMock.getAllServeEvents().getFirst().getRequest().getBodyAsString());
        assertThat(sent.get("model").asString()).isEqualTo("typesafe/jev-1.14");
        assertThat(sent.has("assets")).isFalse();
    }

    @Test
    void capturedClientErrorsAndInvalidJsonBecomeUnavailable() throws IOException {
        wireMock.stubFor(post(urlEqualTo(ENDPOINT)).willReturn(capturedResponse("error-401.json")));
        assertThatThrownBy(() -> client.decide(sampleQuestions(), List.of("state")))
                .isInstanceOf(PrescreenUnavailableException.class);

        wireMock.resetAll();
        wireMock.stubFor(post(urlEqualTo(ENDPOINT)).willReturn(capturedResponse("error-400.json")));
        assertThatThrownBy(() -> client.decide(sampleQuestions(), List.of("state")))
                .isInstanceOf(PrescreenUnavailableException.class);

        wireMock.resetAll();
        Capture goodCapture = capture("a-return-laptop-2.json");
        String malformedCapturedBody = goodCapture.body().substring(0, goodCapture.body().length() - 1);
        wireMock.stubFor(post(urlEqualTo(ENDPOINT)).willReturn(aResponse().withStatus(goodCapture.status())
                .withHeader("Content-Type", goodCapture.contentType()).withBody(malformedCapturedBody)));
        assertThatThrownBy(() -> client.decide(sampleQuestions(), List.of("state")))
                .isInstanceOf(PrescreenUnavailableException.class);
    }

    @Test
    void timeoutBecomesUnavailable() throws IOException {
        client = new DecisionsClient(new AppProperties("test-signing-key", null,
                new Decisions("shadow", "typesafe/jev-1.14", wireMock.baseUrl() + ENDPOINT, .8,
                        Duration.ofSeconds(1))), new LlmProperties(KEY, null, null, null, null, null, null, null));
        Capture captured = capture("a-return-laptop-2.json");
        wireMock.stubFor(post(urlEqualTo(ENDPOINT)).willReturn(aResponse().withFixedDelay(1300)
                .withStatus(captured.status()).withHeader("Content-Type", captured.contentType())
                .withBody(captured.body())));

        assertThatThrownBy(() -> client.decide(sampleQuestions(), List.of("state")))
                .isInstanceOf(PrescreenUnavailableException.class);
    }

    @Test
    void outOfBoundsJpegsAreRejectedWithoutAnyHttpCall() throws IOException {
        assertOutOfBounds(255, 600);
        assertOutOfBounds(279, 280);
        assertThat(wireMock.getAllServeEvents()).isEmpty();
    }

    @Test
    void minimumBoundImageIsAcceptedForSending() throws IOException {
        byte[] jpeg = jpeg(280, 280);
        wireMock.stubFor(post(urlEqualTo(ENDPOINT)).willReturn(capturedResponse("a-return-laptop-2.json")));

        client.decide(sampleQuestions(), List.of(imagePart(jpeg)));

        assertThat(wireMock.getAllServeEvents()).hasSize(1);
    }

    private void assertOutOfBounds(int width, int height) throws IOException {
        byte[] jpeg = jpeg(width, height);
        assertThatThrownBy(() -> client.decide(sampleQuestions(), List.of(imagePart(jpeg))))
                .isInstanceOf(PrescreenUnavailableException.class)
                .hasMessageContaining("IMAGE_OUT_OF_BOUNDS");
    }

    private ResponseDefinitionBuilder capturedResponse(String name) throws IOException {
        Capture capture = capture(name);
        return aResponse().withStatus(capture.status()).withHeader("Content-Type", capture.contentType())
                .withBody(capture.body());
    }

    private Capture capture(String name) throws IOException {
        String raw = Files.readString(CAPTURES.resolve(name));
        JsonNode envelope = mapper.readTree(raw);
        JsonNode contentType = envelope.get("headers").get("content-type").get(0);
        int key = raw.indexOf("\"body\"");
        int start = raw.indexOf(':', key) + 1;
        while (Character.isWhitespace(raw.charAt(start))) {
            start++;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int end = -1;
        for (int index = start; index < raw.length(); index++) {
            char current = raw.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
            } else if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                end = index + 1;
                break;
            }
        }
        if (end < 0) {
            throw new IllegalArgumentException("Malformed response capture envelope: " + name);
        }
        return new Capture(envelope.get("status").asInt(), contentType.asString(), raw.substring(start, end));
    }

    private static Map<String, PrescreenQuestionCatalog.Question> sampleQuestions() {
        return Map.of("photo_usable", new PrescreenQuestionCatalog.Question("noul", "synthetic instruction", null));
    }

    private static Map<String, Object> imagePart(byte[] jpeg) {
        String encoded = java.util.Base64.getEncoder().encodeToString(jpeg);
        return Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + encoded));
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", out);
        return out.toByteArray();
    }

    private record Capture(int status, String contentType, String body) {
    }
}
