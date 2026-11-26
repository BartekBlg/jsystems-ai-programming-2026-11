package pl.elektrodemo.copilot.prescreen;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.llm.LlmProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class DecisionsClient {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Pattern JPEG_DATA_URL = Pattern.compile("^data:image/jpeg;base64,(.+)$", Pattern.DOTALL);
    private static final int MIN_SIDE = 256;
    private static final int MAX_SIDE = 1568;
    private static final long MIN_PIXELS = 78_400;
    private static final long MAX_PIXELS = 2_458_624;

    private final String url;
    private final String model;
    private final String apiKey;
    private final RestClient restClient;

    public DecisionsClient(AppProperties appProperties, LlmProperties llmProperties) {
        AppProperties.Decisions decisions = appProperties.decisions();
        this.url = decisions.url();
        this.model = decisions.model();
        this.apiKey = llmProperties.apiKey();
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(decisions.timeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(decisions.timeout());
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public DecisionsResponse decide(Map<String, PrescreenQuestionCatalog.Question> questions, List<Object> state) {
        validateImageBounds(state);
        if (apiKey == null || apiKey.isBlank()) {
            throw new PrescreenUnavailableException("MISSING_API_KEY");
        }
        DecisionsRequest request = new DecisionsRequest(model, questions, state);
        try {
            String requestJson = MAPPER.writeValueAsString(request);
            String responseJson = restClient.post().uri(url)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);
            if (responseJson == null || responseJson.isBlank()) {
                throw new PrescreenUnavailableException("EMPTY_RESPONSE");
            }
            DecisionsResponse response = MAPPER.readValue(responseJson, DecisionsResponse.class);
            if (response == null || response.answers() == null) {
                throw new PrescreenUnavailableException("INVALID_RESPONSE");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new PrescreenUnavailableException("HTTP_" + exception.getStatusCode().value(), exception);
        } catch (JacksonException | RestClientException | IllegalArgumentException exception) {
            throw new PrescreenUnavailableException("INVALID_OR_UNAVAILABLE_RESPONSE", exception);
        }
    }

    private static void validateImageBounds(List<Object> state) {
        if (state == null) {
            return;
        }
        for (Object part : state) {
            if (!(part instanceof Map<?, ?> partMap) || !"image_url".equals(partMap.get("type"))) {
                continue;
            }
            Object imageUrl = partMap.get("image_url");
            if (!(imageUrl instanceof Map<?, ?> imageMap) || !(imageMap.get("url") instanceof String dataUrl)) {
                throw new PrescreenUnavailableException("IMAGE_OUT_OF_BOUNDS");
            }
            var matcher = JPEG_DATA_URL.matcher(dataUrl);
            if (!matcher.matches()) {
                throw new PrescreenUnavailableException("IMAGE_OUT_OF_BOUNDS");
            }
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(matcher.group(1))));
                if (image == null) {
                    throw new PrescreenUnavailableException("IMAGE_OUT_OF_BOUNDS");
                }
                int width = image.getWidth();
                int height = image.getHeight();
                long pixels = (long) width * height;
                if (width < MIN_SIDE || height < MIN_SIDE || width > MAX_SIDE || height > MAX_SIDE
                        || pixels < MIN_PIXELS || pixels > MAX_PIXELS) {
                    throw new PrescreenUnavailableException("IMAGE_OUT_OF_BOUNDS");
                }
            } catch (IllegalArgumentException | IOException exception) {
                throw new PrescreenUnavailableException("IMAGE_OUT_OF_BOUNDS", exception);
            }
            return;
        }
    }

    public record DecisionsRequest(String model, Map<String, PrescreenQuestionCatalog.Question> questions,
            List<Object> state) {
    }
}
