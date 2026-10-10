package my.poo.mtgcrawl.mtgprint.service.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class HostBlocklistTest {

    private static HostBlocklist of(String... entries) {
        return new HostBlocklist(() -> List.of(entries), Duration.ofMinutes(1), Clock.systemUTC());
    }

    @Test
    void domainSperrtAuchSubdomains() {
        HostBlocklist list = of("Example.com");
        assertTrue(list.isBlocked("example.com"));
        assertTrue(list.isBlocked("api.EXAMPLE.com."));
        assertTrue(list.isBlocked("a.b.example.com"));
        assertFalse(list.isBlocked("notexample.com"));
        assertFalse(list.isBlocked("example.com.evil.org"));
        assertFalse(list.isBlocked("example.org"));
    }

    @Test
    void eintraegeDuerfenUrlsOderWildcardsSein() {
        HostBlocklist list = of("https://www.shop.net/path", "*.cards.io", " ", "");
        assertTrue(list.isBlocked("www.shop.net"));
        assertFalse(list.isBlocked("shop.net"));
        assertTrue(list.isBlocked("cards.io"));
        assertTrue(list.isBlocked("img.cards.io"));
    }

    @Test
    void ipUndNetze() {
        HostBlocklist list = of("93.184.215.14", "203.0.113.0/24", "2001:db8::/32", "[2a00:1450::1]");
        assertTrue(list.isBlocked("93.184.215.14"));
        assertFalse(list.isBlocked("93.184.215.15"));
        assertTrue(list.isBlocked("203.0.113.200"));
        assertFalse(list.isBlocked("203.0.114.1"));
        assertTrue(list.isBlocked("[2001:db8:1::5]"));
        assertTrue(list.isBlocked("2a00:1450::1"));
        assertTrue(list.isBlocked("::ffff:203.0.113.7"));
        assertFalse(list.isBlocked("example.com"));
    }

    @Test
    void configUrlsMitPlatzhalternUndOhneSchema() {
        HostBlocklist list = of("example.com");
        assertEquals(Optional.of("api.example.com"),
                list.blockedHostIn(null, "", "https://other.org", "https://user:pw@api.example.com:8443/v1/{id}?q={input}"));
        assertEquals(Optional.of("www.example.com"), list.blockedHostIn("www.example.com/cards"));
        assertEquals(Optional.of("{sub}.example.com"), list.blockedHostIn("https://{sub}.example.com/x"));
        assertEquals(Optional.empty(), list.blockedHostIn("https://example.org/?ref=example.com", null));
    }

    @Test
    void checkWirftMitCodeUndHost() {
        HostBlocklist list = of("example.com");
        BlockedUrlException e = assertThrows(BlockedUrlException.class, () -> list.checkConfigUrls("https://cdn.example.com/a.png"));
        assertEquals(HostBlocklist.CODE, e.getCode());
        assertEquals("cdn.example.com", e.getParams().get("host"));
        assertThrows(BlockedUrlException.class, () -> list.check(URI.create("http://EXAMPLE.com:80/")));
        list.check(URI.create("https://example.org/"));
    }

    @Test
    void guardSperrtAuchWennInterneAdressenErlaubtSind() {
        OutboundUrlGuard guard = new OutboundUrlGuard(true, of("127.0.0.1", "blocked.test"));
        assertEquals(HostBlocklist.CODE, assertThrows(BlockedUrlException.class, () -> guard.check("http://127.0.0.1:8080/")).getCode());
        assertEquals(HostBlocklist.CODE, assertThrows(BlockedUrlException.class, () -> guard.check("https://img.blocked.test/x")).getCode());
        assertEquals(URI.create("http://127.0.0.2/"), guard.check("http://127.0.0.2/"));
    }

    @Test
    void liestDieListeNachAblaufNeuUndBehaeltSieBeiFehlern() {
        List<String> entries = new ArrayList<>(List.of("a.com"));
        AtomicInteger loads = new AtomicInteger();
        boolean[] fail = { false };
        MutableClock clock = new MutableClock();
        HostBlocklist list = new HostBlocklist(() -> {
            loads.incrementAndGet();
            if (fail[0]) {
                throw new IllegalStateException("db down");
            }
            return List.copyOf(entries);
        }, Duration.ofSeconds(60), clock);

        assertTrue(list.isBlocked("a.com"));
        entries.add("b.com");
        assertFalse(list.isBlocked("b.com"));
        assertEquals(1, loads.get());

        clock.now = clock.now.plusSeconds(60);
        assertTrue(list.isBlocked("b.com"));
        assertEquals(2, loads.get());

        fail[0] = true;
        clock.now = clock.now.plusSeconds(60);
        assertTrue(list.isBlocked("a.com"));
        assertTrue(list.isBlocked("b.com"));
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-10T12:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
