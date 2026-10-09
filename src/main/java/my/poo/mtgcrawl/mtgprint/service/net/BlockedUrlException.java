package my.poo.mtgcrawl.mtgprint.service.net;

import java.util.Map;

import lombok.Getter;

/** Ziel eines Abrufs ist nicht erlaubt (kein http(s), internes Netz, unbekannter Host, zu viele Weiterleitungen). */
@Getter
public class BlockedUrlException extends RuntimeException {

    /** Stabiler Fehlercode, den das Frontend übersetzt (backendErrors.proxy.*). */
    private final String code;
    private final Map<String, String> params;

    /** Die Message ist der englische Fallback. */
    public BlockedUrlException(String code, String message, Map<String, String> params) {
        super(message);
        this.code = code;
        this.params = params;
    }

    /** Body der Fehlerantwort: {"error": fallback, "code": ..., "params": {...}}. */
    public Map<String, Object> toBody() {
        return Map.of("error", getMessage(), "code", code, "params", params);
    }
}
