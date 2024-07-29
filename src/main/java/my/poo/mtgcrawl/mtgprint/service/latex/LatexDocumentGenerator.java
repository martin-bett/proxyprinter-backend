package my.poo.mtgcrawl.mtgprint.service.latex;

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
public class LatexDocumentGenerator {

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
                    drawGridLines(contentStream, verticalLines, horizontalLines, gridMargin, pageWidth, pageHeight);
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

                // Add grid lines for this card
                verticalLines.add(x);
                verticalLines.add(x + cardWidth);
                horizontalLines.add(y);
                horizontalLines.add(y + cardHeight);

                cardCount++;
            }

            drawGridLines(contentStream, verticalLines, horizontalLines, gridMargin, pageWidth, pageHeight);
            contentStream.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private void drawGridLines(PDPageContentStream contentStream, List<Float> verticalLines,
            List<Float> horizontalLines,
            float gridMargin, float pageWidth, float pageHeight) throws IOException {
        contentStream.setStrokingColor(Color.LIGHT_GRAY);

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

    // private String compileTex(String texFilePath) throws IOException,
    // InterruptedException {
    // File texFile = new File(texFilePath);
    // String absoluteTexFilePath = texFile.getAbsolutePath();

    // ProcessBuilder processBuilder = new ProcessBuilder("pdflatex",
    // "-interaction=nonstopmode", "-shell-escape", absoluteTexFilePath);
    // processBuilder.directory(new File(texFilePath).getParentFile());
    // processBuilder.redirectErrorStream(true);
    // processBuilder.inheritIO();
    // Process process = processBuilder.start();

    // // Wait for the process to complete with a timeout
    // boolean completed = process.waitFor(60 * 10, TimeUnit.SECONDS);

    // if (!completed) {
    // process.destroy();
    // throw new RuntimeException("pdflatex process timed out");
    // }

    // int exitCode = process.exitValue();
    // if (exitCode != 0 && exitCode != 1) {
    // throw new RuntimeException("pdflatex process exited with code " + exitCode);
    // }
    // System.out.println("PDF successfully generated.");
    // return texFilePath.replaceAll("\\.tex$", ".pdf");
    // }

    // private void cleanupTempFiles(String dirPath) {
    // try {
    // Files.walk(Paths.get(dirPath))
    // .sorted(java.util.Comparator.reverseOrder())
    // .map(java.nio.file.Path::toFile)
    // .forEach(File::delete);
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }
}