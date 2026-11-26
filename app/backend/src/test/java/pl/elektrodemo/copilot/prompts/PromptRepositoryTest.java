package pl.elektrodemo.copilot.prompts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import pl.elektrodemo.copilot.cases.CaseType;

/** ADR-002 §3.4, AC-29: the five prompt files are loaded once at startup from classpath:prompts/. */
class PromptRepositoryTest {

    private final PromptRepository repository = new PromptRepository(new DefaultResourceLoader());

    @Test
    void imageAssessmentPromptIsPerCaseType() throws IOException {
        assertThat(repository.imageAssessment(CaseType.RETURN)).isEqualTo(classpathText("image-assessment-return.md"));
        assertThat(repository.imageAssessment(CaseType.COMPLAINT)).isEqualTo(classpathText("image-assessment-complaint.md"));
    }

    @Test
    void decisionPromptIsPerCaseType() throws IOException {
        assertThat(repository.decision(CaseType.RETURN)).isEqualTo(classpathText("decision-return.md"));
        assertThat(repository.decision(CaseType.COMPLAINT)).isEqualTo(classpathText("decision-complaint.md"));
    }

    @Test
    void chatPromptIsLoaded() throws IOException {
        assertThat(repository.chat()).isNotBlank().isEqualTo(classpathText("chat.md"));
    }

    @Test
    void differentCaseTypesGetDifferentPrompts() {
        assertThat(repository.imageAssessment(CaseType.RETURN)).isNotEqualTo(repository.imageAssessment(CaseType.COMPLAINT));
        assertThat(repository.decision(CaseType.RETURN)).isNotEqualTo(repository.decision(CaseType.COMPLAINT));
    }

    @Test
    void missingPromptLocationFailsFastWithThePathInTheMessage() {
        assertThatThrownBy(() -> new PromptRepository(new DefaultResourceLoader(), "classpath:no-such-prompts/"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("classpath:no-such-prompts/");
    }

    private static String classpathText(String file) throws IOException {
        try (InputStream in = PromptRepositoryTest.class.getResourceAsStream("/prompts/" + file)) {
            assertThat(in).as("classpath:prompts/" + file).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
