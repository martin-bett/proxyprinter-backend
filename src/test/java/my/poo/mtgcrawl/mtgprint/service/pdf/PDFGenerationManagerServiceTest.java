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

        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, 1);
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

        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, 2);
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

        PDFGenerationManagerService manager = new PDFGenerationManagerService(generation, cache, 1);
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
}
