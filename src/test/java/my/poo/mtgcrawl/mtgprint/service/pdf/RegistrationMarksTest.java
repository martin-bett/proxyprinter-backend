package my.poo.mtgcrawl.mtgprint.service.pdf;

import static my.poo.mtgcrawl.mtgprint.service.pdf.DocumentGenerator.MM_TO_PT;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RegistrationMarkStyle;

/** Silhouette-Passmarken: Ecken bleiben frei, Raster rückt ein, Marken wie in Silhouette Studio. */
class RegistrationMarksTest {

    private static final float A4_W = PDRectangle.A4.getWidth();
    private static final float A4_H = PDRectangle.A4.getHeight();
    private static final float CARD_W = 63.5f * MM_TO_PT;
    private static final float CARD_H = 88.9f * MM_TO_PT;

    @TempDir
    Path tempDir;

    @Test
    void portraitA4HoldsFourRealCardsBetweenTheMarks() {
        // oben 25 mm: 3 Reihen (266,7 mm) passen nicht zwischen die Marken (262 mm)
        RegistrationMarks.Grid grid = RegistrationMarks.grid(A4_W, A4_H, CARD_W, CARD_H, 0, 0);
        assertEquals(2, grid.cols());
        assertEquals(2, grid.rows());
        assertEquals(41.5f * MM_TO_PT, grid.left(), 0.05f);
        // mittig zwischen 25 mm oben und 10 mm unten
        assertEquals((25f + (262f - 2 * 88.9f) / 2) * MM_TO_PT, grid.top(), 0.05f);
        assertEquals(20f, grid.markLengthMm());
    }

    @Test
    void landscapeA4HoldsEightRealCardsWithShortMarks() {
        // rechts 25 mm: 4 Karten mittig zwischen 10 und 272 mm, oben 16,1 mm frei = 6,1 mm hinter den Marken
        RegistrationMarks.Grid grid = RegistrationMarks.grid(A4_H, A4_W, CARD_W, CARD_H, 0, 0);
        assertEquals(4, grid.cols());
        assertEquals(2, grid.rows());
        assertEquals(14f * MM_TO_PT, grid.left(), 0.05f);
        assertEquals(5f, grid.markLengthMm());
    }

    @Test
    void portraitA4HoldsEightLandscapeCards() {
        RegistrationMarks.Grid grid = RegistrationMarks.grid(A4_W, A4_H, CARD_H, CARD_W, 0, 0);
        assertEquals(8, grid.cards());
        assertEquals(29f * MM_TO_PT, grid.top(), 0.05f);
    }

    @Test
    void farSideOfTheFeedKeepsTwentyFiveMillimetres() {
        assertArrayEquals(new float[] { 10, 10, 25, 10 }, RegistrationMarks.insetsMm(true));
        assertArrayEquals(new float[] { 10, 25, 10, 10 }, RegistrationMarks.insetsMm(false));
    }

    @Test
    void gridStaysBetweenTheMarksAndOutOfTheCorners() {
        for (boolean landscape : new boolean[] { false, true }) {
            float pw = landscape ? A4_H : A4_W;
            float ph = landscape ? A4_W : A4_H;
            float[] inset = RegistrationMarks.insetsMm(landscape);
            for (float gap = 0; gap <= 5; gap += 2.5f) {
                RegistrationMarks.Grid grid = RegistrationMarks.grid(pw, ph, CARD_W, CARD_H, gap * MM_TO_PT, gap * MM_TO_PT);
                float gridW = grid.cols() * CARD_W + (grid.cols() - 1) * gap * MM_TO_PT;
                float gridH = grid.rows() * CARD_H + (grid.rows() - 1) * gap * MM_TO_PT;
                float freeX = grid.left() / MM_TO_PT - inset[0];
                float freeY = grid.top() / MM_TO_PT - inset[1];
                String at = (landscape ? "quer" : "hoch") + ", Abstand " + gap;
                assertEquals(freeX, pw / MM_TO_PT - inset[2] - (grid.left() + gridW) / MM_TO_PT, 0.05f, "mittig " + at);
                assertEquals(freeY, ph / MM_TO_PT - inset[3] - (grid.top() + gridH) / MM_TO_PT, 0.05f, "mittig " + at);
                float corner = grid.markLengthMm() + RegistrationMarks.CLEARANCE_MM;
                assertTrue(freeX >= corner - 0.01f || freeY >= corner - 0.01f, "Ecken frei " + at);
                assertTrue(grid.markLengthMm() >= RegistrationMarks.MIN_LENGTH_MM, at);
            }
        }
    }

