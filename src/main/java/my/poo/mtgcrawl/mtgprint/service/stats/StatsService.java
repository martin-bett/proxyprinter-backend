package my.poo.mtgcrawl.mtgprint.service.stats;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;
import my.poo.mtgcrawl.mtgprint.service.searchcache.SearchResultCacheService;

/**
 * Aggregierte Zahlen für GET /api/stats (keine Nutzerdaten). Dateien zählen kostet einen Gang über die Ordner,
 * deshalb gelten diese Zahlen {@link #FILE_COUNT_TTL_MS} lang; die Deck-Zähler kommen jedes Mal frisch aus der DB,
 * damit ein gerade erzeugtes Deck sofort mitzählt.
 */
@Service
public class StatsService {

    static final long FILE_COUNT_TTL_MS = 60_000L;

    private final LongSupplier sharedSearches;
    private final LongSupplier tempImages;
    private final LongSupplier cachedPdfs;
    private final DeckStatsRepository deckStats;
    private final LongSupplier clock;

    private long fileCountsAt = Long.MIN_VALUE;
    private long sharedSearchCount;
    private long tempImageCount;

    @Autowired
    public StatsService(SearchResultCacheService searchCache, FileUploadService uploads, PdfCacheService pdfCache,
            DeckStatsRepository deckStats) {
        this(searchCache::countEntries, uploads::countTempFiles, pdfCache::getCachedPdfCount, deckStats,
                System::currentTimeMillis);
    }

    StatsService(LongSupplier sharedSearches, LongSupplier tempImages, LongSupplier cachedPdfs,
            DeckStatsRepository deckStats, LongSupplier clock) {
        this.sharedSearches = sharedSearches;
        this.tempImages = tempImages;
        this.cachedPdfs = cachedPdfs;
        this.deckStats = deckStats;
        this.clock = clock;
    }

    public Map<String, Long> stats() {
        Map<String, Long> result = new LinkedHashMap<>();
        synchronized (this) {
            long now = clock.getAsLong();
            if (fileCountsAt == Long.MIN_VALUE || now - fileCountsAt > FILE_COUNT_TTL_MS) {
                sharedSearchCount = sharedSearches.getAsLong();
                tempImageCount = tempImages.getAsLong();
                fileCountsAt = now;
            }
            result.put("sharedSearches", sharedSearchCount);
            result.put("tempImages", tempImageCount);
        }
        result.put("cachedPdfs", cachedPdfs.getAsLong());
        result.put("decksToday", deckStats.createdToday());
        result.put("decksTotal", deckStats.createdTotal());
        return result;
    }
}
