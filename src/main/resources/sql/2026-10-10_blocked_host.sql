-- Gesperrte Quellen: Betreiber, die nicht als Kartenquelle genutzt werden wollen (HostBlocklist). Wiederholbar.
-- host: Domain (sperrt auch alle Subdomains) oder IP / Netz (1.2.3.4, 1.2.3.0/24, 2001:db8::/32).
-- Configs mit solchen URLs lassen sich nicht speichern, kopieren oder veröffentlichen, werden nicht ausgeliefert,
-- und das Backend ruft die Hosts nicht mehr ab. Änderungen wirken nach höchstens blocklist.refresh-seconds (60 s), ohne Deploy.
-- Beispiel: INSERT INTO proxymaster.blocked_host (host, reason) VALUES ('example.com', 'Bitte des Betreibers per Mail vom 2026-10-10');
CREATE TABLE IF NOT EXISTS proxymaster.blocked_host (
    id bigserial PRIMARY KEY,
    host varchar(255) NOT NULL UNIQUE,
    reason text,
    created_at timestamp NOT NULL DEFAULT now()
);
