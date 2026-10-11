package my.poo.mtgcrawl.mtgprint.service.optout;

import java.util.Map;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Fehler der Selbst-Sperre mit Code für das Frontend (backendErrors.blocklist.*), die Message ist der englische Fallback. */
@Getter
public class OptOutException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> params;

    public OptOutException(HttpStatus status, String code, String message, Map<String, String> params) {
        super(message);
        this.status = status;
        this.code = code;
        this.params = params;
    }

    public Map<String, Object> toBody() {
        return Map.of("error", getMessage(), "code", code, "params", params);
    }
}
