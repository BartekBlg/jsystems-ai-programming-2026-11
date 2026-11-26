package pl.elektrodemo.copilot.cases;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Checks the structured decision message from LLM step 2 against the rule result (ADR-002 3.6, ADR-005 D5)
 * and normalizes the trailing disclaimer to the canonical text (ADR-002 3.7).
 */
@Component
public class DecisionMessageValidator {

    static final String DISCLAIMER = "To wstępna, automatyczna ocena przygotowana na podstawie formularza i zdjęcia. "
            + "Ostateczną decyzję podejmie nasz zespół serwisu po otrzymaniu i sprawdzeniu urządzenia.";
    static final String RETURN_EXTRA = "Nie ogranicza to Twojego ustawowego prawa do odstąpienia od umowy.";

    private static final List<String> HEADINGS = List.of(
            "Decyzja", "Uzasadnienie", "Co widać na zdjęciu", "Kolejne kroki");
    private static final Pattern DECISION_HEADING = heading("Decyzja");
    private static final Pattern ANY_HEADING = Pattern.compile("(?m)^##\\s");
    private static final Pattern PARAGRAPH_SIGN = Pattern.compile("§\\s?\\d+");
    private static final List<String> DISCLAIMER_MARKERS = List.of(
            "wstępna", "ostateczną decyzję", "ustawowego prawa");

    public ValidationResult validate(DecisionDraft draft, RuleResult rules, CaseType type) {
        String raw = draft.message() == null ? "" : draft.message().replace("\r\n", "\n");
        DecisionCategory category = draft.category();
        List<String> problems = new ArrayList<>();

        if (category == null || !rules.allowedCategories().contains(category)) {
            problems.add("Dozwolone kategorie: " + rules.allowedCategories().stream()
                    .map(DecisionCategory::polishLabel)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
        }
        checkHeadings(raw, problems);
        if (category != null && !decisionSection(raw).contains(category.polishLabel())) {
            problems.add("decision label missing from ## Decyzja: " + category.polishLabel());
        }
        if (!PARAGRAPH_SIGN.matcher(raw).find()) {
            problems.add("missing paragraph citation (§N)");
        }
        String required = rules.requiredReasonText();
        if (required != null && !required.isBlank() && !raw.contains(required)) {
            problems.add("required reason text missing: " + required);
        }
        String citation = rules.rejectionRequiresCitation();
        if (category == DecisionCategory.REJECTED && citation != null && !citation.isBlank()
                && !Pattern.compile(Pattern.quote(citation) + "(?!\\d)").matcher(raw).find()) {
            problems.add("rejection requires citation " + citation);
        }

        return new ValidationResult(problems.isEmpty(), problems, normalize(raw, type));
    }

    private static void checkHeadings(String raw, List<String> problems) {
        int previous = -1;
        boolean ordered = true;
        for (String name : HEADINGS) {
            Matcher m = heading(name).matcher(raw);
            if (!m.find()) {
                problems.add("missing heading: ## " + name);
            } else {
                ordered &= m.start() > previous;
                previous = m.start();
            }
        }
        if (!ordered) {
            problems.add("headings out of order");
        }
    }

    private static String decisionSection(String raw) {
        Matcher start = DECISION_HEADING.matcher(raw);
        if (!start.find()) {
            return "";
        }
        Matcher next = ANY_HEADING.matcher(raw);
        next.region(start.end(), raw.length());
        return next.find() ? raw.substring(start.end(), next.start()) : raw.substring(start.end());
    }

    static String normalize(String raw, CaseType type) {
        String text = raw.strip();
        int lastBreak = text.lastIndexOf("\n\n");
        String lastParagraph = text.substring(lastBreak + 2);
        String body = text;
        if (isDisclaimer(lastParagraph)) {
            body = lastBreak < 0 ? "" : text.substring(0, lastBreak).strip();
        }
        String tail = type == CaseType.RETURN ? DISCLAIMER + " " + RETURN_EXTRA : DISCLAIMER;
        return body.isEmpty() ? tail : body + "\n\n" + tail;
    }

    private static boolean isDisclaimer(String paragraph) {
        String lower = paragraph.toLowerCase(Locale.ROOT);
        return DISCLAIMER_MARKERS.stream().anyMatch(lower::contains);
    }

    private static Pattern heading(String name) {
        return Pattern.compile("(?m)^##\\s*" + Pattern.quote(name));
    }

    public record ValidationResult(boolean valid, List<String> problems, String normalizedMessage) {

        public ValidationResult {
            problems = List.copyOf(problems);
        }
    }
}
