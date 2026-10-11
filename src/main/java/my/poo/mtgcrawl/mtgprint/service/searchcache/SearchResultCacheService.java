package my.poo.mtgcrawl.mtgprint.service.searchcache;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Merkt sich Suchergebnisse, die ein Frontend gefunden hat, als JSON-Dateien auf der Platte,
 * damit andere (oder dasselbe) Frontend dieselbe Suche nicht noch einmal ins Netz schicken muss.
 *
 * Schlüssel ist die Config (Id bzw. Name), ein Hash ihrer Einstellungen, den das Frontend
 * mitschickt (geänderte Config = neuer Eintrag), und der normalisierte Suchbegriff.
 * Ablage: {dir}/{config}/{2 Zeichen}/{sha256}.json. Nichts liegt im Speicher.
 * Einträge gelten searchcache.retention-days Tage, ein nächtlicher Job löscht ältere Dateien.
 */
@Service
public class SearchResultCacheService {

    private static final Logger log = LoggerFactory.getLogger(SearchResultCacheService.class);
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;
    private static final String SUFFIX = ".json";
    private static final int MAX_KEY_PART = 200;

    /** Ein Treffer aus dem Cache: das Ergebnis so, wie das Frontend es gepostet hat. */
    public record CachedSearch(String config, String configHash, String term, long createdAt, JsonNode result) {
    }

    private final Path cacheDir;
    private final int retentionDays;
    private final long maxEntryBytes;
    private final boolean enabled;
    private final ObjectMapper objectMapper;

    @Autowired
    public SearchResultCacheService(
            ObjectMapper objectMapper,
            @Value("${searchcache.enabled:true}") boolean enabled,
            @Value("${searchcache.dir:search-cache}") String dir,
            @Value("${searchcache.retention-days:90}") int retentionDays,
            @Value("${searchcache.max-entry-kb:2048}") int maxEntryKb) {
        this(objectMapper, enabled, Paths.get(dir), retentionDays, maxEntryKb);
    }

