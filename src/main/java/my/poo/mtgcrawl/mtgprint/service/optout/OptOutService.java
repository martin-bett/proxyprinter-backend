package my.poo.mtgcrawl.mtgprint.service.optout;

import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.service.mail.MailService;
import my.poo.mtgcrawl.mtgprint.service.net.HostBlocklist;
import my.poo.mtgcrawl.mtgprint.service.pdf.MailTexts;

/**
 * Selbst-Sperre für Betreiber: Wer nachweist, dass ihm ein Host gehört, setzt ihn ohne Zutun des Admins auf die
 * Blocklist (proxymaster.blocked_host). Nachweis per Mail-Link an eine Standardadresse der Domain (admin@, webmaster@ …)
 * oder per Prüf-Token als DNS-TXT-Eintrag bzw. Datei unter /.well-known (siehe OwnershipCheck).
 * Anfragen liegen in proxymaster.blocklist_request, nur der Hash des Tokens wird gespeichert.
 */
@Slf4j
@Service
public class OptOutService {

    /** Adressen, die laut RFC 2142 / CA/Browser-Forum dem Domain-Inhaber gehören. */
    public static final List<String> MAILBOXES = List.of("admin", "administrator", "webmaster", "hostmaster", "postmaster");
    static final Duration MAIL_VALIDITY = Duration.ofHours(24);
    static final Duration CHALLENGE_VALIDITY = Duration.ofDays(7);
    static final int MAX_REQUESTS_PER_IP_PER_HOUR = 10;
    static final int MAX_REQUESTS_PER_HOST_PER_HOUR = 5;
    static final int MAX_VERIFY_PER_IP_PER_HOUR = 30;
    private static final long HOUR_MS = 60 * 60 * 1000L;
    private static final Pattern DOMAIN = Pattern.compile("^(?=.{1,253}$)([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z][a-z0-9-]{0,61}[a-z0-9]$");
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Method { MAIL, CHALLENGE }

    /** Antwort an das Frontend; je nach status sind nur einzelne Felder gesetzt. */
    public record Result(String status, String host, String email, String token, String dnsName, String dnsValue,
            String fileUrl) {
        static Result blocked(String host) {
            return new Result("blocked", host, null, null, null, null, null);
        }
    }

    private final JdbcTemplate jdbc;
    private final MailService mailService;
    private final OwnershipCheck check;
    private final HostBlocklist blocklist;
    private final String publicUrl;
    private final Clock clock;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    @Autowired
    public OptOutService(JdbcTemplate jdbc, MailService mailService, OwnershipCheck check, HostBlocklist blocklist,
            @Value("${app.public-url:https://proxyprinter.eu}") String publicUrl) {
        this(jdbc, mailService, check, blocklist, publicUrl, Clock.systemDefaultZone());
    }

    OptOutService(JdbcTemplate jdbc, MailService mailService, OwnershipCheck check, HostBlocklist blocklist,
            String publicUrl, Clock clock) {
        this.jdbc = jdbc;
        this.mailService = mailService;
        this.check = check;
        this.blocklist = blocklist;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        this.clock = clock;
    }

    public boolean isMailEnabled() {
        return mailService.isEnabled();
    }

