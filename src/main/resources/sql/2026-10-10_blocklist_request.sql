-- Offene Anfragen der Selbst-Sperre (OptOutService, /api/blocklist): Betreiber weisen per Mail-Link oder
-- DNS-TXT/Datei nach, dass ihnen ein Host gehört; danach landet er in blocked_host. Nur der Token-Hash wird gespeichert.
-- Abgelaufene Zeilen löscht die nächste Anfrage. Wiederholbar.
CREATE TABLE IF NOT EXISTS proxymaster.blocklist_request (
    token_hash varchar(64) PRIMARY KEY,
    host varchar(255) NOT NULL,
    method varchar(10) NOT NULL,
    email varchar(320),
    ip varchar(64),
    created_at timestamp NOT NULL DEFAULT now(),
    expires_at timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS blocklist_request_host_idx ON proxymaster.blocklist_request (host);
