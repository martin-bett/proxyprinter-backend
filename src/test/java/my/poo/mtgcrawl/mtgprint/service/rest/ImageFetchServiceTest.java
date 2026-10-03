package my.poo.mtgcrawl.mtgprint.service.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class ImageFetchServiceTest {

    // 1x1 PNG
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private HttpServer server;
    private String base;
    private final AtomicInteger htmlHits = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // wie Gatherer: alte URL -> 301 -> 308 -> eigentliches Bild
        server.createContext("/Handlers/Image.ashx", ex -> redirect(ex, 301, "/moved/Image.ashx?" + ex.getRequestURI().getQuery()));
        server.createContext("/moved/Image.ashx", ex -> redirect(ex, 308, "/img/card.png"));
        server.createContext("/img/card.png", ex -> send(ex, 200, "image/png", PNG));
        server.createContext("/page", ex -> {
            htmlHits.incrementAndGet();
            send(ex, 200, "text/html", "<html>Gatherer</html>".getBytes());
        });
        server.createContext("/gone", ex -> send(ex, 404, "text/plain", "nope".getBytes()));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void redirect(HttpExchange ex, int status, String location) throws IOException {
        ex.getResponseHeaders().add("Location", location);
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    @Test
    void folgtUmleitungenBisZumBild() {
        ImageFetchService service = new ImageFetchService();
        ImageFetchService.Image image = service.fetch(base + "/Handlers/Image.ashx?multiverseid=405126&type=card");
        assertArrayEquals(PNG, image.content());
        assertEquals("image/png", image.contentType());
    }

    @Test
    void htmlStattBildIstEinFehlerUndWirdGemerkt() {
        ImageFetchService service = new ImageFetchService();
        String url = base + "/page";
        var e = assertThrows(ImageFetchService.ImageFetchException.class, () -> service.fetch(url));
        assertTrue(e.getMessage().startsWith("kein Bild (text/html"), e.getMessage());
        assertNotNull(service.recentFailure(url));
        assertEquals(1, htmlHits.get());
    }

    @Test
    void httpFehlerWirdGemeldet() {
        ImageFetchService service = new ImageFetchService();
        var e = assertThrows(ImageFetchService.ImageFetchException.class, () -> service.fetch(base + "/gone"));
        assertEquals("HTTP 404", e.getMessage());
    }
}
