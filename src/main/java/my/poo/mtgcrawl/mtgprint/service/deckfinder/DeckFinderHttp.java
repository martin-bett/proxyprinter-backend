package my.poo.mtgcrawl.mtgprint.service.deckfinder;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** HTTP-Abruf für den Deckfinder, eigene Klasse, damit Tests ihn ersetzen können. */
@Component
public class DeckFinderHttp {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final String userAgent;

    public DeckFinderHttp(@Value("${deckfinder.user-agent:proxyprinter.eu deckfinder (+https://proxyprinter.eu)}") String userAgent) {
        this.userAgent = userAgent;
    }

    public HttpResponse<String> get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json, text/plain, */*")
                .GET()
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new DeckFinderException(HttpStatus.BAD_GATEWAY, uri.getHost() + " nicht erreichbar: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DeckFinderException(HttpStatus.SERVICE_UNAVAILABLE, "Abgebrochen");
        }
    }
}
