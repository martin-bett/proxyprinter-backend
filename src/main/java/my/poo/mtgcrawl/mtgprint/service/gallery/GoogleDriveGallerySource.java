package my.poo.mtgcrawl.mtgprint.service.gallery;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Liest die Bilder eines öffentlich freigegebenen Google-Drive-Ordners ("Jeder mit dem Link").
 *
 * Ohne Schlüssel: die eingebettete Ordneransicht drive.google.com/embeddedfolderview
 * ist schlichtes HTML mit einem Eintrag pro Datei (id="entry-{fileId}").
 * Mit gallery.google.apikey: Drive API v3 files.list, das ist stabiler und filtert
 * nach Bildtypen. Der Schlüssel gehört nur in die Server-Konfiguration, nie ins Repo.
 */
@Service
public class GoogleDriveGallerySource {

    static final Pattern FOLDER_ID = Pattern.compile("/folders/([A-Za-z0-9_-]{10,})");
    static final Pattern FILE_ID = Pattern.compile("/file/d/([A-Za-z0-9_-]{10,})");
    static final Pattern ID_PARAM = Pattern.compile("[?&]id=([A-Za-z0-9_-]{10,})");
    private static final Pattern IMAGE_NAME = Pattern.compile("(?i).*\\.(png|jpe?g|webp|gif|bmp|tiff?)$");
    private static final Pattern ANY_EXTENSION = Pattern.compile(".*\\.[A-Za-z0-9]{2,5}$");
    private static final String BROWSER_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36";

    private final GalleryHttp http;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GoogleDriveGallerySource(GalleryHttp http, ObjectMapper objectMapper,
            @Value("${gallery.google.apikey:}") String apiKey) {
        this.http = http;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public static boolean isDriveLink(String link) {
        return link != null && link.contains("drive.google.com")
                && (FOLDER_ID.matcher(link).find() || FILE_ID.matcher(link).find() || ID_PARAM.matcher(link).find());
    }

    public GallerySourceResult load(String link) {
        if (link == null || link.isBlank()) {
            throw new GallerySourceException(HttpStatus.BAD_REQUEST, "Kein Google-Drive-Link angegeben");
        }
        Optional<String> fileId = match(FILE_ID, link);
        if (fileId.isPresent()) {
            // Link auf eine einzelne Datei
            return new GallerySourceResult("gdrive", null,
                    List.of(new GalleryImage("Drive " + fileId.get(), imageUrl(fileId.get()))));
        }
        String folderId = match(FOLDER_ID, link).or(() -> match(ID_PARAM, link))
                .orElseThrow(() -> new GallerySourceException(HttpStatus.BAD_REQUEST,
                        "Im Link wurde kein Google-Drive-Ordner gefunden: " + link));
        return apiKey.isBlank() ? loadWithoutKey(folderId) : loadWithApi(folderId);
    }

    /** Direkter Download der Originaldatei; Drive sendet keine CORS-Header, das Frontend lädt sie über /api/proxy/image. */
    static String imageUrl(String fileId) {
        return "https://drive.google.com/uc?export=download&id=" + fileId;
    }

    // ---- ohne API-Schlüssel ---------------------------------------------------

    private GallerySourceResult loadWithoutKey(String folderId) {
        HttpResponse<String> response = http.get(
                "https://drive.google.com/embeddedfolderview?id=" + folderId,
                Map.of("User-Agent", BROWSER_UA, "Accept-Language", "en"));
        if (response.statusCode() == 404 || response.statusCode() == 403
                || response.statusCode() / 100 == 3) {
            // 3xx führt auf die Anmeldeseite: Ordner ist nicht öffentlich
            throw new GallerySourceException(HttpStatus.NOT_FOUND,
                    "Drive-Ordner nicht gefunden oder nicht für \"Jeder mit dem Link\" freigegeben");
        }
        if (response.statusCode() / 100 != 2) {
            throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Google Drive antwortet mit Status " + response.statusCode());
        }
        return parseEmbeddedFolderView(response.body());
    }

    static GallerySourceResult parseEmbeddedFolderView(String html) {
        Document doc = Jsoup.parse(html);
        List<GalleryImage> images = new ArrayList<>();
        for (Element entry : doc.select("div.flip-entry[id^=entry-]")) {
            String fileId = entry.id().substring("entry-".length());
            Element link = entry.selectFirst("a[href]");
            if (link != null && link.attr("href").contains("/folders/")) {
                continue; // Unterordner
            }
            Element titleElement = entry.selectFirst(".flip-entry-title");
            String name = titleElement != null ? titleElement.text().trim() : fileId;
            // Ohne API gibt es keinen MIME-Typ; Dateien mit fremder Endung (pdf, zip ...) überspringen
            if (ANY_EXTENSION.matcher(name).matches() && !IMAGE_NAME.matcher(name).matches()) {
                continue;
            }
            images.add(new GalleryImage(stripExtension(name), imageUrl(fileId)));
        }
        Element title = doc.selectFirst("title");
        return new GallerySourceResult("gdrive", title != null ? title.text() : null, images);
    }

    // ---- mit API-Schlüssel ----------------------------------------------------

    private GallerySourceResult loadWithApi(String folderId) {
        List<GalleryImage> images = new ArrayList<>();
        String query = "'" + folderId + "' in parents and trashed=false and mimeType contains 'image/'";
        String pageToken = null;
        int pages = 0;
        do {
            String url = "https://www.googleapis.com/drive/v3/files"
                    + "?q=" + encode(query)
                    + "&fields=" + encode("nextPageToken,files(id,name)")
                    + "&orderBy=name&pageSize=1000"
                    + "&supportsAllDrives=true&includeItemsFromAllDrives=true"
                    + "&key=" + encode(apiKey)
                    + (pageToken != null ? "&pageToken=" + encode(pageToken) : "");
            HttpResponse<String> response = http.get(url, Map.of("Accept", "application/json"));
            if (response.statusCode() == 404 || response.statusCode() == 403) {
                throw new GallerySourceException(HttpStatus.NOT_FOUND,
                        "Drive-Ordner nicht gefunden, nicht öffentlich oder API-Schlüssel ungültig (" + response.statusCode() + ")");
            }
            if (response.statusCode() / 100 != 2) {
                throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Drive API antwortet mit Status " + response.statusCode());
            }
            JsonNode json;
            try {
                json = objectMapper.readTree(response.body());
            } catch (Exception e) {
                throw new GallerySourceException(HttpStatus.BAD_GATEWAY, "Drive-Antwort ist kein JSON");
            }
            for (JsonNode file : json.path("files")) {
                images.add(new GalleryImage(stripExtension(file.path("name").asText()), imageUrl(file.path("id").asText())));
            }
            pageToken = json.path("nextPageToken").asText(null);
        } while (pageToken != null && ++pages < 20);
        return new GallerySourceResult("gdrive", null, images);
    }

    // ---- Hilfen -------------------------------------------------------------

    private static Optional<String> match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
