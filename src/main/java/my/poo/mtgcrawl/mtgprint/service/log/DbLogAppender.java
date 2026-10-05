package my.poo.mtgcrawl.mtgprint.service.log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.JdbcTemplate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;

/**
 * Schreibt Log-Einträge zusätzlich in die Tabelle proxymaster.app_log, damit man sie im DB-Viewer lesen kann.
 * Die Einträge landen erst in einer Warteschlange und werden von einem eigenen Thread gebündelt geschrieben,
 * damit Logging nie auf die Datenbank warten muss. Ist die Schlange voll oder die DB weg, wird verworfen.
 */
public class DbLogAppender extends AppenderBase<ILoggingEvent> {

    static final String WRITER_THREAD = "db-log-writer";
    static final String INSERT_SQL = "INSERT INTO proxymaster.app_log (logged_at, level, logger, thread, message, exception) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final int QUEUE_SIZE = 10_000;
    private static final int BATCH_SIZE = 500;
    private static final long ERROR_REPORT_INTERVAL_MS = 60_000L;

    private final JdbcTemplate jdbcTemplate;
    private final Level minLevel;
    private final BlockingQueue<Object[]> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private volatile boolean running;
    private Thread writer;
    private long lastErrorReport;

    public DbLogAppender(JdbcTemplate jdbcTemplate, Level minLevel) {
        this.jdbcTemplate = jdbcTemplate;
        this.minLevel = minLevel;
        setName("DB");
    }

    @Override
    public void start() {
        running = true;
        writer = new Thread(this::writeLoop, WRITER_THREAD);
        writer.setDaemon(true);
        writer.start();
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        running = false;
        if (writer != null) {
            writer.interrupt();
            try {
                writer.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        flush();
    }

    @Override
    protected void append(ILoggingEvent event) {
        // Was der Schreib-Thread selbst auslöst (JDBC-Treiber, Pool), nicht wieder in die DB schicken
        if (!event.getLevel().isGreaterOrEqual(minLevel) || WRITER_THREAD.equals(event.getThreadName())) {
            return;
        }
        IThrowableProxy throwable = event.getThrowableProxy();
        queue.offer(new Object[] {
                new Timestamp(event.getTimeStamp()),
                event.getLevel().toString(),
                cut(event.getLoggerName(), 255),
                cut(event.getThreadName(), 100),
                event.getFormattedMessage(),
                throwable == null ? null : ThrowableProxyUtil.asString(throwable) });
    }

    private void writeLoop() {
        while (running) {
            try {
                Object[] first = queue.poll(1, TimeUnit.SECONDS);
                if (first != null) {
                    List<Object[]> batch = new ArrayList<>();
                    batch.add(first);
                    queue.drainTo(batch, BATCH_SIZE - 1);
                    write(batch);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /** Schreibt alles, was noch in der Schlange liegt (beim Herunterfahren). */
    void flush() {
        List<Object[]> batch = new ArrayList<>();
        while (queue.drainTo(batch, BATCH_SIZE) > 0) {
            write(batch);
            batch.clear();
        }
    }

    private void write(List<Object[]> batch) {
        try {
            jdbcTemplate.batchUpdate(INSERT_SQL, batch);
        } catch (Exception e) {
            // Nicht über den Logger melden, sonst Schleife; höchstens einmal pro Minute auf stderr
            long now = System.currentTimeMillis();
            if (now - lastErrorReport > ERROR_REPORT_INTERVAL_MS) {
                lastErrorReport = now;
                System.err.println("DbLogAppender: " + batch.size() + " Log-Einträge verworfen: " + e.getMessage());
            }
        }
    }

    private static String cut(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
