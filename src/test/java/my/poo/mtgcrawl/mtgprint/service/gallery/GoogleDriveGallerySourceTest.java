package my.poo.mtgcrawl.mtgprint.service.gallery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

import java.net.http.HttpResponse;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class GoogleDriveGallerySourceTest {

    private static final String FOLDER_HTML = """
            <html><head><title>Proxies</title></head><body>
            <div class="flip-entries">
              <div class="flip-entry" id="entry-1FileAaaaaaaaaa" tabindex="0" role="link">
                <a href="https://drive.google.com/file/d/1FileAaaaaaaaaa/view?usp=drive_web" target="_blank">
                  <div class="flip-entry-info"><div class="flip-entry-title">Sol Ring.png</div></div></a></div>
              <div class="flip-entry" id="entry-1Subfolderbbbbb" tabindex="0" role="link">
                <a href="https://drive.google.com/drive/folders/1Subfolderbbbbb" target="_blank">
                  <div class="flip-entry-info"><div class="flip-entry-title">Tokens</div></div></a></div>
              <div class="flip-entry" id="entry-1Readmeccccccc" tabindex="0" role="link">
                <a href="https://drive.google.com/file/d/1Readmeccccccc/view?usp=drive_web" target="_blank">
                  <div class="flip-entry-info"><div class="flip-entry-title">readme.pdf</div></div></a></div>
              <div class="flip-entry" id="entry-1Fileddddddddd" tabindex="0" role="link">
                <a href="https://drive.google.com/file/d/1Fileddddddddd/view?usp=drive_web" target="_blank">
                  <div class="flip-entry-info"><div class="flip-entry-title">Lightning Bolt.JPG</div></div></a></div>
            </div></body></html>
            """;

    private final GalleryHttp http = mock(GalleryHttp.class);

    @Test
    void parsesEmbeddedFolderViewAndSkipsFoldersAndNonImages() {
        GallerySourceResult result = GoogleDriveGallerySource.parseEmbeddedFolderView(FOLDER_HTML);

        assertEquals("Proxies", result.title());
        assertEquals(List.of(
                new GalleryImage("Sol Ring", "https://drive.google.com/uc?export=download&id=1FileAaaaaaaaaa"),
                new GalleryImage("Lightning Bolt", "https://drive.google.com/uc?export=download&id=1Fileddddddddd")),
                result.images());
    }

    @Test
    @SuppressWarnings("unchecked")
    void loadsFolderWithoutApiKey() {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(FOLDER_HTML);
        when(http.get(startsWith("https://drive.google.com/embeddedfolderview?id=1FolderXyzXyzXyz"), anyMap())).thenReturn(response);

        GoogleDriveGallerySource source = new GoogleDriveGallerySource(http, new ObjectMapper(), "");
        assertEquals(2, source.load("https://drive.google.com/drive/folders/1FolderXyzXyzXyz?usp=sharing").images().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDriveApiWhenKeyConfigured() {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("{\"files\":[{\"id\":\"1Img\",\"name\":\"Counterspell.webp\"}]}");
        when(http.get(startsWith("https://www.googleapis.com/drive/v3/files?q="), anyMap())).thenReturn(response);

        GoogleDriveGallerySource source = new GoogleDriveGallerySource(http, new ObjectMapper(), "test-key");
        GallerySourceResult result = source.load("https://drive.google.com/drive/u/0/folders/1FolderXyzXyzXyz");

        assertEquals(List.of(new GalleryImage("Counterspell", "https://drive.google.com/uc?export=download&id=1Img")), result.images());
        verify(http).get(contains("key=test-key"), anyMap());
    }

    @Test
    void recognisesDriveLinks() {
        assertTrue(GoogleDriveGallerySource.isDriveLink("https://drive.google.com/drive/folders/1FolderXyzXyzXyz?usp=sharing"));
        assertTrue(GoogleDriveGallerySource.isDriveLink("https://drive.google.com/open?id=1FolderXyzXyzXyz"));
        assertFalse(GoogleDriveGallerySource.isDriveLink("https://imgur.com/a/abc"));
    }

    @Test
    void rejectsNonDriveLinks() {
        GoogleDriveGallerySource source = new GoogleDriveGallerySource(http, new ObjectMapper(), "");
        assertThrows(GallerySourceException.class, () -> source.load("https://example.com/foo"));
    }
}
