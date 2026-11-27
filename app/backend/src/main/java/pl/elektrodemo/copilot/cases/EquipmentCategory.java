package pl.elektrodemo.copilot.cases;

public enum EquipmentCategory {
    LAPTOP("Laptop"),
    SMARTPHONE("Smartfon"),
    TABLET("Tablet"),
    MONITOR("Monitor"),
    HEADPHONES("Słuchawki"),
    SMARTWATCH("Smartwatch"),
    GAME_CONSOLE("Konsola do gier"),
    CAMERA("Aparat fotograficzny"),
    OTHER("Inne");

    private final String polishLabel;

    EquipmentCategory(String polishLabel) {
        this.polishLabel = polishLabel;
    }

    public String polishLabel() {
        return polishLabel;
    }
}