    SearchResultCacheService(ObjectMapper objectMapper, boolean enabled, Path cacheDir, int retentionDays, int maxEntryKb) {
        this.objectMapper = objectMapper;
        this.enabled = enabled && retentionDays > 0;
        this.cacheDir = cacheDir;
        this.retentionDays = Math.max(0, retentionDays);
        this.maxEntryBytes = Math.max(1, maxEntryKb) * 1024L;
        if (this.enabled) {
            try {
                Files.createDirectories(cacheDir);
            } catch (IOException e) {
                log.error("Could not create search cache dir {}", cacheDir.toAbsolutePath(), e);
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    long getMaxAgeMs() {
        return retentionDays * DAY_MS;
    }

    /** Groß-/Kleinschreibung und doppelte Leerzeichen sollen keinen eigenen Eintrag ergeben. */
    static String normalizeTerm(String term) {
        return term == null ? "" : term.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Ordnername für die Config: nur harmlose Zeichen, damit niemand aus dem Cache-Ordner herauskommt. */
    static String configFolder(String config) {
        String safe = config == null ? "" : config.replaceAll("[^A-Za-z0-9_-]", "_");
        if (safe.length() > 64) {
            safe = safe.substring(0, 64);
        }
        return safe.isEmpty() ? "_" : safe;
    }

    private static boolean validKey(String config, String configHash, String term) {
        return config != null && !config.isBlank() && config.length() <= MAX_KEY_PART
                && configHash != null && configHash.length() <= MAX_KEY_PART
                && term != null && !normalizeTerm(term).isEmpty() && term.length() <= 1000;
    }

    Path pathFor(String config, String configHash, String term) {
        String hash = sha256(config + "\n" + configHash + "\n" + normalizeTerm(term));
        return cacheDir.resolve(configFolder(config)).resolve(hash.substring(0, 2)).resolve(hash + SUFFIX);
    }

    public Optional<CachedSearch> get(String config, String configHash, String term) {
        if (!enabled || !validKey(config, configHash, term)) {
            return Optional.empty();
        }
        Path file = pathFor(config, configHash, term);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(file.toFile());
            long createdAt = node.path("createdAt").asLong(0);
            boolean sameKey = config.equals(node.path("config").asText())
                    && configHash.equals(node.path("configHash").asText())
                    && normalizeTerm(term).equals(normalizeTerm(node.path("term").asText()));
            if (!sameKey || System.currentTimeMillis() - createdAt > getMaxAgeMs() || !node.has("result")) {
                return Optional.empty();
            }
            return Optional.of(new CachedSearch(config, configHash, term, createdAt, node.get("result")));
        } catch (IOException e) {
            log.warn("Could not read search cache file {}: {}", file, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Schreibt ein Ergebnis (ersetzt einen vorhandenen Eintrag). false, wenn der Cache aus ist,
     * der Schlüssel ungültig, das Ergebnis kein JSON-Objekt oder größer als searchcache.max-entry-kb ist.
     */
    public boolean put(String config, String configHash, String term, JsonNode result) {
        if (!enabled || !validKey(config, configHash, term) || result == null || !result.isObject()) {
            return false;
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.put("config", config);
        node.put("configHash", configHash);
        node.put("term", term.trim());
        node.put("createdAt", System.currentTimeMillis());
        node.set("result", result);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(node);
        } catch (IOException e) {
            return false;
        }
        if (bytes.length > maxEntryBytes) {
            log.info("Search result for {} / {} too large for cache ({} KB)", config, term, bytes.length / 1024);
            return false;
        }
        Path file = pathFor(config, configHash, term);
        Path tmp = null;
        try {
            Files.createDirectories(file.getParent());
            // Erst in eine temporäre Datei, dann umbenennen: Leser sehen nie eine halb geschriebene Datei
            tmp = Files.createTempFile(file.getParent(), "put-", ".tmp");
            Files.write(tmp, bytes);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            log.warn("Could not write search cache file {}: {}", file, e.getMessage());
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // räumt der nächtliche Job weg
                }
            }
            return false;
        }
    }

    /** Anzahl der Einträge auf der Platte (auch noch nicht weggeräumte abgelaufene), für /api/stats. */
    public long countEntries() {
        if (!Files.isDirectory(cacheDir)) {
            return 0;
        }
        try (Stream<Path> files = Files.walk(cacheDir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(SUFFIX) && Files.isRegularFile(f)).count();
        } catch (IOException | java.io.UncheckedIOException e) {
            log.warn("Could not count search cache entries in {}: {}", cacheDir, e.getMessage());
            return 0;
        }
    }

    /** Löscht abgelaufene Einträge, liegengebliebene Temp-Dateien und leere Ordner. */
    @Scheduled(cron = "${searchcache.cleanup.cron:0 45 3 * * *}")
    public int cleanUp() {
        if (!Files.isDirectory(cacheDir)) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - (enabled ? getMaxAgeMs() : 0);
        int deleted = 0;
        try (Stream<Path> files = Files.walk(cacheDir)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                String name = file.getFileName().toString();
                if (!name.endsWith(SUFFIX) && !name.endsWith(".tmp")) {
                    continue;
                }
                try {
                    if (Files.getLastModifiedTime(file).toMillis() < cutoff && Files.deleteIfExists(file)) {
                        deleted++;
                    }
                } catch (IOException e) {
                    log.warn("Could not delete search cache file {}: {}", file, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Could not clean search cache {}: {}", cacheDir, e.getMessage());
        }
        deleteEmptyDirs(cacheDir, true);
        if (deleted > 0) {
            log.info("Deleted {} search cache entries older than {} days", deleted, retentionDays);
        }
        return deleted;
    }

    private void deleteEmptyDirs(Path dir, boolean isRoot) {
        try (DirectoryStream<Path> children = Files.newDirectoryStream(dir)) {
            for (Path child : children) {
                if (Files.isDirectory(child)) {
                    deleteEmptyDirs(child, false);
                }
            }
        } catch (IOException e) {
            return;
        }
        if (!isRoot) {
            try (Stream<Path> rest = Files.list(dir)) {
                if (rest.findAny().isEmpty()) {
                    Files.deleteIfExists(dir);
                }
            } catch (IOException ignored) {
                // gerade beschrieben, nächstes Mal
            }
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
