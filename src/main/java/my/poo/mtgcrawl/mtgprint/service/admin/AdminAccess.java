package my.poo.mtgcrawl.mtgprint.service.admin;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import my.poo.mtgcrawl.mtgprint.resource.AuthController;
import my.poo.mtgcrawl.mtgprint.resource.LoginRequiredException;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthService;

/**
 * Wer die Admin-Seite benutzen darf: angemeldete Konten, deren E-Mail in admin.emails steht
 * (Umgebungsvariable ADMIN_EMAILS, kommagetrennt, nur auf dem Server setzen). Leer = niemand.
 */
@Component
public class AdminAccess {

    private final AuthService authService;
    private final Set<String> adminEmails;

    public AdminAccess(AuthService authService, @Value("${admin.emails:}") String adminEmails) {
        this.authService = authService;
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(AppUser user) {
        return user != null && adminEmails.contains(user.email().toLowerCase(Locale.ROOT));
    }

    /** Das Admin-Konto der Anfrage, sonst LoginRequiredException (401) oder AdminRequiredException (403). */
    public AppUser require(HttpServletRequest request) {
        AppUser user = AuthController.currentUser(authService, request).orElseThrow(LoginRequiredException::new);
        if (!isAdmin(user)) {
            throw new AdminRequiredException();
        }
        return user;
    }

    /** Die Aktion ist nur für Admins (Antwort 403, Code admin.forbidden). */
    public static class AdminRequiredException extends RuntimeException {
        public AdminRequiredException() {
            super("Admins only.");
        }
    }
}
