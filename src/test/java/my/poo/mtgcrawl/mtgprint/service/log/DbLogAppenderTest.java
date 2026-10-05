package my.poo.mtgcrawl.mtgprint.service.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

class DbLogAppenderTest {

    private final LoggerContext context = new LoggerContext();
    private final Logger logger = context.getLogger("test.Logger");

    private LoggingEvent event(Level level, String message, Throwable t) {
        return new LoggingEvent(Logger.class.getName(), logger, level, message, t, null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void writesOnlyEventsFromMinLevelUp() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        List<List<Object[]>> written = new ArrayList<>();
        when(jdbc.batchUpdate(eq(DbLogAppender.INSERT_SQL), anyList())).thenAnswer(inv -> {
            written.add(new ArrayList<>((List<Object[]>) inv.getArgument(1)));
            return new int[0];
        });
        DbLogAppender appender = new DbLogAppender(jdbc, Level.INFO);
        appender.setContext(context);
        appender.start();

        appender.doAppend(event(Level.DEBUG, "zu leise", null));
        appender.doAppend(event(Level.INFO, "hallo", null));
        appender.doAppend(event(Level.ERROR, "kaputt", new IllegalStateException("boom")));
        appender.stop();

        List<Object[]> rows = written.stream().flatMap(List::stream).toList();
        assertEquals(2, rows.size());
        assertEquals("INFO", rows.get(0)[1]);
        assertEquals("test.Logger", rows.get(0)[2]);
        assertEquals("hallo", rows.get(0)[4]);
        assertEquals("ERROR", rows.get(1)[1]);
        assertNotNull(rows.get(1)[5]);
        assertEquals(true, ((String) rows.get(1)[5]).contains("boom"));
    }

    @Test
    void survivesDatabaseErrors() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.batchUpdate(eq(DbLogAppender.INSERT_SQL), anyList())).thenThrow(new RuntimeException("no table"));
        DbLogAppender appender = new DbLogAppender(jdbc, Level.INFO);
        appender.setContext(context);
        appender.start();

        appender.doAppend(event(Level.WARN, "erster", null));
        verify(jdbc, timeout(3000)).batchUpdate(eq(DbLogAppender.INSERT_SQL), anyList());
        appender.doAppend(event(Level.WARN, "zweiter", null));
        appender.stop();
    }

    @Test
    void ignoresEventsFromWriterThread() throws Exception {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        DbLogAppender appender = new DbLogAppender(jdbc, Level.INFO);
        appender.setContext(context);
        appender.start();

        Thread t = new Thread(() -> appender.doAppend(event(Level.ERROR, "vom Treiber", null)), DbLogAppender.WRITER_THREAD);
        t.start();
        t.join();
        appender.stop();

        verify(jdbc, never()).batchUpdate(eq(DbLogAppender.INSERT_SQL), anyList());
    }
}
