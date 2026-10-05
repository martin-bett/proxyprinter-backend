package my.poo.mtgcrawl.mtgprint.service.deckfinder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import my.poo.mtgcrawl.mtgprint.entitiy.DeckFinderConfig;

class DeckFinderFetchServiceTest {

    private static final String ARCHIDEKT = "archidekt\\.com/(?:api/)?decks/(?<id>\\d+)";

    private final DeckFinderHttp http = mock(DeckFinderHttp.class);
    private final MutableClock clock = new MutableClock();
    private final DeckFinderFetchService service = new DeckFinderFetchService(http, clock);

    private static DeckFinderConfig archidekt() {
        DeckFinderConfig c = new DeckFinderConfig();
        c.setLinkPattern(ARCHIDEKT);
        c.setFetchUrl("https://archidekt.com/api/decks/{id}/");
        c.setDataFormat("json");
        c.setSearchUrl("https://archidekt.com/api/decks/v3/?name={query}&page={page}");
        return c;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(status);
        when(r.body()).thenReturn(body);
        return r;
    }

    @Test
    void readsIdFromLinkOrTakesPlainId() {
        assertEquals("1363300", DeckFinderFetchService.resolveDeckId(ARCHIDEKT, "https://archidekt.com/decks/1363300/commander"));
        assertEquals("1363300", DeckFinderFetchService.resolveDeckId(ARCHIDEKT, "  www.archidekt.com/decks/1363300  "));
        assertEquals("1363300", DeckFinderFetchService.resolveDeckId(ARCHIDEKT, "1363300"));
        assertEquals("atraxa-superfriends", DeckFinderFetchService.resolveDeckId(null, "atraxa-superfriends"));
    }

    @Test
    void rejectsForeignLinksAndBrokenPatterns() {
        DeckFinderException foreign = assertThrows(DeckFinderException.class,
                () -> DeckFinderFetchService.resolveDeckId(ARCHIDEKT, "https://www.moxfield.com/decks/abc"));
        assertTrue(foreign.getMessage().contains("passt nicht"));
        DeckFinderException noGroup = assertThrows(DeckFinderException.class,
                () -> DeckFinderFetchService.resolveDeckId("archidekt\\.com/decks/(\\d+)", "https://archidekt.com/decks/1"));
        assertTrue(noGroup.getMessage().contains("(?<id>"));
        assertThrows(DeckFinderException.class, () -> DeckFinderFetchService.resolveDeckId(ARCHIDEKT, " "));
    }

    @Test
    void encodesTemplateValues() {
        assertEquals("https://x.org/s?name=Atraxa%2C%20Praetors%27%20Voice&page=2",
                DeckFinderFetchService.fillTemplate("https://x.org/s?name={query}&page={page}",
                        Map.of("query", "Atraxa, Praetors' Voice", "page", "2")));
    }

    @Test
    void rejectsNonHttpAndInternalTargets() {
        assertThrows(DeckFinderException.class, () -> DeckFinderFetchService.checkedUri("file:///etc/passwd"));
        assertThrows(DeckFinderException.class, () -> DeckFinderFetchService.checkedUri("http://127.0.0.1:8080/api/configs"));
        assertThrows(DeckFinderException.class, () -> DeckFinderFetchService.checkedUri("http://192.168.1.5/"));
    }

    @Test
    void mapsBlockedAndMissingDecksToReadableErrors() {
        DeckFinderFetchService local = spyWithoutDns();
        HttpResponse<String> notFound = response(404, "");
        HttpResponse<String> blocked = response(403, "");
        when(http.get(any())).thenReturn(notFound, blocked);

        DeckFinderException e1 = assertThrows(DeckFinderException.class, () -> local.fetchDeck(archidekt(), "1"));
        assertEquals(HttpStatus.NOT_FOUND, e1.getStatus());
        DeckFinderException e2 = assertThrows(DeckFinderException.class, () -> local.fetchDeck(archidekt(), "2"));
        assertEquals(HttpStatus.BAD_GATEWAY, e2.getStatus());
        assertTrue(e2.getMessage().contains("blockt"));
    }

    @Test
    void cachesSuccessfulResponsesForTenMinutes() {
        DeckFinderFetchService local = spyWithoutDns();
        HttpResponse<String> ok = response(200, "{\"name\":\"deck\"}");
        when(http.get(any())).thenReturn(ok);

        assertEquals("{\"name\":\"deck\"}", local.fetchDeck(archidekt(), "https://archidekt.com/decks/5/x").body());
        assertEquals("application/json", local.fetchDeck(archidekt(), "5").contentType());
        verify(http, times(1)).get(URI.create("https://archidekt.com/api/decks/5/"));

        clock.now = clock.now.plus(DeckFinderFetchService.CACHE_TTL).plusSeconds(1);
        local.fetchDeck(archidekt(), "5");
        verify(http, times(2)).get(URI.create("https://archidekt.com/api/decks/5/"));
    }

    @Test
    void searchNeedsSearchUrlAndQuery() {
        DeckFinderConfig noSearch = archidekt();
        noSearch.setSearchUrl(null);
        assertThrows(DeckFinderException.class, () -> service.search(noSearch, "atraxa", 1));
        assertThrows(DeckFinderException.class, () -> service.search(archidekt(), "  ", 1));
    }

    /** Abruf ohne DNS-Prüfung, die im Test nicht ins Netz soll. */
    private DeckFinderFetchService spyWithoutDns() {
        return new DeckFinderFetchService(http, clock) {
            @Override
            URI checked(String url) {
                return URI.create(url);
            }
        };
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-05T12:00:00Z");

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
