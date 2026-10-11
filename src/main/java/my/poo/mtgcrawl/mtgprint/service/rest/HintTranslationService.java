package my.poo.mtgcrawl.mtgprint.service.rest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Übersetzt den Hinweistext einer Galerie-Config beim Speichern in die UI-Sprachen des Frontends (DeepL).
 * Ohne deepl.api-key ist die Übersetzung abgeschaltet; Fehler machen das Speichern nie kaputt,
 * es fehlen dann nur die Übersetzungen und das Frontend zeigt den Originaltext.
 */
@Service
public class HintTranslationService {

    private static final Logger log = LoggerFactory.getLogger(HintTranslationService.class);

    /** UI-Sprache des Frontends -> DeepL-Zielsprache. */
    static final Map<String, String> TARGETS = Map.of(
            "de", "DE", "en", "EN-GB", "es", "ES", "fr", "FR", "it", "IT", "ja", "JA", "ru", "RU", "zh", "ZH");

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final String apiKey;
    private final String apiUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Autowired
    public HintTranslationService(
            @Value("${deepl.api-key:}") String apiKey,
            @Value("${deepl.api-url:https://api-free.deepl.com/v2/translate}") String apiUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.apiUrl = apiUrl;
    }

    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    /**
     * Sprachcode -> übersetzter Text, ohne die erkannte Originalsprache (dort gilt der Originaltext).
     * Leer, wenn abgeschaltet, der Text leer ist oder DeepL nicht antwortet.
     */
    public Map<String, String> translate(String text) {
        if (!enabled() || text == null || text.isBlank()) {
            return Map.of();
        }
        Map<String, CompletableFuture<HttpResponse<String>>> calls = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> target : TARGETS.entrySet()) {
                String body = JSON.writeValueAsString(Map.of("text", List.of(text), "target_lang", target.getValue()));
                HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl))
                        .timeout(REQUEST_TIMEOUT)
                        .header("Authorization", "DeepL-Auth-Key " + apiKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                calls.put(target.getKey(), http.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
            }
            CompletableFuture.allOf(calls.values().toArray(new CompletableFuture[0]))
                    .exceptionally(e -> null)
                    .get(REQUEST_TIMEOUT.toSeconds() + 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of();
        } catch (Exception e) {
            log.warn("Hinweis-Übersetzung nicht möglich: {}", e.toString());
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        String detected = null;
        for (Map.Entry<String, CompletableFuture<HttpResponse<String>>> call : calls.entrySet()) {
            try {
                HttpResponse<String> response = call.getValue().getNow(null);
                if (response == null || response.statusCode() != 200) {
                    log.warn("DeepL antwortete für {} mit {}", call.getKey(), response == null ? "keiner Antwort" : response.statusCode());
                    continue;
                }
                JsonNode translation = JSON.readTree(response.body()).path("translations").path(0);
                String translated = translation.path("text").asText("");
                if (!translated.isBlank()) {
                    result.put(call.getKey(), translated);
                }
                String source = translation.path("detected_source_language").asText("");
                if (!source.isEmpty()) {
                    detected = source.toLowerCase();
                }
            } catch (Exception e) {
                log.warn("DeepL-Antwort für {} nicht lesbar: {}", call.getKey(), e.toString());
            }
        }
        if (detected != null) {
            result.remove(detected);
        }
        return result;
    }
}
