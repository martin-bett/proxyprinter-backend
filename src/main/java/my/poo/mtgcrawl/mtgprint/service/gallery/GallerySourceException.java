package my.poo.mtgcrawl.mtgprint.service.gallery;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Fehler beim Lesen einer Galerie-Quelle, mit Status für die Antwort ans Frontend. */
@Getter
public class GallerySourceException extends RuntimeException {

    private final HttpStatus status;

    public GallerySourceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
