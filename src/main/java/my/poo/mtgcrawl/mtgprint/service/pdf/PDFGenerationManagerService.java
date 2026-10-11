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
import my.poo.mtgcrawl.mtgprint.service.stats.UsageMetric;
import my.poo.mtgcrawl.mtgprint.service.stats.UsageTracker;

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
    private final PdfMailNotifier mailNotifier;
    private final UsageTracker usage;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int maxConcurrent;
    private final ExecutorService executor;

    private final Deque<PdfJob> queue = new ArrayDeque<>();
    private final Map<String, PdfJob> allJobs = new ConcurrentHashMap<>();
    private int activeJobs = 0;

    public PDFGenerationManagerService(PDFGenerationService pdfGenerationService, PdfCacheService pdfCacheService,
            PdfMailNotifier mailNotifier, UsageTracker usage, @Value("${pdf.max-concurrent:2}") int maxConcurrent) {
        this.pdfGenerationService = pdfGenerationService;
        this.pdfCacheService = pdfCacheService;
        this.mailNotifier = mailNotifier;
        this.usage = usage;
        this.maxConcurrent = Math.max(1, maxConcurrent);
        this.executor = Executors.newFixedThreadPool(this.maxConcurrent);
    }

    public synchronized String submitJob(CreatePdfDto request) {
        String progressId = computeRequestHash(request);
        String email = request.getEmail();
        String language = request.getLanguage();

        PdfJob existing = allJobs.get(progressId);
        if (existing != null && existing.status != JobStatus.FINISHED) {
            if (email != null) {
                existing.emailRecipients.put(email, MailTexts.normalize(language));
            }
            return progressId;
        }

        PdfJob job = new PdfJob(progressId, request);
        allJobs.put(progressId, job);

        byte[] cachedPdf = pdfCacheService.getFromCache(progressId);
        if (cachedPdf != null) {
            pdfCacheService.refresh(progressId);
            ProgressDto dto = ProgressDto.of(progressId, Category.finishing, "PDF already cached, ready for download.", 1, 1);
            dto.setFileSize(cachedPdf.length);
            job.sendProgress(dto);
            job.setStatus(JobStatus.FINISHED);
            if (email != null) {
                mailNotifier.sendReady(email, MailTexts.normalize(language), progressId, cachedPdf.length);
            }
            return progressId;
        }

        if (email != null) {
            job.emailRecipients.put(email, MailTexts.normalize(language));
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
                usage.count(UsageMetric.DECKS);
                long fileSize = job.progressHistory.isEmpty() ? 0 : last(job.progressHistory).getFileSize();
                job.emailRecipients.forEach((to, lang) -> mailNotifier.sendReady(to, lang, job.progressId, fileSize));
            } catch (Throwable e) {
                if (e instanceof PdfSizeLimit.PdfTooLargeException) {
                    log.info("PDF " + job.progressId + " rejected: " + e.getMessage());
                } else {
                    log.error("PDF generation failed for " + job.progressId, e);
                }
                ProgressDto errorDto = ProgressDto.of(job.progressId, Category.finishing, "Error: " + e.getMessage(), 0, 0);
                errorDto.setError(true);
                job.sendProgress(errorDto);
                job.emailRecipients.forEach((to, lang) -> mailNotifier.sendFailed(to, lang, e.getMessage()));
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
            if (history.isEmpty() || !dto.equals(last(history))) {
                job.sendProgress(dto);
            }
            position++;
        }
    }

    private static ProgressDto last(List<ProgressDto> history) {
        return history.get(history.size() - 1);
    }

    /** true, solange ein Job für diese ID wartet oder läuft. */
    public boolean isInProgress(String progressId) {
        PdfJob job = allJobs.get(progressId);
        return job != null && job.status != JobStatus.FINISHED;
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

    /** Vergisst alle fertigen Jobs, z. B. nachdem der Admin die PDFs gelöscht hat. */
    public void forgetFinishedJobs() {
        allJobs.values().removeIf(job -> job.status == JobStatus.FINISHED);
    }

    /** Fertige Jobs werden nach Ablauf der Cache-Zeit vergessen. */
    @Scheduled(fixedRate = 60 * 60 * 1000L)
    public void cleanUpJobs() {
        long now = System.currentTimeMillis();
        allJobs.values().removeIf(job -> job.status == JobStatus.FINISHED && now - job.finishedAt > pdfCacheService.getMaxAgeMs());
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    /** Die E-Mail-Adresse zählt nicht mit, damit dasselbe Deck nicht für jeden Empfänger neu gebaut wird. */
    private String computeRequestHash(CreatePdfDto dto) {
        try {
            CreatePdfDto content = new CreatePdfDto();
            content.setImages(dto.getImages());
            content.setSettings(dto.getSettings());
            String json = objectMapper.writeValueAsString(content);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash pdf request", e);
        }
    }
}
