package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Hält fertige PDFs 24h auf der Platte, damit derselbe Request nicht neu gebaut
 * werden muss und der Download wiederholt werden kann.
 */
@Service
public class PdfCacheService {

    private static final Logger log = LoggerFactory.getLogger(PdfCacheService.class);
    private static final String PDF_CACHE_PATH = "pdf/cache/";
    static final long MAX_AGE_MS = 24 * 60 * 60 * 1000L;

    private final Map<String, Long> timeStampMap = new ConcurrentHashMap<>();

    public PdfCacheService() {
        File cacheDir = new File(PDF_CACHE_PATH);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
    }

    private Path pathFor(String progressId) {
        return Paths.get(PDF_CACHE_PATH, progressId + ".pdf");
    }

    public void addToCache(String progressId, byte[] pdfContent) {
        try {
            Files.write(pathFor(progressId), pdfContent);
            timeStampMap.put(progressId, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("Could not cache pdf " + progressId, e);
        }
    }

    public byte[] getFromCache(String progressId) {
        if (!timeStampMap.containsKey(progressId)) {
            return null;
        }
        try {
            return Files.readAllBytes(pathFor(progressId));
        } catch (Exception e) {
            log.warn("Could not read cached pdf " + progressId + ": " + e.getMessage());
            return null;
        }
    }

    @Scheduled(fixedRate = 60 * 60 * 1000L)
    public void cleanUpCache() {
        long now = System.currentTimeMillis();
        timeStampMap.entrySet().removeIf(entry -> {
            if (now - entry.getValue() <= MAX_AGE_MS) {
                return false;
            }
            try {
                Files.deleteIfExists(pathFor(entry.getKey()));
            } catch (Exception e) {
                log.warn("Could not delete cached pdf " + entry.getKey() + ": " + e.getMessage());
            }
            return true;
        });
    }

    public int getCachedPdfCount() {
        return timeStampMap.size();
    }
}
