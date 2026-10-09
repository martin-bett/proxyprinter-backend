-- Configs und Ratings gehören einem Konto statt einem Browser-Fingerprint. Wiederholbar, läuft beim Deploy automatisch.
-- Konto gelöscht: eigene unveröffentlichte Configs löscht das Backend vorher, veröffentlichte bleiben ohne Besitzer, Ratings anonym.
ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;
ALTER TABLE proxymaster.deck_finder_config ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;
ALTER TABLE proxymaster.ratings ADD COLUMN IF NOT EXISTS user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS api_config_user_idx ON proxymaster.api_config (user_id);
CREATE INDEX IF NOT EXISTS gallery_config_user_idx ON proxymaster.gallery_config (user_id);
CREATE INDEX IF NOT EXISTS deck_finder_config_user_idx ON proxymaster.deck_finder_config (user_id);
-- Ein Rating pro Konto und Config
CREATE UNIQUE INDEX IF NOT EXISTS ratings_user_config_idx ON proxymaster.ratings (user_id, api_config_id) WHERE user_id IS NOT NULL;
