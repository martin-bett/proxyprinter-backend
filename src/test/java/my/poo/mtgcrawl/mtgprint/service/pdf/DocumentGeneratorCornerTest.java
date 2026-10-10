package my.poo.mtgcrawl.mtgprint.service.pdf;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;

class DocumentGeneratorCornerTest {

    @TempDir
    Path tempDir;

    private static PdfProperties properties(Double cornerSize) {
        PdfProperties p = new PdfProperties();
        p.setWidth(63);
        p.setHeight(88);
        p.setMaxDpi(300);
        p.setCorners(true);
        p.setCornerColorAuto(false);
        p.setCornerColor("#000000");
        p.setCornerSize(cornerSize);
        return p;
    }

    /** Operanden der Pfad-Operatoren m, l und c der Seite 1, in Reihenfolge. */
    private List<float[]> pathOperations(PdfProperties properties) throws Exception {
        BufferedImage img = new BufferedImage(63, 88, BufferedImage.TYPE_INT_RGB);
        File file = tempDir.resolve("card.png").toFile();
        ImageIO.write(img, "png", file);
        ImageDto card = new ImageDto();
        card.setImageUrl(file.getAbsolutePath());
        List<ImageDto> cards = new ArrayList<>(List.of(card));

        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0));
        byte[] pdf = generator.generateDocument(cards, properties, "id", dto -> { });
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDFStreamParser parser = new PDFStreamParser(doc.getPage(0));
            parser.parse();
            List<Object> tokens = parser.getTokens();
            List<float[]> ops = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i) instanceof Operator op) {
                    int n = switch (op.getName()) {
                        case "m", "l" -> 2;
                        case "c" -> 6;
                        default -> 0;
                    };
                    if (n > 0) {
                        float[] values = new float[n];
                        for (int k = 0; k < n; k++) {
                            values[k] = ((COSNumber) tokens.get(i - n + k)).floatValue();
                        }
                        ops.add(values);
                    }
                }
            }
            return ops;
        }
    }

    /** Radius der ersten Eckmarke (oben links) aus dem Content-Stream: lineTo.x - moveTo.x. */
    private float firstCornerRadius(PdfProperties properties) throws Exception {
        List<float[]> ops = pathOperations(properties);
        return ops.get(1)[0] - ops.get(0)[0];
    }

    /**
     * Alle vier Ecken sind dieselbe Form, nur gespiegelt: Abstände der Punkte zur Kartenecke stimmen überein,
     * und die Kurvenmitte liegt auf dem Viertelkreis um den Mittelpunkt (r, r) von der Ecke aus.
     */
    @Test
    void allFourCornersHaveTheSameShape() throws Exception {
        List<float[]> ops = pathOperations(properties(5.0));
        assertEquals(12, ops.size(), "4 corners with moveTo, lineTo, curveTo");
        float r = 5 * 2.83f;
        float[] reference = null;
        for (int corner = 0; corner < 4; corner++) {
            float[] m = ops.get(corner * 3);
            float[] l = ops.get(corner * 3 + 1);
            float[] c = ops.get(corner * 3 + 2);
            // Abstände relativ zur Ecke, Vorzeichen weg (Spiegelung)
            float[] shape = {
                    Math.abs(l[0] - m[0]), Math.abs(l[1] - m[1]),
                    Math.abs(c[0] - m[0]), Math.abs(c[1] - m[1]),
                    Math.abs(c[2] - m[0]), Math.abs(c[3] - m[1]),
                    Math.abs(c[4] - m[0]), Math.abs(c[5] - m[1]) };
            if (reference == null) {
                reference = shape;
            } else {
                assertArrayEquals(reference, shape, 0.01f, "corner " + corner);
            }
            // Kurvenmitte (t = 0.5) im Abstand r vom Kreismittelpunkt
            float midX = (l[0] + 3 * c[0] + 3 * c[2] + c[4]) / 8;
            float midY = (l[1] + 3 * c[1] + 3 * c[3] + c[5]) / 8;
            float centerX = m[0] + Math.signum(l[0] - m[0]) * r;
            float centerY = m[1] + Math.signum(c[5] - m[1]) * r;
            assertEquals(r, (float) Math.hypot(midX - centerX, midY - centerY), 0.01f, "corner " + corner);
        }
        assertEquals(r, reference[0], 0.01f);
        assertEquals(r, reference[7], 0.01f);
    }

    @Test
    void withoutCornerSizeRadiusStaysThreePercentOfCardHeight() throws Exception {
        float cardHeight = 88 * 2.83f;
        assertEquals(cardHeight * 0.03f, DocumentGenerator.cornerRadius(properties(null), 63 * 2.83f, cardHeight));
        assertEquals(cardHeight * 0.03f, firstCornerRadius(properties(null)), 0.001f);
    }

    @Test
    void cornerSizeIsRadiusInMillimetres() throws Exception {
        assertEquals(3.2f * 2.83f, DocumentGenerator.cornerRadius(properties(3.2), 63 * 2.83f, 88 * 2.83f), 0.0001f);
        assertEquals(3.2f * 2.83f, firstCornerRadius(properties(3.2)), 0.001f);
    }

    @Test
    void cornerSizeIsCappedAtHalfTheShorterSide() {
        assertEquals(63 * 2.83f / 2, DocumentGenerator.cornerRadius(properties(100.0), 63 * 2.83f, 88 * 2.83f), 0.0001f);
    }

    @Test
    void missingCornerSizeKeepsRequestJsonUnchanged() throws Exception {
        String json = new ObjectMapper().writeValueAsString(properties(null));
        assertFalse(json.contains("cornerSize"), json);
    }
}
