package my.poo.mtgcrawl.mtgprint.service.gallery;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Zugangsdaten für Gallery-Configs, nur in der Server-Konfiguration (Umgebungsvariablen), nie im Repo
 * und nie im Frontend. Eine Config nennt nur den Namen ("credential"); der Server hängt Cookie,
 * Authorization oder User-Agent an, aber ausschließlich bei Anfragen an den hinterlegten Host
 * (samt Subdomains). Eine fremde Config kann die Werte so nicht an einen anderen Server schicken.
 *
 * Beispiel: GALLERY_CREDENTIALS_REDDIT_HOST=reddit.com, GALLERY_CREDENTIALS_REDDIT_COOKIE=...
 */
@Component
@ConfigurationProperties(prefix = "gallery")
@Getter
@Setter
public class GalleryCredentials {

    @Getter
    @Setter
    public static class Credential {
        private String host;
        private String cookie;
        private String authorization;
        private String userAgent;
    }

    private Map<String, Credential> credentials = new HashMap<>();

    /** Gibt es einen Eintrag mit diesem Namen (auch ohne Werte)? */
    public boolean exists(String name) {
        return name != null && credentials.containsKey(name);
    }

    /** Header für diesen Namen, leer wenn nichts hinterlegt ist oder der Host nicht passt. */
    public Map<String, String> headersFor(String name, String requestHost) {
        Map<String, String> headers = new HashMap<>();
        Credential credential = name == null ? null : credentials.get(name);
        if (credential == null || requestHost == null || !hostMatches(credential.getHost(), requestHost)) {
            return headers;
        }
        put(headers, "Cookie", credential.getCookie());
        put(headers, "Authorization", credential.getAuthorization());
        put(headers, "User-Agent", credential.getUserAgent());
        return headers;
    }

    static boolean hostMatches(String credentialHost, String requestHost) {
        if (credentialHost == null || credentialHost.isBlank()) {
            return false;
        }
        String allowed = credentialHost.trim().toLowerCase();
        String host = requestHost.toLowerCase();
        return host.equals(allowed) || host.endsWith("." + allowed);
    }

    private static void put(Map<String, String> headers, String name, String value) {
        Optional.ofNullable(value).filter(v -> !v.isBlank()).ifPresent(v -> headers.put(name, v));
    }
}