    /** Neue Anfrage: Mail-Link verschicken oder Prüf-Token für DNS/Datei ausgeben. */
    public Result request(String rawHost, String rawMethod, String mailbox, String language, String ip) {
        String host = normalizeHost(rawHost);
        if (blocklist.isBlocked(host)) {
            return Result.blocked(host);
        }
        Method method = "mail".equalsIgnoreCase(rawMethod) ? Method.MAIL : Method.CHALLENGE;
        String email = null;
        if (method == Method.MAIL) {
            if (HostBlocklist.isIpLiteral(host)) {
                throw new OptOutException(HttpStatus.BAD_REQUEST, "blocklist.mailNeedsDomain",
                        "Confirmation by mail needs a domain, for an IP use the file check.", Map.of("host", host));
            }
            if (mailbox == null || !MAILBOXES.contains(mailbox.toLowerCase(Locale.ROOT))) {
                throw new OptOutException(HttpStatus.BAD_REQUEST, "blocklist.invalidMailbox", "Unknown mailbox.", Map.of());
            }
            if (!mailService.isEnabled()) {
                throw new OptOutException(HttpStatus.SERVICE_UNAVAILABLE, "blocklist.mailDisabled",
                        "Mail delivery is not available, use the DNS or file check.", Map.of());
            }
            email = mailbox.toLowerCase(Locale.ROOT) + "@" + host;
        }
        if (!allow("ip:" + ip, MAX_REQUESTS_PER_IP_PER_HOUR) || !allow("host:" + host, MAX_REQUESTS_PER_HOST_PER_HOUR)) {
            throw rateLimited();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        jdbc.update("DELETE FROM proxymaster.blocklist_request WHERE expires_at < ?", Timestamp.valueOf(now));
        String token = randomToken();
        Duration validity = method == Method.MAIL ? MAIL_VALIDITY : CHALLENGE_VALIDITY;
        jdbc.update("INSERT INTO proxymaster.blocklist_request (token_hash, host, method, email, ip, created_at, expires_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)", hash(token), host, method.name(), email, ip,
                Timestamp.valueOf(now), Timestamp.valueOf(now.plus(validity)));
        if (method == Method.MAIL) {
            String lang = MailTexts.normalize(language);
            String link = publicUrl + "/?blockverify=" + token;
            String text = MailTexts.get(lang, "greeting") + "\n\n"
                    + MailTexts.get(lang, "optout.body", host, publicUrl) + "\n\n"
                    + MailTexts.get(lang, "optout.link", MAIL_VALIDITY.toHours()) + "\n" + link + "\n\n"
                    + MailTexts.get(lang, "optout.ignore", host) + "\n\n"
                    + "Proxyprinter\n" + publicUrl + "\n";
            if (!mailService.send(email, MailTexts.get(lang, "optout.subject", host), text)) {
                throw rateLimited();
            }
            log.info("Blocklist confirmation mail for {} sent to {}", host, email);
            return new Result("sent", host, email, null, null, null, null);
        }
        String fileHost = host.contains(":") ? "[" + host + "]" : host;
        return new Result("challenge", host, null, token,
                HostBlocklist.isIpLiteral(host) ? null : OwnershipCheck.DNS_PREFIX + host,
                OwnershipCheck.DNS_VALUE_PREFIX + token,
                "https://" + fileHost + OwnershipCheck.FILE_PATH);
    }

    /** Mail-Link einlösen oder DNS/Datei prüfen. Bei Erfolg steht der Host auf der Blocklist. */
    public Result verify(String token, String ip) {
        if (!allow("verify:" + ip, MAX_VERIFY_PER_IP_PER_HOUR)) {
            throw rateLimited();
        }
        if (token == null || token.isBlank()) {
            throw invalidToken();
        }
        String trimmed = token.trim();
        Optional<Map<String, Object>> row = jdbc.queryForList(
                "SELECT host, method, email FROM proxymaster.blocklist_request WHERE token_hash = ? AND expires_at >= ?",
                hash(trimmed), Timestamp.valueOf(LocalDateTime.now(clock))).stream().findFirst();
        if (row.isEmpty()) {
            throw invalidToken();
        }
        String host = (String) row.get().get("host");
        String reason;
        if (Method.MAIL.name().equals(row.get().get("method"))) {
            reason = "self-service, confirmed by mail to " + row.get().get("email");
        } else if (!HostBlocklist.isIpLiteral(host) && check.dnsHasToken(host, trimmed)) {
            reason = "self-service, verified by DNS TXT record";
        } else if (check.fileHasToken(host, trimmed)) {
            reason = "self-service, verified by " + OwnershipCheck.FILE_PATH;
        } else {
            throw new OptOutException(HttpStatus.CONFLICT, "blocklist.notVerified",
                    "Neither the DNS record nor the file contains the token yet.", Map.of("host", host));
        }
        jdbc.update("INSERT INTO proxymaster.blocked_host (host, reason) VALUES (?, ?) ON CONFLICT (host) DO NOTHING", host, reason);
        jdbc.update("DELETE FROM proxymaster.blocklist_request WHERE host = ?", host);
        blocklist.invalidate();
        log.warn("{} added to the blocklist ({}, from {})", host, reason, ip);
        return Result.blocked(host);
    }

    /** Domain oder IP aus Eingabe oder URL; Domains als ASCII (Punycode), ohne Port und Pfad. */
    static String normalizeHost(String raw) {
        Optional<String> parsed = HostBlocklist.hostOf(raw);
        if (parsed.isPresent()) {
            String host = parsed.get();
            if (HostBlocklist.isIpLiteral(host)) {
                return host;
            }
            try {
                String ascii = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
                if (DOMAIN.matcher(ascii).matches()) {
                    return ascii;
                }
            } catch (IllegalArgumentException e) {
                // ungültig, unten
            }
        }
        throw new OptOutException(HttpStatus.BAD_REQUEST, "blocklist.invalidHost",
                "Enter a domain (example.com) or an IP address.", Map.of());
    }

    private static OptOutException invalidToken() {
        return new OptOutException(HttpStatus.NOT_FOUND, "blocklist.tokenInvalid",
                "This link or token is unknown or has expired.", Map.of());
    }

    private static OptOutException rateLimited() {
        return new OptOutException(HttpStatus.TOO_MANY_REQUESTS, "blocklist.rateLimited",
                "Too many requests, try again later.", Map.of());
    }

    private boolean allow(String key, int max) {
        long now = clock.millis();
        Deque<Long> times = attempts.computeIfAbsent(key, k -> new ArrayDeque<>());
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
        byte[] bytes = new byte[24];
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
