package my.poo.mtgcrawl.mtgprint.service.auth;

import java.time.LocalDateTime;

/** Ein Konto. Es besteht nur aus der bestätigten E-Mail-Adresse (immer klein geschrieben). */
public record AppUser(long id, String email, LocalDateTime createdAt) {
}
