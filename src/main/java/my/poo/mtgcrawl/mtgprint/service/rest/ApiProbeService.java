package my.poo.mtgcrawl.mtgprint.service.rest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.service.net.BlockedUrlException;
import my.poo.mtgcrawl.mtgprint.service.net.GuardedHttp;
import my.poo.mtgcrawl.mtgprint.service.net.OutboundUrlGuard;

/**
 * Testabfrage für den API-Wizard: schickt eine Anfrage an eine fremde API und gibt
 * Status, Content-Type und Rohantwort zurück, auch bei HTTP-Fehlern, damit der Wizard
 * zeigen kann, was die Quelle wirklich antwortet.
 */
@Service
public class ApiProbeService {

    static final int MAX_BODY_BYTES = 3 * 1024 * 1024;

    private final HttpClient client;
    private final OutboundUrlGuard guard;

    @Autowired
    public ApiProbeService(OutboundUrlGuard guard) {
        this(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .proxy(ProxySelector.getDefault())
                .build(), guard);
    }

    ApiProbeService(HttpClient client, OutboundUrlGuard guard) {
        this.client = client;
        this.guard = guard;
    }

    public record ProbeRequest(String url, String method, String body, String requestFormat) {
    }

    public record ProbeResult(int status, String contentType, String finalUrl, String body, boolean truncated,
            String error) {
    }

    public ProbeResult probe(ProbeRequest req) {
        String url = req.url() == null ? "" : req.url().trim();
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            return error("URL muss mit http:// oder https:// beginnen");
        }
        boolean post = "POST".equalsIgnoreCase(req.method());
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json, text/html;q=0.9, */*;q=0.8")
                    .header("User-Agent", "Mozilla/5.0 (compatible; proxyprinter.eu api wizard)");
            if (post) {
                builder.header("Content-Type", contentType(req.requestFormat()));
                builder.POST(HttpRequest.BodyPublishers.ofString(req.body() == null ? "" : req.body()));
            } else {
                builder.GET();
            }
            HttpResponse<InputStream> response = GuardedHttp.send(client, guard, builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            String type = response.headers().firstValue("Content-Type").orElse("");
            byte[] bytes;
            boolean truncated;
            try (InputStream in = response.body()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1 && out.size() < MAX_BODY_BYTES) {
                    out.write(buf, 0, Math.min(n, MAX_BODY_BYTES - out.size()));
                }
                truncated = n != -1;
                bytes = out.toByteArray();
            }
            return new ProbeResult(response.statusCode(), type, response.uri().toString(),
                    new String(bytes, charset(type)), truncated, null);
        } catch (BlockedUrlException e) {
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return error("Ungültige URL: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return error("abgebrochen");
        } catch (Exception e) {
            return error(e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : ""));
        }
    }

    private static ProbeResult error(String message) {
        return new ProbeResult(0, null, null, null, false, message);
    }

    static String contentType(String requestFormat) {
        if ("json".equalsIgnoreCase(requestFormat)) {
            return "application/json";
        }
        return "application/x-www-form-urlencoded";
    }

    private static Charset charset(String contentType) {
        String lower = contentType.toLowerCase(Locale.ROOT);
        int i = lower.indexOf("charset=");
        if (i >= 0) {
            String name = contentType.substring(i + 8).split("[;\\s]")[0].replace("\"", "");
            try {
                return Charset.forName(name);
            } catch (Exception ignored) {
                // unbekannter Zeichensatz: UTF-8 versuchen
            }
        }
        return StandardCharsets.UTF_8;
    }
}
