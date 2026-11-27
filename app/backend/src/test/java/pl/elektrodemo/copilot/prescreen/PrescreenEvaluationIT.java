package pl.elektrodemo.copilot.prescreen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;
import pl.elektrodemo.copilot.cases.ImageAssessment;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;
import pl.elektrodemo.copilot.cases.ComplaintImageAssessment;
import pl.elektrodemo.copilot.image.ImageDownscaler;
import pl.elektrodemo.copilot.llm.LlmGateway;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;

@Tag("eval")
@SpringBootTest
class PrescreenEvaluationIT {

    private static final Path EXAMPLE_IMAGES = Path.of("../../assets/example-images");
    private static final Path EXPECTED = Path.of("src/test/resources/prescreen/expected.yaml");
    private static final Pattern IMAGE_LINE = Pattern.compile("^  ([^:]+):$");
    private static final Pattern CASE_LINE = Pattern.compile("^    (return|complaint):$");
    private static final Pattern FIELD_LINE = Pattern.compile("^      (photoSufficient|damageVisible|probableCause): (.+)$");

    @Autowired
    private PrescreenService prescreenService;
    @Autowired
    private PrescreenAssessmentMapper mapper;
    @Autowired
    private ImageDownscaler downscaler;
    @Autowired
    private LlmGateway llmGateway;

