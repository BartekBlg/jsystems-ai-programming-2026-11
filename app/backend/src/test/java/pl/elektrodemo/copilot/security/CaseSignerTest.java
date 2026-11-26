package pl.elektrodemo.copilot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import pl.elektrodemo.copilot.cases.Confidence;
import pl.elektrodemo.copilot.cases.DecisionCategory;
import pl.elektrodemo.copilot.cases.ImageAssessment;
import pl.elektrodemo.copilot.cases.ReturnImageAssessment;
import pl.elektrodemo.copilot.cases.RuleResult;
import pl.elektrodemo.copilot.cases.SignOfUse;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;
import pl.elektrodemo.copilot.web.dto.CaseFormDto;
import pl.elektrodemo.copilot.web.dto.DecisionRef;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class CaseSignerTest {

    private static final String KEY_A = Base64.getEncoder().encodeToString(bytes(32, 1));
    private static final String KEY_B = Base64.getEncoder().encodeToString(bytes(32, 2));

    private static byte[] bytes(int n, int seed) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) (seed * 31 + i);
        }
        return b;
    }

    private static CaseSigner signer(String key) {
        return new CaseSigner(new AppProperties(key, null, null));
    }

    private final CaseSigner signer = signer(KEY_A);

    @Test
    void signThenVerifySucceeds() {
        CaseContextDto ctx = CaseContextFixtures.returnCase();

        String signature = signer.sign(ctx);

        assertThat(signer.verify(ctx, signature)).isTrue();
    }

    @Test
    void signatureIsBase64UrlWithoutPadding() {
        String signature = signer.sign(CaseContextFixtures.returnCase());

        assertThat(signature).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void signingIsDeterministicForTheSameKey() {
        CaseContextDto ctx = CaseContextFixtures.returnCase();

        assertThat(signer(KEY_A).sign(ctx)).isEqualTo(signer(KEY_A).sign(ctx));
    }

    @Test
    void everySingleFieldChangeInvalidatesSignature() {
        CaseContextDto original = CaseContextFixtures.returnCase();
        String signature = signer.sign(original);

        List<UnaryOperator<CaseContextDto>> mutations = List.of(
                ctx -> withVersion(ctx, ctx.contextVersion() + 1),
                ctx -> withAssessment(ctx, new ReturnImageAssessment(true, "", false, List.of(SignOfUse.SCRATCHES),
                        true, "Zarysowania na obudowie.", Confidence.LOW)),
                ctx -> withForm(ctx, new CaseFormDto(ctx.form().caseType(), ctx.form().category(),
                        ctx.form().modelName(), LocalDate.of(2026, 10, 31), ctx.form().reason())),
                ctx -> withRuleFacts(ctx, new RuleResult(ctx.ruleFacts().daysSincePurchase(),
                        ctx.ruleFacts().returnWindowStatus(), false, Collections.singleton(DecisionCategory.ACCEPTED),
                        null, ctx.ruleFacts().chatForbiddenCategories(), null, "R6")),
                ctx -> withDecision(ctx, new DecisionRef(DecisionCategory.REJECTED)),
                ctx -> new CaseContextDto(ctx.contextVersion(), ctx.caseType(), ctx.form(), ctx.submittedAt(),
                        ctx.assessment(), ctx.assessmentSource(), CaseContextFixtures.returnCaseWithPrescreen().prescreen(),
                        ctx.consistencySignals(), ctx.ruleFacts(), ctx.decision()));

        for (UnaryOperator<CaseContextDto> mutation : mutations) {
            CaseContextDto changed = mutation.apply(original);
            assertThat(changed).isNotEqualTo(original);
            assertThat(signer.verify(changed, signature)).isFalse();
        }
    }

    @Test
    void wrongKeyFailsVerification() {
        CaseContextDto ctx = CaseContextFixtures.returnCase();
        String signature = signer(KEY_A).sign(ctx);

        assertThat(signer(KEY_B).verify(ctx, signature)).isFalse();
    }

    @Test
    void nullBlankAndMalformedSignaturesAreRejected() {
        CaseContextDto ctx = CaseContextFixtures.returnCase();
        String valid = signer.sign(ctx);

        assertThat(signer.verify(ctx, null)).isFalse();
        assertThat(signer.verify(ctx, "")).isFalse();
        assertThat(signer.verify(ctx, "   ")).isFalse();
        assertThat(signer.verify(ctx, valid + "=")).isFalse();
        assertThat(signer.verify(ctx, valid.substring(1))).isFalse();
        assertThat(signer.verify(ctx, "!!!not-base64url!!!")).isFalse();
    }

    @Test
    void jsRoundTripWithReorderedKeysStillVerifies() throws Exception {
        CaseContextDto original = CaseContextFixtures.returnCaseWithPrescreen();
        String signature = signer.sign(original);

        JsonMapper spring = CaseContextFixtures.springLikeMapper();
        String fromServer = spring.writeValueAsString(original);

        // What the browser does: parse into a generic object and serialize again with arbitrary key order.
        JsonMapper generic = JsonMapper.builder().build();
        Object parsed = generic.readValue(fromServer, Map.class);
        String reordered = generic.writeValueAsString(reverseKeys(parsed));

        CaseContextDto back = spring.readValue(reordered, CaseContextDto.class);

        assertThat(back).isEqualTo(original);
        assertThat(signer.verify(back, signature)).isTrue();
    }

    @Test
    void jsRoundTripWithIntegralDoublesInPrescreenStillVerifies() throws Exception {
        CaseContextDto base = CaseContextFixtures.returnCase();
        ObjectNode prescreen = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        prescreen.put("confidence", 1.0);
        prescreen.putObject("consistencySignals").put("other", 0.0).put("p", 0.85);
        CaseContextDto original = new CaseContextDto(base.contextVersion(), base.caseType(), base.form(),
                base.submittedAt(), base.assessment(), base.assessmentSource(), prescreen, null, base.ruleFacts(),
                base.decision());
        String signature = signer.sign(original);

        JsonMapper spring = CaseContextFixtures.springLikeMapper();
        String fromServer = spring.writeValueAsString(original);
        assertThat(fromServer).contains("\"confidence\":1.0");
        // Browser JSON.parse + JSON.stringify writes integral numbers without a fraction.
        String browser = fromServer.replace("\"confidence\":1.0", "\"confidence\":1")
                .replace("\"other\":0.0", "\"other\":0");
        CaseContextDto back = spring.readValue(browser, CaseContextDto.class);

        assertThat(signer.verify(back, signature)).isTrue();
    }

    @Test
    void blankKeyGeneratesRandomKeyAndWarnsWithoutLeakingIt(CapturedOutput output) {
        CaseSigner generated = signer("");
        CaseContextDto ctx = CaseContextFixtures.returnCase();
        String signature = generated.sign(ctx);

        assertThat(generated.verify(ctx, signature)).isTrue();
        assertThat(signer(KEY_A).verify(ctx, signature)).isFalse();
        assertThat(output.getOut() + output.getErr())
                .contains("CASE_SIGNING_KEY not set; signatures become invalid after restart")
                .doesNotContain(KEY_A);
    }

    @Test
    void keyShorterThan32BytesFailsStartupWithClearMessage() {
        String shortKey = Base64.getEncoder().encodeToString(bytes(31, 3));

        assertThatThrownBy(() -> signer(shortKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CASE_SIGNING_KEY")
                .hasMessageContaining("32");
    }

    @Test
    void keyThatIsNotBase64FailsStartup() {
        assertThatThrownBy(() -> signer("%%% not base64 %%%"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CASE_SIGNING_KEY");
    }

    private static CaseContextDto withVersion(CaseContextDto c, int v) {
        return new CaseContextDto(v, c.caseType(), c.form(), c.submittedAt(), c.assessment(), c.assessmentSource(),
                c.prescreen(), c.consistencySignals(), c.ruleFacts(), c.decision());
    }

    private static CaseContextDto withAssessment(CaseContextDto c, ImageAssessment assessment) {
        return new CaseContextDto(c.contextVersion(), c.caseType(), c.form(), c.submittedAt(),
                assessment, c.assessmentSource(), c.prescreen(),
                c.consistencySignals(), c.ruleFacts(), c.decision());
    }

    private static CaseContextDto withForm(CaseContextDto c, CaseFormDto form) {
        return new CaseContextDto(c.contextVersion(), c.caseType(), form, c.submittedAt(), c.assessment(),
                c.assessmentSource(), c.prescreen(), c.consistencySignals(), c.ruleFacts(), c.decision());
    }

    private static CaseContextDto withRuleFacts(CaseContextDto c, RuleResult rules) {
        return new CaseContextDto(c.contextVersion(), c.caseType(), c.form(), c.submittedAt(), c.assessment(),
                c.assessmentSource(), c.prescreen(), c.consistencySignals(), rules, c.decision());
    }

    private static CaseContextDto withDecision(CaseContextDto c, DecisionRef decision) {
        return new CaseContextDto(c.contextVersion(), c.caseType(), c.form(), c.submittedAt(), c.assessment(),
                c.assessmentSource(), c.prescreen(), c.consistencySignals(), c.ruleFacts(), decision);
    }

    /** Rebuilds every map with its entries in reverse order, recursively. */
    @SuppressWarnings("unchecked")
    static Object reverseKeys(Object node) {
        if (node instanceof Map<?, ?> map) {
            List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
            Collections.reverse(entries);
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : entries) {
                out.put((String) e.getKey(), reverseKeys(e.getValue()));
            }
            return out;
        }
        if (node instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            for (Object o : list) {
                out.add(reverseKeys(o));
            }
            return out;
        }
        return node;
    }
}
