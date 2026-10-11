package my.poo.mtgcrawl.mtgprint.service.pdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto.Category;
import my.poo.mtgcrawl.mtgprint.service.stats.UsageMetric;
import my.poo.mtgcrawl.mtgprint.service.stats.UsageTracker;

class PDFGenerationManagerServiceTest {

    private static CreatePdfDto request(String url) {
        CreatePdfDto dto = new CreatePdfDto();
        ImageDto image = new ImageDto();
        image.setImageUrl(url);
        dto.getImages().add(image);
        return dto;
    }

    private static ProgressDto last(List<ProgressDto> history) {
        return history.get(history.size() - 1);
    }

    @Test
    void queuesJobsBeyondLimitAndDeduplicatesRequests() throws Exception {
        PDFGenerationService generation = mock(PDFGenerationService.class);
        PdfCacheService cache = mock(PdfCacheService.class);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        doAnswer(inv -> {
            release.await(5, TimeUnit.SECONDS);
            done.countDown();
            return null;
        }).when(generation).generateDocument(any());

        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, mock(PdfMailNotifier.class), mock(UsageTracker.class), 1);
        try {
            String first = manager.submitJob(request("a"));
            String second = manager.submitJob(request("b"));

            assertNotEquals(first, second);
            assertEquals(first, manager.submitJob(request("a")), "same request must reuse the running job");
            assertEquals(1, manager.getActiveJobCount());
            assertEquals(1, manager.getQueueSize());

            ProgressDto waiting = last(manager.getProgressHistory(second));
            assertEquals(Category.queued, waiting.getCategory());
            assertEquals("Waiting in queue (1 of 1)", waiting.getMessage());

            release.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
            verify(generation, times(2)).generateDocument(any());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void returnsCachedPdfWithoutRebuilding() throws Exception {
        PDFGenerationService generation = mock(PDFGenerationService.class);
        PdfCacheService cache = mock(PdfCacheService.class);
        when(cache.getFromCache(any())).thenReturn(new byte[] { 1, 2, 3 });

        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, mock(PdfMailNotifier.class), mock(UsageTracker.class), 2);
        try {
            String id = manager.submitJob(request("a"));
            ProgressDto dto = last(manager.getProgressHistory(id));
            assertEquals(Category.finishing, dto.getCategory());
            assertEquals(3, dto.getFileSize());
            assertFalse(dto.isError());
            verify(generation, never()).generateDocument(any());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void reportsErrorWhenGenerationFails() throws Exception {
        PDFGenerationService generation = mock(PDFGenerationService.class);
        PdfCacheService cache = mock(PdfCacheService.class);
        doThrow(new IllegalStateException("boom")).when(generation).generateDocument(any());

        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, mock(PdfMailNotifier.class), mock(UsageTracker.class), 1);
        try {
            String id = manager.submitJob(request("a"));
            ProgressDto dto = null;
            for (int i = 0; i < 50; i++) {
                dto = last(manager.getProgressHistory(id));
                if (dto.isError()) break;
                Thread.sleep(50);
            }
            assertTrue(dto.isError());
            assertEquals("Error: boom", dto.getMessage());
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void mailsDownloadLinkToEveryRecipientWithoutRebuilding() throws Exception {
        PDFGenerationService generation = mock(PDFGenerationService.class);
        PdfCacheService cache = mock(PdfCacheService.class);
        PdfMailNotifier mail = mock(PdfMailNotifier.class);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(inv -> {
            release.await(5, TimeUnit.SECONDS);
            PdfJob job = inv.getArgument(0);
            ProgressDto done = ProgressDto.of(job.progressId, Category.finishing, "PDF compilation completed", 1, 1);
            done.setFileSize(42);
            job.sendProgress(done);
            return null;
        }).when(generation).generateDocument(any());

        UsageTracker stats = mock(UsageTracker.class);
        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, mail, stats, 1);
        try {
            CreatePdfDto first = request("a");
            first.setEmail("one@example.com");
            CreatePdfDto second = request("a");
            second.setEmail("two@example.com");
            second.setLanguage("de");

            String id = manager.submitJob(first);
            assertEquals(id, manager.submitJob(second), "email must not change the progress id");
            assertEquals(id, manager.submitJob(request("a")));

            release.countDown();
            verify(mail, timeout(5000)).sendReady("one@example.com", "en", id, 42);
            verify(mail, timeout(5000)).sendReady("two@example.com", "de", id, 42);
            verify(generation, times(1)).generateDocument(any());
            verify(mail, never()).sendFailed(any(), any(), any());
            verify(stats, times(1)).count(UsageMetric.DECKS);
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void mailsFailureAndCachedPdfs() throws Exception {
        PDFGenerationService generation = mock(PDFGenerationService.class);
        PdfCacheService cache = mock(PdfCacheService.class);
        PdfMailNotifier mail = mock(PdfMailNotifier.class);
        doThrow(new IllegalStateException("boom")).when(generation).generateDocument(any());

        UsageTracker stats = mock(UsageTracker.class);
        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, mail, stats, 1);
        try {
            CreatePdfDto failing = request("a");
            failing.setEmail("one@example.com");
            manager.submitJob(failing);
            verify(mail, timeout(5000)).sendFailed("one@example.com", "en", "boom");

            when(cache.getFromCache(any())).thenReturn(new byte[] { 1, 2, 3 });
            CreatePdfDto cached = request("b");
            cached.setEmail("two@example.com");
            String id = manager.submitJob(cached);
            verify(mail).sendReady("two@example.com", "en", id, 3);
            verify(stats, never()).count(UsageMetric.DECKS);
        } finally {
            manager.shutdown();
        }
    }
}
