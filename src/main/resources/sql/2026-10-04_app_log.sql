-- Tabelle für die Backend-Logs (ab INFO), lesbar im DB-Viewer. Einmal auf der bestehenden Datenbank ausführen.
-- Neueste zuerst: SELECT * FROM proxymaster.app_log ORDER BY logged_at DESC LIMIT 200;
-- Nur Fehler:     SELECT * FROM proxymaster.app_log WHERE level IN ('WARN', 'ERROR') ORDER BY logged_at DESC;
CREATE TABLE IF NOT EXISTS proxymaster.app_log (
    id bigserial PRIMARY KEY,
    logged_at timestamptz NOT NULL,
    level varchar(5) NOT NULL,
    logger varchar(255),
    thread varchar(100),
    message text,
    exception text
);
CREATE INDEX IF NOT EXISTS app_log_logged_at_idx ON proxymaster.app_log (logged_at);
