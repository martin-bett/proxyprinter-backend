package my.poo.mtgcrawl.mtgprint.service.admin;

import java.util.Map;

import org.springframework.http.HttpStatus;

/** Abgelehnte Admin-Aktion mit Fehlercode für das Frontend (backendErrors.<code>). */
public class AdminException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, Object> params;

    public AdminException(HttpStatus status, String code, String message, Map<String, Object> params) {
        super(message);
        this.status = status;
        this.code = code;
        this.params = params;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> toBody() {
        return Map.of("error", getMessage(), "code", code, "params", params);
    }
}
