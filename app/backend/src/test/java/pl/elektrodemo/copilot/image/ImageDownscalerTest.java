package pl.elektrodemo.copilot.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import pl.elektrodemo.copilot.web.UnsupportedImageException;

class ImageDownscalerTest {

    private static final Path EXAMPLES = Path.of("../../assets/example-images");

    private final ImageDownscaler downscaler = new ImageDownscaler();

    @Test
    void largeJpegDisguisedAsPngIsFittedToLongerSide1536() throws IOException {
        byte[] out = downscaler.toAnalysisJpeg(read("laptop-1.png"));

        BufferedImage img = decode(out);
        assertJpeg(out);
        assertThat(Math.max(img.getWidth(), img.getHeight())).isEqualTo(1536);
        // 3024x3135 scaled by 1536/3135 -> width ~1481.6, height 1536
        assertThat(img.getHeight()).isEqualTo(1536);
        assertThat(img.getWidth()).isBetween(1481, 1482);
    }

    @Test
    void smallWebpIsReEncodedAtOriginalSize() throws IOException {
        byte[] out = downscaler.toAnalysisJpeg(read("laptop-2.webp"));

        BufferedImage img = decode(out);
        assertJpeg(out);
        assertThat(img.getWidth()).isEqualTo(863);
        assertThat(img.getHeight()).isEqualTo(843);
    }

    @Test
    void smallJpegIsNotUpscaled() throws IOException {
        byte[] out = downscaler.toAnalysisJpeg(read("phone-2.jpeg"));

        BufferedImage img = decode(out);
        assertJpeg(out);
        assertThat(img.getWidth()).isEqualTo(387);
        assertThat(img.getHeight()).isEqualTo(516);
    }

    @Test
    void exifSegmentIsStrippedFromRealExampleWithExif() throws IOException {
        byte[] out = downscaler.toAnalysisJpeg(read("phone-1.jpg"));

        assertJpeg(out);
        assertNoApp1(out);
    }

    @Test
    void fourThousandByThreeThousandIsFittedTo1536x1152() throws IOException {
        byte[] out = downscaler.toAnalysisJpeg(encode(solid(4000, 3000, Color.BLUE), "png"));

        BufferedImage img = decode(out);
        assertThat(img.getWidth()).isEqualTo(1536);
        assertThat(img.getHeight()).isEqualTo(1152);
    }

    @Test
    void transparentAreaIsFlattenedOntoWhite() throws IOException {
        BufferedImage argb = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                argb.setRGB(x, y, x < 50 ? 0x00000000 : 0xFF000000); // left half fully transparent
            }
        }

        byte[] out = downscaler.toAnalysisJpeg(encode(argb, "png"));

        BufferedImage img = decode(out);
        assertJpeg(out);
        int left = img.getRGB(10, 50) & 0xFFFFFF;
        int right = img.getRGB(90, 50) & 0xFFFFFF;
        assertThat(channelsAtLeast(left, 250)).as("transparent area must be white").isTrue();
        assertThat(right & 0xFF).as("opaque black area must stay dark").isLessThan(20);
    }

    @Test
    void exifOrientation6RotatesOutputAndSwapsDimensions() throws IOException {
        byte[] jpeg = withExifOrientation(encode(solid(400, 200, Color.GREEN), "jpg"), 6);

        byte[] out = downscaler.toAnalysisJpeg(jpeg);

        BufferedImage img = decode(out);
        assertJpeg(out);
        assertThat(img.getWidth()).isEqualTo(200);
        assertThat(img.getHeight()).isEqualTo(400);
        assertNoApp1(out);
    }

    @Test
    void imageHeaderAbove40MegapixelsIsRejectedBeforeDecoding() throws IOException {
        byte[] huge = pngHeaderWithDimensions(7000, 6000);

        assertThatThrownBy(() -> downscaler.toAnalysisJpeg(huge))
                .isInstanceOf(UnsupportedImageException.class);
    }

    @Test
    void pixelLimitIsExactly40Megapixels() {
        assertThat(ImageDownscaler.MAX_PIXELS).isEqualTo(40_000_000L);
    }

    @Test
    void undecodableBytesAreRejected() {
        byte[] garbage = "definitely not an image".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> downscaler.toAnalysisJpeg(garbage))
                .isInstanceOf(UnsupportedImageException.class);
    }

    @Test
    void emptyInputIsRejected() {
        assertThatThrownBy(() -> downscaler.toAnalysisJpeg(new byte[0]))
                .isInstanceOf(UnsupportedImageException.class);
    }

    // ---- helpers -----------------------------------------------------------

    private static byte[] read(String name) throws IOException {
        return Files.readAllBytes(EXAMPLES.resolve(name));
    }

    private static void assertJpeg(byte[] out) {
        assertThat(out[0] & 0xFF).isEqualTo(0xFF);
        assertThat(out[1] & 0xFF).isEqualTo(0xD8);
    }

    /** APP1 (FF E1) cannot occur in entropy-coded data (FF is always stuffed with 00), so a byte scan is exact. */
    private static void assertNoApp1(byte[] out) {
        for (int i = 0; i + 1 < out.length; i++) {
            assertThat(out[i] == (byte) 0xFF && out[i + 1] == (byte) 0xE1)
                    .as("APP1 marker at offset %d", i)
                    .isFalse();
        }
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        assertThat(img).as("output must be decodable").isNotNull();
        return img;
    }

    private static BufferedImage solid(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, color.getRGB());
            }
        }
        return img;
    }

    private static byte[] encode(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertThat(ImageIO.write(img, format, bos)).isTrue();
        return bos.toByteArray();
    }

    private static boolean channelsAtLeast(int rgb, int min) {
        return ((rgb >> 16) & 0xFF) >= min && ((rgb >> 8) & 0xFF) >= min && (rgb & 0xFF) >= min;
    }

    /**
     * Inserts an APP1 Exif segment with a single Orientation tag (0x0112) right after SOI.
     * The JPEG body is untouched; only the metadata segment is added.
     */
    static byte[] withExifOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {
            'I', 'I', 42, 0, 8, 0, 0, 0,          // little-endian TIFF header, IFD0 at offset 8
            1, 0,                                 // one IFD entry
            0x12, 0x01, 3, 0, 1, 0, 0, 0,         // tag 0x0112 Orientation, SHORT, count 1
            (byte) orientation, 0, 0, 0,          // value (left-justified)
            0, 0, 0, 0                            // next IFD offset
        };
        byte[] exifHeader = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
        int segmentLength = 2 + exifHeader.length + tiff.length;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2); // SOI FF D8
        out.write(0xFF);
        out.write(0xE1);
        out.write((segmentLength >> 8) & 0xFF);
        out.write(segmentLength & 0xFF);
        out.write(exifHeader, 0, exifHeader.length);
        out.write(tiff, 0, tiff.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }

    /**
     * A 1x1 PNG whose IHDR declares the given size. ImageIO reads the size from the header only,
     * so this exercises the pixel-count gate without allocating the full raster.
     */
    static byte[] pngHeaderWithDimensions(int width, int height) throws IOException {
        byte[] png = encode(solid(1, 1, Color.WHITE), "png");
        // IHDR data starts at offset 16: width (4 bytes), height (4 bytes)
        writeInt(png, 16, width);
        writeInt(png, 20, height);
        // IHDR CRC covers chunk type + data (offsets 12..28) and is stored at 29..32
        CRC32 crc = new CRC32();
        crc.update(png, 12, 17);
        writeInt(png, 29, (int) crc.getValue());
        return png;
    }

    private static void writeInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }
}