    @Test
    void evaluatesEveryExampleImageForBothCaseTypesAndWritesMarkdownReport() throws IOException {
        List<Path> images;
        try (var paths = Files.list(EXAMPLE_IMAGES)) {
            images = paths.filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().equals("README.md"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
        }
        assertThat(images).isNotEmpty();
        Map<String, Map<CaseType, Map<String, String>>> expected = readExpected();
        List<String> report = new ArrayList<>(List.of("# Prescreen evaluation", "",
                "Real API evaluation; Decisions and vision LLM each run once per image and case type.", "",
                "| Image | Case | Gate | Reasons | Agreement with LLM | Agreement with expected | Decisions ms | LLM ms | Cost USD |",
                "|---|---|---|---|---|---|---:|---:|---:|"));
        long decisionsMsTotal = 0;
        long llmMsTotal = 0;
        double costTotal = 0;
        for (Path path : images) {
            String filename = path.getFileName().toString();
            byte[] jpeg = downscaler.toAnalysisJpeg(Files.readAllBytes(path));
            EquipmentCategory category = filename.startsWith("laptop-") ? EquipmentCategory.LAPTOP
                    : EquipmentCategory.SMARTPHONE;
            for (CaseType caseType : CaseType.values()) {
                CaseFormDto form = new CaseFormDto(caseType, category, "Evaluation model", LocalDate.of(2026, 1, 1),
                        "Synthetic evaluation reason");
                PrescreenResult result = prescreenService.prescreen(caseType, category, form, jpeg);
                long llmStarted = System.nanoTime();
                ImageAssessment llm = llmGateway.assessImage(caseType, category, jpeg, null);
                long llmMs = (System.nanoTime() - llmStarted) / 1_000_000;
                ImageAssessment decisions = result.status() == PrescreenStatus.OK
                        && result.gate() == GateStatus.CONFIDENT ? mapper.map(caseType, result) : null;
                String llmAgreement = decisions == null ? "n/a" : agreement(decisions, llm);
                String expectedAgreement = expectedAgreement(filename, caseType, expected, decisions);
                report.add("| " + filename + " | " + caseType + " | " + result.gate() + " | "
                        + String.join(", ", result.gateReasons()) + " | " + llmAgreement + " | " + expectedAgreement
                        + " | " + result.latencyMs() + " | " + llmMs + " | " + result.cost() + " |");
                decisionsMsTotal += result.latencyMs();
                llmMsTotal += llmMs;
                costTotal += result.cost();
            }
        }
        report.add("");
        report.add("Totals: Decisions " + decisionsMsTotal + " ms; vision LLM " + llmMsTotal + " ms; Decisions cost USD "
                + costTotal + ".");
        Path output = Path.of("target/prescreen-eval.md");
        Files.createDirectories(output.getParent());
        Files.writeString(output, String.join("\n", report) + "\n");
    }

    private static Map<String, Map<CaseType, Map<String, String>>> readExpected() throws IOException {
        Map<String, Map<CaseType, Map<String, String>>> result = new LinkedHashMap<>();
        String image = null;
        CaseType caseType = null;
        for (String line : Files.readAllLines(EXPECTED)) {
            Matcher imageMatcher = IMAGE_LINE.matcher(line);
            Matcher caseMatcher = CASE_LINE.matcher(line);
            Matcher fieldMatcher = FIELD_LINE.matcher(line);
            if (imageMatcher.matches()) {
                image = imageMatcher.group(1);
                result.put(image, new EnumMap<>(CaseType.class));
            } else if (caseMatcher.matches()) {
                caseType = "return".equals(caseMatcher.group(1)) ? CaseType.RETURN : CaseType.COMPLAINT;
                if (image != null) {
                    result.get(image).put(caseType, new LinkedHashMap<>());
                }
            } else if (fieldMatcher.matches() && image != null && caseType != null) {
                result.get(image).get(caseType).put(fieldMatcher.group(1), fieldMatcher.group(2));
            }
        }
        return result;
    }

    private static String expectedAgreement(String filename, CaseType caseType,
            Map<String, Map<CaseType, Map<String, String>>> expected, ImageAssessment assessment) {
        Map<CaseType, Map<String, String>> byCase = expected.get(filename);
        Map<String, String> values = byCase == null ? null : byCase.get(caseType);
        if (values == null || values.values().stream().allMatch("TODO"::equals)) {
            return "pending expected.yaml";
        }
        if (assessment == null) {
            return "n/a (gate uncertain)";
        }
        boolean matches = true;
        if (values.containsKey("photoSufficient") && !"TODO".equals(values.get("photoSufficient"))) {
            matches &= Boolean.toString(assessment.photoSufficient()).equalsIgnoreCase(values.get("photoSufficient"));
        }
        if (values.containsKey("damageVisible") && !"TODO".equals(values.get("damageVisible"))) {
            boolean actual = assessment instanceof ReturnImageAssessment ret ? ret.damageVisible()
                    : ((ComplaintImageAssessment) assessment).defectVisible();
            matches &= Boolean.toString(actual).equalsIgnoreCase(values.get("damageVisible"));
        }
        if (values.containsKey("probableCause") && !"TODO".equals(values.get("probableCause"))) {
            String cause = assessment instanceof ComplaintImageAssessment complaint
                    ? complaint.probableCause().name() : "n/a";
            matches &= cause.equalsIgnoreCase(values.get("probableCause"));
        }
        return Boolean.toString(matches);
    }

    private static String agreement(ImageAssessment decisions, ImageAssessment llm) {
        List<String> agreements = new ArrayList<>();
        agreements.add("photo=" + (decisions.photoSufficient() == llm.photoSufficient()));
        agreements.add("confidence=" + (decisions.confidence() == llm.confidence()));
        if (decisions instanceof ReturnImageAssessment prescreen && llm instanceof ReturnImageAssessment vision) {
            agreements.add("damage=" + (prescreen.damageVisible() == vision.damageVisible()));
            agreements.add("signs=" + (java.util.Set.copyOf(prescreen.signsOfUse())
                    .equals(java.util.Set.copyOf(vision.signsOfUse()))));
            agreements.add("resellable=" + (prescreen.resellableAsNew() == vision.resellableAsNew()));
        }
        if (decisions instanceof ComplaintImageAssessment prescreen && llm instanceof ComplaintImageAssessment vision) {
            agreements.add("damage=" + (prescreen.defectVisible() == vision.defectVisible()));
            agreements.add("cause=" + (prescreen.probableCause() == vision.probableCause()));
        }
        return String.join(", ", agreements);
    }
}
