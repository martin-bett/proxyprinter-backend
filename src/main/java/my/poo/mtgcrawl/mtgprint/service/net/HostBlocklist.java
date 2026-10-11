package my.poo.mtgcrawl.mtgprint.service.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Domains und IPs, deren Betreiber nicht als Quelle genutzt werden wollen (Tabelle proxymaster.blocked_host).
 * Eine Domain sperrt auch alle Subdomains, eine IP-Zeile darf ein Netz sein (1.2.3.0/24, 2001:db8::/32).
 * Gesperrte Hosts lassen sich nicht in Configs speichern, Configs mit ihnen werden nicht ausgeliefert,
 * und der OutboundUrlGuard lässt keinen Abruf zu ihnen durch (auch nicht über Weiterleitungen).
 * Die Liste wird höchstens alle blocklist.refresh-seconds neu gelesen, Änderungen in der DB wirken also ohne Deploy.
 */
@Slf4j
@Component
public class HostBlocklist {

    public static final String CODE = "blocklist.blockedHost";

    /** Sperrt nichts, für Tests und Klassen ohne Spring. */
    public static final HostBlocklist NONE = new HostBlocklist(List::of, Duration.ofDays(365_000), Clock.systemUTC());

    private static final Pattern URL_HOST = Pattern.compile("^\\s*[a-zA-Z][a-zA-Z0-9+.-]*://([^/?#\\s]*)");
    private static final Pattern IPV4 = Pattern.compile("^[0-9]+(\\.[0-9]+){0,3}$");

    private final Supplier<List<String>> loader;
    private final Duration refresh;
    private final Clock clock;

    private volatile Rules rules = new Rules(List.of(), List.of());
    private volatile Instant loadedAt;

    @Autowired
    public HostBlocklist(JdbcTemplate jdbc, @Value("${blocklist.refresh-seconds:60}") long refreshSeconds) {
        this(() -> jdbc.queryForList("SELECT host FROM proxymaster.blocked_host", String.class),
                Duration.ofSeconds(refreshSeconds), Clock.systemUTC());
    }

    public HostBlocklist(Supplier<List<String>> loader, Duration refresh, Clock clock) {
        this.loader = loader;
        this.refresh = refresh;
        this.clock = clock;
    }

