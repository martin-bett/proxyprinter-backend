package my.poo.mtgcrawl.mtgprint.service.pdf;

import static my.poo.mtgcrawl.mtgprint.service.pdf.DocumentGenerator.MM_TO_PT;

import java.awt.Color;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.RegistrationMarkStyle;

/**
 * Passmarken für Silhouette Print and Cut. Der Plotter sucht die Marken mit seinem optischen Sensor und richtet den
 * Schnitt danach aus. Form und Maße wie in Silhouette Studio (Seite einrichten, Passermarken): Winkel aus zwei Armen,
 * deren Ecke {@link #INSET_MM} von beiden Blattkanten entfernt liegt; beim Muster mit drei Marken oben links statt
 * des Winkels ein gefülltes Quadrat. Studio muss beim Schneiden dieselben Werte haben (Abstand, Länge, Dicke).
 * <p>
 * In den Ecken darf nichts gedruckt sein, sonst verwechselt der Sensor Karte und Marke. Das Raster wird deshalb so
 * gewählt, dass es auf mindestens einer Achse an den Eckbereichen (Abstand + Armlänge + {@link #CLEARANCE_MM})
 * vorbeigeht; die Arme werden so lang wie möglich (5 bis 20 mm, auf ganze mm abgerundet).
 * Gleiche Rechnung im Frontend: src/viewer/pdf/pdfLayout.ts.
 */
final class RegistrationMarks {

    /** Abstand der Marken zur Blattkante; Standard und Minimum in Silhouette Studio. */
    static final float INSET_MM = 10f;
    /** Armlänge: kleinster und größter Wert, den Silhouette Studio zulässt. */
    static final float MIN_LENGTH_MM = 5f;
    static final float MAX_LENGTH_MM = 20f;
    /** Linienstärke; Minimum in Silhouette Studio, dünn liest der Sensor am genauesten. */
    static final float THICKNESS_MM = 0.5f;
    /** Kantenlänge des Quadrats oben links (Muster mit drei Marken). */
    static final float SQUARE_MM = 5f;
    /** Freier Abstand zwischen Marke und Karte. */
    static final float CLEARANCE_MM = 2f;

    private RegistrationMarks() {
    }

    /**
     * Kartenraster mit Passmarken: Spalten, Reihen, Rand links und oben (in Punkt, Raster mittig) und Armlänge in mm.
     */
    record Grid(int cols, int rows, float left, float top, float markLengthMm) {
        int cards() {
            return cols * rows;
        }
    }

    /**
     * Meiste Karten, die zwischen die Marken passen, ohne in einen Eckbereich zu reichen; bei Gleichstand längere
     * Arme. Passt keine einzige Karte frei, liegt eine Karte mittig und die Arme sind so kurz wie möglich.
     */
    static Grid grid(float pageWidth, float pageHeight, float cardWidth, float cardHeight, float hGap, float vGap) {
        float inset = INSET_MM * MM_TO_PT;
        int maxCols = cardsBetween(pageWidth - 2 * inset, cardWidth, hGap);
        int maxRows = cardsBetween(pageHeight - 2 * inset, cardHeight, vGap);
        Grid best = null;
        for (int cols = maxCols; cols >= 1; cols--) {
            for (int rows = maxRows; rows >= 1; rows--) {
                float left = DocumentGenerator.centeredOffset(pageWidth, cols, cardWidth, hGap);
                float top = DocumentGenerator.centeredOffset(pageHeight, rows, cardHeight, vGap);
                float length = markLength(Math.max(left, top) / MM_TO_PT);
                if (length < MIN_LENGTH_MM) {
                    continue;
                }
                Grid grid = new Grid(cols, rows, left, top, length);
                if (best == null || grid.cards() > best.cards()
                        || (grid.cards() == best.cards() && length > best.markLengthMm())) {
                    best = grid;
                }
            }
        }
        if (best == null) {
            return new Grid(1, 1, DocumentGenerator.centeredOffset(pageWidth, 1, cardWidth, hGap),
                    DocumentGenerator.centeredOffset(pageHeight, 1, cardHeight, vGap), MIN_LENGTH_MM);
        }
        return best;
    }

    /** Längste Armlänge in ganzen mm, bei der ein Raster mit {@code freeMm} Rand frei bleibt, höchstens 20 mm. */
    static float markLength(float freeMm) {
        float length = freeMm - INSET_MM - CLEARANCE_MM;
        return (float) Math.floor(Math.min(MAX_LENGTH_MM, length) + 0.001f);
    }

    private static int cardsBetween(float space, float cardSize, float gap) {
        if (cardSize <= 0 || space <= 0) {
            return 0;
        }
        return Math.max(0, (int) ((space + gap) / (cardSize + gap)));
    }

    /** Zeichnet die Marken auf eine Seite (PDF-Koordinaten, Ursprung unten links). */
    static void draw(PDPageContentStream contentStream, RegistrationMarkStyle style, float pageWidth, float pageHeight,
            float lengthMm) throws IOException {
        float inset = INSET_MM * MM_TO_PT;
        float length = lengthMm * MM_TO_PT;
        float left = inset;
        float right = pageWidth - inset;
        float top = pageHeight - inset;
        float bottom = inset;

        contentStream.saveGraphicsState();
        contentStream.setStrokingColor(Color.BLACK);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.setLineWidth(THICKNESS_MM * MM_TO_PT);
        contentStream.setLineCapStyle(0);
        contentStream.setLineJoinStyle(0);
        if (style == RegistrationMarkStyle.SILHOUETTE_4) {
            angle(contentStream, left, top, 1, -1, length);
        } else {
            float square = SQUARE_MM * MM_TO_PT;
            contentStream.addRect(left, top - square, square, square);
            contentStream.fill();
        }
        angle(contentStream, right, top, -1, -1, length);
        angle(contentStream, left, bottom, 1, 1, length);
        if (style == RegistrationMarkStyle.SILHOUETTE_4) {
            angle(contentStream, right, bottom, -1, 1, length);
        }
        contentStream.restoreGraphicsState();
    }

    /** Winkel mit Ecke (x, y); dx/dy zeigen von der Ecke ins Blatt. */
    private static void angle(PDPageContentStream contentStream, float x, float y, float dx, float dy, float length)
            throws IOException {
        contentStream.moveTo(x + dx * length, y);
        contentStream.lineTo(x, y);
        contentStream.lineTo(x, y + dy * length);
        contentStream.stroke();
    }
}
