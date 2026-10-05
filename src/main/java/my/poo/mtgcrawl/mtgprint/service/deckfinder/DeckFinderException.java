package my.poo.mtgcrawl.mtgprint.service.deckfinder;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class DeckFinderException extends RuntimeException {
    private final HttpStatus status;

    public DeckFinderException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
