-- Neues Feld last_used je API-Config: wird bei Suche, Anlegen, Kopieren und Bearbeiten gesetzt.
-- ApiConfigCleanupService löscht jede Nacht User-Configs, die länger als
-- apiconfig.cleanup.max-unused-days (Standard 180) unbenutzt sind. Standard-Configs ohne ip_address bleiben.
-- ddl-auto ist nicht gesetzt, die Spalte muss also von Hand angelegt werden. Kann mehrfach laufen.
ALTER TABLE proxymaster.api_config ADD COLUMN IF NOT EXISTS last_used timestamp NULL;

-- Bestehende Configs starten mit heute, damit nichts sofort gelöscht wird
UPDATE proxymaster.api_config SET last_used = now() WHERE last_used IS NULL;
