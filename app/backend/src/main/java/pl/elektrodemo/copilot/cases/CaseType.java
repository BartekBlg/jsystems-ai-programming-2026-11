package pl.elektrodemo.copilot.cases;

public enum CaseType {
    RETURN("Zwrot"),
    COMPLAINT("Reklamacja");

    private final String polishLabel;

    CaseType(String polishLabel) {
        this.polishLabel = polishLabel;
    }

    public String polishLabel() {
        return polishLabel;
    }
}
