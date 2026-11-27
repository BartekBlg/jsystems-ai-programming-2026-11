package pl.elektrodemo.copilot.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.config.AppProperties;
import pl.elektrodemo.copilot.web.dto.CaseContextDto;

/** ADR-000 8.4: HMAC-SHA256 over the canonical JSON of the case context. Key material is never logged. */
@Component
public class CaseSigner {

    private static final Logger log = LoggerFactory.getLogger(CaseSigner.class);
    private static final int MIN_KEY_BYTES = 32;
    private static final String ALGORITHM = "HmacSHA256";

    private final CanonicalJson canonicalJson = new CanonicalJson();
    private final byte[] key;

    public CaseSigner(AppProperties properties) {
        String configured = properties.caseSigningKey();
        if (configured == null || configured.isBlank()) {
            this.key = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(this.key);
            log.warn("CASE_SIGNING_KEY not set; signatures become invalid after restart");
            return;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configured.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("CASE_SIGNING_KEY must be Base64-encoded (at least 32 bytes)", e);
        }
        if (decoded.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "CASE_SIGNING_KEY must decode to at least 32 bytes, got " + decoded.length);
        }
        this.key = decoded;
    }

    public String sign(CaseContextDto ctx) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac(ctx));
    }

    public boolean verify(CaseContextDto ctx, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        byte[] expected = sign(ctx).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = signature.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] mac(CaseContextDto ctx) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(canonicalJson.write(ctx).getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}
