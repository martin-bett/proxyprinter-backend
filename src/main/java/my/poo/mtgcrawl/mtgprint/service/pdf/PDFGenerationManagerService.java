package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto.Category;

/**
 * Nimmt PDF-Aufträge an, stellt sie in eine Warteschlange und baut höchstens
 * {@code pdf.max-concurrent} gleichzeitig. Die progressId ist der Hash des
 * Requests: identische Aufträge laufen nur einmal und kommen danach aus dem Cache.
 */
@Service
public class PDFGenerationManagerService {

    private static final Logger log = LoggerFactory.getLogger(PDFGenerationManagerService.class);

    private final PDFGenerationService pdfGenerationService;
    private final PdfCacheService pdfCacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int maxConcurrent;
    private final ExecutorService executor;

    private final Deque<PdfJob> queue = new ArrayDeque<>();
    private final Map<String, PdfJob> allJobs = new ConcurrentHashMap<>();
    private int activeJobs = 0;

    public PDFGenerationManagerService(PDFGenerationService pdfGenerationService, PdfCacheService pdfCacheService,
            @Value("${pdf.max-concurrent:2}") int maxConcurrent) {
        this.pdfGenerationService = pdfGenerationService;
        this.pdfCacheService = pdfCacheService;
        this.maxConcurrent = Math.max(1, maxConcurrent);
        this.executor = Executors.newFixedThreadPool(this.maxConcurrent);
    }

    public synchronized String submitJob(CreatePdfDto request) {
        String progressId = computeRequestHash(request);

        PdfJob existing = allJobs.get(progressId);
        if (existing != null && existing.status != JobStatus.FINISHED) {
            return progressId;
        }

        PdfJob job = new PdfJob(progressId, request);
        allJobs.put(progressId, job);

        byte[] cachedPdf = pdfCacheService.getFromCache(progressId);
        if (cachedPdf != null) {
            ProgressDto dto = ProgressDto.of(progressId, Category.finishing, "PDF already cached, ready for download.", 1, 1);
            dto.setFileSize(cachedPdf.length);
            job.sendProgress(dto);
            job.setStatus(JobStatus.FINISHED);
            return progressId;
        }

        queue.addLast(job);
        sendQueuePositions();
        startNextIfPossible();
        return progressId;
    }

    private synchronized void startNextIfPossible() {
        while (activeJobs < maxConcurrent && !queue.isEmpty()) {
            startJob(queue.pollFirst());
        }
        sendQueuePositions();
    }

    private void startJob(PdfJob job) {
        activeJobs++;
        job.setStatus(JobStatus.RUNNING);
        job.sendProgress(ProgressDto.of(job.progressId, Category.queued, "Leaving queue, starting generation", 1, 1));
        executor.submit(() -> {
            try {
                pdfGenerationService.generateDocument(job);
            } catch (Throwable e) {
                if (e instanceof PdfSizeLimit.PdfTooLargeException) {
                    log.info("PDF " + job.progressId + " rejected: " + e.getMessage());
                } else {
                    log.error("PDF generation failed for " + job.progressId, e);
                }
                ProgressDto errorDto = ProgressDto.of(job.progressId, Category.finishing, "Error: " + e.getMessage(), 0, 0);
                errorDto.setError(true);
                job.sendProgress(errorDto);
            } finally {
                job.setStatus(JobStatus.FINISHED);
                synchronized (this) {
                    activeJobs--;
                }
                startNextIfPossible();
            }
        });
    }

    /** Teilt jedem wartenden Job seine aktuelle Position mit. */
    private synchronized void sendQueuePositions() {
        int size = queue.size();
        int position = 1;
        for (PdfJob job : queue) {
            ProgressDto dto = ProgressDto.of(job.progressId, Category.queued,
                    "Waiting in queue (" + position + " of " + size + ")", position, size);
            List<ProgressDto> history = job.progressHistory;
            ProgressDto last = history.isEmpty() ? null : history.get(history.size() - 1);
            if (last == null || !dto.equals(last)) {
                job.sendProgress(dto);
            }
            position++;
        }
    }

    public List<ProgressDto> getProgressHistory(String progressId) {
        PdfJob job = allJobs.get(progressId);
        return job == null ? null : new ArrayList<>(job.progressHistory);
    }

    public synchronized int getQueueSize() {
        return queue.size();
    }

    public synchronized int getActiveJobCount() {
        return activeJobs;
    }

    public int getMaxConcurrentJobs() {
        return maxConcurrent;
    }

    /** Fertige Jobs werden nach Ablauf der Cache-Zeit vergessen. */
    @Scheduled(fixedRate = 60 * 60 * 1000L)
    public void cleanUpJobs() {
        long now = System.currentTimeMillis();
        allJobs.values().removeIf(job -> job.status == JobStatus.FINISHED && now - job.finishedAt > PdfCacheService.MAX_AGE_MS);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private String computeRequestHash(CreatePdfDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash pdf request", e);
        }
    }
}
