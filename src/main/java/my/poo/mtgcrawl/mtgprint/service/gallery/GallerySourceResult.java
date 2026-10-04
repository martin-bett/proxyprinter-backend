package my.poo.mtgcrawl.mtgprint.service.gallery;

import java.util.List;

/**
 * Einheitliche Antwort aller Galerie-Quellen. Die Gallery-Configs lesen sie
 * mit data_list_field=images, name_field=name, image_url_field=imageUrl.
 */
public record GallerySourceResult(String source, String title, List<GalleryImage> images) {
}
