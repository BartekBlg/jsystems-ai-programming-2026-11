package pl.elektrodemo.copilot.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
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
}
