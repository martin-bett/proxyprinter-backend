package my.poo.mtgcrawl.mtgprint.service.gallery;

import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Liest die Bilder eines Reddit-Posts über die JSON-Schnittstelle statt über das HTML.
 *
 * Reddit liefert im HTML keine verwertbaren Galerie-Links mehr und der "Teilen"-Knopf
 * erzeugt nur noch interne Kurzlinks (/r/xyz/s/AbCd). Deshalb: Link auflösen,
 * Post-ID herausziehen, /comments/{id}.json lesen und die Bilder aus
 * gallery_data + media_metadata (Galerie), url (Einzelbild) oder preview bauen.
 *
 * Ohne Zugangsdaten nutzt das den öffentlichen .json-Endpunkt (streng limitiert,
 * Server-IPs werden oft mit 403 abgewiesen). Mit gallery.reddit.clientid/-clientsecret
 * holt der Dienst ein App-Token und fragt oauth.reddit.com.
 */
@Service
@Slf4j
public class RedditGallerySource {

    static final Pattern POST_ID = Pattern.compile(
            "(?:/comments/|/gallery/|redd\\.it/|^t3_)([a-z0-9]{4,12})(?:[/?#]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHARE_LINK = Pattern.compile("/r/[^/]+/s/[A-Za-z0-9]+");
    private static final Pattern URL_IN_TEXT = Pattern.compile("https?://[^\\s)\\]>\"]+");
    private static final Set<String> IMAGE_HOSTS = Set.of("i.redd.it", "i.imgur.com");

    private final GalleryHttp http;
    private final ObjectMapper objectMapper;
    private final String clientId;
    private final String clientSecret;
    private final String userAgent;

    private String accessToken;
    private Instant accessTokenExpiry = Instant.EPOCH;

    public RedditGallerySource(GalleryHttp http, ObjectMapper objectMapper,
            @Value("${gallery.reddit.clientid:}") String clientId,
            @Value("${gallery.reddit.clientsecret:}") String clientSecret,
            @Value("${gallery.reddit.useragent:web:eu.proxyprinter:v1.0 (gallery import)}") String userAgent) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.userAgent = userAgent;
    }

    /** Ergebnis inklusive verlinkter Drive-Ordner, die der Aufrufer noch nachladen kann. */
    public record RedditPost(GallerySourceResult result, List<String> linkedUrls) {
    }

    public RedditPost load(String link) {
        String postId = resolvePostId(link);
        JsonNode listing = fetchPost(postId);
        return parse(listing);
    }

    // ---- Link -> Post-ID ----------------------------------------------------

    String resolvePostId(String link) {
        if (link == null || link.isBlank()) {
            throw new GallerySourceException(HttpStatus.BAD_REQUEST, "Kein Reddit-Link angegeben");
        }
        String current = link.trim();
        if (!current.contains("://") && !current.startsWith("t3_")) {
            current = "https://" + current;
        }
        // Teilen-Links (/r/xyz/s/..) und redd.it-Kurzlinks leiten erst auf den Post weiter
        for (int hop = 0; hop < 5; hop++) {
            Optional<String> id = extractPostId(current);
            if (id.isPresent()) {
                return id.get();
            }
            if (!SHARE_LINK.matcher(current).find()) {
                break;
            }
            HttpResponse<String> response = http.get(current, Map.of("User-Agent", userAgent));
            Optional<String> location = response.headers().firstValue("location");
            if (location.isEmpty()) {
                break;
            }
            current = URI.create(current).resolve(location.get()).toString();
        }
        throw new GallerySourceException(HttpStatus.BAD_REQUEST,
                "Im Link wurde kein Reddit-Post gefunden: " + link);
    }

    static Optional<String> extractPostId(String link) {
        Matcher matcher = POST_ID.matcher(link);
        return matcher.find() ? Optional.of(matcher.group(1).toLowerCase()) : Optional.empty();
    }

    // ---- Abruf --------------------------------------------------------------

