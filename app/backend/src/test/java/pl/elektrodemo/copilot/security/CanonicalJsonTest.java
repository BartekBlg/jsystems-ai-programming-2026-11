package pl.elektrodemo.copilot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.cases.CaseType;
import pl.elektrodemo.copilot.cases.EquipmentCategory;

class CanonicalJsonTest {

    private final CanonicalJson canonicalJson = new CanonicalJson();

    record Sample(String zeta, String alpha, Map<String, Integer> counts, String nothing) {
    }

    @Test
    void sortsPropertiesAlphabetically() {
        Sample sample = new Sample("z", "a", new LinkedHashMap<>(), null);

        assertThat(canonicalJson.write(sample))
                .isEqualTo("{\"alpha\":\"a\",\"counts\":{},\"nothing\":null,\"zeta\":\"z\"}");
    }

    @Test
    void sortsMapEntriesByKeyRegardlessOfInsertionOrder() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("b", 2);
        counts.put("c", 3);
        counts.put("a", 1);

        assertThat(canonicalJson.write(counts)).isEqualTo("{\"a\":1,\"b\":2,\"c\":3}");
    }

    @Test
    void writesWithoutWhitespaceAndKeepsNulls() {
        String json = canonicalJson.write(new Sample(null, "x", Map.of(), null));

        assertThat(json).doesNotContain("\n").doesNotContain(" ");
        assertThat(json).contains("\"zeta\":null").contains("\"nothing\":null");
    }

    @Test
    void writesDatesAsIsoStringsNotTimestamps() {
        String json = canonicalJson.write(new CaseFormDto(CaseType.RETURN, EquipmentCategory.TABLET, "Model",
                LocalDate.of(2026, 11, 1), "r"));

        assertThat(json).contains("\"purchaseDate\":\"2026-11-01\"");
    }

    @Test
    void sortsKeysOfNestedFixtureTree() {
        String json = canonicalJson.write(CaseContextFixtures.returnCaseWithPrescreen());

        assertThat(json).contains("\"prescreen\":{\"alpha\":\"a\",\"nested\":{\"a\":false,\"b\":true},\"zeta\":1}");
        assertThat(json.indexOf("\"assessment\"")).isLessThan(json.indexOf("\"caseType\""));
        assertThat(json.indexOf("\"caseType\"")).isLessThan(json.indexOf("\"contextVersion\""));
    }

    @Test
    void integralDoublesAreWrittenLikeJavaScript() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", 1.0);
        m.put("b", 0.0);
        m.put("c", -0.0);
        m.put("d", 0.5f);
        m.put("e", 100.0);

        assertThat(canonicalJson.write(m)).isEqualTo("{\"a\":1,\"b\":0,\"c\":0,\"d\":0.5,\"e\":100}");
    }

    @Test
    void nonIntegralNumbersFollowJavaScriptFormatting() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", 1.0E-5);
        m.put("b", 1.0E-6);
        m.put("c", 1.0E-7);
        m.put("d", 1.5E20);
        m.put("e", 1.0E21);
        m.put("f", 0.87);
        m.put("g", 123456.789);
        m.put("h", -2.5E-3);
        m.put("i", 12345678901234567890.0);

        assertThat(canonicalJson.write(m)).isEqualTo("{\"a\":0.00001,\"b\":0.000001,\"c\":1e-7,"
                + "\"d\":150000000000000000000,\"e\":1e+21,\"f\":0.87,\"g\":123456.789,\"h\":-0.0025,"
                + "\"i\":12345678901234567000}");
    }

    @Test
    void integralDoubleAndIntegerNodesCanonicalizeIdentically() {
        ObjectNode d = JsonNodeFactory.instance.objectNode();
        d.put("confidence", 1.0);
        ObjectNode i = JsonNodeFactory.instance.objectNode();
        i.put("confidence", 1);

        assertThat(canonicalJson.write(d)).isEqualTo(canonicalJson.write(i));
    }
}
