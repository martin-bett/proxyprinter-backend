package my.poo.mtgcrawl.mtgprint.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import my.poo.mtgcrawl.mtgprint.resource.LoginRequiredException;
import my.poo.mtgcrawl.mtgprint.service.admin.AdminAccess.AdminRequiredException;
import my.poo.mtgcrawl.mtgprint.service.admin.AdminException;
import my.poo.mtgcrawl.mtgprint.service.net.BlockedUrlException;

/** Fehler, die mehrere Controller werfen, als Antwort mit Fehlercode (das Frontend übersetzt backendErrors.<code>). */
@RestControllerAdvice
public class ApiErrorHandler {

    /** Abgelehnte Ziele (internes Netz, kein http(s), zu viele Weiterleitungen). */
    @ExceptionHandler(BlockedUrlException.class)
    public ResponseEntity<Map<String, Object>> handle(BlockedUrlException e) {
        return ResponseEntity.badRequest().body(e.toBody());
    }

    /** Anlegen, Bearbeiten und Bewerten nur mit Konto. */
    @ExceptionHandler(LoginRequiredException.class)
    public ResponseEntity<Map<String, Object>> handle(LoginRequiredException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", e.getMessage(), "code", "auth.loginRequired", "params", Map.of()));
    }

    /** Admin-Seite: angemeldet, aber nicht in admin.emails. */
    @ExceptionHandler(AdminRequiredException.class)
    public ResponseEntity<Map<String, Object>> handle(AdminRequiredException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage(), "code", "admin.forbidden", "params", Map.of()));
    }

    @ExceptionHandler(AdminException.class)
    public ResponseEntity<Map<String, Object>> handle(AdminException e) {
        return ResponseEntity.status(e.getStatus()).body(e.toBody());
    }
}
