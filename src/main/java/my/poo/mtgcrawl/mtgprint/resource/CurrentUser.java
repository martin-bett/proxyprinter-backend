package my.poo.mtgcrawl.mtgprint.resource;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthService;

/** Konto der Anfrage (Sitzungs-Cookie) für Controller, die Besitz prüfen. */
@Component
@AllArgsConstructor
public class CurrentUser {

    private final AuthService authService;

    /** Id des angemeldeten Kontos oder null. */
    public Long idOrNull(HttpServletRequest request) {
        return AuthController.currentUser(authService, request).map(AppUser::id).orElse(null);
    }

    /** Id des angemeldeten Kontos, sonst LoginRequiredException (401). */
    public long require(HttpServletRequest request) {
        Long id = idOrNull(request);
        if (id == null) {
            throw new LoginRequiredException();
        }
        return id;
    }
}
