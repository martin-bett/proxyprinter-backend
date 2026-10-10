package my.poo.mtgcrawl.mtgprint.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;

class DocumentGeneratorLandscapeTest {

    @TempDir
    Path tempDir;

    private List<ImageDto> cards(int count) throws Exception {
        File file = tempDir.resolve("card.png").toFile();
        ImageIO.write(new BufferedImage(63, 88, BufferedImage.TYPE_INT_RGB), "png", file);
        List<ImageDto> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ImageDto dto = new ImageDto();
            dto.setImageUrl(file.getAbsolutePath());
            cards.add(dto);
        }
        return cards;
    }

    private static PdfProperties properties(boolean landscape) {
        PdfProperties p = new PdfProperties();
        p.setWidth(63);
        p.setHeight(88);
        p.setMaxDpi(300);
        p.setLandscape(landscape);
        return p;
    }

    private int[] generate(boolean landscape, int cardCount) throws Exception {
        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0));
        byte[] pdf = generator.generateDocument(cards(cardCount), properties(landscape), "id", dto -> { });
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDRectangle box = doc.getPage(0).getMediaBox();
            return new int[] { Math.round(box.getWidth()), Math.round(box.getHeight()), doc.getNumberOfPages() };
        }
    }

    @Test
    void portraitIsDefault() throws Exception {
        int[] result = generate(false, 9);
        assertTrue(result[0] < result[1], "hochkant");
        assertEquals(1, result[2], "3 x 3 Karten passen auf eine Seite");
    }

    @Test
    void landscapeSwapsPageAndFitsFourByTwo() throws Exception {
        int[] result = generate(true, 9);
        assertEquals(Math.round(PDRectangle.A4.getHeight()), result[0]);
        assertEquals(Math.round(PDRectangle.A4.getWidth()), result[1]);
        assertEquals(2, result[2], "quer passen 4 x 2 = 8 Karten, die neunte kommt auf Seite 2");
    }
}
