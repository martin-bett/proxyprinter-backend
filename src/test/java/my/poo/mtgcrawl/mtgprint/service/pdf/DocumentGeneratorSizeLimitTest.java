package my.poo.mtgcrawl.mtgprint.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;

class DocumentGeneratorSizeLimitTest {

    @TempDir
    Path tempDir;

    /** Rauschen lässt sich kaum komprimieren, ein Bild landet mit gut 2 MB im PDF. */
    private List<ImageDto> noisyCards(int count) throws Exception {
        BufferedImage img = new BufferedImage(744, 1039, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, random.nextInt(0xFFFFFF));
            }
        }
        File file = tempDir.resolve("noise.png").toFile();
        ImageIO.write(img, "png", file);
        List<ImageDto> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ImageDto dto = new ImageDto();
            dto.setImageUrl(file.getAbsolutePath());
            cards.add(dto);
        }
        return cards;
    }

    private static PdfProperties properties() {
        PdfProperties p = new PdfProperties();
        p.setWidth(63);
        p.setHeight(88);
        p.setMaxDpi(300);
        return p;
    }

    @Test
    void abortsWhenEmbeddedImagesExceedLimit() throws Exception {
        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(3));
        PdfSizeLimit.PdfTooLargeException e = assertThrows(PdfSizeLimit.PdfTooLargeException.class,
                () -> generator.generateDocument(noisyCards(3), properties(), "id", dto -> { }));
        assertTrue(e.getMessage().contains("limit of 3 MB"), e.getMessage());
        assertTrue(e.getMessage().contains("card 2 of 3"), e.getMessage());
    }

    @Test
    void buildsWhenBelowLimit() throws Exception {
        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(10));
        byte[] pdf = generator.generateDocument(noisyCards(2), properties(), "id", dto -> { });
        assertTrue(pdf.length > 0);
    }

    @Test
    void zeroDisablesLimit() {
        assertDoesNotThrow(() -> new PdfSizeLimit(0).check(Long.MAX_VALUE, 1, 1));
    }
}
