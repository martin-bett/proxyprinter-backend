package my.poo.mtgcrawl.mtgprint.service.stats;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Zählt fertig erzeugte PDFs pro Tag (UTC) in proxymaster.deck_stats (siehe sql/2026-10-09_deck_stats.sql).
 * Fehler werden nur geloggt: an der Statistik darf kein PDF scheitern.
 */
@Repository
public class DeckStatsRepository {

    private static final Logger log = LoggerFactory.getLogger(DeckStatsRepository.class);

    private final JdbcTemplate jdbc;

    public DeckStatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public void countCreated() {
        try {
            jdbc.update("INSERT INTO proxymaster.deck_stats AS s (day, created) VALUES (?, 1) "
                    + "ON CONFLICT (day) DO UPDATE SET created = s.created + 1", Date.valueOf(today()));
        } catch (RuntimeException e) {
            log.warn("Could not count created deck: {}", e.getMessage());
        }
    }

    public long createdToday() {
        return query("SELECT COALESCE(SUM(created), 0) FROM proxymaster.deck_stats WHERE day = ?", Date.valueOf(today()));
    }

    public long createdTotal() {
        return query("SELECT COALESCE(SUM(created), 0) FROM proxymaster.deck_stats");
    }

    private long query(String sql, Object... args) {
        try {
            Long value = jdbc.queryForObject(sql, Long.class, args);
            return value == null ? 0 : value;
        } catch (RuntimeException e) {
            log.warn("Could not read deck stats: {}", e.getMessage());
            return 0;
        }
    }
}
