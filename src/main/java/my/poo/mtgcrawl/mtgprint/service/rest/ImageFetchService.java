package my.poo.mtgcrawl.mtgprint.service.rest;

import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.service.net.BlockedUrlException;
import my.poo.mtgcrawl.mtgprint.service.net.GuardedHttp;
import my.poo.mtgcrawl.mtgprint.service.net.OutboundUrlGuard;

import lombok.extern.slf4j.Slf4j;

/**
 * Lädt Bilder für /api/proxy/image. Folgt Umleitungen (auch http→https und 308), jedes Ziel geprüft
 * vom OutboundUrlGuard, prüft, dass wirklich ein Bild zurückkommt, und merkt sich
 * fehlgeschlagene URLs eine Weile, damit eine Quelle nicht wiederholt angefragt wird.
 */
@Service
@Slf4j
public class ImageFetchService {

    static final long FAILURE_TTL_MS = TimeUnit.MINUTES.toMillis(30);
    private static final int MAX_FAILURES = 10_000;

    private final HttpClient client;
    private final OutboundUrlGuard guard;
    private final Tika tika = new Tika();
    private final Map<String, Failure> failures = new ConcurrentHashMap<>();

    public ImageFetchService(OutboundUrlGuard guard) {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .proxy(ProxySelector.getDefault())
                .build(), guard);
    }

    ImageFetchService(HttpClient client, OutboundUrlGuard guard) {
        this.client = client;
        this.guard = guard;
    }

    public record Image(byte[] content, String contentType) {
    }

    public record Failure(String reason, long at) {
    }

    public static class ImageFetchException extends RuntimeException {
        public ImageFetchException(String reason) {
            super(reason);
        }
    }

    /** Letzter Fehler für diese URL, falls er noch nicht abgelaufen ist. */
    public Failure recentFailure(String url) {
        Failure failure = failures.get(url);
        if (failure == null) return null;
        if (System.currentTimeMillis() - failure.at() > FAILURE_TTL_MS) {
            failures.remove(url);
            return null;
        }
        return failure;
    }

    public Image fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (compatible; proxyprinter.eu image proxy)")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = GuardedHttp.send(client, guard, request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                String location = response.headers().firstValue("Location").map(l -> " -> " + l).orElse("");
                throw fail(url, "HTTP " + status + location);
            }
            byte[] body = response.body();
            if (body == null || body.length == 0) {
                throw fail(url, "leere Antwort");
            }
            String contentType = tika.detect(body);
            if (!contentType.startsWith("image/")) {
                throw fail(url, "kein Bild (" + contentType + ") von " + response.uri().getHost());
            }
            failures.remove(url);
            return new Image(body, contentType);
        } catch (ImageFetchException e) {
            throw e;
        } catch (BlockedUrlException e) {
            throw fail(url, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw fail(url, "abgebrochen");
        } catch (Exception e) {
            throw fail(url, e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
        }
    }

    private ImageFetchException fail(String url, String reason) {
        log.warn("Bild konnte nicht geladen werden: {} ({})", url, reason);
        if (failures.size() >= MAX_FAILURES) {
            failures.clear();
        }
        failures.put(url, new Failure(reason, System.currentTimeMillis()));
        return new ImageFetchException(reason);
    }
}
