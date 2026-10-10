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

    private static PdfProperties properties(boolean pageLandscape, boolean cardsLandscape) {
        PdfProperties p = new PdfProperties();
        p.setWidth(63);
        p.setHeight(88);
        p.setMaxDpi(300);
        p.setPageLandscape(pageLandscape);
        p.setCardsLandscape(cardsLandscape);
        return p;
    }

    private int[] generate(boolean pageLandscape, boolean cardsLandscape, int cardCount) throws Exception {
        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0));
        byte[] pdf = generator.generateDocument(cards(cardCount), properties(pageLandscape, cardsLandscape), "id", dto -> { });
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDRectangle box = doc.getPage(0).getMediaBox();
            return new int[] { Math.round(box.getWidth()), Math.round(box.getHeight()), doc.getNumberOfPages() };
        }
    }

    @Test
    void portraitIsDefault() throws Exception {
        int[] result = generate(false, false, 9);
        assertTrue(result[0] < result[1], "hochkant");
        assertEquals(1, result[2], "3 x 3 Karten passen auf eine Seite");
    }

    @Test
    void landscapeSwapsPageAndFitsFourByTwo() throws Exception {
        int[] result = generate(true, false, 9);
        assertEquals(Math.round(PDRectangle.A4.getHeight()), result[0]);
        assertEquals(Math.round(PDRectangle.A4.getWidth()), result[1]);
        assertEquals(2, result[2], "quer passen 4 x 2 = 8 Karten, die neunte kommt auf Seite 2");
    }

    @Test
    void landscapeCardsOnPortraitPageFitTwoByFour() throws Exception {
        int[] result = generate(false, true, 9);
        assertTrue(result[0] < result[1], "Blatt bleibt hochkant");
        assertEquals(2, result[2], "2 x 4 = 8 querliegende Karten pro Seite");
    }

    @Test
    void landscapeCardsOnLandscapePageFitThreeByThree() throws Exception {
        int[] result = generate(true, true, 9);
        assertTrue(result[0] > result[1], "Blatt quer");
        assertEquals(1, result[2], "3 x 3 querliegende Karten auf einer Seite");
    }

    @Test
    void rotatesCardImageClockwise() {
        BufferedImage img = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFF0000); // links oben
        BufferedImage rotated = DocumentGenerator.rotateClockwise(img);
        assertEquals(3, rotated.getWidth());
        assertEquals(2, rotated.getHeight());
        assertEquals(0xFF0000, rotated.getRGB(2, 0) & 0xFFFFFF, "links oben landet rechts oben");
    }
}
