/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CropSettingsDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.service.pdf.BufferedImageCacheService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocumentGenerator {
    private static final Logger log = LoggerFactory.getLogger(DocumentGenerator.class);
    private final BufferedImageCacheService bufferedImageCacheService;
    public static final String TEMP_PATH = "tmp";
    private static final int OFFSET = 1;

    public DocumentGenerator(BufferedImageCacheService bufferedImageCacheService) {
        this.bufferedImageCacheService = bufferedImageCacheService;
        ImageIO.scanForPlugins();
    }

    public byte[] generateDocument(List<ImageDto> imagePaths, PdfProperties properties, String progressId, Consumer<ProgressDto> emitter) throws IOException {
        try (PDDocument document = new PDDocument();){
            PDRectangle pageSize = PDRectangle.A4;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float pageMargin = 28.3f;
            float gridMargin = pageMargin / 6.0f;
            float hMargin = (float)(properties.getHmargin() != null ? properties.getHmargin() : 0) * 2.83f;
            float vMargin = (float)(properties.getVmargin() != null ? properties.getVmargin() : 0) * 2.83f;
            float cardWidth = (float)properties.getWidth().intValue() * 2.83f;
            float cardHeight = (float)properties.getHeight().intValue() * 2.83f;
            int cardsPerRow = (int)((pageWidth - 2.0f * pageMargin + hMargin) / (cardWidth + hMargin));
            int cardsPerColumn = (int)((pageHeight - 2.0f * pageMargin + vMargin) / (cardHeight + vMargin));
            float startY = pageHeight - pageMargin - cardHeight;
            float startX = pageMargin;
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            int cardCount = 0;
            ArrayList<Float> verticalLines = new ArrayList<Float>();
            ArrayList<Float> horizontalLines = new ArrayList<Float>();
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            int totalCards = imagePaths.size();
            int cardsPerPage = cardsPerRow * cardsPerColumn;
            int pageCount = (int)Math.ceil((double)totalCards / (double)cardsPerPage);
            int globalCardIndex = 0;
            int accumulatedFileSize = 0;
            for (int pageNum = 0; pageNum < pageCount; ++pageNum) {
                startY = pageHeight - pageMargin - cardHeight;
                startX = pageMargin;
                cardCount = 0;
                verticalLines.clear();
                horizontalLines.clear();
                for (int cardOnPage = 0; cardOnPage < cardsPerPage && globalCardIndex < totalCards; ++cardOnPage, ++globalCardIndex) {
                    ImageDto imagePath = imagePaths.get(globalCardIndex);
                    int row = cardCount / cardsPerRow;
                    int col = cardCount % cardsPerRow;
                    float x = startX + (float)col * (cardWidth + hMargin);
                    float y = startY - (float)row * (cardHeight + vMargin);
                    PDImageXObject image = this.createImageForPDF(imagePath, document, properties);
                    contentStream.drawImage(image, x, y, cardWidth, cardHeight);
                    log.debug("added image" + imagePath.getImageUrl());
                    if (properties.isCorners()) {
                        Color c = properties.isCornerColorAuto() ? DocumentGenerator.getBorderColor(image.getImage()) : Color.decode(properties.getCornerColor());
                        this.drawCorners(contentStream, x, y, cardWidth, cardHeight, c);
                    }
                    verticalLines.add(Float.valueOf(x));
                    verticalLines.add(Float.valueOf(x + cardWidth));
                    horizontalLines.add(Float.valueOf(y));
                    horizontalLines.add(Float.valueOf(y + cardHeight));
                    try {
                        File f = new File(imagePath.getImageUrl());
                        if (f.exists()) {
                            accumulatedFileSize += (int)f.length();
                        }
                    }
                    catch (Exception f) {
                        // empty catch block
                    }
                    try {
                        ProgressDto progress = new ProgressDto();
                        progress.setCategory(ProgressDto.Category.build);
                        progress.setMessage("Placing card " + (globalCardIndex + 1) + " of " + totalCards);
                        progress.setCurrent(globalCardIndex + 1);
                        progress.setTotal(totalCards);
                        progress.setPageCount(pageNum + 1);
                        progress.setFileSize(accumulatedFileSize);
                        progress.setProgressId(progressId);
                        emitter.accept(progress);
                    }
                    catch (Exception e) {
                        log.warn("Could not send SSE event: " + e.getMessage());
                    }
                    ++cardCount;
                }
                if (properties.isCutlines()) {
                    this.drawGridLines(contentStream, verticalLines, horizontalLines, gridMargin, pageWidth, pageHeight, properties.getCutlineColor(), properties.getCutlineThickness());
                }
                contentStream.close();
                if (globalCardIndex >= totalCards) continue;
                page = new PDPage(pageSize);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            byte[] result = baos.toByteArray();
            log.debug("finished document " + result.length / 1000 + " KB");
            byte[] byArray = result;
            return byArray;
        }
    }

    public PDImageXObject createImageForPDF(ImageDto imagePath, PDDocument document, PdfProperties properties) throws IOException {
        CropSettingsDto cropSettings = imagePath.getCropSettings();
        BufferedImage bufferedImage = this.bufferedImageCacheService.getImage(imagePath.getImageUrl());
        if (bufferedImage == null) {
            try {
                bufferedImage = ImageIO.read(new File(imagePath.getImageUrl()));
                if (bufferedImage == null) {
                    throw new IOException("Bildformat wird nicht unterst\u00fctzt: " + String.valueOf(imagePath));
                }
                this.bufferedImageCacheService.addImage(imagePath.getImageUrl(), bufferedImage);
            }
            catch (Exception e) {
                throw new IOException("Fehler beim Laden des Bildes: " + e.getMessage());
            }
        }
        if (cropSettings != null) {
            bufferedImage = DocumentGenerator.cropImage(bufferedImage, cropSettings);
        }
        int maxDpi = properties.getMaxDpi() != null ? properties.getMaxDpi() : 300;
        float mmToInches = 25.4f;
        int maxWidthPx = (int)((float)(properties.getWidth() * maxDpi) / mmToInches);
        int maxHeightPx = (int)((float)(properties.getHeight() * maxDpi) / mmToInches);
        if (bufferedImage.getWidth() > maxWidthPx || bufferedImage.getHeight() > maxHeightPx) {
            bufferedImage = this.scaleImage(bufferedImage, maxWidthPx, maxHeightPx);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write((RenderedImage)bufferedImage, "png", baos);
        byte[] imageData = baos.toByteArray();
        return PDImageXObject.createFromByteArray(document, imageData, "image");
    }

    private BufferedImage scaleImage(BufferedImage originalImage, int maxWidth, int maxHeight) {
        int newWidth = originalImage.getWidth();
        int newHeight = originalImage.getHeight();
        if (originalImage.getWidth() > maxWidth) {
            newWidth = maxWidth;
            newHeight = originalImage.getHeight() * maxWidth / originalImage.getWidth();
        }
        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = originalImage.getWidth() * maxHeight / originalImage.getHeight();
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
        int cropWidth = cropSettings.getWidth() != null && cropSettings.getWidth() > 0 ? cropSettings.getWidth().intValue() : image.getWidth();
        int cropHeight = cropSettings.getHeight() != null && cropSettings.getHeight() > 0 ? cropSettings.getHeight().intValue() : image.getHeight();
        int x = Math.max(0, offsetX + padding);
        int y = Math.max(0, offsetY + padding);
        int width = Math.min(image.getWidth() - x, cropWidth - 2 * padding);
        int height = Math.min(image.getHeight() - y, cropHeight - 2 * padding);
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid crop dimensions: width=" + width + ", height=" + height);
        }
        return image.getSubimage(x, y, width, height);
    }

    public static Color getBorderColor(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        Color[] colors = new Color[]{new Color(img.getRGB(w / 2, 1)), new Color(img.getRGB(w / 2, h - 1 - 1)), new Color(img.getRGB(1, h / 2)), new Color(img.getRGB(w - 1 - 1, h / 2))};
        return DocumentGenerator.getAverageColor(colors);
    }

    private static Color getAverageColor(Color[] colors) {
        int redSum = 0;
        int greenSum = 0;
        int blueSum = 0;
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

    private void drawGridLines(PDPageContentStream contentStream, List<Float> verticalLines, List<Float> horizontalLines, float gridMargin, float pageWidth, float pageHeight, String colorHex, Integer thickness) throws IOException {
        Color color = Color.decode(colorHex);
        contentStream.setStrokingColor(color);
        contentStream.setLineWidth((float)(thickness != null ? thickness : 3) * 0.5f);
        verticalLines = new ArrayList<Float>(new TreeSet<Float>(verticalLines));
        horizontalLines = new ArrayList<Float>(new TreeSet<Float>(horizontalLines));
        verticalLines.add(0, Float.valueOf(gridMargin));
        verticalLines.add(Float.valueOf(pageWidth - gridMargin));
        horizontalLines.add(0, Float.valueOf(gridMargin));
        horizontalLines.add(Float.valueOf(pageHeight - gridMargin));
        for (float x : verticalLines) {
            contentStream.moveTo(x, gridMargin);
            contentStream.lineTo(x, pageHeight - gridMargin);
            contentStream.stroke();
        }
        for (float y : horizontalLines) {
            contentStream.moveTo(gridMargin, y);
            contentStream.lineTo(pageWidth - gridMargin, y);
            contentStream.stroke();
        }
    }

    private void drawCorners(PDPageContentStream contentStream, float x, float y, float width, float height, Color color) throws IOException {
        contentStream.setNonStrokingColor(color);
        float cornerRadius = height * 0.03f;
        this.drawRoundedCorner(contentStream, x, y + height, cornerRadius, 0);
        this.drawRoundedCorner(contentStream, x + width, y + height, cornerRadius, 1);
        this.drawRoundedCorner(contentStream, x, y, cornerRadius, 2);
        this.drawRoundedCorner(contentStream, x + width, y, cornerRadius, 3);
    }

    private void drawRoundedCorner(PDPageContentStream contentStream, float cornerX, float cornerY, float radius, int quadrant) throws IOException {
        float startAngle;
        float y2;
        float x1;
        boolean bottomSide;
        boolean leftSide;
        float y1 = cornerY;
        float x2 = cornerX;
        float endAngle = switch (quadrant) {
            case 0 -> {
                leftSide = true;
                bottomSide = false;
                x1 = cornerX + radius;
                y2 = cornerY - radius;
                startAngle = 90.0f;
                yield 180.0f;
            }
            case 1 -> {
                bottomSide = false;
                leftSide = false;
                x1 = cornerX - radius;
                y2 = cornerY - radius;
                startAngle = 0.0f;
                yield 90.0f;
            }
            case 2 -> {
                bottomSide = true;
                leftSide = true;
                x1 = cornerX + radius;
                y2 = cornerY + radius;
                startAngle = 180.0f;
                yield 270.0f;
            }
            case 3 -> {
                leftSide = false;
                bottomSide = true;
                x1 = cornerX - radius;
                y2 = cornerY + radius;
                startAngle = 270.0f;
                yield 360.0f;
            }
            default -> throw new IllegalArgumentException("Invalid quadrant");
        };
        contentStream.moveTo(cornerX, cornerY);
        contentStream.lineTo(x1, y1);
        contentStream.curveTo((float)((double)(cornerX + (float)(leftSide ? 1 : -1) * radius) + (double)radius * Math.cos(Math.toRadians(startAngle + 35.0f))), (float)((double)(cornerY + (float)(bottomSide ? 1 : -1) * radius) + (double)radius * Math.sin(Math.toRadians(startAngle + 35.0f))), (float)((double)(cornerX + (float)(leftSide ? 1 : -1) * radius) + (double)radius * Math.cos(Math.toRadians(endAngle - 35.0f))), (float)((double)(cornerY + (float)(bottomSide ? 1 : -1) * radius) + (double)radius * Math.sin(Math.toRadians(endAngle - 35.0f))), x2, y2);
        contentStream.closePath();
        contentStream.fill();
    }
}

