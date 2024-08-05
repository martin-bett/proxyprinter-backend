package my.poo.mtgcrawl.mtgprint.service.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import my.poo.mtgcrawl.mtgprint.entitiy.PdfProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class DocumentGenerator {

    public static final String TEMP_PATH = "tmp";

    public byte[] generateDocument(List<String> imagePaths, PdfProperties properties) throws IOException {
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

            for (String imagePath : imagePaths) {
                if (cardCount >= cardsPerRow * cardsPerColumn) {
                    if (properties.isCutlines()) {
                        drawGridLines(contentStream, verticalLines, horizontalLines, gridMargin, pageWidth, pageHeight, properties.getCutlineColor());
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

                PDImageXObject image = PDImageXObject.createFromFile(imagePath, document);
                contentStream.drawImage(image, x, y, cardWidth, cardHeight);

                if (properties.isCorners()) {
                    drawCorners(contentStream, x, y, cardWidth, cardHeight, properties.getCornerColor());
                }

                // Add grid lines for this card
                verticalLines.add(x);
                verticalLines.add(x + cardWidth);
                horizontalLines.add(y);
                horizontalLines.add(y + cardHeight);

                cardCount++;
            }

            if (properties.isCutlines()) {
                drawGridLines(contentStream, verticalLines, horizontalLines, gridMargin, pageWidth, pageHeight, properties.getCutlineColor());
            }
            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private void drawGridLines(PDPageContentStream contentStream, List<Float> verticalLines,
                               List<Float> horizontalLines, float gridMargin, float pageWidth, float pageHeight, String colorHex) throws IOException {
        Color color = Color.decode(colorHex);
        contentStream.setStrokingColor(color);

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

    private void drawCorners(PDPageContentStream contentStream, float x, float y, float width, float height, String colorHex) throws IOException {
        Color color = Color.decode(colorHex);
        contentStream.setStrokingColor(color);

        float cornerRadius = height * 0.05f; // Radius der abgerundeten Ecke

        // Top-left corner
        drawRoundedCorner(contentStream, x, y + height, cornerRadius, 0);

        // Top-right corner
        drawRoundedCorner(contentStream, x + width, y + height, cornerRadius, 1);

        // Bottom-left corner
        drawRoundedCorner(contentStream, x, y, cornerRadius, 2);

        // Bottom-right corner
        drawRoundedCorner(contentStream, x + width, y, cornerRadius, 3);
    }

    private void drawRoundedCorner(PDPageContentStream contentStream, float cornerX, float cornerY, float radius, int quadrant) throws IOException {
        float startAngle, endAngle;

        switch (quadrant) {
            case 0: // Top-left
                startAngle = 0;
                endAngle = 90;
                break;
            case 1: // Top-right
                startAngle = 90;
                endAngle = 180;
                break;
            case 2: // Bottom-left
                startAngle = 270;
                endAngle = 360;
                break;
            case 3: // Bottom-right
                startAngle = 180;
                endAngle = 270;
                break;
            default:
                throw new IllegalArgumentException("Invalid quadrant");
        }

        float x1 = (float) (cornerX + radius * Math.cos(Math.toRadians(startAngle)));
        float y1 = (float) (cornerY + radius * Math.sin(Math.toRadians(startAngle)));
        float x2 = (float) (cornerX + radius * Math.cos(Math.toRadians(endAngle)));
        float y2 = (float) (cornerY + radius * Math.sin(Math.toRadians(endAngle)));

        contentStream.moveTo(cornerX, cornerY);
        contentStream.lineTo(x1, y1);
        contentStream.curveTo(
            (float) (cornerX + radius * Math.cos(Math.toRadians(startAngle + 30))),
            (float) (cornerY + radius * Math.sin(Math.toRadians(startAngle + 30))),
            (float) (cornerX + radius * Math.cos(Math.toRadians(endAngle - 30))),
            (float) (cornerY + radius * Math.sin(Math.toRadians(endAngle - 30))),
            x2, y2
        );
    }
}
