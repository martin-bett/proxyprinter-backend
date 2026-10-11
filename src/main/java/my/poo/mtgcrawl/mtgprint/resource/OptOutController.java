package my.poo.mtgcrawl.mtgprint.resource;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.service.optout.OptOutException;
import my.poo.mtgcrawl.mtgprint.service.optout.OptOutService;

/** Öffentliche Selbst-Sperre für Betreiber von Quellen, ohne Konto (siehe OptOutService). */
@RestController
@RequestMapping("/api/blocklist")
@AllArgsConstructor
public class OptOutController {

    private final OptOutService service;

    /** Welche Nachweise gerade gehen: {"mail": bool, "mailboxes": [...]}. */
    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of("mail", service.isMailEnabled(), "mailboxes", OptOutService.MAILBOXES);
    }

    /** Body: {"host": "...", "method": "mail"|"challenge", "mailbox": "admin", "language": "de"}. */
    @PostMapping("/request")
    public OptOutService.Result request(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        return service.request(body.get("host"), body.get("method"), body.get("mailbox"), body.get("language"),
                httpRequest.getRemoteAddr());
    }

    /** Body: {"token": "..."} aus dem Mail-Link oder der Prüf-Anleitung. */
    @PostMapping("/verify")
    public OptOutService.Result verify(@RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        return service.verify(body.get("token"), httpRequest.getRemoteAddr());
    }

    @ExceptionHandler(OptOutException.class)
    public ResponseEntity<Map<String, Object>> handle(OptOutException e) {
        return ResponseEntity.status(e.getStatus()).body(e.toBody());
    }
}
