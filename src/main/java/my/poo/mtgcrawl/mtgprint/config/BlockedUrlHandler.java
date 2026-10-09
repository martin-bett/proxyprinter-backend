package my.poo.mtgcrawl.mtgprint.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import my.poo.mtgcrawl.mtgprint.service.net.BlockedUrlException;

/** Abgelehnte Ziele (internes Netz, kein http(s), zu viele Weiterleitungen) als 400 mit Fehlercode. */
@RestControllerAdvice
public class BlockedUrlHandler {

    @ExceptionHandler(BlockedUrlException.class)
    public ResponseEntity<Map<String, Object>> handle(BlockedUrlException e) {
        return ResponseEntity.badRequest().body(e.toBody());
    }
}
