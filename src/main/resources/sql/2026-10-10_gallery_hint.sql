-- Hinweistext je Galerie-Config (in der Sprache des Autors) und seine Übersetzungen als JSON {"de": "..."}.
-- Wiederholbar.
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS hint varchar(1000) NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS hint_translations text NULL;
