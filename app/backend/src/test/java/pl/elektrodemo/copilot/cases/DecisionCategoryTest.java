package pl.elektrodemo.copilot.cases;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DecisionCategoryTest {

    @Test
    void polishLabels() {
        assertThat(DecisionCategory.ACCEPTED.polishLabel()).isEqualTo("Przyjęte");
        assertThat(DecisionCategory.REJECTED.polishLabel()).isEqualTo("Odrzucone");
        assertThat(DecisionCategory.MORE_INFO_NEEDED.polishLabel()).isEqualTo("Potrzebne dodatkowe informacje");
        assertThat(DecisionCategory.ESCALATED.polishLabel()).isEqualTo("Przekazane do specjalisty");
    }

    @Test
    void fromPolishLabelIsExactAndTrimmed() {
        for (DecisionCategory c : DecisionCategory.values()) {
            assertThat(DecisionCategory.fromPolishLabel(c.polishLabel())).contains(c);
            assertThat(DecisionCategory.fromPolishLabel("  " + c.polishLabel() + "\n")).contains(c);
        }
    }

    @Test
    void fromPolishLabelUnknownIsEmpty() {
        assertThat(DecisionCategory.fromPolishLabel("przyjęte")).isEmpty();
        assertThat(DecisionCategory.fromPolishLabel("ACCEPTED")).isEmpty();
        assertThat(DecisionCategory.fromPolishLabel("")).isEmpty();
        assertThat(DecisionCategory.fromPolishLabel(null)).isEmpty();
    }

    @Test
    void otherLabels() {
        assertThat(CaseType.RETURN.polishLabel()).isEqualTo("Zwrot");
        assertThat(CaseType.COMPLAINT.polishLabel()).isEqualTo("Reklamacja");
        assertThat(EquipmentCategory.HEADPHONES.polishLabel()).isEqualTo("Słuchawki");
        assertThat(EquipmentCategory.GAME_CONSOLE.polishLabel()).isEqualTo("Konsola do gier");
        assertThat(EquipmentCategory.CAMERA.polishLabel()).isEqualTo("Aparat fotograficzny");
        assertThat(EquipmentCategory.OTHER.polishLabel()).isEqualTo("Inne");
    }
}
