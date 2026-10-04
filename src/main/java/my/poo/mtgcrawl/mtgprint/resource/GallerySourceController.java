package my.poo.mtgcrawl.mtgprint.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.service.gallery.GalleryImage;
import my.poo.mtgcrawl.mtgprint.service.gallery.GallerySourceException;
import my.poo.mtgcrawl.mtgprint.service.gallery.GallerySourceResult;
import my.poo.mtgcrawl.mtgprint.service.gallery.GoogleDriveGallerySource;
import my.poo.mtgcrawl.mtgprint.service.gallery.RedditGallerySource;

/**
 * Galerie-Quellen, die sich nicht als reine Config abbilden lassen. Liefern alle
 * dasselbe JSON ({@link GallerySourceResult}), eine Gallery-Config zeigt mit
 * base_url=/api/gallery/source/{typ} und Parameter "url" darauf.
 */
@RestController
@RequestMapping("/api/gallery/source")
@AllArgsConstructor
@Slf4j
public class GallerySourceController {

    private final RedditGallerySource reddit;
    private final GoogleDriveGallerySource googleDrive;

    @GetMapping("/reddit")
    public GallerySourceResult reddit(@RequestParam("url") String url) {
        RedditGallerySource.RedditPost post = reddit.load(url);
        if (!post.result().images().isEmpty()) {
            return post.result();
        }
        // Viele Posts verlinken nur einen Drive-Ordner; dann den gleich mitlesen
        List<GalleryImage> images = new ArrayList<>();
        for (String link : post.linkedUrls()) {
            if (GoogleDriveGallerySource.isDriveLink(link)) {
                try {
                    images.addAll(googleDrive.load(link).images());
                } catch (GallerySourceException e) {
                    log.info("Verlinkter Drive-Ordner {} nicht lesbar: {}", link, e.getMessage());
                }
            }
        }
        return new GallerySourceResult("reddit", post.result().title(), images);
    }

    @GetMapping("/gdrive")
    public GallerySourceResult googleDrive(@RequestParam("url") String url) {
        return googleDrive.load(url);
    }

    @ExceptionHandler(GallerySourceException.class)
    public ResponseEntity<Map<String, String>> handle(GallerySourceException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }
}
