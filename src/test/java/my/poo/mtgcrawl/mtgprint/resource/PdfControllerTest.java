package my.poo.mtgcrawl.mtgprint.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PdfControllerTest {

    @Test
    void downloadFileNameIsSafe() {
        assertEquals("deck.pdf", PdfController.downloadFileName(null));
        assertEquals("deck.pdf", PdfController.downloadFileName("   "));
        assertEquals("60-Karten-Lightning-Bolt.pdf", PdfController.downloadFileName("60 Karten Lightning Bolt"));
        assertEquals("etcpasswd.pdf", PdfController.downloadFileName("../etc/passwd"));
        assertEquals("deck.pdf", PdfController.downloadFileName("deck.PDF"));
        assertEquals("Snapshot-1.pdf", PdfController.downloadFileName("Snapshot \"1\"\r\n"));
    }
}
