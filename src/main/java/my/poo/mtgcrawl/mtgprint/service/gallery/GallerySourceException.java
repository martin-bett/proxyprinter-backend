package my.poo.mtgcrawl.mtgprint.service.gallery;

import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Fehler beim Lesen einer Galerie-Quelle, mit Status für die Antwort ans Frontend. */
@Getter
public class GallerySourceException extends RuntimeException {

    private final HttpStatus status;
    /** Stable error code the frontend translates; null for untranslated legacy errors. */
    private final String code;
    private final Map<String, String> params;

    public GallerySourceException(HttpStatus status, String message) {
        this(status, null, message, Map.of());
    }

    /** The message is the English fallback; the frontend shows its own text for the code. */
    public GallerySourceException(HttpStatus status, String code, String message, Map<String, String> params) {
        super(message);
        this.status = status;
        this.code = code;
        this.params = params;
    }

    /** Body of the error response: {"error": fallback, "code": ..., "params": {...}}. */
    public Map<String, Object> toBody() {
        return code == null
                ? Map.of("error", getMessage())
                : Map.of("error", getMessage(), "code", code, "params", params);
    }
}
