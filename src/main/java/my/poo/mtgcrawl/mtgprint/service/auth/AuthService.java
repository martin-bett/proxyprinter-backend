package my.poo.mtgcrawl.mtgprint.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.service.mail.MailService;

/**
 * Login ohne Passwort: Wer seine Adresse eingibt, bekommt eine Mail mit Link und 6-stelligem Code.
 * Beides löst denselben Login ein und bestätigt damit die Adresse. Danach gilt ein Cookie
 * (nur als Hash in der DB), das bei Nutzung länger gültig bleibt.
 */
@Service
public class AuthService {

    public static final int SESSION_DAYS = 180;
    static final int TOKEN_MINUTES = 15;
    static final int MAX_CODE_ATTEMPTS = 5;
    static final int MAX_REQUESTS_PER_EMAIL_PER_HOUR = 5;
    static final int MAX_REQUESTS_PER_IP_PER_HOUR = 20;
    private static final long HOUR_MS = 60 * 60 * 1000L;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthRepository repository;
    private final MailService mailService;
    private final String publicUrl;
    private final boolean logLinks;
    private final boolean showCode;
    private final Map<String, Deque<Long>> requestsPerKey = new ConcurrentHashMap<>();

    public AuthService(AuthRepository repository, MailService mailService,
            @Value("${app.public-url:https://proxyprinter.eu}") String publicUrl,
            @Value("${auth.dev-log-links:false}") boolean logLinks,
            @Value("${auth.dev-show-code:false}") boolean showCode) {
        this.repository = repository;
        this.mailService = mailService;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        this.logLinks = logLinks;
        this.showCode = showCode;
    }

    /** Login geht nur, wenn Mails verschickt werden können (oder lokal/in Testumgebungen der Code anders sichtbar wird). */
    public boolean isLoginEnabled() {
        return mailService.isEnabled() || logLinks || showCode;
    }

    public enum RequestResult { SENT, INVALID_EMAIL, DISABLED, RATE_LIMITED }

    /** Ergebnis einer Login-Anfrage; devCode ist nur in Testumgebungen (auth.dev-show-code) gesetzt. */
    public record LoginRequest(RequestResult result, String devCode) {
        static LoginRequest of(RequestResult result) {
            return new LoginRequest(result, null);
        }
    }

    public LoginRequest requestLogin(String rawEmail, String ip) {
        if (!isLoginEnabled()) {
            return LoginRequest.of(RequestResult.DISABLED);
        }
        String email = normalize(rawEmail);
        if (email == null) {
            return LoginRequest.of(RequestResult.INVALID_EMAIL);
        }
        if (!allow("mail:" + email, MAX_REQUESTS_PER_EMAIL_PER_HOUR) || !allow("ip:" + ip, MAX_REQUESTS_PER_IP_PER_HOUR)) {
            return LoginRequest.of(RequestResult.RATE_LIMITED);
        }
        String token = randomToken();
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        repository.replaceLoginToken(new LoginToken(email, hash(token), hash(email + ":" + code), 0,
                LocalDateTime.now().plusMinutes(TOKEN_MINUTES)));
        String link = publicUrl + "/?login=" + token;
        String text = "Hi,\n\n"
                + "log in to Proxyprinter with this link:\n\n" + link + "\n\n"
                + "Or enter this code in the login window: " + code + "\n\n"
                + "The link and the code work once and expire in " + TOKEN_MINUTES + " minutes. "
                + "If you did not ask for this, just ignore this mail.\n\n"
                + "Proxyprinter\n" + publicUrl + "\n";
        if (mailService.isEnabled()) {
            mailService.send(email, "Your Proxyprinter login", text);
        } else {
            log.info("Login for {} (mail delivery is off, auth.dev-log-links=true): {} code {}", email, link, code);
        }
        return new LoginRequest(RequestResult.SENT, showCode ? code : null);
    }

    /** Löst den Link-Token ein. Gibt das Session-Token für das Cookie zurück oder leer. */
    public Optional<String> verifyToken(String token, String userAgent) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<LoginToken> found = repository.findLoginTokenByHash(hash(token.trim()));
        if (found.isEmpty() || found.get().expiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        return Optional.of(startSession(found.get().email(), userAgent));
    }

    /** Löst den Code ein. Nach {@value #MAX_CODE_ATTEMPTS} Fehlversuchen ist der Login verbraucht. */
    public Optional<String> verifyCode(String rawEmail, String code, String userAgent) {
        String email = normalize(rawEmail);
        if (email == null || code == null) {
            return Optional.empty();
        }
        Optional<LoginToken> found = repository.findLoginTokenByEmail(email);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        LoginToken token = found.get();
        if (token.expiresAt().isBefore(LocalDateTime.now()) || token.attempts() >= MAX_CODE_ATTEMPTS) {
            repository.deleteLoginToken(email);
            return Optional.empty();
        }
        if (!MessageDigest.isEqual(token.codeHash().getBytes(StandardCharsets.UTF_8),
                hash(email + ":" + code.trim()).getBytes(StandardCharsets.UTF_8))) {
            repository.incrementAttempts(email);
            return Optional.empty();
        }
        return Optional.of(startSession(email, userAgent));
    }

    private String startSession(String email, String userAgent) {
        repository.deleteLoginToken(email);
        AppUser user = repository.findOrCreateUser(email);
        String session = randomToken();
        repository.insertSession(hash(session), user.id(), userAgent);
        return session;
    }

    public Optional<AppUser> findUser(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        String hash = hash(sessionToken);
        Optional<AppUser> user = repository.findUserBySession(hash);
        user.ifPresent(u -> repository.touchSession(hash));
        return user;
    }

    public void logout(String sessionToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            repository.deleteSession(hash(sessionToken));
        }
    }

    public void logoutEverywhere(AppUser user) {
        repository.deleteSessionsOf(user.id());
    }

    public void deleteAccount(AppUser user) {
        repository.deleteUser(user.id());
        log.info("Deleted account {}", user.id());
    }

    @Scheduled(cron = "${auth.cleanup.cron:0 15 4 * * *}")
    public int cleanup() {
        int deleted = repository.deleteExpired(LocalDateTime.now(), LocalDateTime.now().minusDays(SESSION_DAYS));
        if (deleted > 0) {
            log.info("Deleted {} expired login tokens and sessions", deleted);
        }
        return deleted;
    }

    /** Kleinbuchstaben, ohne Leerzeichen; null bei ungültiger Adresse. */
    static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String e = email.trim().toLowerCase(Locale.ROOT);
        return MailService.isValidAddress(e) ? e : null;
    }

    private boolean allow(String key, int max) {
        long now = System.currentTimeMillis();
        Deque<Long> times = requestsPerKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && now - times.peekFirst() > HOUR_MS) {
                times.pollFirst();
            }
            if (times.size() >= max) {
                return false;
            }
            times.addLast(now);
            return true;
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
