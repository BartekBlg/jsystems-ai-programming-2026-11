package pl.elektrodemo.copilot.security;

import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Canonical JSON for HMAC (ADR-005 D6): sorted properties and map keys, no whitespace, nulls kept,
 * ISO-8601 dates. Uses its own Jackson 3 mapper, never the SDK's Jackson 2 mapper.
 */
@Component
public class CanonicalJson {

    private final JsonMapper mapper = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(SerializationFeature.INDENT_OUTPUT)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public String write(Object value) {
        JsonNode tree = mapper.valueToTree(value);
        return mapper.writeValueAsString(sortKeys(tree));
    }

    /** Rebuilds every object with alphabetically ordered keys, so JsonNode-backed fields are canonical too. */
    private JsonNode sortKeys(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            TreeMap<String, JsonNode> sorted = new TreeMap<>();
            for (Map.Entry<String, JsonNode> e : node.properties()) {
                sorted.put(e.getKey(), sortKeys(e.getValue()));
            }
            ObjectNode out = mapper.createObjectNode();
            for (Map.Entry<String, JsonNode> e : sorted.entrySet()) {
                out.set(e.getKey(), e.getValue());
            }
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = mapper.createArrayNode();
            for (JsonNode child : node) {
                out.add(sortKeys(child));
            }
            return out;
        }
        return node;
    }
}
