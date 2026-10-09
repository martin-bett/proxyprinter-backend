package my.poo.mtgcrawl.mtgprint.service.deckfinder;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.entitiy.DeckFinderConfig;

/**
 * Lädt Decks und Suchergebnisse für eine Deckfinder-Config. Die Antwort geht unverändert ans
 * Frontend, das die Felder liest. Abgerufen wird nur die URL aus der Config, kurz gecacht,
 * damit wiederholte Klicks die Seite nicht mehrfach treffen.
 */
@Service
@Slf4j
public class DeckFinderFetchService {

    static final Duration CACHE_TTL = Duration.ofMinutes(10);
    static final int CACHE_SIZE = 200;
    private static final Pattern PLAIN_ID = Pattern.compile("[\\w-]+");

    public record DeckFinderResponse(String contentType, String body) {}

    private record CacheEntry(DeckFinderResponse response, Instant expires) {}

    private final DeckFinderHttp http;
    private final Clock clock;
    private final Map<String, CacheEntry> cache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    @Autowired
    public DeckFinderFetchService(DeckFinderHttp http) {
        this(http, Clock.systemUTC());
    }

    DeckFinderFetchService(DeckFinderHttp http, Clock clock) {
        this.http = http;
        this.clock = clock;
    }

    public DeckFinderResponse fetchDeck(DeckFinderConfig config, String input) {
        if (isBlank(config.getFetchUrl())) {
            throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.noFetchUrl", "The config has no fetch URL.", Map.of());
        }
        String id = resolveDeckId(config.getLinkPattern(), input);
        String url = fillTemplate(config.getFetchUrl(), Map.of("id", id));
        String contentType = "text".equalsIgnoreCase(config.getDataFormat()) ? "text/plain" : "application/json";
        return load(url, contentType, "Deck");
    }

    public DeckFinderResponse search(DeckFinderConfig config, String query, int page) {
        if (isBlank(config.getSearchUrl())) {
            throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.noNameSearch", "This source does not support search by name.", Map.of());
        }
        if (isBlank(query)) {
            throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.nameRequired", "Please enter a name.", Map.of());
        }
        String url = fillTemplate(config.getSearchUrl(), Map.of("query", query.trim(), "page", String.valueOf(Math.max(1, page))));
        return load(url, "application/json", "Search");
    }

    /** Liest die Deck-ID aus einem Link (Gruppe "id" der Regex) oder nimmt die Eingabe selbst als ID. */
    static String resolveDeckId(String linkPattern, String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.deckRequired", "Please enter a deck link or ID.", Map.of());
        }
        if (!isBlank(linkPattern)) {
            try {
                Matcher m = Pattern.compile(linkPattern).matcher(value);
                if (m.find()) {
                    return m.group("id");
                }
            } catch (PatternSyntaxException e) {
                throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.invalidLinkPattern",
                        "Link Pattern is not a valid regex: " + e.getDescription(), Map.of("description", e.getDescription()));
            } catch (IllegalArgumentException e) {
                throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.linkPatternNoGroup", "Link Pattern needs a group (?<id>...).", Map.of());
            }
        }
        if (PLAIN_ID.matcher(value).matches()) {
            return value;
        }
        throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.linkMismatch", "Link does not match this source.", Map.of());
    }

    /** Ersetzt {name} durch den URL-kodierten Wert. */
    static String fillTemplate(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            String encoded = URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8).replace("+", "%20");
            result = result.replace("{" + e.getKey() + "}", encoded);
        }
        return result;
    }

    private DeckFinderResponse load(String url, String contentType, String what) {
        Instant now = clock.instant();
        synchronized (cache) {
            CacheEntry hit = cache.get(url);
            if (hit != null && hit.expires().isAfter(now)) {
                return hit.response();
            }
        }
        URI uri = checked(url);
        HttpResponse<String> response = http.get(uri);
        int status = response.statusCode();
        if (status == 404 || status == 410) {
            throw new DeckFinderException(HttpStatus.NOT_FOUND, "Deck".equals(what) ? "deckfinder.deckNotFound" : "deckfinder.searchNotFound",
                    what + " not found (private or deleted?).", Map.of());
        }
        if (status == 401 || status == 403 || status == 429 || status == 503) {
            throw new DeckFinderException(HttpStatus.BAD_GATEWAY, "deckfinder.blocked",
                    uri.getHost() + " blocks the request (HTTP " + status + ").",
                    Map.of("host", uri.getHost(), "status", String.valueOf(status)));
        }
        if (status >= 400) {
            throw new DeckFinderException(HttpStatus.BAD_GATEWAY, "deckfinder.httpError",
                    uri.getHost() + " responded with HTTP " + status + ".",
                    Map.of("host", uri.getHost(), "status", String.valueOf(status)));
        }
        DeckFinderResponse result = new DeckFinderResponse(contentType, response.body());
        synchronized (cache) {
            cache.put(url, new CacheEntry(result, now.plus(CACHE_TTL)));
        }
        return result;
    }

    URI checked(String url) {
        return checkedUri(url);
    }

    /** Nur http(s) und keine internen Adressen, damit die Config nicht ins eigene Netz zeigt. */
    public static URI checkedUri(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.invalidUrl", "Invalid URL: " + url, Map.of("url", url));
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null || scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.httpOnly", "Only http(s) URLs are allowed: " + url, Map.of("url", url));
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                        || address.isAnyLocalAddress()) {
                    throw new DeckFinderException(HttpStatus.BAD_REQUEST, "deckfinder.internalAddress", "Internal addresses are not allowed.", Map.of());
                }
            }
        } catch (UnknownHostException e) {
            throw new DeckFinderException(HttpStatus.BAD_GATEWAY, "deckfinder.hostNotFound", uri.getHost() + " not found.", Map.of("host", uri.getHost()));
        }
        return uri;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
