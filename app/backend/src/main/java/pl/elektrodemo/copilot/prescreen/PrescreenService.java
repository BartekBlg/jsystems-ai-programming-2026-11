package pl.elektrodemo.copilot.prescreen;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.stereotype.Service;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PrescreenService {

    private static final JsonMapper DEFAULT_MAPPER = JsonMapper.builder().build();
    private final DecisionsClient client;
    private final PrescreenQuestionCatalog catalog;
    private final ConfidenceGate gate;
    private final AppProperties.Decisions properties;
    private final Clock clock;
    private final JsonMapper mapper;

    public PrescreenService(DecisionsClient client, PrescreenQuestionCatalog catalog, ConfidenceGate gate,
            AppProperties properties, Clock clock, JsonMapper mapper) {
        this.client = client;
        this.catalog = catalog;
        this.gate = gate;
        this.properties = properties.decisions();
        this.clock = clock;
        this.mapper = mapper == null ? DEFAULT_MAPPER : mapper;
    }

    public PrescreenResult prescreen(CaseType caseType, EquipmentCategory category, CaseFormDto form, byte[] jpeg) {
        long started = System.nanoTime();
        String version = PrescreenQuestionCatalog.CATALOG_VERSION;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            LocalDate submissionDate = LocalDate.now(clock);
            Future<DecisionsResponse> requestA = executor.submit(() -> client.decide(catalog.questionsA(caseType),
                    catalog.stateA(caseType, category, jpeg)));
            Future<DecisionsResponse> requestB = executor.submit(() -> client.decide(
                    catalog.questionsB(caseType, form.reason()), catalog.stateB(form, submissionDate, jpeg)));

            DecisionsResponse responseA;
            try {
                responseA = requestA.get();
            } catch (ExecutionException exception) {
                return PrescreenResult.unavailable(reason(exception.getCause()), version, elapsedMs(started));
            }
            if (responseA == null || responseA.answers() == null) {
                return PrescreenResult.unavailable("INVALID_RESPONSE", version, elapsedMs(started));
            }

            List<PrescreenAnswer> photoAnswers = normalize(responseA, catalog.questionsA(caseType));
            DecisionsResponse responseB = null;
            try {
                responseB = requestB.get();
            } catch (ExecutionException ignored) {
                // Request B only enriches the decision prompt; it cannot invalidate successful photo answers.
            }
            List<PrescreenAnswer> signals = responseB == null ? List.of()
                    : normalize(responseB, catalog.questionsB(caseType, form.reason()));
            GateResult gateResult = gate.evaluate(caseType, category, photoAnswers, signals,
                    properties.confidenceThreshold());
            return new PrescreenResult(PrescreenStatus.OK, photoAnswers, signals, gateResult.status(),
                    gateResult.reasons(), version, elapsedMs(started), cost(responseA) + cost(responseB),
                    responseA.model(), null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return PrescreenResult.unavailable("INTERRUPTED", version, elapsedMs(started));
        } catch (RuntimeException exception) {
            return PrescreenResult.unavailable(reason(exception), version, elapsedMs(started));
        }
    }

    public PrescreenContextNodes contextNodes(PrescreenResult result) {
        List<Map<String, Object>> photo = new ArrayList<>();
        for (PrescreenAnswer answer : result.photoAnswers()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", answer.questionId());
            entry.put("value", answer.value());
            entry.put("confidence", answer.confidence());
            photo.add(entry);
        }
        List<Map<String, Object>> signals = new ArrayList<>();
        for (PrescreenAnswer answer : result.consistencySignals()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", answer.questionId());
            entry.put("probability", answer.probability() == null ? null : roundProbability(answer.probability()));
            entry.put("confidence", answer.confidence());
            signals.add(entry);
        }
        return new PrescreenContextNodes(mapper.valueToTree(photo), mapper.valueToTree(signals));
    }

    private static List<PrescreenAnswer> normalize(DecisionsResponse response,
            Map<String, PrescreenQuestionCatalog.Question> questions) {
        List<PrescreenAnswer> answers = new ArrayList<>();
        for (String id : questions.keySet()) {
            if (response.answers().containsKey(id)) {
                answers.add(response.normalize(id));
            }
        }
        return answers;
    }

    private static double cost(DecisionsResponse response) {
        if (response == null || response.usage() == null) {
            return 0;
        }
        if (response.usage().costDetails() != null && response.usage().costDetails().upstreamInferenceCost() != null) {
            return response.usage().costDetails().upstreamInferenceCost();
        }
        return response.usage().cost() == null ? 0 : response.usage().cost();
    }

    private static double roundProbability(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }

    private static String reason(Throwable exception) {
        if (exception instanceof PrescreenUnavailableException unavailable) {
            return unavailable.getMessage();
        }
        return "DECISIONS_ERROR";
    }
}
