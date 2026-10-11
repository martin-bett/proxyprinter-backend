package my.poo.mtgcrawl.mtgprint.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;

/** Automatische Eckfarbe: je Ecke an den beiden Startpunkten des Bogens gemessen. */
class DocumentGeneratorCornerColorTest {

    @TempDir
    Path tempDir;

    /** Karte mit 6 px Rahmen: oben/unten {@code horizontal}, links/rechts {@code vertical} (in den Ecken), innen grau. */
    private static BufferedImage framed(int type, Color horizontal, Color vertical) {
        BufferedImage img = new BufferedImage(200, 280, type);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 200, 280);
        g.setColor(horizontal);
        g.fillRect(0, 0, 200, 6);
        g.fillRect(0, 274, 200, 6);
        g.setColor(vertical);
        g.fillRect(0, 0, 6, 280);
        g.fillRect(194, 0, 6, 280);
        g.dispose();
        return img;
    }

    @Test
    void measuresBothEdgesOfEachCorner() {
        BufferedImage img = framed(BufferedImage.TYPE_INT_RGB, Color.RED, Color.BLUE);
        // Radius 10 % der Breite: Messpunkt auf der Oberkante bei x ~ 24, auf der Seite bei y ~ 24 (im blauen Teil)
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            Color[] edge = DocumentGenerator.cornerEdgeColors(img, quadrant, 0.1f, 0.1f);
            assertEquals(Color.RED, edge[0], "waagerechte Kante, Ecke " + quadrant);
            assertEquals(Color.BLUE, edge[1], "senkrechte Kante, Ecke " + quadrant);
        }
    }

    @Test
    void transparentPixelsFallBackToAverageBorderColor() {
        BufferedImage img = new BufferedImage(200, 280, BufferedImage.TYPE_INT_ARGB);
        Color[] edge = DocumentGenerator.cornerEdgeColors(img, 0, 0.1f, 0.1f);
        assertEquals(DocumentGenerator.getBorderColor(img), edge[0]);
        assertEquals(DocumentGenerator.getBorderColor(img), edge[1]);
    }

    private List<String> operators(BufferedImage card) throws Exception {
        File file = tempDir.resolve("card.png").toFile();
        ImageIO.write(card, "png", file);
        ImageDto dto = new ImageDto();
        dto.setImageUrl(file.getAbsolutePath());
        PdfProperties p = new PdfProperties();
        p.setWidth(63.5);
        p.setHeight(88.9);
        p.setMaxDpi(300);
        p.setCorners(true);
        p.setCornerColorAuto(true);
        byte[] pdf = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0))
                .generateDocument(new ArrayList<>(List.of(dto)), p, "id", d -> { });
        try (PDDocument doc = PDDocument.load(pdf)) {
            PDFStreamParser parser = new PDFStreamParser(doc.getPage(0));
            parser.parse();
            List<String> ops = new ArrayList<>();
            for (Object token : parser.getTokens()) {
                if (token instanceof Operator op) {
                    ops.add(op.getName());
                }
            }
            return ops;
        }
    }

    @Test
    void differentEdgeColorsGetAGradientPerCorner() throws Exception {
        List<String> ops = operators(framed(BufferedImage.TYPE_INT_RGB, Color.RED, Color.BLUE));
        assertEquals(4, ops.stream().filter("sh"::equals).count(), "ein Verlauf je Ecke");
        assertEquals(4, ops.stream().filter("W"::equals).count(), "auf die Eckfläche beschnitten");
    }

    @Test
    void sameEdgeColorsFillPlainly() throws Exception {
        List<String> ops = operators(framed(BufferedImage.TYPE_INT_RGB, Color.BLACK, Color.BLACK));
        assertFalse(ops.contains("sh"));
        assertEquals(4, ops.stream().filter("f"::equals).count(), "vier einfarbige Ecken");
    }
}
