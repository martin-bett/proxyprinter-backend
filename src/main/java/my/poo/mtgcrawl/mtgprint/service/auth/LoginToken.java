package my.poo.mtgcrawl.mtgprint.service.auth;

import java.time.LocalDateTime;

/** Ein offener Login: Link-Token und Code liegen nur als SHA-256-Hash in der DB. */
public record LoginToken(String email, String tokenHash, String codeHash, int attempts, LocalDateTime expiresAt) {
}
