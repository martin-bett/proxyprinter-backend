package my.poo.mtgcrawl.mtgprint.service.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/** Weiterleitungen werden nur auf geprüfte Ziele verfolgt, für HttpClient und RestTemplate. */
class GuardedHttpTest {

    private HttpServer server;
    private String base;
    private String internal;
    private final AtomicInteger secretHits = new AtomicInteger();
    private final AtomicReference<String> lastMethod = new AtomicReference<>();

    /** 127.0.0.1 gilt hier als „öffentlich“, localhost bleibt intern. */
    private final OutboundUrlGuard guard = new OutboundUrlGuard(false) {
        @Override
        public URI check(URI uri) {
            return "127.0.0.1".equals(uri.getHost()) ? uri : super.check(uri);
        }
    };

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", ex -> {
            lastMethod.set(ex.getRequestMethod());
            send(ex, 200, "ok");
        });
        server.createContext("/moved", ex -> redirect(ex, 302, "/ok"));
        server.createContext("/see-other", ex -> redirect(ex, 303, base + "/ok"));
        server.createContext("/to-internal", ex -> redirect(ex, 302, internal + "/secret"));
        server.createContext("/loop", ex -> redirect(ex, 302, "/loop"));
        server.createContext("/secret", ex -> {
            secretHits.incrementAndGet();
            send(ex, 200, "secret");
        });
        server.start();
        int port = server.getAddress().getPort();
        base = "http://127.0.0.1:" + port;
        internal = "http://localhost:" + port;
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static void redirect(HttpExchange ex, int status, String location) throws IOException {
        ex.getRequestBody().readAllBytes();
        ex.getResponseHeaders().add("Location", location);
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }

    private HttpResponse<String> get(String url) throws Exception {
        return GuardedHttp.send(client, guard, HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void folgtErlaubtenWeiterleitungen() throws Exception {
        HttpResponse<String> response = get(base + "/moved");
        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
    }

    @Test
    void seeOtherWirdZuGet() throws Exception {
        HttpResponse<String> response = GuardedHttp.send(client, guard,
                HttpRequest.newBuilder(URI.create(base + "/see-other")).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("GET", lastMethod.get());
    }

    @Test
    void weiterleitungInsInterneNetzWirdAbgelehnt() {
        BlockedUrlException e = assertThrows(BlockedUrlException.class, () -> get(base + "/to-internal"));
        assertEquals("proxy.internalAddress", e.getCode());
        assertEquals(0, secretHits.get());
    }

    @Test
    void direktesInternesZielWirdAbgelehnt() {
        assertThrows(BlockedUrlException.class, () -> get(internal + "/secret"));
        assertEquals(0, secretHits.get());
    }

    @Test
    void zuVieleWeiterleitungen() {
        assertEquals("proxy.tooManyRedirects", assertThrows(BlockedUrlException.class, () -> get(base + "/loop")).getCode());
    }

    private RestTemplate restTemplate() {
        GuardedRequestFactory factory = new GuardedRequestFactory(guard);
        RestTemplate template = new RestTemplate(factory);
        template.getInterceptors().add(factory.redirectInterceptor());
        return template;
    }

    @Test
    void restTemplateFolgtErlaubtenWeiterleitungen() {
        ResponseEntity<String> response = restTemplate().exchange(base + "/moved", HttpMethod.GET, HttpEntity.EMPTY, String.class);
        assertEquals("ok", response.getBody());
    }

    @Test
    void restTemplateLehntInternesZielAb() {
        assertThrows(BlockedUrlException.class,
                () -> restTemplate().exchange(internal + "/secret", HttpMethod.GET, HttpEntity.EMPTY, String.class));
        BlockedUrlException e = assertThrows(BlockedUrlException.class,
                () -> restTemplate().exchange(base + "/to-internal", HttpMethod.GET, HttpEntity.EMPTY, String.class));
        assertEquals("proxy.internalAddress", e.getCode());
        assertEquals(0, secretHits.get());
    }
}
