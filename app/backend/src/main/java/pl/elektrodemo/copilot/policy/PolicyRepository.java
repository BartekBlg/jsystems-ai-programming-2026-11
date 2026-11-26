package pl.elektrodemo.copilot.policy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.cases.CaseType;

/**
 * Policy texts from classpath:policies/ (copied from docs/policies at build time, ADR-001 §3.1/§3.2).
 * Loaded once at construction; a missing file fails startup.
 */
@Component
public class PolicyRepository {

    static final String DEFAULT_LOCATION = "classpath:policies/";

    private final String returnPolicy;
    private final String complaintPolicy;

    @Autowired
    public PolicyRepository(ResourceLoader resourceLoader) {
        this(resourceLoader, DEFAULT_LOCATION);
    }

    PolicyRepository(ResourceLoader resourceLoader, String location) {
        this.returnPolicy = load(resourceLoader, location + "return-policy.md");
        this.complaintPolicy = load(resourceLoader, location + "complaint-policy.md");
    }

    public String policyFor(CaseType type) {
        return switch (type) {
            case RETURN -> returnPolicy;
            case COMPLAINT -> complaintPolicy;
        };
    }

    private static String load(ResourceLoader resourceLoader, String path) {
        Resource resource = resourceLoader.getResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("Policy resource not found: " + path);
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read policy resource: " + path, e);
        }
    }
}
