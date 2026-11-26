package pl.elektrodemo.copilot.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ImageTypeDetectorTest {

    private final ImageTypeDetector detector = new ImageTypeDetector();

    static Stream<Arguments> signatures() {
        return Stream.of(
                Arguments.of("JPEG magic", "FFD8FFE000104A4649460001", ImageType.JPEG),
                Arguments.of("JPEG magic exactly 3 bytes", "FFD8FF", ImageType.JPEG),
                Arguments.of("PNG signature", "89504E470D0A1A0A0000000D", ImageType.PNG),
                Arguments.of("WebP RIFF....WEBP", "52494646AABBCCDD57454250565038", ImageType.WEBP));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("signatures")
    void detectsSupportedSignatures(String name, String hex, ImageType expected) {
        assertThat(detector.detect(bytes(hex))).contains(expected);
    }

    static Stream<Arguments> unsupportedOrTruncated() {
        return Stream.of(
                Arguments.of("GIF87a/89a", "47494638396101000100"),
                Arguments.of("HEIC ftypheic", "0000001866747970686569630000"),
                Arguments.of("empty array", ""),
                Arguments.of("JPEG prefix truncated to 2 bytes", "FFD8"),
                Arguments.of("PNG prefix truncated (3 bytes)", "89504E"),
                Arguments.of("RIFF without WEBP tag (WAV)", "52494646AABBCCDD57415645666D7420"),
                Arguments.of("plain text", "48656C6C6F2C20776F726C6421"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsupportedOrTruncated")
    void rejectsUnsupportedOrTruncatedInput(String name, String hex) {
        assertThat(detector.detect(bytes(hex))).isEmpty();
    }

    @Test
    void nullInputIsTreatedAsUnknown() {
        assertThat(detector.detect(null)).isEmpty();
    }

    @Test
    void detectsByContentNotByExtension() {
        byte[] textNamedAsPng = "GIF89a-not-a-png".getBytes(StandardCharsets.US_ASCII);
        assertThat(detector.detect(textNamedAsPng)).isEmpty();
    }

    private static byte[] bytes(String hex) {
        return HexFormat.of().parseHex(hex);
    }
}
