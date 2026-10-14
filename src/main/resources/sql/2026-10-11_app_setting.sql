-- Werte, die der Admin auf /admin geändert hat (RuntimeSettingsService). Sie gehen den Werten aus
-- application.properties / Umgebungsvariablen vor; ohne Zeile gilt der Wert aus der Konfiguration. Wiederholbar.
CREATE TABLE IF NOT EXISTS proxymaster.app_setting (
    setting_key varchar(100) PRIMARY KEY,
    setting_value varchar(200) NOT NULL,
    updated_at timestamp NOT NULL DEFAULT now(),
    updated_by varchar(320)
);
