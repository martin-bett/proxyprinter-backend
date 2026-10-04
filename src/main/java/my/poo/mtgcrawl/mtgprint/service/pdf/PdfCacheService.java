package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Hält fertige PDFs eine Zeit lang (pdf.retention-days, Standard 7 Tage) auf der Platte,
 * damit derselbe Request nicht neu gebaut werden muss und der Download wiederholt werden
 * kann. Danach werden sie sicher gelöscht, auch über Neustarts hinweg: Beim Start wird
 * der Index aus dem Ordner aufgebaut, und das Aufräumen prüft jede Datei im Ordner.
 */
@Service
public class PdfCacheService {

    private static final Logger log = LoggerFactory.getLogger(PdfCacheService.class);
    private static final String PDF_CACHE_PATH = "pdf/cache/";
    private static final String SUFFIX = ".pdf";
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    /** Größe und Zeitpunkte eines gecachten PDFs, für GET /api/pdf/{id}/info. */
    public record PdfInfo(long fileSize, long createdAt, long expiresAt) {
    }

    private final Path cacheDir;
    private final int retentionDays;
    private final Map<String, Long> timeStampMap = new ConcurrentHashMap<>();

    @Autowired
    public PdfCacheService(@Value("${pdf.retention-days:7}") int retentionDays) {
        this(Paths.get(PDF_CACHE_PATH), retentionDays);
    }

    PdfCacheService(Path cacheDir, int retentionDays) {
        this.cacheDir = cacheDir;
        this.retentionDays = Math.max(1, retentionDays);
        File dir = cacheDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        loadIndexFromDisk();
        cleanUpCache();
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public long getMaxAgeMs() {
        return retentionDays * DAY_MS;
    }

    private Path pathFor(String progressId) {
        return cacheDir.resolve(progressId + SUFFIX);
    }

    /** Nach einem Neustart sind die PDFs noch auf der Platte; ohne Index wären sie weder abrufbar noch würden sie gelöscht. */
    private void loadIndexFromDisk() {
        File[] files = cacheDir.toFile().listFiles((d, name) -> name.endsWith(SUFFIX));
        if (files == null) {
            return;
        }
        for (File f : files) {
            String progressId = f.getName().substring(0, f.getName().length() - SUFFIX.length());
            timeStampMap.putIfAbsent(progressId, f.lastModified());
        }
        if (files.length > 0) {
            log.info("Loaded " + files.length + " cached pdfs from " + cacheDir);
        }
    }

    public void addToCache(String progressId, byte[] pdfContent) {
        try {
            Files.write(pathFor(progressId), pdfContent);
            timeStampMap.put(progressId, System.currentTimeMillis());
        } catch (Exception e) {
            log.error("Could not cache pdf " + progressId, e);
        }
    }

    /**
     * Wird dasselbe PDF erneut angefordert, beginnt die Aufbewahrungszeit von vorn,
     * so als wäre es gerade neu erzeugt worden.
     */
    public void refresh(String progressId) {
        if (!isCached(progressId)) {
            return;
        }
        long now = System.currentTimeMillis();
        timeStampMap.put(progressId, now);
        if (!pathFor(progressId).toFile().setLastModified(now)) {
            log.warn("Could not refresh timestamp of cached pdf " + progressId);
        }
    }

    public byte[] getFromCache(String progressId) {
        if (!isCached(progressId)) {
            return null;
        }
        try {
            return Files.readAllBytes(pathFor(progressId));
        } catch (Exception e) {
            log.warn("Could not read cached pdf " + progressId + ": " + e.getMessage());
            return null;
        }
    }

    public boolean isCached(String progressId) {
        Long createdAt = timeStampMap.get(progressId);
        return createdAt != null && System.currentTimeMillis() - createdAt <= getMaxAgeMs();
    }

    public Optional<PdfInfo> getInfo(String progressId) {
        if (!isCached(progressId)) {
            return Optional.empty();
        }
        long createdAt = timeStampMap.get(progressId);
        try {
            long size = Files.size(pathFor(progressId));
            return Optional.of(new PdfInfo(size, createdAt, createdAt + getMaxAgeMs()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Scheduled(fixedRate = 60 * 60 * 1000L)
    public void cleanUpCache() {
        long now = System.currentTimeMillis();
        long maxAge = getMaxAgeMs();
        timeStampMap.entrySet().removeIf(entry -> {
            if (now - entry.getValue() <= maxAge) {
                return false;
            }
            try {
                Files.deleteIfExists(pathFor(entry.getKey()));
            } catch (Exception e) {
                log.warn("Could not delete cached pdf " + entry.getKey() + ": " + e.getMessage());
            }
            return true;
        });
        // Dateien, die nicht (mehr) im Index stehen, z.B. von abgebrochenen Schreibvorgängen
        File[] files = cacheDir.toFile().listFiles((d, name) -> name.endsWith(SUFFIX));
        if (files == null) {
            return;
        }
        for (File f : files) {
            if (now - f.lastModified() > maxAge && !f.delete()) {
                log.warn("Could not delete expired pdf file " + f.getName());
            }
        }
    }

    public int getCachedPdfCount() {
        return timeStampMap.size();
    }
}
