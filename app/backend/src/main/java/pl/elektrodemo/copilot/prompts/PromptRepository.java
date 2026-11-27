package pl.elektrodemo.copilot.prompts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.cases.CaseType;

/**
 * Prompt templates from classpath:prompts/ (ADR-002 §3.4). All five files are loaded once at
 * construction; a missing file fails startup.
 */
@Component
public class PromptRepository {

    static final String DEFAULT_LOCATION = "classpath:prompts/";

    private final String imageAssessmentReturn;
    private final String imageAssessmentComplaint;
    private final String decisionReturn;
    private final String decisionComplaint;
    private final String chat;

    @Autowired
    public PromptRepository(ResourceLoader resourceLoader) {
        this(resourceLoader, DEFAULT_LOCATION);
    }

    PromptRepository(ResourceLoader resourceLoader, String location) {
        this.imageAssessmentReturn = load(resourceLoader, location + "image-assessment-return.md");
        this.imageAssessmentComplaint = load(resourceLoader, location + "image-assessment-complaint.md");
        this.decisionReturn = load(resourceLoader, location + "decision-return.md");
        this.decisionComplaint = load(resourceLoader, location + "decision-complaint.md");
        this.chat = load(resourceLoader, location + "chat.md");
    }

    public String imageAssessment(CaseType type) {
        return switch (type) {
            case RETURN -> imageAssessmentReturn;
            case COMPLAINT -> imageAssessmentComplaint;
        };
    }

    public String decision(CaseType type) {
        return switch (type) {
            case RETURN -> decisionReturn;
            case COMPLAINT -> decisionComplaint;
        };
    }

    public String chat() {
        return chat;
    }

    private static String load(ResourceLoader resourceLoader, String path) {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt resource not found: " + path);
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read prompt resource: " + path, e);
        }
    }
}
