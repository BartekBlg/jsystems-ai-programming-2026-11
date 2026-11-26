package pl.elektrodemo.copilot.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import pl.elektrodemo.copilot.cases.CaseType;

/** ADR-001 §3.2, AC-29: policy texts are loaded once at startup from classpath:policies/. */
class PolicyRepositoryTest {

    private final PolicyRepository repository = new PolicyRepository(new DefaultResourceLoader());

    @Test
    void returnCaseGetsReturnPolicyTextByteEqualToDocsFile() throws IOException {
        byte[] expected = Files.readAllBytes(Path.of("../../docs/policies/return-policy.md"));

        assertThat(repository.policyFor(CaseType.RETURN).getBytes(StandardCharsets.UTF_8)).isEqualTo(expected);
    }

    @Test
    void complaintCaseGetsComplaintPolicyTextByteEqualToDocsFile() throws IOException {
        byte[] expected = Files.readAllBytes(Path.of("../../docs/policies/complaint-policy.md"));

        assertThat(repository.policyFor(CaseType.COMPLAINT).getBytes(StandardCharsets.UTF_8)).isEqualTo(expected);
    }

    @Test
    void eachCaseTypeGetsItsOwnPolicyFile() {
        assertThat(repository.policyFor(CaseType.RETURN)).isNotBlank().isNotEqualTo(repository.policyFor(CaseType.COMPLAINT));
    }

    @Test
    void missingPolicyLocationFailsFastWithThePathInTheMessage() {
        assertThatThrownBy(() -> new PolicyRepository(new DefaultResourceLoader(), "classpath:no-such-policies/"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("classpath:no-such-policies/return-policy.md");
    }
}
