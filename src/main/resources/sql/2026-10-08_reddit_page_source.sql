-- Reddit-Galerie liest die Beitragsseite (HTML) statt der Post-JSON. Reddit blockt Server-Abrufe, deshalb fügt man den
-- Seitenquelltext (Strg+A, Strg+C auf der Seite "Seitenquelltext anzeigen") in die Galerie-Suche ein; das Frontend
-- wertet ihn mit diesen Feldern aus. Die Galerie liefert die Originale als i.redd.it, der Name ist die Bild-ID.
-- Wiederholbar; betrifft nur die mitgelieferte Config (ohne Besitzer).
UPDATE proxymaster.gallery_config
SET base_url = 'https://www.reddit.com/comments/{id}/', data_format = 'html',
    data_list_field = 'gallery-carousel li', name_field = 'zoomable-img img | attr:src',
    image_url_field = 'zoomable-img img | attr:src', pagination_field = NULL,
    image_url_pattern = NULL, image_url_replacement = NULL,
    name_pattern = '^.*/([^/.]+)\.[a-z0-9]+$', name_replacement = '$1'
WHERE "name" = 'reddit' AND ip_address IS NULL AND base_url LIKE 'https://www.reddit.com/comments/%';