    @Test
    void markLengthIsWholeMillimetresBetweenFiveAndTwenty() {
        assertEquals(20f, RegistrationMarks.markLength(31.5f));
        assertEquals(5f, RegistrationMarks.markLength(6.1f));
        assertTrue(RegistrationMarks.markLength(5.5f) < RegistrationMarks.MIN_LENGTH_MM);
    }

    @Test
    void threeMarksAreSquareAndTwoAnglesWithoutCutlines() throws Exception {
        List<Object> tokens = firstPageTokens(RegistrationMarkStyle.SILHOUETTE_3, 7, false);
        assertEquals(4, count(tokens, "cm"), "4 Karten auf Seite 1");
        assertEquals(2, count(tokens, "S"), "zwei Winkel, keine Schnittlinien");
        assertEquals(1, count(tokens, "re"), "Quadrat");
        // hoch: Quadrat unten links, 10 mm von beiden Kanten
        float[] square = rect(tokens);
        assertEquals(10f * MM_TO_PT, square[0], 0.01f);
        assertEquals(10f * MM_TO_PT, square[1], 0.01f);
        // erster Winkel oben links, 25 mm von oben
        float[] angle = firstMoveAndLine(tokens);
        assertEquals(30f * MM_TO_PT, angle[0], 0.01f);
        assertEquals(A4_H - 25f * MM_TO_PT, angle[1], 0.01f);
    }

    @Test
    void landscapeThreeMarksHaveTheSquareTopLeftAndTheFarAngleAt25mm() throws Exception {
        List<Object> tokens = firstPageTokens(RegistrationMarkStyle.SILHOUETTE_3, 1, true);
        float[] square = rect(tokens);
        assertEquals(10f * MM_TO_PT, square[0], 0.01f);
        assertEquals(A4_W - 15f * MM_TO_PT, square[1], 0.01f, "Unterkante 15 mm von oben");
        // erster Winkel oben rechts: Ecke 25 mm vom rechten Rand, Arm 5 mm nach links
        float[] angle = firstMoveAndLine(tokens);
        assertEquals(A4_H - 30f * MM_TO_PT, angle[0], 0.01f);
        assertEquals(A4_W - 10f * MM_TO_PT, angle[1], 0.01f);
        assertEquals(A4_H - 25f * MM_TO_PT, angle[2], 0.01f);
    }

    @Test
    void fourMarksAreAnglesInEveryCorner() throws Exception {
        List<Object> tokens = firstPageTokens(RegistrationMarkStyle.SILHOUETTE_4, 1, false);
        assertEquals(4, count(tokens, "S"));
        assertEquals(0, count(tokens, "re"));
    }

    @Test
    void withoutMarksTheLayoutStaysAsBefore() throws Exception {
        List<Object> tokens = firstPageTokens(null, 9, false);
        assertEquals(9, count(tokens, "cm"));
    }

    private List<Object> firstPageTokens(RegistrationMarkStyle style, int cards, boolean landscape) throws Exception {
        File file = tempDir.resolve("card.png").toFile();
        ImageIO.write(new BufferedImage(64, 89, BufferedImage.TYPE_INT_RGB), "png", file);
        List<ImageDto> images = new ArrayList<>();
        for (int i = 0; i < cards; i++) {
            ImageDto dto = new ImageDto();
            dto.setImageUrl(file.getAbsolutePath());
            images.add(dto);
        }
        PdfProperties p = new PdfProperties();
        p.setWidth(63.5);
        p.setHeight(88.9);
        p.setMaxDpi(300);
        p.setCutlines(true);
        p.setCutlineColor("#43FE43");
        p.setRegistrationMarks(style);
        p.setPageLandscape(landscape);
        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0));
        byte[] pdf = generator.generateDocument(images, p, "id", dto -> { });
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDFStreamParser parser = new PDFStreamParser(doc.getPage(0));
            parser.parse();
            return parser.getTokens();
        }
    }

    private static long count(List<Object> tokens, String operator) {
        return tokens.stream().filter(t -> t instanceof Operator op && op.getName().equals(operator)).count();
    }

    /** x, y des ersten Rechtecks (re). */
    private static float[] rect(List<Object> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) instanceof Operator op && op.getName().equals("re")) {
                return new float[] { ((COSNumber) tokens.get(i - 4)).floatValue(), ((COSNumber) tokens.get(i - 3)).floatValue() };
            }
        }
        throw new AssertionError("kein Rechteck");
    }

    /** x, y des ersten moveTo und x des folgenden lineTo. */
    private static float[] firstMoveAndLine(List<Object> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) instanceof Operator op && op.getName().equals("m")) {
                return new float[] { ((COSNumber) tokens.get(i - 2)).floatValue(), ((COSNumber) tokens.get(i - 1)).floatValue(),
                        ((COSNumber) tokens.get(i + 1)).floatValue() };
            }
        }
        throw new AssertionError("kein moveTo");
    }
}
