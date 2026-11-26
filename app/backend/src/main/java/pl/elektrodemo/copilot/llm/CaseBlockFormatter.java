package pl.elektrodemo.copilot.llm;

import java.util.Collection;
import java.util.stream.Collectors;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.RuleResult;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders the "Dane zgłoszenia" user message (ADR-002 §3.4). The layout follows the T04 request
 * captures; "Źródło oceny", the required-reason line and the signals section are additions.
 * Customer-written fields are delimited by tags and angle brackets inside them are escaped, so
 * they cannot close the delimiter. Pure text; no SDK types.
 */
final class CaseBlockFormatter {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private CaseBlockFormatter() {
    }

    static String decisionBlock(DecisionPrompt p) {
        return block(p, null, false);
    }

    static String chatBlock(ChatPrompt chat) {
        return block(chat.decisionPrompt(), chat, true);
    }

    private static String block(DecisionPrompt p, ChatPrompt chat, boolean forChat) {
        CaseFormDto form = p.form();
        RuleResult rules = p.ruleResult();
        StringBuilder sb = new StringBuilder("# Dane zgłoszenia\n\n");
        sb.append("Rodzaj zgłoszenia: ").append(p.caseType().polishLabel()).append('\n');
        sb.append("Kategoria sprzętu: ").append(form.category().polishLabel()).append('\n');
        sb.append("Model urządzenia: <model_urzadzenia>").append(escape(form.modelName())).append("</model_urzadzenia>\n");
        sb.append("Data zakupu: ").append(form.purchaseDate()).append('\n');
        sb.append("Data zgłoszenia: ").append(p.submittedAt().toLocalDate()).append('\n');
        sb.append("Powód: <opis_klienta>").append(escape(form.reason())).append("</opis_klienta>\n");
        sb.append("Ocena zdjęcia (JSON): ").append(MAPPER.writeValueAsString(p.assessment())).append('\n');
        sb.append("Źródło oceny: ").append(p.assessmentSource().name()).append('\n');
        sb.append("\n## Fakty wyliczone przez system\n");
        sb.append("Dni od zakupu: ").append(rules.daysSincePurchase()).append('\n');
        sb.append("Dozwolone kategorie: ").append(names(rules.allowedCategories())).append('\n');
        if (rules.requiredReasonText() != null && !rules.requiredReasonText().isBlank()) {
            sb.append("Wymagany tekst uzasadnienia: ").append(rules.requiredReasonText()).append('\n');
        }
        if (forChat) {
            if (chat.currentCategory() != null) {
                sb.append("Aktualna decyzja: ").append(chat.currentCategory().polishLabel()).append('\n');
            }
            if (!rules.chatForbiddenCategories().isEmpty()) {
                sb.append("Niedozwolone w rozmowie: ").append(labels(rules.chatForbiddenCategories())).append('\n');
            }
        }
        if (present(p.prescreen()) || present(p.consistencySignals())) {
            sb.append("\n## Sygnały wstępnej analizy (probabilistyczne, do weryfikacji)\n");
            if (present(p.prescreen())) {
                sb.append("Analiza zdjęcia: ").append(p.prescreen()).append('\n');
            }
            if (present(p.consistencySignals())) {
                sb.append("Spójność formularza ze zdjęciem: ").append(p.consistencySignals()).append('\n');
            }
        }
        return sb.toString();
    }

    private static boolean present(JsonNode node) {
        return node != null && !node.isNull() && !node.isMissingNode() && !node.isEmpty();
    }

    private static String names(Collection<DecisionCategory> categories) {
        return categories.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    static String labels(Collection<DecisionCategory> categories) {
        return categories.stream().map(DecisionCategory::polishLabel).collect(Collectors.joining(", "));
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("<", "&lt;").replace(">", "&gt;");
    }
}
