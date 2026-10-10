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

    /** Radius der ersten Eckmarke (oben links) aus dem Content-Stream: lineTo.x - moveTo.x. */
    private float firstCornerRadius(PdfProperties properties) throws Exception {
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
            Float moveX = null;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i) instanceof Operator op) {
                    if (moveX == null && op.getName().equals("m")) {
                        moveX = ((COSNumber) tokens.get(i - 2)).floatValue();
                    } else if (moveX != null && op.getName().equals("l")) {
                        return ((COSNumber) tokens.get(i - 2)).floatValue() - moveX;
                    }
                }
            }
        }
        throw new AssertionError("no corner path found");
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
