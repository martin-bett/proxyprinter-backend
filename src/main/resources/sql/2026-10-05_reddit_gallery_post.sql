-- Reddit-Galerie per POST abfragen: das Feld "url" darf jetzt auch den im Browser kopierten
-- Reddit-Post (JSON) enthalten, der passt nicht in eine GET-Adresse. Idempotent.
UPDATE proxymaster.gallery_config
SET "method" = 'POST', request_format = NULL
WHERE "name" IN ('reddit', 'reddit_gallery') AND ip_address IS NULL
  AND base_url = '/api/gallery/source/reddit';
