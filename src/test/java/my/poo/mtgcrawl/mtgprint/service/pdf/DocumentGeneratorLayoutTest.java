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

/** Echte Kartengröße (63,5 x 88,9 mm): 3 x 3 auf A4, exakt in mm, Raster mittig. */
class DocumentGeneratorLayoutTest {

    @TempDir
    Path tempDir;

    private List<ImageDto> cards(int count) throws Exception {
        File file = tempDir.resolve("card.png").toFile();
        ImageIO.write(new BufferedImage(64, 89, BufferedImage.TYPE_INT_RGB), "png", file);
        List<ImageDto> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ImageDto dto = new ImageDto();
            dto.setImageUrl(file.getAbsolutePath());
            cards.add(dto);
        }
        return cards;
    }

    private static PdfProperties realCards() {
        PdfProperties p = new PdfProperties();
        p.setWidth(63.5);
        p.setHeight(88.9);
        p.setMaxDpi(300);
        return p;
    }

    @Test
    void millimetresAreExactPoints() {
        assertEquals(72f, 25.4f * MM_TO_PT, 0.0001f);
    }

    @Test
    void nineRealCardsFitOnPortraitA4() {
        float w = PDRectangle.A4.getWidth();
        float h = PDRectangle.A4.getHeight();
        assertEquals(3, DocumentGenerator.cardsThatFit(w, 63.5f * MM_TO_PT, 0));
        assertEquals(3, DocumentGenerator.cardsThatFit(h, 88.9f * MM_TO_PT, 0));
        // (210 - 3 * 63,5) / 2 = 9,75 mm links und rechts
        assertEquals(9.75f * MM_TO_PT, DocumentGenerator.centeredOffset(w, 3, 63.5f * MM_TO_PT, 0), 0.05f);
    }

    @Test
    void gapsCountIntoTheCenteredGrid() {
        // 2 Karten zu 60 mm mit 5 mm Abstand auf 200 mm: Raster 125 mm, Rand je 37,5 mm
        assertEquals(37.5f * MM_TO_PT,
                DocumentGenerator.centeredOffset(200 * MM_TO_PT, 2, 60 * MM_TO_PT, 5 * MM_TO_PT), 0.01f);
    }

    @Test
    void nineRealCardsArePlacedCenteredInRealSize() throws Exception {
        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0));
        byte[] pdf = generator.generateDocument(cards(10), realCards(), "id", dto -> { });
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertEquals(2, doc.getNumberOfPages(), "9 Karten auf Seite 1, die zehnte auf Seite 2");

            PDFStreamParser parser = new PDFStreamParser(doc.getPage(0));
            parser.parse();
            List<Object> tokens = parser.getTokens();
            List<float[]> placements = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i) instanceof Operator op && op.getName().equals("cm")) {
                    float[] m = new float[6];
                    for (int k = 0; k < 6; k++) {
                        m[k] = ((COSNumber) tokens.get(i - 6 + k)).floatValue();
                    }
                    placements.add(m);
                }
            }
            assertEquals(9, placements.size());
            float[] first = placements.get(0);
            assertEquals(63.5f * MM_TO_PT, first[0], 0.01f, "Breite");
            assertEquals(88.9f * MM_TO_PT, first[3], 0.01f, "Höhe");
            assertEquals(9.75f * MM_TO_PT, first[4], 0.05f, "links mittig");
            // oben derselbe Rand wie unten: (297 - 3 * 88,9) / 2 = 15,15 mm
            float top = PDRectangle.A4.getHeight() - first[5] - first[3];
            assertEquals(15.15f * MM_TO_PT, top, 0.05f, "oben mittig");
        }
    }
}
