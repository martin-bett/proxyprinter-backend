package my.poo.mtgcrawl.mtgprint.service.pdf;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CropSettingsDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

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
    private final PdfSizeLimit pdfSizeLimit;

    public DocumentGenerator(BufferedImageCacheService bufferedImageCacheService, PdfSizeLimit pdfSizeLimit) {
        this.bufferedImageCacheService = bufferedImageCacheService;
        this.pdfSizeLimit = pdfSizeLimit;
        ImageIO.scanForPlugins();
    }

    public static final String TEMP_PATH = "tmp";

    /** Millimeter in PDF-Punkt (1 pt = 1/72 Zoll), exakt, damit Karten in Originalgröße gedruckt werden. */
    static final float MM_TO_PT = 72f / 25.4f;
    /** Mindestrand zwischen Kartenraster und Seitenkante. */
    static final float MIN_PAGE_MARGIN_MM = 5f;
    /** Abstand der Schnittlinien-Enden zur Seitenkante. */
    static final float GRID_MARGIN_MM = 10f / 6;

    /** Wie viele Karten nebeneinander (bzw. untereinander) passen, mit Mindestrand auf beiden Seiten. */
    static int cardsThatFit(float pageSize, float cardSize, float gap) {
        if (cardSize <= 0) {
            return 0;
        }
        return Math.max(0, (int) ((pageSize - 2 * MIN_PAGE_MARGIN_MM * MM_TO_PT + gap) / (cardSize + gap)));
    }

    /** Abstand vom Seitenrand, damit das Raster aus {@code count} Karten mittig sitzt. */
    static float centeredOffset(float pageSize, int count, float cardSize, float gap) {
        float gridSize = count * cardSize + Math.max(0, count - 1) * gap;
        return Math.max(0, (pageSize - gridSize) / 2);
    }

    public byte[] generateDocument(List<ImageDto> imagePaths, PdfProperties properties, String progressId,
            Consumer<ProgressDto> progress) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDRectangle pageSize = PDRectangle.A4;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();

            float gridMargin = GRID_MARGIN_MM * MM_TO_PT; // Rand der Schnittlinien zum Seitenrand
            float hMargin = (float) properties.getHmargin() * MM_TO_PT;
            float vMargin = (float) properties.getVmargin() * MM_TO_PT;

            float cardWidth = (float) properties.getWidth() * MM_TO_PT;
            float cardHeight = (float) properties.getHeight() * MM_TO_PT;

            int cardsPerRow = cardsThatFit(pageWidth, cardWidth, hMargin);
            int cardsPerColumn = cardsThatFit(pageHeight, cardHeight, vMargin);

            // Raster mittig auf der Seite, damit 3 x 3 echte Karten (63,5 mm) auf A4 passen
            float startX = centeredOffset(pageWidth, cardsPerRow, cardWidth, hMargin);
            float startY = pageHeight - centeredOffset(pageHeight, cardsPerColumn, cardHeight, vMargin) - cardHeight;

            PDPage page = new PDPage(pageSize);
            document.addPage(page);

            int cardCount = 0;

            List<Float> verticalLines = new ArrayList<>();
            List<Float> horizontalLines = new ArrayList<>();

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            int totalCards = imagePaths.size();
            int placedCards = 0;
            int pageCount = 1;
            long accumulatedFileSize = 0;
            long embeddedImageBytes = 0;

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
                    pageCount++;
                    cardCount = 0;
                    verticalLines.clear();
                    horizontalLines.clear();
                }

                int row = cardCount / cardsPerRow;
                int col = cardCount % cardsPerRow;

                float x = startX + col * (cardWidth + hMargin);
                float y = startY - row * (cardHeight + vMargin);

                PDImageXObject image = createImageForPDF(imagePath, document, properties);
                // Bilder machen fast die ganze PDF-Größe aus, jede Kopie wird einzeln eingebettet
                embeddedImageBytes += embeddedSize(image);
                pdfSizeLimit.check(embeddedImageBytes, placedCards + 1, totalCards);

                contentStream.drawImage(image, x, y, cardWidth, cardHeight);
                log.debug("added image"+ imagePath.getImageUrl());
                if (properties.isCorners()) {
                    float radius = cornerRadius(properties, cardWidth, cardHeight);
                    if (properties.isCornerColorAuto()) {
                        drawAutoColoredCorners(contentStream, image.getImage(), x, y, radius, cardWidth, cardHeight);
                    } else {
                        drawCorners(contentStream, x, y, radius, cardWidth, cardHeight,
                                Color.decode(properties.getCornerColor()));
                    }
                }

                // Add grid lines for this card
                verticalLines.add(x);
                verticalLines.add(x + cardWidth);
                horizontalLines.add(y);
                horizontalLines.add(y + cardHeight);

                cardCount++;
                placedCards++;
                File imageFile = new File(imagePath.getImageUrl());
                if (imageFile.exists()) {
                    accumulatedFileSize += imageFile.length();
                }
                ProgressDto dto = ProgressDto.of(progressId, ProgressDto.Category.build,
                        "Placing card " + placedCards + " of " + totalCards, placedCards, totalCards);
                dto.setPageCount(pageCount);
                dto.setFileSize(accumulatedFileSize);
                progress.accept(dto);
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

    private static long embeddedSize(PDImageXObject image) throws IOException {
        long size = image.getCOSObject().getLength();
        PDImageXObject softMask = image.getSoftMask();
        if (softMask != null) {
            size += softMask.getCOSObject().getLength();
        }
        return size;
    }

    public PDImageXObject createImageForPDF(ImageDto imagePath, PDDocument document, PdfProperties properties) throws IOException {
        CropSettingsDto cropSettings = imagePath.getCropSettings();
        BufferedImage bufferedImage;
    
        // Überprüfe den Cache nach dem Bild
        bufferedImage = bufferedImageCacheService.getImage(imagePath.getImageUrl());
        if (bufferedImage == null) {
            File file = new File(imagePath.getImageUrl());
            try {
                bufferedImage = ImageDecoder.read(file);
            } catch (Exception e) {
                String source = imagePath.getSourceUrl() != null ? imagePath.getSourceUrl() : imagePath.getImageUrl();
                log.error("Bild {} ({}) lässt sich nicht lesen", source, file, e);
                // Kaputter Download: löschen, damit der nächste Versuch neu lädt statt wieder an derselben Datei zu scheitern
                if (imagePath.getSourceUrl() != null && file.delete()) {
                    log.info("Fehlerhafte Datei {} aus dem Download-Cache gelöscht", file);
                }
                throw new IOException("Fehler beim Laden des Bildes " + source + ": " + e.getMessage(), e);
            }
            bufferedImageCacheService.addImage(imagePath.getImageUrl(), bufferedImage);
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
    
        // getType() ist bei manchen Formaten TYPE_CUSTOM (0), das kann man nicht neu anlegen
        int type = originalImage.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, type);
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

    /** Echte Karten: Eckradius 1/8 Zoll (3,175 mm) bei 88,9 mm Höhe; der automatische Radius wächst mit der Kartenhöhe. */
    static final float AUTO_CORNER_RATIO = 3.175f / 88.9f;

    /**
     * Radius der abgerundeten Ecke in Punkt. Ohne cornerSize automatisch (AUTO_CORNER_RATIO der Kartenhöhe);
     * sonst der Wert in mm, höchstens die halbe kürzere Kartenseite.
     */
    static float cornerRadius(PdfProperties properties, float cardWidth, float cardHeight) {
        Double size = properties.getCornerSize();
        if (size == null || size < 0) {
            return cardHeight * AUTO_CORNER_RATIO;
        }
        return Math.min(size.floatValue() * MM_TO_PT, Math.min(cardWidth, cardHeight) / 2);
    }

    /**
     * Automatische Eckfarbe je Ecke: gemessen wird dort, wo der Viertelkreis auf die beiden Kartenkanten trifft.
     * Sind beide Farben gleich, wird die Ecke einfarbig gefüllt, sonst mit einem Verlauf von der einen Kante zur
     * anderen, damit sie an beiden Seiten nahtlos an den Kartenrand anschließt.
     */
    private void drawAutoColoredCorners(PDPageContentStream contentStream, BufferedImage img, float x, float y,
            float radius, float width, float height) throws IOException {
        float[][] corners = { { x, y + height }, { x + width, y + height }, { x, y }, { x + width, y } };
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            Color[] edge = cornerEdgeColors(img, quadrant, radius / width, radius / height);
            drawRoundedCorner(contentStream, corners[quadrant][0], corners[quadrant][1], radius, quadrant,
                    edge[0], edge[1]);
        }
    }

    /**
     * Farben an den beiden Startpunkten des Eckbogens: [0] auf der waagerechten Kante (oben bzw. unten),
     * [1] auf der senkrechten Kante (links bzw. rechts). Gemessen knapp hinter dem Startpunkt und etwas
     * innerhalb der Karte, gemittelt über 3 x 3 Pixel; durchsichtige Pixel zählen nicht. Ohne Messwert gilt
     * die alte Durchschnittsfarbe der Randmitten.
     */
    static Color[] cornerEdgeColors(BufferedImage img, int quadrant, float radiusOfWidth, float radiusOfHeight) {
        int w = img.getWidth();
        int h = img.getHeight();
        int inset = OFFSET + 1;
        int along = 1 + inset;
        int rx = Math.min(w - 1 - inset, Math.round(radiusOfWidth * w) + along);
        int ry = Math.min(h - 1 - inset, Math.round(radiusOfHeight * h) + along);
        boolean left = quadrant == 0 || quadrant == 2;
        boolean top = quadrant == 0 || quadrant == 1;
        int edgeX = left ? inset : w - 1 - inset;
        int edgeY = top ? inset : h - 1 - inset;
        int alongX = left ? rx : w - 1 - rx;
        int alongY = top ? ry : h - 1 - ry;

        Color horizontal = sampleColor(img, alongX, edgeY);
        Color vertical = sampleColor(img, edgeX, alongY);
        if (horizontal == null || vertical == null) {
            Color fallback = getBorderColor(img);
            horizontal = horizontal != null ? horizontal : fallback;
            vertical = vertical != null ? vertical : fallback;
        }
        return new Color[] { horizontal, vertical };
    }

    /** Mittelwert über 3 x 3 Pixel um (x, y), ohne (fast) durchsichtige Pixel; null, wenn keiner übrig bleibt. */
    static Color sampleColor(BufferedImage img, int x, int y) {
        int r = 0, g = 0, b = 0, n = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int px = Math.max(0, Math.min(img.getWidth() - 1, x + dx));
                int py = Math.max(0, Math.min(img.getHeight() - 1, y + dy));
                int argb = img.getRGB(px, py);
                if (img.getColorModel().hasAlpha() && (argb >>> 24) < 128) {
                    continue;
                }
                r += (argb >> 16) & 0xFF;
                g += (argb >> 8) & 0xFF;
                b += argb & 0xFF;
                n++;
            }
        }
        return n == 0 ? null : new Color(r / n, g / n, b / n);
    }

    private void drawCorners(PDPageContentStream contentStream, float x, float y, float cornerRadius, float width,
            float height, Color color) throws IOException {

        contentStream.setNonStrokingColor(color);

        // Top-left corner
        drawRoundedCorner(contentStream, x, y + height, cornerRadius, 0);

        // Top-right corner
        drawRoundedCorner(contentStream, x + width, y + height, cornerRadius, 1);

        // Bottom-left corner
        drawRoundedCorner(contentStream, x, y, cornerRadius, 2);

        // Bottom-right corner
        drawRoundedCorner(contentStream, x + width, y, cornerRadius, 3);
    }

    /** Bézier-Faktor für einen Viertelkreis: Kontrollpunkte liegen KAPPA * r auf den Tangenten. */
    static final float KAPPA = 0.5523f;

    /**
     * Füllt die Fläche zwischen Kartenecke und Viertelkreis mit Radius {@code radius}: Kartenecke, entlang der
     * waagerechten Kante, Viertelkreis bis zur senkrechten Kante, zurück zur Ecke. Alle vier Ecken nutzen dieselbe
     * Konstruktion, nur gespiegelt. Früher lagen die Kontrollpunkte bei oben rechts und unten links in vertauschter
     * Reihenfolge, deshalb waren diese Ecken flacher als die anderen beiden.
     */
    static void drawRoundedCorner(PDPageContentStream contentStream, float cornerX, float cornerY, float radius,
            int quadrant) throws IOException {
        // dx/dy zeigen von der Ecke ins Kartenfeld (PDF-Koordinaten: y wächst nach oben)
        float dx, dy;
        switch (quadrant) {
            case 0: // Top-left
                dx = 1;
                dy = -1;
                break;
            case 1: // Top-right
                dx = -1;
                dy = -1;
                break;
            case 2: // Bottom-left
                dx = 1;
                dy = 1;
                break;
            case 3: // Bottom-right
                dx = -1;
                dy = 1;
                break;
            default:
                throw new IllegalArgumentException("Invalid quadrant");
        }
        cornerPath(contentStream, cornerX, cornerY, radius, dx, dy);
        contentStream.fill();
    }

    /**
     * Wie {@link #drawRoundedCorner(PDPageContentStream, float, float, float, int)}, aber mit den Farben der
     * beiden angrenzenden Kartenkanten: gleich (bis auf Rundung) = einfarbig, sonst ein Verlauf von der
     * waagerechten zur senkrechten Kante, auf die Eckfläche beschnitten.
     */
    static void drawRoundedCorner(PDPageContentStream contentStream, float cornerX, float cornerY, float radius,
            int quadrant, Color horizontalEdge, Color verticalEdge) throws IOException {
        if (colorDistance(horizontalEdge, verticalEdge) <= 6) {
            contentStream.setNonStrokingColor(mix(horizontalEdge, verticalEdge));
            drawRoundedCorner(contentStream, cornerX, cornerY, radius, quadrant);
            return;
        }
        float dx = quadrant == 0 || quadrant == 2 ? 1 : -1;
        float dy = quadrant == 0 || quadrant == 1 ? -1 : 1;

        COSDictionary function = new COSDictionary();
        function.setInt(COSName.FUNCTION_TYPE, 2);
        function.setItem(COSName.DOMAIN, floats(0, 1));
        function.setItem(COSName.C0, floats(horizontalEdge.getRGBColorComponents(null)));
        function.setItem(COSName.C1, floats(verticalEdge.getRGBColorComponents(null)));
        function.setInt(COSName.N, 1);

        PDShadingType2 shading = new PDShadingType2(new COSDictionary());
        shading.setShadingType(PDShading.SHADING_TYPE2);
        shading.setColorSpace(PDDeviceRGB.INSTANCE);
        shading.setCoords(floats(cornerX + dx * radius, cornerY, cornerX, cornerY + dy * radius));
        shading.setFunction(new PDFunctionType2(function));
        COSArray extend = new COSArray();
        extend.add(COSBoolean.TRUE);
        extend.add(COSBoolean.TRUE);
        shading.setExtend(extend);

        contentStream.saveGraphicsState();
        cornerPath(contentStream, cornerX, cornerY, radius, dx, dy);
        contentStream.clip();
        contentStream.shadingFill(shading);
        contentStream.restoreGraphicsState();
    }

    /** Eckfläche: Kartenecke, entlang der waagerechten Kante, Viertelkreis bis zur senkrechten Kante, zurück. */
    private static void cornerPath(PDPageContentStream contentStream, float cornerX, float cornerY, float radius,
            float dx, float dy) throws IOException {
        float handle = radius * (1 - KAPPA);
        contentStream.moveTo(cornerX, cornerY);
        contentStream.lineTo(cornerX + dx * radius, cornerY);
        contentStream.curveTo(
                cornerX + dx * handle, cornerY,
                cornerX, cornerY + dy * handle,
                cornerX, cornerY + dy * radius);
        contentStream.closePath();
    }

    static int colorDistance(Color a, Color b) {
        return Math.max(Math.abs(a.getRed() - b.getRed()),
                Math.max(Math.abs(a.getGreen() - b.getGreen()), Math.abs(a.getBlue() - b.getBlue())));
    }

    static Color mix(Color a, Color b) {
        return new Color((a.getRed() + b.getRed()) / 2, (a.getGreen() + b.getGreen()) / 2, (a.getBlue() + b.getBlue()) / 2);
    }

    private static COSArray floats(float... values) {
        COSArray array = new COSArray();
        for (float v : values) {
            array.add(new COSFloat(v));
        }
        return array;
    }
}