    /** Wirft, wenn der Host der URL oder (bei IP-Zeilen) eine seiner Adressen gesperrt ist. */
    public void check(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return;
        }
        if (isBlocked(host)) {
            throw blocked(host);
        }
        Rules current = rules();
        if (current.networks.isEmpty() || literal(host).isPresent()) {
            return;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (current.matches(address)) {
                    throw blocked(host);
                }
            }
        } catch (UnknownHostException e) {
            // Meldet der OutboundUrlGuard (proxy.hostNotFound), hier gibt es nichts zu sperren
        }
    }

    /**
     * Prüft URLs einer Config (dürfen Platzhalter wie {id} enthalten) nur anhand des Hostnamens, ohne DNS.
     * Liefert den ersten gesperrten Host.
     */
    public Optional<String> blockedHostIn(String... urls) {
        for (String url : urls) {
            Optional<String> host = hostOf(url);
            if (host.isPresent() && isBlocked(host.get())) {
                return host;
            }
        }
        return Optional.empty();
    }

    /** Für das Speichern einer Config: wirft mit Code blocklist.blockedHost, wenn eine der URLs gesperrt ist. */
    public void checkConfigUrls(String... urls) {
        blockedHostIn(urls).ifPresent(host -> {
            throw blocked(host);
        });
    }

    public boolean isBlocked(String host) {
        String normalized = normalize(host);
        if (normalized.isEmpty()) {
            return false;
        }
        Rules current = rules();
        Optional<InetAddress> address = literal(normalized);
        if (address.isPresent()) {
            return current.matches(address.get());
        }
        return current.domains.stream().anyMatch(d -> normalized.equals(d) || normalized.endsWith("." + d));
    }

    /** Nach einer Änderung der Tabelle: beim nächsten Zugriff neu lesen. */
    public void invalidate() {
        loadedAt = null;
    }

    /** IP-Literal ohne DNS-Abfrage? */
    public static boolean isIpLiteral(String host) {
        return literal(normalize(host)).isPresent();
    }

    private static BlockedUrlException blocked(String host) {
        return new BlockedUrlException(CODE, host + " is blocked at the request of its operator.", Map.of("host", host));
    }

    /** Host einer (auch unvollständigen) URL ohne Benutzer und Port; ohne Schema wird der Anfang als Host gelesen. */
    public static Optional<String> hostOf(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        Matcher m = URL_HOST.matcher(url);
        String authority = m.find() ? m.group(1) : url.trim().split("[/?#\\s]", 2)[0];
        authority = authority.substring(authority.lastIndexOf('@') + 1);
        String host;
        if (authority.startsWith("[")) {
            int end = authority.indexOf(']');
            host = end > 0 ? authority.substring(1, end) : authority.substring(1);
        } else if (authority.indexOf(':') != authority.lastIndexOf(':')) {
            host = authority; // IPv6 ohne Klammern (nur in Einträgen der Liste)
        } else {
            int colon = authority.indexOf(':');
            host = colon >= 0 ? authority.substring(0, colon) : authority;
        }
        String normalized = normalize(host);
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }

    private static String normalize(String host) {
        String h = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (h.startsWith("[") && h.endsWith("]")) {
            h = h.substring(1, h.length() - 1);
        }
        while (h.endsWith(".")) {
            h = h.substring(0, h.length() - 1);
        }
        return h;
    }

    /** IP-Literal ohne DNS-Abfrage, sonst leer. */
    private static Optional<InetAddress> literal(String host) {
        if (!(IPV4.matcher(host).matches() || host.contains(":"))) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByName(host));
        } catch (UnknownHostException | SecurityException e) {
            return Optional.empty();
        }
    }

    private Rules rules() {
        Instant now = clock.instant();
        Instant last = loadedAt;
        if (last == null || !now.isBefore(last.plus(refresh))) {
            synchronized (this) {
                if (loadedAt == null || !now.isBefore(loadedAt.plus(refresh))) {
                    reload();
                    loadedAt = now;
                }
            }
        }
        return rules;
    }

    // Fehler beim Lesen (z. B. Tabelle noch nicht angelegt): alte Liste behalten, beim nächsten Intervall erneut versuchen
    private void reload() {
        try {
            rules = Rules.parse(loader.get());
        } catch (RuntimeException e) {
            log.warn("Blocklist could not be loaded, keeping {} entries: {}", rules.size(), e.getMessage());
        }
    }

    private record Network(byte[] address, int prefix) {
        boolean contains(InetAddress candidate) {
            byte[] other = candidate.getAddress();
            if (other.length != address.length) {
                return false;
            }
            int full = prefix / 8;
            for (int i = 0; i < full; i++) {
                if (other[i] != address[i]) {
                    return false;
                }
            }
            int rest = prefix % 8;
            if (rest == 0) {
                return true;
            }
            int mask = (0xff << (8 - rest)) & 0xff;
            return (other[full] & mask) == (address[full] & mask);
        }
    }

    private record Rules(List<String> domains, List<Network> networks) {

        int size() {
            return domains.size() + networks.size();
        }

        boolean matches(InetAddress address) {
            InetAddress plain = unmapped(address);
            return networks.stream().anyMatch(n -> n.contains(plain));
        }

        static Rules parse(List<String> entries) {
            List<String> domains = new ArrayList<>();
            List<Network> networks = new ArrayList<>();
            for (String raw : entries) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String entry = raw.trim().toLowerCase(Locale.ROOT);
                String prefix = null;
                int slash = entry.indexOf('/');
                if (slash > 0 && !entry.contains("://")) {
                    prefix = entry.substring(slash + 1);
                    entry = entry.substring(0, slash);
                }
                Optional<String> host = hostOf(entry);
                if (host.isEmpty()) {
                    continue;
                }
                String h = host.get();
                while (h.startsWith("*.") || h.startsWith(".")) {
                    h = h.substring(h.indexOf('.') + 1);
                }
                Optional<InetAddress> ip = literal(h);
                if (ip.isPresent()) {
                    InetAddress address = unmapped(ip.get());
                    int bits = address.getAddress().length * 8;
                    int p = bits;
                    if (prefix != null) {
                        try {
                            p = Math.max(0, Math.min(bits, Integer.parseInt(prefix.trim())));
                        } catch (NumberFormatException e) {
                            log.warn("Blocklist entry {} has an invalid prefix, blocking the single address", raw);
                        }
                    }
                    networks.add(new Network(address.getAddress(), p));
                } else if (!h.isEmpty()) {
                    domains.add(h);
                }
            }
            return new Rules(List.copyOf(domains), List.copyOf(networks));
        }

        /** ::ffff:a.b.c.d wie a.b.c.d behandeln. */
        private static InetAddress unmapped(InetAddress address) {
            byte[] b = address.getAddress();
            if (b.length != 16 || address instanceof Inet4Address) {
                return address;
            }
            for (int i = 0; i < 10; i++) {
                if (b[i] != 0) {
                    return address;
                }
            }
            if (b[10] != (byte) 0xff || b[11] != (byte) 0xff) {
                return address;
            }
            try {
                return InetAddress.getByAddress(new byte[] { b[12], b[13], b[14], b[15] });
            } catch (UnknownHostException e) {
                return address;
            }
        }
    }
}
