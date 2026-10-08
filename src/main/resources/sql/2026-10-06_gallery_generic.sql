-- Gallery-Configs generisch: Link-Pattern, Zugang (credential) und Nachbearbeitung der Felder als Config-Felder.
-- Reddit und Google Drive laufen damit ohne eigenen Code, die Endpunkte /api/gallery/source/... entfallen.
-- Wiederholbar, läuft beim Deploy automatisch. Betrifft nur die mitgelieferten Configs (ohne Besitzer).
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS link_pattern varchar(500) NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS follow_input_redirects bool DEFAULT false NOT NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS credential varchar(255) NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS image_url_pattern varchar(500) NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS image_url_replacement varchar(500) NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS name_pattern varchar(500) NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS name_replacement varchar(500) NULL;

-- Reddit: Post-ID aus dem Link, Post-JSON mit media_metadata (Objekt, Einträge werden zur Liste),
-- Originalbild i.redd.it aus der preview-URL. Teilen-Links (/r/x/s/..) werden erst aufgelöst.
-- Ohne Zugang blockt Reddit Server meist (403); Zugang "reddit" per GALLERY_CREDENTIALS_REDDIT_COOKIE.
UPDATE proxymaster.gallery_config
SET base_url = 'https://www.reddit.com/comments/{id}.json?raw_json=1&limit=1', data_format = 'json',
    data_list_field = '0.data.children.0.data.media_metadata', name_field = 'id', image_url_field = 's.u',
    pagination_field = NULL, "method" = 'GET', request_format = NULL, use_backend_proxy = false,
    link_pattern = '(?:/comments/|/gallery/|redd\.it/)(?<id>[a-z0-9]{4,12})(?:[/?#]|$)',
    follow_input_redirects = true, credential = 'reddit',
    image_url_pattern = '^https://(?:preview|external-preview)\.redd\.it/([^?]+).*$',
    image_url_replacement = 'https://i.redd.it/$1',
    name_pattern = NULL, name_replacement = NULL
WHERE "name" IN ('reddit', 'reddit_gallery') AND ip_address IS NULL AND base_url LIKE '/api/gallery/source/%';

-- Google Drive: Ordner-ID aus dem Link, öffentliche Ordneransicht (HTML) ohne Schlüssel, Datei-ID aus dem Eintrag.
-- Unterordner fallen über den Selektor weg, Endung wird aus dem Namen entfernt.
UPDATE proxymaster.gallery_config
SET base_url = 'https://drive.google.com/embeddedfolderview?id={id}', data_format = 'html',
    data_list_field = 'div.flip-entry:not(:has(a[href*="/folders/"]))',
    name_field = '.flip-entry-title | innerText', image_url_field = 'a[href] | attr:href',
    pagination_field = NULL, "method" = 'GET', request_format = NULL, use_backend_proxy = false,
    link_pattern = '(?:/folders/|[?&]id=)(?<id>[A-Za-z0-9_-]{10,})',
    follow_input_redirects = false, credential = NULL,
    image_url_pattern = '^.*/file/d/([A-Za-z0-9_-]+).*$',
    image_url_replacement = 'https://drive.google.com/uc?export=download&id=$1',
    name_pattern = '\.[A-Za-z0-9]{2,5}$', name_replacement = ''
WHERE "name" = 'google_drive' AND ip_address IS NULL AND base_url LIKE '/api/gallery/source/%';
