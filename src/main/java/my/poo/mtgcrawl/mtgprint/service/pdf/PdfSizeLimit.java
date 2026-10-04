package my.poo.mtgcrawl.mtgprint.service.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Obergrenze für die Größe eines PDFs, einstellbar über {@code pdf.max-size-mb}.
 * Das Frontend liest denselben Wert über GET /api/pdf/limits und warnt vorab;
 * hier wird er beim Bauen tatsächlich durchgesetzt.
 */
@Component
public class PdfSizeLimit {

    private static final long MB = 1024L * 1024L;

    private final long maxSizeMb;

    public PdfSizeLimit(@Value("${pdf.max-size-mb:300}") long maxSizeMb) {
        this.maxSizeMb = maxSizeMb;
    }

    public long getMaxSizeMb() {
        return maxSizeMb;
    }

    public long getMaxBytes() {
        return maxSizeMb * MB;
    }

    /** Bricht ab, sobald {@code bytes} über der Grenze liegt. 0 oder weniger schaltet die Grenze ab. */
    public void check(long bytes, int card, int totalCards) {
        if (maxSizeMb > 0 && bytes > getMaxBytes()) {
            throw new PdfTooLargeException("The PDF would be larger than the limit of " + maxSizeMb
                    + " MB (reached at card " + card + " of " + totalCards
                    + "). Split the deck into several PDFs or lower Max DPI in the settings.");
        }
    }

    public static class PdfTooLargeException extends RuntimeException {
        public PdfTooLargeException(String message) {
            super(message);
        }
    }
}
