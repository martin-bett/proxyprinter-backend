package my.poo.mtgcrawl.mtgprint.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CropSettingsDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class DocumentGenerator {

    private final BufferedImageCacheService bufferedImageCacheService;

    public DocumentGenerator(BufferedImageCacheService bufferedImageCacheService) {
        this.bufferedImageCacheService = bufferedImageCacheService;
        ImageIO.scanForPlugins();
    }

    public static final String TEMP_PATH = "tmp";

    public byte[] generateDocument(List<ImageDto> imagePaths, PdfProperties properties) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            float pageMargin = 10 * 2.83f; // 10mm Seitenrand
            float gridMargin = pageMargin / 6; // Verringerter Rand für Gridlines
            float hMargin = (properties.getHmargin() != null ? properties.getHmargin() : 0) * 2.83f;
            float vMargin = (properties.getVmargin() != null ? properties.getVmargin() : 0) * 2.83f;

            float cardWidth = properties.getWidth() * 2.83f;
            float cardHeight = properties.getHeight() * 2.83f;

            int cardsPerRow = (int) ((pageWidth - 2 * pageMargin + hMargin) / (cardWidth + hMargin));
            int cardsPerColumn = (int) ((pageHeight - 2 * pageMargin + vMargin) / (cardHeight + vMargin));

            float startY = pageHeight - pageMargin - cardHeight;
            float startX = pageMargin;

            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            int cardCount = 0;

            List<Float> verticalLines = new ArrayList<>();
            List<Float> horizontalLines = new ArrayList<>();

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            for (ImageDto imagePath : imagePaths) {
                if (cardCount >= cardsPerRow * cardsPerColumn) {
                    if (properties.isCutlines()) {
                        drawGridLines(contentStream, verticalLines, horizontalLines, gridMargin, pageWidth, pageHeight,
                                properties.getCutlineColor(), properties.getCutlineThickness());
                    }
                    contentStream.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    startY = pageHeight - pageMargin - cardHeight;
                    startX = pageMargin;
                    cardCount = 0;
                    verticalLines.clear();
                    horizontalLines.clear();
                }

                int row = cardCount / cardsPerRow;
                int col = cardCount % cardsPerRow;

                float x = startX + col * (cardWidth + hMargin);
                float y = startY - row * (cardHeight + vMargin);

                PDImageXObject image = createImageForPDF(imagePath, document, properties);

                contentStream.drawImage(image, x, y, cardWidth, cardHeight);
                log.debug("added image"+ imagePath.getImageUrl());
                if (properties.isCorners()) {
                    Color c;
                    if (properties.isCornerColorAuto()) {
                        c = getBorderColor(image.getImage());
                    } else {
                        c = Color.decode(properties.getCornerColor());
                    }
                    drawCorners(contentStream, x, y, cardWidth, cardHeight, c);
                }

                // Add grid lines for this card
                verticalLines.add(x);
                verticalLines.add(x + cardWidth);
                horizontalLines.add(y);
                horizontalLines.add(y + cardHeight);

                cardCount++;
            }

            if (properties.isCutlines()) {
                drawGridLines(contentStream, verticalLines, horizontalLines, gridMargin, pageWidth, pageHeight,
                        properties.getCutlineColor(), properties.getCutlineThickness());
            }
            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            byte[] result = baos.toByteArray();
            log.debug("finished document "+ result.length/1000 +" KB");
            return result;
        }
    }

    public PDImageXObject createImageForPDF(ImageDto imagePath, PDDocument document, PdfProperties properties) throws IOException {
        CropSettingsDto cropSettings = imagePath.getCropSettings();
        BufferedImage bufferedImage;
    
        // Überprüfe den Cache nach dem Bild
        bufferedImage = bufferedImageCacheService.getImage(imagePath.getImageUrl());
        if (bufferedImage == null) {
            try {
                bufferedImage = ImageIO.read(new File(imagePath.getImageUrl()));
                if (bufferedImage == null) {
                    throw new IOException("Bildformat wird nicht unterstützt: " + imagePath);
                }
                bufferedImageCacheService.addImage(imagePath.getImageUrl(), bufferedImage);
            } catch (Exception e) {
                throw new IOException("Fehler beim Laden des Bildes: " + e.getMessage());
            }
        }
    
        // Zuschneiden des Bildes, wenn CropSettings angegeben sind
        if (cropSettings != null) {
            bufferedImage = cropImage(bufferedImage, cropSettings);
        }
    
        // Berechnung der maximalen Dimensionen basierend auf PdfProperties
        int maxDpi = properties.getMaxDpi() != null ? properties.getMaxDpi() : 300; // Maximalauflösung
        float mmToInches = 25.4f;
        int maxWidthPx = (int) (properties.getWidth() * maxDpi / mmToInches);
        int maxHeightPx = (int) (properties.getHeight() * maxDpi / mmToInches);
    
        // Skalierung des Bildes, falls erforderlich
        if (bufferedImage.getWidth() > maxWidthPx || bufferedImage.getHeight() > maxHeightPx) {
            bufferedImage = scaleImage(bufferedImage, maxWidthPx, maxHeightPx);
        }
    
        // Konvertiere BufferedImage zu byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        byte[] imageData = baos.toByteArray();
    
        // Erstelle PDImageXObject aus dem byte array
        return PDImageXObject.createFromByteArray(document, imageData, "image");
    }
    
    
    private BufferedImage scaleImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int newWidth = originalImage.getWidth();
        int newHeight = originalImage.getHeight();
    
        if (originalImage.getWidth() > maxWidth) {
            newWidth = maxWidth;
            newHeight = (originalImage.getHeight() * maxWidth) / originalImage.getWidth();
        }
    
        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = (originalImage.getWidth() * maxHeight) / originalImage.getHeight();
        }
    
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaledImage;
    }
    

    private static BufferedImage cropImage(BufferedImage image, CropSettingsDto cropSettings) {
        int padding = cropSettings.getPadding() != null ? cropSettings.getPadding() : 0;
        int offsetX = cropSettings.getOffSetX() != null ? cropSettings.getOffSetX() : 0;
        int offsetY = cropSettings.getOffSetY() != null ? cropSettings.getOffSetY() : 0;
        int cropWidth = cropSettings.getWidth() != null && cropSettings.getWidth() > 0? cropSettings.getWidth() : image.getWidth();
        int cropHeight = cropSettings.getHeight() != null && cropSettings.getHeight() > 0 ? cropSettings.getHeight() : image.getHeight();

        // Berechnung der Zuschnitte unter Berücksichtigung der Bildgrenzen
        int x = Math.max(0, offsetX + padding);
        int y = Math.max(0, offsetY + padding);
        int width = Math.min(image.getWidth() - x, cropWidth - 2 * padding);
        int height = Math.min(image.getHeight() - y, cropHeight - 2 * padding);

        // Sicherstellen, dass die Dimensionen gültig sind
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid crop dimensions: width=" + width + ", height=" + height);
        }

        // Zuschneiden und zurückgeben
        return image.getSubimage(x, y, width, height);
    }

    private static final int OFFSET = 1; // Pixel-Offset vom Rand

    public static Color getBorderColor(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // Farben an den vier Seiten sammeln
        Color[] colors = new Color[4];
        colors[0] = new Color(img.getRGB(w / 2, OFFSET)); // Oben
        colors[1] = new Color(img.getRGB(w / 2, h - 1 - OFFSET)); // Unten
        colors[2] = new Color(img.getRGB(OFFSET, h / 2)); // Links
        colors[3] = new Color(img.getRGB(w - 1 - OFFSET, h / 2)); // Rechts

        return getAverageColor(colors);
    }

    private static Color getAverageColor(Color[] colors) {
        int redSum = 0, greenSum = 0, blueSum = 0;

        for (Color color : colors) {
            redSum += color.getRed();
            greenSum += color.getGreen();
            blueSum += color.getBlue();
        }

        int redAvg = redSum / colors.length;
        int greenAvg = greenSum / colors.length;
        int blueAvg = blueSum / colors.length;

        return new Color(redAvg, greenAvg, blueAvg);
    }

    private void drawGridLines(PDPageContentStream contentStream, List<Float> verticalLines,
            List<Float> horizontalLines, float gridMargin, float pageWidth, float pageHeight, String colorHex,
            Integer thickness) throws IOException {
        Color color = Color.decode(colorHex);
        contentStream.setStrokingColor(color);
        contentStream.setLineWidth(((thickness != null ? thickness : 3) * 0.5f));

        // Sort and remove duplicates
        verticalLines = new ArrayList<>(new TreeSet<>(verticalLines));
        horizontalLines = new ArrayList<>(new TreeSet<>(horizontalLines));

        // Extend grid lines to page edges
        verticalLines.add(0, gridMargin);
        verticalLines.add(pageWidth - gridMargin);
        horizontalLines.add(0, gridMargin);
        horizontalLines.add(pageHeight - gridMargin);

        // Draw vertical lines
        for (float x : verticalLines) {
            contentStream.moveTo(x, gridMargin);
            contentStream.lineTo(x, pageHeight - gridMargin);
            contentStream.stroke();
        }

        // Draw horizontal lines
        for (float y : horizontalLines) {
            contentStream.moveTo(gridMargin, y);
            contentStream.lineTo(pageWidth - gridMargin, y);
            contentStream.stroke();
        }
    }

    private void drawCorners(PDPageContentStream contentStream, float x, float y, float width, float height,
            Color color) throws IOException {

        contentStream.setNonStrokingColor(color);

        float cornerRadius = height * 0.03f; // Radius der abgerundeten Ecke

        // Top-left corner
        drawRoundedCorner(contentStream, x, y + height, cornerRadius, 0);

        // Top-right corner
        drawRoundedCorner(contentStream, x + width, y + height, cornerRadius, 1);

        // Bottom-left corner
        drawRoundedCorner(contentStream, x, y, cornerRadius, 2);

        // Bottom-right corner
        drawRoundedCorner(contentStream, x + width, y, cornerRadius, 3);
    }

    private void drawRoundedCorner(PDPageContentStream contentStream, float cornerX, float cornerY, float radius,
            int quadrant) throws IOException {
        float startAngle, endAngle;
        boolean leftSide, bottomSide;
        float x1, x2, y1, y2;
        y1 = cornerY;
        x2 = cornerX;
        switch (quadrant) {
            case 0: // Top-left
                leftSide = true;
                bottomSide = false;
                x1 = cornerX + radius;
                y2 = cornerY - radius;
                startAngle = 90;
                endAngle = 180;
                break;
            case 1: // Top-right
                bottomSide = false;
                leftSide = false;
                x1 = cornerX - radius;
                y2 = cornerY - radius;
                startAngle = 00;
                endAngle = 90;
                break;
            case 2: // Bottom-left
                bottomSide = true;
                leftSide = true;
                x1 = cornerX + radius;
                y2 = cornerY + radius;
                startAngle = 180;
                endAngle = 270;
                break;
            case 3: // Bottom-right
                leftSide = false;
                bottomSide = true;
                x1 = cornerX - radius;
                y2 = cornerY + radius;
                startAngle = 270;
                endAngle = 360;
                break;
            default:
                throw new IllegalArgumentException("Invalid quadrant");
        }

        contentStream.moveTo(cornerX, cornerY);
        contentStream.lineTo(x1, y1);
        contentStream.curveTo(
                (float) (cornerX + (leftSide ? 1 : -1) * radius + radius * Math.cos(Math.toRadians(startAngle + 35))),
                (float) (cornerY + (bottomSide ? 1 : -1) * radius + radius * Math.sin(Math.toRadians(startAngle + 35))),
                (float) (cornerX + (leftSide ? 1 : -1) * radius + radius * Math.cos(Math.toRadians(endAngle - 35))),
                (float) (cornerY + (bottomSide ? 1 : -1) * radius + radius * Math.sin(Math.toRadians(endAngle - 35))),
                x2, y2);
        contentStream.closePath();
        contentStream.fill();
    }
}
