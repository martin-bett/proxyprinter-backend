-- Nutzungszähler pro Tag (UTC) und Kennzahl, nur Summen, keine IPs oder Personendaten (siehe service/stats/UsageMetric).
-- Löst deck_stats ab: die bisherigen PDF-Zähler werden als Kennzahl 'decks' übernommen, deck_stats wird nicht mehr geschrieben.
-- Wiederholbar, läuft beim Deploy automatisch.
-- Letzte 30 Tage: SELECT * FROM proxymaster.usage_stats WHERE day > current_date - 30 ORDER BY day DESC, metric;
CREATE TABLE IF NOT EXISTS proxymaster.usage_stats (
    day date NOT NULL,
    metric varchar(40) NOT NULL,
    count bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (day, metric)
);

DO $$
BEGIN
    IF to_regclass('proxymaster.deck_stats') IS NOT NULL THEN
        INSERT INTO proxymaster.usage_stats (day, metric, count)
        SELECT day, 'decks', created FROM proxymaster.deck_stats
        ON CONFLICT (day, metric) DO NOTHING;
    END IF;
END $$;
