package pl.elektrodemo.copilot.prescreen;

import tools.jackson.databind.JsonNode;

public record PrescreenContextNodes(JsonNode prescreen, JsonNode consistencySignals) {
}
