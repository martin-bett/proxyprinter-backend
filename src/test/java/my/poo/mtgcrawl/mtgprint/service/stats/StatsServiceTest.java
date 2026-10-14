package my.poo.mtgcrawl.mtgprint.service.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class StatsServiceTest {

    @Test
    void cachesFileCountsButReadsDeckCountsFresh() {
        AtomicLong now = new AtomicLong(1_000_000);
        AtomicLong searches = new AtomicLong(15152);
        AtomicLong images = new AtomicLong(1241);
        UsageStatsRepository decks = mock(UsageStatsRepository.class);
        when(decks.today(UsageMetric.DECKS)).thenReturn(3L, 4L);
        when(decks.total(UsageMetric.DECKS)).thenReturn(30L, 31L);
        StatsService service = new StatsService(searches::get, images::get, () -> 5, decks, now::get);

        Map<String, Long> first = service.stats();
        assertEquals(List.of("sharedSearches", "tempImages", "cachedPdfs", "decksToday", "decksTotal"),
                List.copyOf(first.keySet()));
        assertEquals(15152L, first.get("sharedSearches"));
        assertEquals(1241L, first.get("tempImages"));
        assertEquals(5L, first.get("cachedPdfs"));
        assertEquals(3L, first.get("decksToday"));
        assertEquals(30L, first.get("decksTotal"));

        searches.set(1);
        images.set(2);
        now.addAndGet(StatsService.FILE_COUNT_TTL_MS);
        Map<String, Long> second = service.stats();
        assertEquals(15152L, second.get("sharedSearches"));
        assertEquals(1241L, second.get("tempImages"));
        assertEquals(4L, second.get("decksToday"));
        assertEquals(31L, second.get("decksTotal"));

        now.addAndGet(1);
        Map<String, Long> third = service.stats();
        assertEquals(1L, third.get("sharedSearches"));
        assertEquals(2L, third.get("tempImages"));
    }
}
