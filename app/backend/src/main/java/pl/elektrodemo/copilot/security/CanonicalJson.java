package pl.elektrodemo.copilot.security;

import java.math.BigDecimal;
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
        StringBuilder out = new StringBuilder();
        append(sortKeys(tree), out);
        return out.toString();
    }

    /**
     * Writes the tree compactly, with numbers in the form of JavaScript {@code JSON.stringify}: the browser
     * round trip turns {@code 1.0} into {@code 1}, so the signed form must not depend on the Java node type.
     */
    private void append(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            out.append("null");
        } else if (node.isObject()) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, JsonNode> e : node.properties()) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                out.append(mapper.writeValueAsString(e.getKey())).append(':');
                append(e.getValue(), out);
            }
            out.append('}');
        } else if (node.isArray()) {
            out.append('[');
            boolean first = true;
            for (JsonNode child : node) {
                if (!first) {
                    out.append(',');
                }
                first = false;
                append(child, out);
            }
            out.append(']');
        } else if (node.isNumber()) {
            out.append(jsNumber(node));
        } else {
            out.append(mapper.writeValueAsString(node));
        }
    }

    static String jsNumber(JsonNode node) {
        if (node.isIntegralNumber()) {
            return node.bigIntegerValue().toString();
        }
        double d = node.isFloat() ? Double.parseDouble(Float.toString(node.floatValue())) : node.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "null";
        }
        if (d == 0) {
            return "0";
        }
        BigDecimal bd = new BigDecimal(Double.toString(Math.abs(d))).stripTrailingZeros();
        String digits = bd.unscaledValue().toString();
        int k = digits.length();
        int n = k - bd.scale();
        StringBuilder sb = new StringBuilder();
        if (d < 0) {
            sb.append('-');
        }
        if (k <= n && n <= 21) {
            sb.append(digits).append("0".repeat(n - k));
        } else if (0 < n && n <= 21) {
            sb.append(digits, 0, n).append('.').append(digits, n, k);
        } else if (-6 < n && n <= 0) {
            sb.append("0.").append("0".repeat(-n)).append(digits);
        } else {
            int e = n - 1;
            sb.append(digits.charAt(0));
            if (k > 1) {
                sb.append('.').append(digits, 1, k);
            }
            sb.append('e').append(e < 0 ? '-' : '+').append(Math.abs(e));
        }
        return sb.toString();
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
