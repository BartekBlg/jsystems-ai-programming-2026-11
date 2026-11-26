package pl.elektrodemo.copilot.cases;

import java.util.Arrays;
import java.util.Optional;

public enum DecisionCategory {
    ACCEPTED("Przyjęte"),
    REJECTED("Odrzucone"),
    MORE_INFO_NEEDED("Potrzebne dodatkowe informacje"),
    ESCALATED("Przekazane do specjalisty");

    private final String polishLabel;

    DecisionCategory(String polishLabel) {
        this.polishLabel = polishLabel;
    }

    public String polishLabel() {
        return polishLabel;
    }

    public static Optional<DecisionCategory> fromPolishLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        String trimmed = label.trim();
        return Arrays.stream(values()).filter(c -> c.polishLabel.equals(trimmed)).findFirst();
    }
}
