-- Galerie-Quellen auf die Backend-Endpunkte umstellen (Reddit per JSON statt HTML) und Google Drive ergänzen.
-- Einmal auf der bestehenden Datenbank ausführen. Betrifft nur die mitgelieferten Configs (ohne Besitzer).
BEGIN;

UPDATE proxymaster.gallery_config
SET base_url = '/api/gallery/source/reddit', data_format = 'json', data_list_field = 'images',
    name_field = 'name', image_url_field = 'imageUrl', pagination_field = NULL,
    "method" = 'GET', request_format = NULL, use_backend_proxy = false,
    official_website = 'https://www.reddit.com/r/magicproxies/'
WHERE "name" IN ('reddit', 'reddit_gallery') AND ip_address IS NULL;

-- Die alte HTML-Config "reddit" war unveröffentlicht; die neue "reddit" ersetzt beide
UPDATE proxymaster.gallery_config SET published = true
WHERE "name" = 'reddit' AND ip_address IS NULL;
UPDATE proxymaster.gallery_config SET published = false
WHERE "name" = 'reddit_gallery' AND ip_address IS NULL;

INSERT INTO proxymaster.gallery_config
(category, "name", base_url, data_format, image_url_field, name_field, data_list_field, official_website, published, "method", use_backend_proxy)
SELECT 'MISC'::proxymaster."api_category", 'google_drive', '/api/gallery/source/gdrive', 'json', 'imageUrl', 'name', 'images',
       'https://drive.google.com/', true, 'GET', false
WHERE NOT EXISTS (SELECT 1 FROM proxymaster.gallery_config WHERE "name" = 'google_drive' AND ip_address IS NULL);

-- Alte Parameter (subreddit, id_comment, fullLink) durch ein Eingabefeld "url" ersetzen
DELETE FROM proxymaster.gallery_config_parameter
WHERE gallery_id IN (SELECT id FROM proxymaster.gallery_config
                     WHERE "name" IN ('reddit', 'reddit_gallery', 'google_drive') AND ip_address IS NULL);
INSERT INTO proxymaster.gallery_config_parameter (gallery_id, "name", category, value)
SELECT id, 'url', 'request', NULL FROM proxymaster.gallery_config
WHERE "name" IN ('reddit', 'reddit_gallery', 'google_drive') AND ip_address IS NULL;

COMMIT;
