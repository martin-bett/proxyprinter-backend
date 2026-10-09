package my.poo.mtgcrawl.mtgprint.resource;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthService;

/** Login ohne Passwort (Link oder Code per Mail), Sitzung per HttpOnly-Cookie. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String COOKIE = "pp_session";

    private final AuthService authService;
    private final boolean secureCookie;

    public AuthController(AuthService authService, @Value("${app.public-url:https://proxyprinter.eu}") String publicUrl) {
        this.authService = authService;
        this.secureCookie = publicUrl.startsWith("https://");
    }

    /** Gibt den angemeldeten Benutzer der Anfrage zurück, falls es einen gibt. */
    public static Optional<AppUser> currentUser(AuthService authService, HttpServletRequest request) {
        return authService.findUser(sessionToken(request));
    }

    static String sessionToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /** Ob Login möglich ist und wer gerade angemeldet ist. */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("loginEnabled", authService.isLoginEnabled());
        Optional<AppUser> user = currentUser(authService, request);
        body.put("email", user.map(AppUser::email).orElse(null));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        String token = sessionToken(request);
        if (user.isPresent() && token != null) {
            response.header(HttpHeaders.SET_COOKIE, cookie(token, Duration.ofDays(AuthService.SESSION_DAYS)).toString());
        }
        return response.body(body);
    }

    /** Antwort: {"status": "sent"}; in Testumgebungen (auth.dev-show-code) zusätzlich {"devCode": "123456"}. */
    @PostMapping("/login/request")
    public ResponseEntity<Map<String, Object>> requestLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
        AuthService.LoginRequest result = authService.requestLogin(body.get("email"), request.getRemoteAddr());
        return switch (result.result()) {
            case SENT -> {
                Map<String, Object> ok = new LinkedHashMap<>();
                ok.put("status", "sent");
                if (result.devCode() != null) {
                    ok.put("devCode", result.devCode());
                }
                yield ResponseEntity.ok(ok);
            }
            case INVALID_EMAIL -> ResponseEntity.badRequest().build();
            case DISABLED -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
            case RATE_LIMITED -> ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        };
    }

    /** Body: {"token": "..."} (Link) oder {"email": "...", "code": "123456"}. */
    @PostMapping("/login/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        Optional<String> session = body.get("token") != null
                ? authService.verifyToken(body.get("token"), userAgent)
                : authService.verifyCode(body.get("email"), body.get("code"), userAgent);
        if (session.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("loginEnabled", true);
        result.put("email", authService.findUser(session.get()).map(AppUser::email).orElse(null));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(session.get(), Duration.ofDays(AuthService.SESSION_DAYS)).toString())
                .body(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(sessionToken(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutEverywhere(HttpServletRequest request) {
        Optional<AppUser> user = currentUser(authService, request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.logoutEverywhere(user.get());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(HttpServletRequest request) {
        Optional<AppUser> user = currentUser(authService, request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.deleteAccount(user.get());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE, value).httpOnly(true).secure(secureCookie).sameSite("Lax").path("/")
                .maxAge(maxAge).build();
    }
}
