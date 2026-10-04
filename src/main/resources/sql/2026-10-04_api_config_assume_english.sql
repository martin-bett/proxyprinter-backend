-- Neues Feld "Assume English" je API-Config: Karten ohne Sprachangabe werden im Frontend als Englisch markiert.
-- ddl-auto ist nicht gesetzt (Default "none"), die Spalte muss also von Hand angelegt werden.
ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS assume_english bool NULL;
