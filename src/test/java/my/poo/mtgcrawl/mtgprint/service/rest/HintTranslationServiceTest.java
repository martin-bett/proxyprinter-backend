package my.poo.mtgcrawl.mtgprint.service.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class HintTranslationServiceTest {

    private HttpServer server;
    private String url;
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private final List<String> auth = new CopyOnWriteArrayList<>();
    private volatile int status = 200;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/translate", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            bodies.add(body);
            auth.add(ex.getRequestHeaders().getFirst("Authorization"));
            // Zielsprache aus dem Body übernehmen, damit man sieht, welche Sprache übersetzt wurde
            String target = body.replaceAll(".*\"target_lang\":\"([A-Z-]+)\".*", "$1");
            send(ex, status, "{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":\"[" + target + "] Link\"}]}");
        });
        server.start();
        url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v2/translate";
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    @Test
    void translatesIntoAllUiLanguagesExceptTheDetectedSource() {
        Map<String, String> result = new HintTranslationService("key-1", url).translate("Paste the link");

        assertEquals(7, result.size());
        assertFalse(result.containsKey("en"), "Originalsprache bleibt beim Originaltext");
        assertEquals("[DE] Link", result.get("de"));
        assertEquals("[ZH] Link", result.get("zh"));
        assertEquals(8, bodies.size());
        assertTrue(auth.stream().allMatch("DeepL-Auth-Key key-1"::equals));
        assertTrue(bodies.stream().anyMatch(b -> b.contains("\"target_lang\":\"EN-GB\"")));
    }

    @Test
    void withoutKeyNothingIsSent() {
        HintTranslationService service = new HintTranslationService("  ", url);

        assertFalse(service.enabled());
        assertTrue(service.translate("Paste the link").isEmpty());
        assertTrue(bodies.isEmpty());
    }

    @Test
    void blankTextIsNotTranslated() {
        assertTrue(new HintTranslationService("key", url).translate("   ").isEmpty());
        assertTrue(bodies.isEmpty());
    }

    @Test
    void errorResponseGivesNoTranslations() {
        status = 456; // DeepL: Kontingent aufgebraucht
        assertTrue(new HintTranslationService("key", url).translate("Paste the link").isEmpty());
    }

    @Test
    void unreachableServiceGivesNoTranslations() {
        server.stop(0);
        assertTrue(new HintTranslationService("key", url).translate("Paste the link").isEmpty());
    }
}
