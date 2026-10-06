package my.poo.mtgcrawl.mtgprint.service.gallery;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.service.deckfinder.DeckFinderException;
import my.poo.mtgcrawl.mtgprint.service.deckfinder.DeckFinderFetchService;

/**
 * Führt eine Gallery-Config serverseitig aus, wenn das Frontend es nicht selbst kann: Der Link des
 * Nutzers wird per link_pattern in Variablen zerlegt ({id} usw.), base_url damit gefüllt, optional mit
 * den Zugangsdaten der Config (credential) abgerufen und unverändert zurückgegeben. Die Felder liest
 * wie bei jeder Gallery-Config das Frontend. Es gibt keine Quellen im Code, nur diese Bausteine.
 */
@Service
@AllArgsConstructor
public class GalleryFetchService {

    static final int MAX_HOPS = 5;
    static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";

    public record GalleryResponse(String contentType, String body) {}

    private final GalleryHttp http;
    private final GalleryCredentials credentials;

    public GalleryResponse fetch(GalleryConfig config, String input) {
        if (isBlank(config.getBaseUrl()) || !config.getBaseUrl().matches("(?i)https?://.*")) {
            throw new GallerySourceException(HttpStatus.BAD_REQUEST, "Die Config braucht eine absolute Base URL (http/https).");
        }
        if (!isBlank(config.getCredential()) && !credentials.exists(config.getCredential())) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY,
                    "Zugang \"" + config.getCredential() + "\" ist auf dem Server nicht eingerichtet.");
        }
        String url = fillTemplate(config.getBaseUrl(), variables(config, input));
        HttpResponse<String> response = get(url, config.getCredential(), true);
        int status = response.statusCode();
        String host = response.uri().getHost();
        if (status == 404 || status == 410) {
            throw new GallerySourceException(HttpStatus.NOT_FOUND, "Nichts gefunden (privat oder gelöscht?).");
        }
        if (status == 401 || status == 403 || status == 429 || status == 503) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, host + " blockt den Abruf (HTTP " + status + ")."
                    + (isBlank(config.getCredential()) ? "" : " Zugang \"" + config.getCredential() + "\" evtl. abgelaufen."));
        }
        if (status >= 300) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, host + " antwortet mit HTTP " + status + ".");
        }
        String contentType = "html".equalsIgnoreCase(config.getDataFormat()) ? "text/html" : "application/json";
        return new GalleryResponse(contentType, response.body());
    }

    /** Variablen für die URL: Namen der Gruppen aus link_pattern, immer auch {input}. Ohne Eingabe bleibt die Map leer. */
    Map<String, String> variables(GalleryConfig config, String input) {
        Map<String, String> values = new LinkedHashMap<>();
        String value = input == null ? "" : input.trim();
        if (isBlank(config.getLinkPattern())) {
            if (!value.isEmpty()) {
                values.put("input", value);
            }
            return values;
        }
        if (value.isEmpty()) {
            throw new GallerySourceException(HttpStatus.BAD_REQUEST, "Bitte einen Link eingeben.");
        }
        Pattern pattern = compile(config.getLinkPattern());
        Optional<Matcher> match = match(pattern, value);
        String current = value;
        for (int hop = 0; match.isEmpty() && config.isFollowInputRedirects() && hop < MAX_HOPS; hop++) {
            Optional<String> next = redirectTarget(current, config.getCredential());
            if (next.isEmpty()) {
                break;
            }
            current = next.get();
            match = match(pattern, current);
        }
        Matcher matcher = match.orElseThrow(
                () -> new GallerySourceException(HttpStatus.BAD_REQUEST, "Link passt nicht zu dieser Quelle."));
        values.put("input", current);
        matcher.namedGroups().keySet().forEach(name -> {
            if (matcher.group(name) != null) {
                values.put(name, matcher.group(name));
            }
        });
        return values;
    }

    private Optional<Matcher> match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(matcher) : Optional.empty();
    }

    private static Pattern compile(String regex) {
        try {
            return Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new GallerySourceException(HttpStatus.BAD_REQUEST, "Link Pattern ist keine gültige Regex: " + e.getDescription());
        }
    }

    /** Ein Schritt einer Weiterleitung des Nutzer-Links (z. B. Kurz- oder Teilen-Links), ohne dem Ziel zu folgen. */
    private Optional<String> redirectTarget(String link, String credential) {
        String absolute = link.contains("://") ? link : "https://" + link;
        HttpResponse<String> response = getOnce(absolute, credential);
        if (response.statusCode() / 100 != 3) {
            return Optional.empty();
        }
        return response.headers().firstValue("location").map(location -> URI.create(absolute).resolve(location).toString());
    }

    /** Folgt Weiterleitungen selbst, damit jede Station geprüft wird und Zugangsdaten nur zum passenden Host gehen. */
    private HttpResponse<String> get(String url, String credential, boolean followRedirects) {
        String current = url;
        for (int hop = 0; hop <= MAX_HOPS; hop++) {
            HttpResponse<String> response = getOnce(current, credential);
            Optional<String> location = response.headers().firstValue("location");
            if (!followRedirects || response.statusCode() / 100 != 3 || location.isEmpty()) {
                return response;
            }
            current = URI.create(current).resolve(location.get()).toString();
        }
        throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Zu viele Weiterleitungen.");
    }

    private HttpResponse<String> getOnce(String url, String credential) {
        URI uri = checked(url);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("User-Agent", DEFAULT_USER_AGENT);
        headers.put("Accept", "application/json, text/html;q=0.9, */*;q=0.8");
        headers.put("Accept-Language", "en");
        headers.putAll(credentials.headersFor(credential, uri.getHost()));
        return http.get(uri.toString(), headers);
    }

    /** Gleiche Prüfung wie beim Deckfinder: nur http(s), keine internen Adressen. */
    URI checked(String url) {
        try {
            return DeckFinderFetchService.checkedUri(url);
        } catch (DeckFinderException e) {
            throw new GallerySourceException(e.getStatus(), e.getMessage());
        }
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
