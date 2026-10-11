package my.poo.mtgcrawl.mtgprint.resource;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.service.searchcache.SearchResultCacheService;
import my.poo.mtgcrawl.mtgprint.service.searchcache.SearchResultCacheService.CachedSearch;
import my.poo.mtgcrawl.mtgprint.service.stats.UsageMetric;
import my.poo.mtgcrawl.mtgprint.service.stats.UsageTracker;

/**
 * Gemeinsamer Such-Cache für alle Frontends. Das Frontend fragt vor einer Suche hier nach
 * (lookup) und postet danach, was es im Netz gefunden hat (store).
 */
@RestController
@RequestMapping("/api/search-cache")
@AllArgsConstructor
public class SearchCacheController {

    /** Höchstens so viele Einträge pro Request, damit Antworten nicht riesig werden. */
    static final int MAX_LOOKUP = 100;
    static final int MAX_STORE = 50;

    public record CacheKey(String config, String configHash, String term) {
    }

    public record LookupRequest(List<CacheKey> entries) {
    }

    public record LookupResponse(List<CachedSearch> hits) {
    }

    public record StoreEntry(String config, String configHash, String term, JsonNode result) {
    }

    public record StoreRequest(List<StoreEntry> entries) {
    }

    private final SearchResultCacheService cache;
    private final UsageTracker usage;

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of("enabled", cache.isEnabled(), "retentionDays", cache.getRetentionDays());
    }

    /** Liefert nur die Treffer; was fehlt, sucht das Frontend selbst. */
    @PostMapping("/lookup")
    public ResponseEntity<LookupResponse> lookup(@RequestBody LookupRequest request) {
        List<CacheKey> keys = request == null || request.entries() == null ? List.of() : request.entries();
        if (keys.size() > MAX_LOOKUP) {
            return ResponseEntity.badRequest().build();
        }
        usage.count(UsageMetric.SEARCH_TERMS, keys.size());
        List<CachedSearch> hits = keys.stream()
                .filter(Objects::nonNull)
                .map(key -> cache.get(key.config(), key.configHash(), key.term()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.ok(new LookupResponse(hits));
    }

    @PutMapping
    public ResponseEntity<Map<String, Integer>> store(@RequestBody StoreRequest request) {
        List<StoreEntry> entries = request == null || request.entries() == null ? List.of() : request.entries();
        if (entries.size() > MAX_STORE) {
            return ResponseEntity.badRequest().build();
        }
        int stored = 0;
        for (StoreEntry entry : entries) {
            if (entry != null && cache.put(entry.config(), entry.configHash(), entry.term(), entry.result())) {
                stored++;
            }
        }
        return ResponseEntity.ok(Map.of("stored", stored));
    }
}
