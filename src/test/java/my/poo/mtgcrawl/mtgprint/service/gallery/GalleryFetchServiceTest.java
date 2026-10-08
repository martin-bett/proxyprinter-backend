package my.poo.mtgcrawl.mtgprint.service.gallery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;

class GalleryFetchServiceTest {

    private static final String REDDIT_PATTERN = "(?:/comments/|/gallery/|redd\\.it/)(?<id>[a-z0-9]{4,12})(?:[/?#]|$)";

    private final GalleryHttp http = mock(GalleryHttp.class);
    private final GalleryCredentials credentials = new GalleryCredentials();
    // DNS-Prüfung entfällt im Test
    private final GalleryFetchService service = new GalleryFetchService(http, credentials) {
        @Override
        URI checked(String url) {
            return URI.create(url);
        }
    };

    private static GalleryConfig reddit() {
        GalleryConfig c = new GalleryConfig();
        c.setBaseUrl("https://www.reddit.com/comments/{id}.json?raw_json=1&limit=1");
        c.setDataFormat("json");
        c.setLinkPattern(REDDIT_PATTERN);
        c.setFollowInputRedirects(true);
        c.setCredential("reddit");
        return c;
    }

    private void redditCredential(String cookie) {
        GalleryCredentials.Credential credential = new GalleryCredentials.Credential();
        credential.setHost("reddit.com");
        credential.setCookie(cookie);
        credentials.setCredentials(Map.of("reddit", credential));
    }

    private void stub(String url, HttpResponse<String> response) {
        when(http.get(eq(url), anyMap())).thenReturn(response);
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(String url, int status, String body, String location) {
        HttpResponse<String> r = mock(HttpResponse.class);
        when(r.statusCode()).thenReturn(status);
        when(r.body()).thenReturn(body);
        when(r.uri()).thenReturn(URI.create(url));
        Map<String, List<String>> headers = location == null ? Map.of() : Map.of("location", List.of(location));
        when(r.headers()).thenReturn(HttpHeaders.of(headers, (a, b) -> true));
        return r;
    }

    @Test
    void fillsUrlFromNamedGroupsAndSendsCredentialToMatchingHost() {
        redditCredential("session=abc");
        String url = "https://www.reddit.com/comments/1abc23x.json?raw_json=1&limit=1";
        stub(url, response(url, 200, "[]", null));

        GalleryFetchService.GalleryResponse result =
                service.fetch(reddit(), "https://www.reddit.com/r/magicproxies/comments/1abc23x/my_proxies/");

        assertEquals("[]", result.body());
        assertEquals("application/json", result.contentType());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(http).get(eq(url), headers.capture());
        assertEquals("session=abc", headers.getValue().get("Cookie"));
    }

    @Test
    void neverSendsCredentialToOtherHosts() {
        redditCredential("session=abc");
        GalleryConfig config = reddit();
        config.setBaseUrl("https://evil.example/{id}");
        HttpResponse<String> ok = response("https://evil.example/1abc23x", 200, "x", null);
        when(http.get(startsWith("https://evil.example/"), anyMap())).thenReturn(ok);

        service.fetch(config, "https://www.reddit.com/comments/1abc23x/");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(http).get(startsWith("https://evil.example/"), headers.capture());
        assertFalse(headers.getValue().containsKey("Cookie"));
    }

    @Test
    void followsShareLinkRedirectUntilPatternMatches() {
        redditCredential("session=abc");
        String share = "https://www.reddit.com/r/magicproxies/s/AbCdEf";
        stub(share, response(share, 301, "", "/r/magicproxies/comments/1abc23x/post/"));

        Map<String, String> values = service.variables(reddit(), share);

        assertEquals("1abc23x", values.get("id"));
    }

    @Test
    void rejectsLinksThatDoNotMatchAndRedirectsWhenNotAllowed() {
        GalleryConfig config = reddit();
        config.setFollowInputRedirects(false);
        GallerySourceException e = assertThrows(GallerySourceException.class,
                () -> service.variables(config, "https://example.com/foo"));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        verifyNoInteractions(http);
    }

    @Test
    void configWithoutPatternOrInputRunsWithEmptyParams() {
        GalleryConfig config = new GalleryConfig();
        config.setBaseUrl("https://example.com/gallery.json");
        config.setDataFormat("json");
        assertTrue(service.variables(config, null).isEmpty());
        assertEquals("v", service.variables(config, " v ").get("input"));
    }

    @Test
    void mapsBlockedAndMissingAnswers() {
        redditCredential("session=abc");
        String url = "https://www.reddit.com/comments/1abc23x.json?raw_json=1&limit=1";
        stub(url, response(url, 403, "", null));
        GallerySourceException blocked = assertThrows(GallerySourceException.class,
                () -> service.fetch(reddit(), "reddit.com/comments/1abc23x"));
        assertEquals(HttpStatus.BAD_GATEWAY, blocked.getStatus());
        assertTrue(blocked.getMessage().contains("blockt"));

        stub(url, response(url, 404, "", null));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(GallerySourceException.class, () -> service.fetch(reddit(), "reddit.com/comments/1abc23x")).getStatus());
    }

    @Test
    void reportsMissingServerCredential() {
        GallerySourceException e = assertThrows(GallerySourceException.class,
                () -> service.fetch(reddit(), "reddit.com/comments/1abc23x"));
        assertTrue(e.getMessage().contains("nicht eingerichtet"));
    }

    @Test
    void hostMatchingIncludesSubdomainsOnly() {
        assertTrue(GalleryCredentials.hostMatches("reddit.com", "oauth.reddit.com"));
        assertTrue(GalleryCredentials.hostMatches("reddit.com", "reddit.com"));
        assertFalse(GalleryCredentials.hostMatches("reddit.com", "notreddit.com"));
        assertFalse(GalleryCredentials.hostMatches(null, "reddit.com"));
    }
}
