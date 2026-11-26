package pl.elektrodemo.copilot.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** ADR-001 §3.1: docs/policies is the single source of truth, copied to classpath:policies/. */
class PoliciesOnClasspathTest {

    @ParameterizedTest
    @ValueSource(strings = {"return-policy.md", "complaint-policy.md"})
    void policyOnClasspathIsByteEqualToDocsPolicy(String file) throws IOException {
        byte[] expected = Files.readAllBytes(Path.of("../../docs/policies", file));
        try (InputStream in = getClass().getResourceAsStream("/policies/" + file)) {
            assertThat(in).as("classpath:policies/" + file).isNotNull();
            assertThat(in.readAllBytes()).isEqualTo(expected);
        }
    }
}
