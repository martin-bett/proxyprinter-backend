package my.poo.mtgcrawl.mtgprint.service.pdf;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfCacheServiceTest {

    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    @TempDir
    Path dir;

    private Path pdf(String id, long ageMs) throws Exception {
        Path file = dir.resolve(id + ".pdf");
        Files.write(file, new byte[] { 1, 2, 3 });
        assertTrue(file.toFile().setLastModified(System.currentTimeMillis() - ageMs));
        return file;
    }

    @Test
    void keepsPdfsForTheConfiguredDays() {
        PdfCacheService cache = new PdfCacheService(dir, 7);
        cache.addToCache("abc", new byte[] { 1, 2, 3 });

        assertEquals(7 * DAY_MS, cache.getMaxAgeMs());
        assertArrayEquals(new byte[] { 1, 2, 3 }, cache.getFromCache("abc"));
        PdfCacheService.PdfInfo info = cache.getInfo("abc").orElseThrow();
        assertEquals(3, info.fileSize());
        assertEquals(info.createdAt() + 7 * DAY_MS, info.expiresAt());
        assertTrue(cache.getInfo("unknown").isEmpty());
    }

    @Test
    void picksUpPdfsFromDiskAfterRestart() throws Exception {
        pdf("fresh", 2 * DAY_MS);

        PdfCacheService cache = new PdfCacheService(dir, 7);

        assertNotNull(cache.getFromCache("fresh"), "a pdf written before the restart must still be downloadable");
        assertEquals(1, cache.getCachedPdfCount());
    }

    @Test
    void deletesExpiredPdfsEvenWhenTheyWereNotInTheIndex() throws Exception {
        Path old = pdf("old", 8 * DAY_MS);
        Path fresh = pdf("fresh", DAY_MS);

        PdfCacheService cache = new PdfCacheService(dir, 7);

        assertFalse(Files.exists(old), "expired pdf from before the restart must be deleted");
        assertTrue(Files.exists(fresh));
        assertNull(cache.getFromCache("old"));
    }

    @Test
    void refreshRestartsTheRetention() throws Exception {
        pdf("again", 6 * DAY_MS);
        PdfCacheService cache = new PdfCacheService(dir, 7);
        long before = cache.getInfo("again").orElseThrow().expiresAt();

        cache.refresh("again");

        long after = cache.getInfo("again").orElseThrow().expiresAt();
        assertTrue(after - before > 5 * DAY_MS);
    }
}
