package my.poo.mtgcrawl.mtgprint.service.gallery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class RedditGallerySourceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final GalleryHttp http = mock(GalleryHttp.class);
    private final RedditGallerySource source = new RedditGallerySource(http, mapper, "", "", "test-agent");

    private JsonNode listing(String postJson) throws Exception {
        return mapper.readTree("[{\"kind\":\"Listing\",\"data\":{\"children\":[{\"kind\":\"t3\",\"data\":"
                + postJson.replace('\'', '"') + "}]}},{\"kind\":\"Listing\",\"data\":{\"children\":[]}}]");
    }

    @Test
    void extractsPostIdFromKnownLinkForms() {
        assertEquals("1gwkskz", RedditGallerySource.extractPostId(
                "https://www.reddit.com/r/magicproxies/comments/1gwkskz/iron_fist_proxy_mockups/").orElseThrow());
        assertEquals("1gwkskz", RedditGallerySource.extractPostId("https://old.reddit.com/comments/1gwkskz").orElseThrow());
        assertEquals("1gwkskz", RedditGallerySource.extractPostId("https://www.reddit.com/gallery/1gwkskz").orElseThrow());
        assertEquals("1gwkskz", RedditGallerySource.extractPostId("https://redd.it/1gwkskz").orElseThrow());
        assertTrue(RedditGallerySource.extractPostId("https://www.reddit.com/r/magicproxies/s/AbCdEf123").isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void followsShareLinkToPost() {
        HttpResponse<String> redirect = mock(HttpResponse.class);
        when(redirect.headers()).thenReturn(HttpHeaders.of(
                Map.of("location", List.of("https://www.reddit.com/r/magicproxies/comments/1abcde/some_title/?share_id=x")),
                (a, b) -> true));
        when(http.get(eq("https://www.reddit.com/r/magicproxies/s/AbCdEf123"), anyMap())).thenReturn(redirect);

        assertEquals("1abcde", source.resolvePostId("https://www.reddit.com/r/magicproxies/s/AbCdEf123"));
    }

    @Test
    void rejectsLinksWithoutPost() {
        GallerySourceException e = assertThrows(GallerySourceException.class,
                () -> source.resolvePostId("https://www.reddit.com/r/magicproxies/"));
        assertTrue(e.getMessage().contains("kein Reddit-Post"));
    }

    @Test
    void galleryUsesOrderOfGalleryDataAndOriginalFiles() throws Exception {
        RedditGallerySource.RedditPost post = source.parse(listing("{'title':'Iron Fist','is_gallery':true,"
                + "'gallery_data':{'items':[{'media_id':'bbb','id':1},{'media_id':'aaa','caption':'Sol Ring','id':2},{'media_id':'gif','id':3}]},"
                + "'media_metadata':{"
                + "'aaa':{'status':'valid','e':'Image','m':'image/png','s':{'u':'https://preview.redd.it/aaa.png?width=1&s=x'}},"
                + "'bbb':{'status':'valid','e':'Image','m':'image/jpg','s':{'u':'https://preview.redd.it/bbb.jpg'}},"
                + "'gif':{'status':'valid','e':'AnimatedImage','m':'image/gif'}}}"));

        assertEquals(List.of(
                new GalleryImage("Iron Fist 1", "https://i.redd.it/bbb.jpg"),
                new GalleryImage("Sol Ring", "https://i.redd.it/aaa.png")), post.result().images());
    }

    @Test
    void crosspostTakesImagesFromOriginal() throws Exception {
        RedditGallerySource.RedditPost post = source.parse(listing("{'title':'Crosspost','url':'/r/magicproxies/comments/1x/',"
                + "'crosspost_parent_list':[{'title':'Original','url':'https://i.redd.it/single.jpeg'}]}"));

        assertEquals(List.of(new GalleryImage("Crosspost", "https://i.redd.it/single.jpeg")), post.result().images());
    }

    @Test
    void selfPostWithDriveLinkReportsTheLink() throws Exception {
        RedditGallerySource.RedditPost post = source.parse(listing("{'title':'My set','is_self':true,"
                + "'url':'https://www.reddit.com/r/magicproxies/comments/1x/my_set/',"
                + "'selftext':'Alle Karten hier: [Drive](https://drive.google.com/drive/folders/1AbCdEfGhIjKlMn?usp=sharing) viel Spass'}"));

        assertTrue(post.result().images().isEmpty());
        assertEquals(List.of("https://drive.google.com/drive/folders/1AbCdEfGhIjKlMn?usp=sharing"), post.linkedUrls());
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsBlockedRequestsClearly() {
        HttpResponse<String> blocked = mock(HttpResponse.class);
        when(blocked.statusCode()).thenReturn(403);
        when(http.get(eq("https://www.reddit.com/comments/1abcde.json?raw_json=1&limit=1"), anyMap())).thenReturn(blocked);

        GallerySourceException e = assertThrows(GallerySourceException.class,
                () -> source.load("https://www.reddit.com/comments/1abcde"));
        assertTrue(e.getMessage().contains("403"));
    }
}
