package pl.elektrodemo.copilot.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import pl.elektrodemo.copilot.web.UnsupportedImageException;

/**
 * Produces the analysis copy sent to the LLM: EXIF-oriented, at most 1536 px on the longer side,
 * never upscaled, alpha flattened onto white, re-encoded as JPEG q=0.85 without EXIF/APP1 metadata.
 */
@Component
public class ImageDownscaler {

    static final long MAX_PIXELS = 40_000_000L;
    static final int MAX_SIDE = 1536;
    static final float JPEG_QUALITY = 0.85f;

    public byte[] toAnalysisJpeg(byte[] original) {
        if (original == null || original.length == 0) {
            throw new UnsupportedImageException();
        }
        checkPixelBudget(original);
        try {
            BufferedImage oriented = orientAndFit(original);
            return encodeJpeg(flattenOnWhite(oriented));
        } catch (IOException | RuntimeException e) {
            throw new UnsupportedImageException();
        }
    }

    /** Reads only the header via ImageIO readers, so oversized images are rejected before any full decode. */
    private static void checkPixelBudget(byte[] original) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(original))) {
            if (iis == null) {
                throw new UnsupportedImageException();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new UnsupportedImageException();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    throw new UnsupportedImageException();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof UnsupportedImageException u) {
                throw u;
            }
            throw new UnsupportedImageException();
        }
    }

    private static BufferedImage orientAndFit(byte[] original) throws IOException {
        BufferedImage probe = ImageIO.read(new ByteArrayInputStream(original));
        if (probe == null) {
            throw new UnsupportedImageException();
        }
        int longest = Math.max(probe.getWidth(), probe.getHeight());
        var builder = Thumbnails.of(new ByteArrayInputStream(original)).useExifOrientation(true);
        // Thumbnailator's size() also upscales smaller sources, so resize only when the longer side exceeds the bound
        return longest > MAX_SIDE
                ? builder.size(MAX_SIDE, MAX_SIDE).asBufferedImage()
                : builder.scale(1.0).asBufferedImage();
    }

    private static BufferedImage flattenOnWhite(BufferedImage src) {
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private static byte[] encodeJpeg(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }
}
