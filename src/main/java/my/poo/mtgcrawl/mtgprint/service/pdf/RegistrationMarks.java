package my.poo.mtgcrawl.mtgprint.service.pdf;

import static my.poo.mtgcrawl.mtgprint.service.pdf.DocumentGenerator.MM_TO_PT;

import java.awt.Color;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.RegistrationMarkStyle;

/**
 * Passmarken für Silhouette Print and Cut. Der Plotter sucht die Marken mit seinem optischen Sensor und richtet den
 * Schnitt danach aus; Studio muss dieselben Marken haben. Lage wie Silhouette Studio (A4, CAMEO 5) mit den
 * Vorgaben: Abstand zur Blattkante {@link #INSET_MM}, nur auf der Seite gegenüber dem Einzug (quer rechts,
 * hoch oben) {@link #FAR_INSET_MM}, kleiner lässt Studio dort nicht zu. Drei Marken: quer Quadrat oben links,
 * Winkel oben rechts und unten links; hoch dasselbe um 90° gedreht (Quadrat unten links, Winkel oben links und
 * unten rechts). Vier Marken: Winkel in allen Ecken.
 * <p>
 * In den Ecken darf nichts gedruckt sein, sonst verwechselt der Sensor Karte und Marke. Das Raster liegt mittig
 * zwischen den Marken (nicht auf der Seite, weil die Abstände ungleich sind) und reicht auf mindestens einer Achse
 * nicht in die Eckbereiche (Abstand + Armlänge + {@link #CLEARANCE_MM}); die Arme werden so lang wie möglich
 * (5 bis 20 mm, auf ganze mm abgerundet). Gleiche Rechnung im Frontend: src/viewer/pdf/pdfLayout.ts.
 */
final class RegistrationMarks {

    /** Abstand der Marken zur Blattkante; Standard und Minimum in Silhouette Studio. */
    static final float INSET_MM = 10f;
    /** Abstand auf der Seite gegenüber dem Einzug (quer rechts, hoch oben); Minimum in Silhouette Studio. */
    static final float FAR_INSET_MM = 25f;
    /** Armlänge: kleinster und größter Wert, den Silhouette Studio zulässt. */
    static final float MIN_LENGTH_MM = 5f;
    static final float MAX_LENGTH_MM = 20f;
    /** Linienstärke; Minimum in Silhouette Studio, dünn liest der Sensor am genauesten. */
    static final float THICKNESS_MM = 0.5f;
    /** Kantenlänge des Quadrats (Muster mit drei Marken). */
    static final float SQUARE_MM = 5f;
    /** Freier Abstand zwischen Marke und Karte. */
    static final float CLEARANCE_MM = 1f;

    private RegistrationMarks() {
    }

    /** Abstände der Marken zu den Blattkanten in mm: links, oben, rechts, unten. */
    static float[] insetsMm(boolean landscape) {
        return landscape
                ? new float[] { INSET_MM, INSET_MM, FAR_INSET_MM, INSET_MM }
                : new float[] { INSET_MM, FAR_INSET_MM, INSET_MM, INSET_MM };
    }

    /**
     * Kartenraster mit Passmarken: Spalten, Reihen, Rand links und oben (in Punkt) und Armlänge in mm.
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
        float[] inset = insetsMm(pageWidth > pageHeight);
        float left = inset[0] * MM_TO_PT;
        float top = inset[1] * MM_TO_PT;
        float width = pageWidth - (inset[0] + inset[2]) * MM_TO_PT;
        float height = pageHeight - (inset[1] + inset[3]) * MM_TO_PT;
        int maxCols = cardsBetween(width, cardWidth, hGap);
        int maxRows = cardsBetween(height, cardHeight, vGap);
        Grid best = null;
        for (int cols = maxCols; cols >= 1; cols--) {
            for (int rows = maxRows; rows >= 1; rows--) {
                // Abstand Raster zu den Marken, auf beiden Seiten gleich
                float freeX = DocumentGenerator.centeredOffset(width, cols, cardWidth, hGap);
                float freeY = DocumentGenerator.centeredOffset(height, rows, cardHeight, vGap);
                float length = markLength(Math.max(freeX, freeY) / MM_TO_PT);
                if (length < MIN_LENGTH_MM) {
                    continue;
                }
                Grid grid = new Grid(cols, rows, left + freeX, top + freeY, length);
                if (best == null || grid.cards() > best.cards()
                        || (grid.cards() == best.cards() && length > best.markLengthMm())) {
                    best = grid;
                }
            }
        }
        if (best == null) {
            return new Grid(1, 1, left + DocumentGenerator.centeredOffset(width, 1, cardWidth, hGap),
                    top + DocumentGenerator.centeredOffset(height, 1, cardHeight, vGap), MIN_LENGTH_MM);
        }
        return best;
    }

    /** Längste Armlänge in ganzen mm, wenn das Raster {@code freeMm} Abstand zu den Marken hat, höchstens 20 mm. */
    static float markLength(float freeMm) {
        float length = freeMm - CLEARANCE_MM;
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
        boolean landscape = pageWidth > pageHeight;
        float[] inset = insetsMm(landscape);
        float length = lengthMm * MM_TO_PT;
        float left = inset[0] * MM_TO_PT;
        float top = pageHeight - inset[1] * MM_TO_PT;
        float right = pageWidth - inset[2] * MM_TO_PT;
        float bottom = inset[3] * MM_TO_PT;
        boolean four = style == RegistrationMarkStyle.SILHOUETTE_4;

        contentStream.saveGraphicsState();
        contentStream.setStrokingColor(Color.BLACK);
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.setLineWidth(THICKNESS_MM * MM_TO_PT);
        contentStream.setLineCapStyle(0);
        contentStream.setLineJoinStyle(0);
        float square = SQUARE_MM * MM_TO_PT;
        if (landscape) {
            // Quadrat oben links, Winkel oben rechts und unten links (vier Marken: auch unten rechts)
            if (four) {
                angle(contentStream, left, top, 1, -1, length);
            } else {
                contentStream.addRect(left, top - square, square, square);
                contentStream.fill();
            }
            angle(contentStream, right, top, -1, -1, length);
            angle(contentStream, left, bottom, 1, 1, length);
            if (four) {
                angle(contentStream, right, bottom, -1, 1, length);
            }
        } else {
            // Um 90° gedreht: Quadrat unten links, Winkel oben links und unten rechts (vier Marken: auch oben rechts)
            if (four) {
                angle(contentStream, left, bottom, 1, 1, length);
            } else {
                contentStream.addRect(left, bottom, square, square);
                contentStream.fill();
            }
            angle(contentStream, left, top, 1, -1, length);
            angle(contentStream, right, bottom, -1, 1, length);
            if (four) {
                angle(contentStream, right, top, -1, -1, length);
            }
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
