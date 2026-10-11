package my.poo.mtgcrawl.mtgprint.service.admin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.service.deckfinder.DeckFinderFetchService;
import my.poo.mtgcrawl.mtgprint.service.pdf.BufferedImageCacheService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PDFGenerationManagerService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;
import my.poo.mtgcrawl.mtgprint.service.rest.ImageFetchService;
import my.poo.mtgcrawl.mtgprint.service.searchcache.SearchResultCacheService;

/** Übersicht (Anzahl, Größe, ältester Eintrag) und "Jetzt leeren" für die Caches des Backends. Keine Inhalte. */
@Service
public class CacheAdminService {

    private static final Logger log = LoggerFactory.getLogger(CacheAdminService.class);

    public static final String SEARCH_CACHE = "searchCache";
    public static final String PDFS = "pdfs";
    public static final String UPLOADS = "uploads";
    public static final String IMAGES = "images";
    public static final String MEMORY = "memory";

    /**
     * Ein Cache, wie die Admin-Seite ihn zeigt. bytes und oldest fehlen (-1 / null) bei Caches im Speicher;
     * setting ist die Einstellung für die Laufzeit (null = feste Laufzeit oder unbegrenzt).
     */
    public record CacheView(String name, long entries, long bytes, Long oldest, String setting) {
    }

    record DirStats(long files, long bytes, Long oldest) {
    }

    private final SearchResultCacheService searchCache;
    private final PdfCacheService pdfCache;
    private final PDFGenerationManagerService pdfJobs;
    private final FileUploadService uploads;
    private final FileDownloadService downloads;
    private final BufferedImageCacheService bufferedImages;
    private final ImageFetchService imageFetch;
    private final DeckFinderFetchService deckFinder;

    public CacheAdminService(SearchResultCacheService searchCache, PdfCacheService pdfCache,
            PDFGenerationManagerService pdfJobs, FileUploadService uploads, FileDownloadService downloads,
            BufferedImageCacheService bufferedImages, ImageFetchService imageFetch, DeckFinderFetchService deckFinder) {
        this.searchCache = searchCache;
        this.pdfCache = pdfCache;
        this.pdfJobs = pdfJobs;
        this.uploads = uploads;
        this.downloads = downloads;
        this.bufferedImages = bufferedImages;
        this.imageFetch = imageFetch;
        this.deckFinder = deckFinder;
    }

    public List<CacheView> list() {
        List<CacheView> result = new ArrayList<>();
        result.add(fromDir(SEARCH_CACHE, searchCache.getCacheDir(), ".json", "searchcache.retention-days"));
        result.add(fromDir(PDFS, pdfCache.getCacheDir(), ".pdf", "pdf.retention-days"));
        result.add(fromDir(UPLOADS, Paths.get(FileUploadService.IMG_TEMP_FOLDER), null, "upload.retention-days"));
        result.add(fromDir(IMAGES, FileDownloadService.cacheDir(), null, null));
        long memory = (long) bufferedImages.size() + imageFetch.failureCount() + deckFinder.cacheSize();
        result.add(new CacheView(MEMORY, memory, -1, null, null));
        return result;
    }

    /** Leert einen Cache sofort und gibt die Anzahl gelöschter Einträge zurück. */
    public int clear(String name, String admin) {
        int deleted = switch (name) {
            case SEARCH_CACHE -> searchCache.clear();
            case PDFS -> {
                requireNoPdfJobs();
                int n = pdfCache.clear();
                pdfJobs.forgetFinishedJobs();
                yield n;
            }
            case UPLOADS -> uploads.clear();
            case IMAGES -> {
                // Laufende PDFs lesen Bilder aus diesem Ordner
                requireNoPdfJobs();
                yield downloads.clear() + bufferedImages.clear();
            }
            case MEMORY -> bufferedImages.clear() + imageFetch.clearFailures() + deckFinder.clearCache();
            default -> throw new AdminException(HttpStatus.NOT_FOUND, "admin.unknownCache", "Unknown cache.",
                    Map.of("name", String.valueOf(name)));
        };
        log.info("Admin {} cleared cache {} ({} entries)", admin, name, deleted);
        return deleted;
    }

    private void requireNoPdfJobs() {
        int running = pdfJobs.getActiveJobCount() + pdfJobs.getQueueSize();
        if (running > 0) {
            throw new AdminException(HttpStatus.CONFLICT, "admin.pdfJobsRunning",
                    "PDFs are being generated right now, try again later.", Map.of("jobs", running));
        }
    }

    private static CacheView fromDir(String name, Path dir, String suffix, String setting) {
        DirStats stats = dirStats(dir, suffix);
        return new CacheView(name, stats.files(), stats.bytes(), stats.oldest(), setting);
    }

    static DirStats dirStats(Path dir, String suffix) {
        if (!Files.isDirectory(dir)) {
            return new DirStats(0, 0, null);
        }
        long files = 0;
        long bytes = 0;
        Long oldest = null;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                if (!Files.isRegularFile(p) || (suffix != null && !p.getFileName().toString().endsWith(suffix))) {
                    continue;
                }
                try {
                    long size = Files.size(p);
                    long modified = Files.getLastModifiedTime(p).toMillis();
                    files++;
                    bytes += size;
                    oldest = oldest == null ? modified : Math.min(oldest, modified);
                } catch (IOException e) {
                    // gerade gelöscht
                }
            }
        } catch (IOException | UncheckedIOException e) {
            log.warn("Could not read {}: {}", dir, e.getMessage());
        }
        return new DirStats(files, bytes, oldest);
    }
}
