/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.CreatePdfDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ProgressDto;
import my.poo.mtgcrawl.mtgprint.service.MailingService;
import my.poo.mtgcrawl.mtgprint.service.pdf.JobStatus;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PDFGenerationManagerService {
    private static final Logger log = LoggerFactory.getLogger(PDFGenerationManagerService.class);
    private final PDFGenerationService pdfGenerationService;
    private final MailingService mailingService;
    private final PdfCacheService pdfCacheService;
    private final int MAX_CONCURRENT = 2;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Queue<PdfJob> queue = new ConcurrentLinkedQueue<PdfJob>();
    private final Map<String, PdfJob> allJobs = new ConcurrentHashMap<String, PdfJob>();

    public PDFGenerationManagerService(PDFGenerationService pdfGenerationService, PdfCacheService pdfCacheService, MailingService mailingService) {
        this.pdfGenerationService = pdfGenerationService;
        this.pdfCacheService = pdfCacheService;
        this.mailingService = mailingService;
    }

    public int getQueueSize() {
        return this.queue.size();
    }

    public int getMaxConcurrentJobs() {
        return 2;
    }

    public String submitJob(CreatePdfDto request) {
        String hash = this.computeRequestHash(request);
        String progressId = hash;
        if (this.allJobs.containsKey(progressId) && this.allJobs.get((Object)progressId).status != JobStatus.FINISHED) {
            return progressId;
        }
        PdfJob job = new PdfJob(progressId, request);
        this.allJobs.put(progressId, job);
        byte[] cachedPdf = this.pdfCacheService.getFromCache(progressId);
        if (cachedPdf != null) {
            ProgressDto dto = new ProgressDto();
            dto.setProgressId(progressId);
            dto.setCategory(ProgressDto.Category.finishing);
            dto.setMessage("PDF already cached, ready for download.");
            dto.setCurrent(0);
            dto.setTotal(0);
            dto.setPageCount(null);
            dto.setFileSize(cachedPdf.length);
            job.sendProgress(dto);
            job.setStatus(JobStatus.FINISHED);
            return progressId;
        }
        if (this.getActiveJobCount() < 2) {
            this.startJob(job);
        } else {
            this.queue.add(job);
            this.sendQueuedProgress(job);
        }
        return progressId;
    }

    private void startJob(PdfJob job) {
        job.setStatus(JobStatus.RUNNING);
        this.sendQueuedFinish(job);
        this.executor.submit(() -> {
            try {
                int total = 15;
                for (int t = 0; t < total; ++t) {
                    ProgressDto dto = new ProgressDto();
                    dto.setProgressId(job.progressId);
                    dto.setCategory(ProgressDto.Category.queued);
                    dto.setMessage("Simuliertes Warten: Noch " + (total - t) + " Sekunden");
                    dto.setCurrent(t + 1);
                    dto.setTotal(total);
                    dto.setPageCount(null);
                    dto.setFileSize(0);
                    job.sendProgress(dto);
                    Thread.sleep(1000L);
                }
                this.pdfGenerationService.generateDocument(job);
            }
            catch (Exception e) {
                ProgressDto errorDto = new ProgressDto();
                errorDto.setProgressId(job.progressId);
                errorDto.setCategory(ProgressDto.Category.finishing);
                errorDto.setMessage("Fehler: " + e.getMessage());
                errorDto.setCurrent(0);
                errorDto.setTotal(0);
                errorDto.setPageCount(null);
                errorDto.setFileSize(0);
                job.sendProgress(errorDto);
            }
            finally {
                job.setStatus(JobStatus.FINISHED);
                this.startNextIfPossible();
            }
        });
    }

    private void startNextIfPossible() {
        while (this.getActiveJobCount() < 2 && !this.queue.isEmpty()) {
            PdfJob next = this.queue.poll();
            if (next == null) continue;
            this.startJob(next);
        }
    }

    public int getActiveJobCount() {
        return (int)this.allJobs.values().stream().filter(j -> j.status == JobStatus.RUNNING).count();
    }

    private void sendQueuedProgress(PdfJob job) {
        ProgressDto dto = new ProgressDto();
        dto.setProgressId(job.progressId);
        dto.setCategory(ProgressDto.Category.queued);
        dto.setMessage("Waiting in queue (" + (this.queue.stream().toList().indexOf(job) + 1) + " of " + this.queue.size() + ")");
        dto.setCurrent(0);
        dto.setTotal(0);
        dto.setPageCount(null);
        dto.setFileSize(0);
        job.sendProgress(dto);
    }

    private void sendQueuedFinish(PdfJob job) {
        ProgressDto dto = new ProgressDto();
        dto.setProgressId(job.progressId);
        dto.setCategory(ProgressDto.Category.queued);
        dto.setMessage("Finished Queue");
        dto.setCurrent(1);
        dto.setTotal(1);
        dto.setPageCount(null);
        dto.setFileSize(0);
        job.sendProgress(dto);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<ProgressDto> getProgressHistory(String progressId) {
        PdfJob job;
        if (this.allJobs.containsKey(progressId) && (job = this.allJobs.get(progressId)) != null) {
            List<ProgressDto> list = job.progressHistory;
            synchronized (list) {
                return new ArrayList<ProgressDto>(job.progressHistory);
            }
        }
        return null;
    }

    private String computeRequestHash(CreatePdfDto dto) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(dto);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

