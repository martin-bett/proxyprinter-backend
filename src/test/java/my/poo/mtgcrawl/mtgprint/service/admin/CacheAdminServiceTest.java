package my.poo.mtgcrawl.mtgprint.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.poo.mtgcrawl.mtgprint.service.deckfinder.DeckFinderFetchService;
import my.poo.mtgcrawl.mtgprint.service.pdf.BufferedImageCacheService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;
import my.poo.mtgcrawl.mtgprint.service.rest.ImageFetchService;
import my.poo.mtgcrawl.mtgprint.service.searchcache.SearchResultCacheService;

class CacheAdminServiceTest {

    @TempDir
    Path dir;

    private final SearchResultCacheService searchCache = mock(SearchResultCacheService.class);
    private final PdfCacheService pdfCache = mock(PdfCacheService.class);
    private final PDFGenerationManagerService pdfJobs = mock(PDFGenerationManagerService.class);
    private final FileDownloadService downloads = mock(FileDownloadService.class);
    private final CacheAdminService service = new CacheAdminService(searchCache, pdfCache, pdfJobs,
            mock(FileUploadService.class), downloads, mock(BufferedImageCacheService.class),
            mock(ImageFetchService.class), mock(DeckFinderFetchService.class));

    @Test
    void dirStatsCountsMatchingFilesInSubfolders() throws Exception {
        Files.createDirectories(dir.resolve("a/b"));
        Files.write(dir.resolve("a/b/one.json"), new byte[100]);
        Files.write(dir.resolve("a/two.json"), new byte[50]);
        Files.write(dir.resolve("a/put-1.tmp"), new byte[999]);
        Files.setLastModifiedTime(dir.resolve("a/two.json"), FileTime.fromMillis(1_000_000L));

        var stats = CacheAdminService.dirStats(dir, ".json");
        assertEquals(2, stats.files());
        assertEquals(150, stats.bytes());
        assertEquals(1_000_000L, stats.oldest());

        assertEquals(0, CacheAdminService.dirStats(dir.resolve("missing"), null).files());
    }

    @Test
    void pdfsAndImagesStayWhilePdfsAreBeingGenerated() {
        when(pdfJobs.getActiveJobCount()).thenReturn(1);
        AdminException e = assertThrows(AdminException.class, () -> service.clear(CacheAdminService.PDFS, "a"));
        assertEquals("admin.pdfJobsRunning", e.toBody().get("code"));
        assertThrows(AdminException.class, () -> service.clear(CacheAdminService.IMAGES, "a"));
        verify(pdfCache, never()).clear();
        verify(downloads, never()).clear();

        when(pdfJobs.getActiveJobCount()).thenReturn(0);
        when(pdfCache.clear()).thenReturn(3);
        assertEquals(3, service.clear(CacheAdminService.PDFS, "a"));
        verify(pdfJobs).forgetFinishedJobs();
    }

    @Test
    void unknownCacheIsRejected() {
        AdminException e = assertThrows(AdminException.class, () -> service.clear("nope", "a"));
        assertEquals("admin.unknownCache", e.toBody().get("code"));
    }
}
