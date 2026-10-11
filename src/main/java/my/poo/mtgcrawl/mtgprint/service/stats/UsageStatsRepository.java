package my.poo.mtgcrawl.mtgprint.service.stats;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Zählt Nutzung pro Tag (UTC) und Kennzahl in proxymaster.usage_stats (siehe sql/2026-10-11_usage_stats.sql).
 * Fehler werden nur geloggt: an der Statistik darf keine Anfrage scheitern.
 */
@Repository
public class UsageStatsRepository {

    private static final Logger log = LoggerFactory.getLogger(UsageStatsRepository.class);

    /** Ein Tag (ISO, z. B. 2026-10-11) mit allen Kennzahlen (fehlende = 0), Schlüssel wie {@link UsageMetric#key()}. */
    public record UsageDay(String day, Map<String, Long> counts) {
    }

    private final JdbcTemplate jdbc;

    public UsageStatsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public void count(UsageMetric metric) {
        count(metric, 1);
    }

    public void count(UsageMetric metric, long amount) {
        if (amount <= 0) {
            return;
        }
        try {
            jdbc.update("INSERT INTO proxymaster.usage_stats AS s (day, metric, count) VALUES (?, ?, ?) "
                    + "ON CONFLICT (day, metric) DO UPDATE SET count = s.count + EXCLUDED.count",
                    Date.valueOf(today()), metric.key(), amount);
        } catch (RuntimeException e) {
            log.warn("Could not count {}: {}", metric.key(), e.getMessage());
        }
    }

    public long today(UsageMetric metric) {
        return query("SELECT COALESCE(SUM(count), 0) FROM proxymaster.usage_stats WHERE day = ? AND metric = ?",
                Date.valueOf(today()), metric.key());
    }

    public long total(UsageMetric metric) {
        return query("SELECT COALESCE(SUM(count), 0) FROM proxymaster.usage_stats WHERE metric = ?", metric.key());
    }

    /** Summe aller Tage je Kennzahl. */
    public Map<String, Long> totals() {
        Map<String, Long> result = emptyCounts();
        try {
            jdbc.query("SELECT metric, SUM(count) FROM proxymaster.usage_stats GROUP BY metric",
                    rs -> { putKnown(result, rs.getString(1), rs.getLong(2)); });
        } catch (RuntimeException e) {
            log.warn("Could not read usage totals: {}", e.getMessage());
        }
        return result;
    }

    /** Die letzten {@code days} Tage bis heute, neuester zuerst, auch Tage ohne Zähler. */
    public List<UsageDay> recentDays(int days) {
        LocalDate today = today();
        LocalDate from = today.minusDays(days - 1L);
        Map<LocalDate, Map<String, Long>> byDay = new LinkedHashMap<>();
        for (LocalDate day = today; !day.isBefore(from); day = day.minusDays(1)) {
            byDay.put(day, emptyCounts());
        }
        try {
            jdbc.query("SELECT day, metric, count FROM proxymaster.usage_stats WHERE day >= ?", rs -> {
                Map<String, Long> counts = byDay.get(rs.getDate(1).toLocalDate());
                if (counts != null) {
                    putKnown(counts, rs.getString(2), rs.getLong(3));
                }
            }, Date.valueOf(from));
        } catch (RuntimeException e) {
            log.warn("Could not read usage stats: {}", e.getMessage());
        }
        List<UsageDay> result = new ArrayList<>();
        byDay.forEach((day, counts) -> result.add(new UsageDay(day.toString(), counts)));
        return result;
    }

    private static Map<String, Long> emptyCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (UsageMetric metric : UsageMetric.values()) {
            counts.put(metric.key(), 0L);
        }
        return counts;
    }

    /** Unbekannte Kennzahlen (z. B. aus einer neueren Version) werden ignoriert. */
    private static void putKnown(Map<String, Long> counts, String metric, long value) {
        if (counts.containsKey(metric)) {
            counts.put(metric, value);
        }
    }

    private long query(String sql, Object... args) {
        try {
            Long value = jdbc.queryForObject(sql, Long.class, args);
            return value == null ? 0 : value;
        } catch (RuntimeException e) {
            log.warn("Could not read usage stats: {}", e.getMessage());
            return 0;
        }
    }
}