    private JsonNode fetchPost(String postId) {
        String url;
        Map<String, String> headers;
        if (hasCredentials()) {
            url = "https://oauth.reddit.com/comments/" + postId + "?raw_json=1&limit=1";
            headers = Map.of("User-Agent", userAgent, "Authorization", "Bearer " + token());
        } else {
            url = "https://www.reddit.com/comments/" + postId + ".json?raw_json=1&limit=1";
            headers = Map.of("User-Agent", userAgent, "Accept", "application/json");
        }
        HttpResponse<String> response = http.get(url, headers);
        int status = response.statusCode();
        if (status == 403 || status == 429) {
            log.warn("Reddit lehnt Abruf von {} ab: {}", postId, status);
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Reddit hat den Abruf abgelehnt (" + status + ")."
                    + (hasCredentials() ? "" : " Ohne Reddit-App-Zugang sperrt Reddit Server oft; siehe gallery.reddit.clientid."));
        }
        if (status == 404) {
            throw new GallerySourceException(HttpStatus.NOT_FOUND, "Reddit-Post " + postId + " nicht gefunden");
        }
        if (status / 100 != 2) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Reddit antwortet mit Status " + status);
        }
        try {
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Reddit-Antwort ist kein JSON");
        }
    }

    private boolean hasCredentials() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    private synchronized String token() {
        if (accessToken != null && Instant.now().isBefore(accessTokenExpiry)) {
            return accessToken;
        }
        String basic = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> response = http.postForm("https://www.reddit.com/api/v1/access_token",
                "grant_type=client_credentials",
                Map.of("Authorization", "Basic " + basic, "User-Agent", userAgent));
        try {
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() / 100 != 2 || !json.hasNonNull("access_token")) {
                throw new IllegalStateException("Status " + response.statusCode());
            }
            accessToken = json.get("access_token").asText();
            // eine Minute Puffer vor dem Ablauf
            accessTokenExpiry = Instant.now().plusSeconds(Math.max(60, json.path("expires_in").asLong(3600) - 60));
            return accessToken;
        } catch (Exception e) {
            log.warn("Reddit-Token konnte nicht geholt werden: {}", e.getMessage());
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Reddit-Anmeldung fehlgeschlagen");
        }
    }

    // ---- JSON -> Bilder -----------------------------------------------------

    /** Erwartet die Antwort von /comments/{id}.json: [Post-Listing, Kommentar-Listing]. */
    RedditPost parse(JsonNode listing) {
        JsonNode post = listing.path(0).path("data").path("children").path(0).path("data");
        if (post.isMissingNode()) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Unerwartete Reddit-Antwort");
        }
        String title = post.path("title").asText("");

        List<GalleryImage> images = collectImages(post, title);
        // Crossposts tragen die Bilder nur im Original
        if (images.isEmpty()) {
            for (JsonNode parent : post.path("crosspost_parent_list")) {
                images = collectImages(parent, title);
                if (!images.isEmpty()) {
                    break;
                }
            }
        }

        Set<String> links = new LinkedHashSet<>();
        addLink(links, post.path("url").asText(null));
        addLinksFromText(links, post.path("selftext").asText(""));
        for (JsonNode parent : post.path("crosspost_parent_list")) {
            addLink(links, parent.path("url").asText(null));
            addLinksFromText(links, parent.path("selftext").asText(""));
        }

        return new RedditPost(new GallerySourceResult("reddit", title, images), new ArrayList<>(links));
    }

    private List<GalleryImage> collectImages(JsonNode post, String title) {
        List<GalleryImage> images = new ArrayList<>();
        JsonNode metadata = post.path("media_metadata");

        // 1. Galerie: Reihenfolge aus gallery_data, Dateien aus media_metadata
        for (JsonNode item : post.path("gallery_data").path("items")) {
            String mediaId = item.path("media_id").asText();
            String url = mediaUrl(metadata.path(mediaId), mediaId);
            if (url != null) {
                images.add(new GalleryImage(nameFor(item.path("caption").asText(""), title, images.size() + 1), url));
            }
        }
        if (!images.isEmpty()) {
            return images;
        }

        // 2. Bilder im Text eines Self-Posts stehen nur in media_metadata
        if (metadata.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = metadata.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String url = mediaUrl(entry.getValue(), entry.getKey());
                if (url != null) {
                    images.add(new GalleryImage(nameFor("", title, images.size() + 1), url));
                }
            }
            if (!images.isEmpty()) {
                return images;
            }
        }

        // 3. Einzelbild-Post
        String url = post.path("url_overridden_by_dest").asText(post.path("url").asText(""));
        if (isDirectImage(url)) {
            images.add(new GalleryImage(title, url));
            return images;
        }

        // 4. Vorschau (z.B. Link-Posts mit Bild), raw_json=1 liefert sie unescaped
        String preview = post.path("preview").path("images").path(0).path("source").path("url").asText("");
        if (!preview.isBlank() && "image".equals(post.path("post_hint").asText())) {
            images.add(new GalleryImage(title, preview));
        }
        return images;
    }

    /** Original-Datei auf i.redd.it statt der verkleinerten preview.redd.it-Variante. */
    private static String mediaUrl(JsonNode meta, String mediaId) {
        if (meta.isMissingNode() || !"valid".equals(meta.path("status").asText("valid"))) {
            return null;
        }
        if (!"Image".equals(meta.path("e").asText())) {
            return null; // AnimatedImage, RedditVideo
        }
        String mime = meta.path("m").asText("");
        if (mime.startsWith("image/")) {
            String ext = mime.substring("image/".length());
            if ("jpeg".equals(ext)) {
                ext = "jpg";
            }
            return "https://i.redd.it/" + mediaId + "." + ext;
        }
        String source = meta.path("s").path("u").asText("");
        return source.isBlank() ? null : source;
    }

    private static String nameFor(String caption, String title, int index) {
        if (!caption.isBlank()) {
            return caption.trim();
        }
        return (title.isBlank() ? "Reddit" : title.trim()) + " " + index;
    }

    private static boolean isDirectImage(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
            return IMAGE_HOSTS.contains(uri.getHost())
                    && path.matches(".*\\.(png|jpe?g|webp)$");
        } catch (Exception e) {
            return false;
        }
    }

    private static void addLink(Set<String> links, String url) {
        if (url != null && url.startsWith("http") && !url.contains("reddit.com/")) {
            links.add(url);
        }
    }

    private static void addLinksFromText(Set<String> links, String text) {
        Matcher matcher = URL_IN_TEXT.matcher(text);
        while (matcher.find()) {
            addLink(links, matcher.group());
        }
    }
}
