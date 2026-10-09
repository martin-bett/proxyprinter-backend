-- Konten ohne Passwort: bestätigte E-Mail, Login per Link/Code, Sitzung per Cookie. Wiederholbar, läuft beim Deploy automatisch.
CREATE TABLE IF NOT EXISTS proxymaster.app_user (
    id bigserial PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE,
    created_at timestamp NOT NULL DEFAULT now(),
    last_login timestamp
);

CREATE TABLE IF NOT EXISTS proxymaster.login_token (
    token_hash varchar(64) PRIMARY KEY,
    email varchar(254) NOT NULL,
    code_hash varchar(64) NOT NULL,
    attempts int NOT NULL DEFAULT 0,
    expires_at timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS login_token_email_idx ON proxymaster.login_token (email);

CREATE TABLE IF NOT EXISTS proxymaster.user_session (
    token_hash varchar(64) PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES proxymaster.app_user (id) ON DELETE CASCADE,
    created_at timestamp NOT NULL DEFAULT now(),
    last_seen timestamp NOT NULL DEFAULT now(),
    user_agent varchar(255)
);
CREATE INDEX IF NOT EXISTS user_session_user_idx ON proxymaster.user_session (user_id);
