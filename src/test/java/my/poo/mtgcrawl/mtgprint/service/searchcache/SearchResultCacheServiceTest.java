package my.poo.mtgcrawl.mtgprint.service.searchcache;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class SearchResultCacheServiceTest {

    private static final long DAY_MS = 24 * 60 * 60 * 1000L;
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path dir;

    private SearchResultCacheService cache(int days) {
        return new SearchResultCacheService(mapper, true, dir, days, 64);
    }

    private JsonNode result(String name) throws Exception {
        return mapper.readTree("{\"results\":[{\"name\":\"" + name + "\",\"apiName\":\"scryfall\",\"imageUrl\":\"https://x/1.jpg\"}],\"rawResponse\":\"{}\"}");
    }

    @Test
    void storesAndFindsResultsAsJsonFiles() throws Exception {
        SearchResultCacheService cache = cache(7);
        assertTrue(cache.put("12", "abc", "Lightning Bolt", result("Lightning Bolt")));

        // Schreibweise des Suchbegriffs spielt keine Rolle
        SearchResultCacheService.CachedSearch hit = cache.get("12", "abc", "  lightning   BOLT ").orElseThrow();
        assertEquals("Lightning Bolt", hit.result().path("results").get(0).path("name").asText());

        Path file = cache.pathFor("12", "abc", "Lightning Bolt");
        assertTrue(Files.isRegularFile(file));
        assertEquals(dir.resolve("12"), file.getParent().getParent());
    }

    @Test
    void changedConfigOrOtherTermIsAMiss() throws Exception {
        SearchResultCacheService cache = cache(7);
        cache.put("12", "abc", "Lightning Bolt", result("Lightning Bolt"));

        assertTrue(cache.get("12", "def", "Lightning Bolt").isEmpty());
        assertTrue(cache.get("13", "abc", "Lightning Bolt").isEmpty());
        assertTrue(cache.get("12", "abc", "Shock").isEmpty());
    }

    @Test
    void expiredEntriesAreIgnoredAndCleanedUp() throws Exception {
        SearchResultCacheService cache = cache(7);
        cache.put("12", "abc", "Old", result("Old"));
        cache.put("12", "abc", "New", result("New"));
        Path old = cache.pathFor("12", "abc", "Old");
        // createdAt in der Datei und Änderungszeit auf 8 Tage zurück
        long past = System.currentTimeMillis() - 8 * DAY_MS;
        Files.writeString(old, Files.readString(old).replaceAll("\"createdAt\":\\d+", "\"createdAt\":" + past));
        assertTrue(old.toFile().setLastModified(past));

        assertTrue(cache.get("12", "abc", "Old").isEmpty());
        assertEquals(1, cache.cleanUp());
        assertFalse(Files.exists(old));
        assertTrue(cache.get("12", "abc", "New").isPresent());
    }

    @Test
    void rejectsTooLargeOrInvalidEntries() throws Exception {
        SearchResultCacheService cache = cache(7);
        String big = "x".repeat(70 * 1024);
        assertFalse(cache.put("12", "abc", "Big", mapper.readTree("{\"rawResponse\":\"" + big + "\"}")));
        assertFalse(cache.put("12", "abc", "  ", result("x")));
        assertFalse(cache.put("12", "abc", "Array", mapper.readTree("[1]")));
        assertFalse(cache.put("", "abc", "Shock", result("Shock")));
        try (Stream<Path> files = Files.walk(dir)) {
            assertEquals(0, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    void configNameCannotEscapeTheCacheDir() throws Exception {
        SearchResultCacheService cache = cache(7);
        assertTrue(cache.put("../../etc", "abc", "Shock", result("Shock")));
        assertTrue(cache.pathFor("../../etc", "abc", "Shock").normalize().startsWith(dir));
        assertEquals("______etc", SearchResultCacheService.configFolder("../../etc"));
    }

    @Test
    void disabledCacheStoresNothing() throws Exception {
        SearchResultCacheService cache = new SearchResultCacheService(mapper, false, dir, 7, 64);
        assertFalse(cache.put("12", "abc", "Shock", result("Shock")));
        assertTrue(cache.get("12", "abc", "Shock").isEmpty());
    }
}
