-- Neues Feld "Default Language" je API- und Gallery-Config: Karten ohne Sprachangabe bekommen im Frontend diese Sprache
-- (ISO-639-1-Code, z. B. 'en' oder 'de'; NULL = keine Sprache).
ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS default_language varchar(16) NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS default_language varchar(16) NULL;
