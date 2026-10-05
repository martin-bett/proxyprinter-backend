package my.poo.mtgcrawl.mtgprint.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;

class ImageDecoderTest {

    @TempDir
    Path tempDir;

    private static PdfProperties properties() {
        PdfProperties p = new PdfProperties();
        p.setWidth(63);
        p.setHeight(88);
        p.setMaxDpi(300);
        return p;
    }

    @Test
    void readsNormalPng() throws Exception {
        BufferedImage img = new BufferedImage(20, 30, BufferedImage.TYPE_INT_RGB);
        img.setRGB(5, 5, 0x123456);
        File file = tempDir.resolve("card.png").toFile();
        ImageIO.write(img, "png", file);

        BufferedImage read = ImageDecoder.read(file);
        assertEquals(20, read.getWidth());
        assertEquals(0x123456, read.getRGB(5, 5) & 0xFFFFFF);
    }

    @Test
    void buildsImageFromRaster() {
        WritableRaster rgba = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 2, 1, 4, null);
        rgba.setPixel(1, 0, new int[] { 10, 20, 30, 128 });
        BufferedImage image = ImageDecoder.fromRaster(rgba);
        assertEquals(0x800A141E, image.getRGB(1, 0));

        WritableRaster gray = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 1, 1, 1, null);
        gray.setSample(0, 0, 0, 200);
        assertEquals(0xFFC8C8C8, ImageDecoder.fromRaster(gray).getRGB(0, 0));
    }

    @Test
    void brokenDownloadNamesUrlAndIsRemovedFromCache() throws Exception {
        File file = tempDir.resolve("abc.png").toFile();
        Files.write(file.toPath(), new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R' });
        ImageDto dto = new ImageDto();
        dto.setImageUrl(file.getPath());
        dto.setSourceUrl("https://example.org/card.png");

        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0));
        IOException e = assertThrows(IOException.class,
                () -> generator.generateDocument(List.of(dto), properties(), "id", p -> { }));
        assertTrue(e.getMessage().contains("https://example.org/card.png"), e.getMessage());
        assertFalse(file.exists(), "kaputter Download bleibt sonst für immer im Cache");
    }

    @Test
    void scalesImagesWithCustomType() throws Exception {
        // Grau mit Alpha hat keinen Standard-BufferedImage-Typ (TYPE_CUSTOM)
        ComponentColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), true, false,
                ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        BufferedImage img = new BufferedImage(cm, cm.createCompatibleWritableRaster(1500, 2000), false, null);
        assertEquals(BufferedImage.TYPE_CUSTOM, img.getType());
        File file = tempDir.resolve("gray-alpha.png").toFile();
        ImageIO.write(img, "png", file);
        ImageDto dto = new ImageDto();
        dto.setImageUrl(file.getPath());

        DocumentGenerator generator = new DocumentGenerator(new BufferedImageCacheService(), new PdfSizeLimit(0));
        try (PDDocument document = new PDDocument()) {
            PDImageXObject image = generator.createImageForPDF(dto, document, properties());
            assertTrue(image.getWidth() <= 745, "auf maxDpi verkleinert: " + image.getWidth());
        }
    }
}
