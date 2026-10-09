-- Zählt fertig erzeugte PDFs pro Tag (UTC) für die Statistik im Frontend (/api/stats). Wiederholbar, läuft beim Deploy automatisch.
CREATE TABLE IF NOT EXISTS proxymaster.deck_stats (
    day date PRIMARY KEY,
    created int NOT NULL DEFAULT 0
);
