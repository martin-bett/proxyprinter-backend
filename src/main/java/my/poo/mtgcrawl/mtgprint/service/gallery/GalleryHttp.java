package my.poo.mtgcrawl.mtgprint.service.gallery;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Kleiner HTTP-Helfer für die Galerie-Quellen, damit die Quellen in Tests ersetzbar sind. */
@Component
public class GalleryHttp {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public HttpResponse<String> get(String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET();
        headers.forEach(builder::header);
        return send(builder.build());
    }

    public HttpResponse<String> postForm(String url, String body, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(builder::header);
        return send(builder.build());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "gallery.unreachable",
                    request.uri().getHost() + " not reachable: " + e.getMessage(),
                    Map.of("host", String.valueOf(request.uri().getHost()), "detail", String.valueOf(e.getMessage())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GallerySourceException(HttpStatus.SERVICE_UNAVAILABLE, "gallery.cancelled", "Cancelled", Map.of());
        }
    }
}
