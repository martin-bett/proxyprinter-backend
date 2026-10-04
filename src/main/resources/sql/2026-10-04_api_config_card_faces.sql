-- Mehrseitige Karten: neue Felder je API-Config. Jede Seite mit eigenem Bild wird ein eigener Treffer.
-- Scryfall liefert bei Transform-/MDFC-Karten oben kein image_uris, die Bilder stehen in card_faces[].image_uris.
-- Einmal auf der bestehenden Datenbank ausführen (ddl-auto ist "none").
BEGIN;

ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS card_faces_data_list_field varchar(255) NULL;
ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS card_faces_image_url_field varchar(255) NULL;
ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS card_faces_name_field varchar(255) NULL;

-- Mitgelieferte Scryfall-Config (ohne Besitzer)
UPDATE proxymaster.api_config
SET card_faces_data_list_field = 'card_faces',
    card_faces_image_url_field = 'image_uris.png',
    card_faces_name_field = 'name'
WHERE "name" = 'scryfall' AND ip_address IS NULL;

COMMIT;
