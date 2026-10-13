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
    void portraitA4HoldsSixRealCardsWithFullLengthMarks() {
        // 3 Spalten (190,5 mm) passen nicht zwischen die Marken (190 mm), 2 x 3 lassen links 41,5 mm frei
        RegistrationMarks.Grid grid = RegistrationMarks.grid(A4_W, A4_H, CARD_W, CARD_H, 0, 0);
        assertEquals(2, grid.cols());
        assertEquals(3, grid.rows());
        assertEquals(41.5f * MM_TO_PT, grid.left(), 0.05f);
        assertEquals(20f, grid.markLengthMm());
    }

    @Test
    void landscapeA4HoldsEightRealCardsWithShorterMarks() {
        // 4 x 2: links 21,5 mm frei, abzüglich 10 mm Abstand und 2 mm Luft bleiben 9 mm für die Arme
        RegistrationMarks.Grid grid = RegistrationMarks.grid(A4_H, A4_W, CARD_W, CARD_H, 0, 0);
        assertEquals(4, grid.cols());
        assertEquals(2, grid.rows());
        assertEquals(9f, grid.markLengthMm());
    }

    @Test
    void gridNeverReachesIntoACornerArea() {
        for (float gap = 0; gap <= 5; gap += 2.5f) {
            RegistrationMarks.Grid grid = RegistrationMarks.grid(A4_W, A4_H, CARD_W, CARD_H, gap * MM_TO_PT, gap * MM_TO_PT);
            float corner = (RegistrationMarks.INSET_MM + grid.markLengthMm() + RegistrationMarks.CLEARANCE_MM) * MM_TO_PT;
            assertTrue(grid.left() >= corner - 0.01f || grid.top() >= corner - 0.01f, "Abstand " + gap);
            assertTrue(grid.left() >= RegistrationMarks.INSET_MM * MM_TO_PT, "zwischen den Marken");
            assertTrue(grid.markLengthMm() >= RegistrationMarks.MIN_LENGTH_MM);
        }
    }

    @Test
    void markLengthIsWholeMillimetresBetweenFiveAndTwenty() {
        assertEquals(20f, RegistrationMarks.markLength(41.5f));
        assertEquals(9f, RegistrationMarks.markLength(21.5f));
        assertEquals(5f, RegistrationMarks.markLength(17f));
        assertTrue(RegistrationMarks.markLength(16f) < RegistrationMarks.MIN_LENGTH_MM);
    }

    @Test
    void threeMarksAreSquareAndTwoAnglesWithoutCutlines() throws Exception {
        List<Object> tokens = firstPageTokens(RegistrationMarkStyle.SILHOUETTE_3, 7);
        assertEquals(6, count(tokens, "cm"), "6 Karten auf Seite 1");
        assertEquals(2, count(tokens, "S"), "zwei Winkel, keine Schnittlinien");
        assertEquals(1, count(tokens, "re"), "Quadrat oben links");
    }

    @Test
    void fourMarksAreAnglesInEveryCorner() throws Exception {
        List<Object> tokens = firstPageTokens(RegistrationMarkStyle.SILHOUETTE_4, 1);
        assertEquals(4, count(tokens, "S"));
        assertEquals(0, count(tokens, "re"));
        // Ecke oben links 10 mm von beiden Kanten, Arm 20 mm nach rechts
        float[] firstAngle = firstMoveAndLine(tokens);
        assertEquals(30f * MM_TO_PT, firstAngle[0], 0.01f);
        assertEquals(A4_H - 10f * MM_TO_PT, firstAngle[1], 0.01f);
        assertEquals(10f * MM_TO_PT, firstAngle[2], 0.01f);
    }

    @Test
    void withoutMarksTheLayoutStaysAsBefore() throws Exception {
        List<Object> tokens = firstPageTokens(null, 9);
        assertEquals(9, count(tokens, "cm"));
    }

    private List<Object> firstPageTokens(RegistrationMarkStyle style, int cards) throws Exception {
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
