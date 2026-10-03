-- Neues Feld "Proxy Images" je API-Config: Bilder werden über /api/proxy/image geladen.
-- ddl-auto ist nicht gesetzt (Default "none"), die Spalte muss also von Hand angelegt werden.
ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS proxy_images bool NULL;
