package my.poo.mtgcrawl.mtgprint.service.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class ApiProbeServiceTest {

    private HttpServer server;
    private String base;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastType = new AtomicReference<>();
    private final ApiProbeService service = new ApiProbeService();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/search", ex -> send(ex, 200, "application/json; charset=utf-8",
                "{\"data\":[{\"name\":\"Lightning Bolt\"}]}"));
        server.createContext("/post", ex -> {
            lastBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastType.set(ex.getRequestHeaders().getFirst("Content-Type"));
            send(ex, 200, "text/html", "<div class=card>ok</div>");
        });
        server.createContext("/missing", ex -> send(ex, 404, "application/json", "{\"error\":\"not found\"}"));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    @Test
    void getReturnsBodyAndContentType() {
        ApiProbeService.ProbeResult r = service.probe(new ApiProbeService.ProbeRequest(base + "/search?q=x", "GET", null, null));
        assertEquals(200, r.status());
        assertTrue(r.contentType().startsWith("application/json"));
        assertTrue(r.body().contains("Lightning Bolt"));
        assertNull(r.error());
    }

    @Test
    void postSendsBodyWithFormat() {
        ApiProbeService.ProbeResult r = service.probe(new ApiProbeService.ProbeRequest(base + "/post", "post", "{\"q\":\"Bolt\"}", "json"));
        assertEquals(200, r.status());
        assertEquals("{\"q\":\"Bolt\"}", lastBody.get());
        assertEquals("application/json", lastType.get());
        assertEquals("<div class=card>ok</div>", r.body());
    }

    @Test
    void httpErrorStillReturnsBody() {
        ApiProbeService.ProbeResult r = service.probe(new ApiProbeService.ProbeRequest(base + "/missing", "GET", null, null));
        assertEquals(404, r.status());
        assertTrue(r.body().contains("not found"));
    }

    @Test
    void rejectsNonHttpUrls() {
        ApiProbeService.ProbeResult r = service.probe(new ApiProbeService.ProbeRequest("file:///etc/passwd", "GET", null, null));
        assertEquals(0, r.status());
        assertNotNull(r.error());
    }
}
