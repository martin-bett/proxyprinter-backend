package my.poo.mtgcrawl.mtgprint.service.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PreDestroy;

/**
 * Hängt den {@link DbLogAppender} an den Root-Logger, sobald die Anwendung läuft, und löscht
 * einmal am Tag Einträge, die älter als log.db.retention-days sind.
 */
@Service
public class DbLogService {

    private static final Logger log = LoggerFactory.getLogger(DbLogService.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final Level minLevel;
    private volatile int retentionDays;
    private DbLogAppender appender;

    public DbLogService(JdbcTemplate jdbcTemplate,
            @Value("${log.db.enabled:true}") boolean enabled,
            @Value("${log.db.level:INFO}") String minLevel,
            @Value("${log.db.retention-days:30}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.minLevel = Level.toLevel(minLevel, Level.INFO);
        this.retentionDays = retentionDays;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void attach() {
        if (!enabled || appender != null) {
            return;
        }
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender = new DbLogAppender(jdbcTemplate, minLevel);
        appender.setContext(context);
        appender.start();
        context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
        log.info("Logs werden ab {} zusätzlich in proxymaster.app_log geschrieben", minLevel);
    }

    @PreDestroy
    public void detach() {
        if (appender == null) {
            return;
        }
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender);
        appender.stop();
        appender = null;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    /** Neue Aufbewahrungszeit (Admin-Einstellung), gilt ab dem nächsten nächtlichen Aufräumen. */
    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${log.db.cleanup-cron:0 30 3 * * *}")
    public void deleteOldEntries() {
        if (!enabled || retentionDays <= 0) {
            return;
        }
        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM proxymaster.app_log WHERE logged_at < now() - make_interval(days => ?)", retentionDays);
            log.info("{} Log-Einträge älter als {} Tage gelöscht", deleted, retentionDays);
        } catch (Exception e) {
            log.warn("Alte Log-Einträge konnten nicht gelöscht werden: {}", e.getMessage());
        }
    }
}
