package pl.elektrodemo.copilot.image;

import java.util.Optional;
import org.springframework.stereotype.Component;

/** Detects the image format from magic bytes only; file names and client MIME types are never trusted. */
@Component
public class ImageTypeDetector {

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] RIFF = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP = {0x57, 0x45, 0x42, 0x50};

    public Optional<ImageType> detect(byte[] firstBytes) {
        if (firstBytes == null) {
            return Optional.empty();
        }
        if (regionEquals(firstBytes, 0, JPEG_MAGIC)) {
            return Optional.of(ImageType.JPEG);
        }
        if (regionEquals(firstBytes, 0, PNG_MAGIC)) {
            return Optional.of(ImageType.PNG);
        }
        if (regionEquals(firstBytes, 0, RIFF) && regionEquals(firstBytes, 8, WEBP)) {
            return Optional.of(ImageType.WEBP);
        }
        return Optional.empty();
    }

    private static boolean regionEquals(byte[] data, int offset, byte[] expected) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
