package my.poo.mtgcrawl.mtgprint.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import my.poo.mtgcrawl.mtgprint.service.admin.RuntimeSettingsService.Setting;

class RuntimeSettingsServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AtomicInteger days = new AtomicInteger(7);
    private final RuntimeSettingsService service = new RuntimeSettingsService(jdbc,
            List.of(new Setting("pdf.retention-days", 1, 365, days::get, days::set)));

    @Test
    void updateStoresAndAppliesTheValueAndResetGoesBackToTheConfiguredOne() {
        var view = service.update("pdf.retention-days", 30, "admin@example.org");

        assertEquals(30, days.get());
        assertEquals(30, view.value());
        assertEquals(7, view.defaultValue());
        assertTrue(view.overridden());
        verify(jdbc).update(contains("ON CONFLICT"), eq("pdf.retention-days"), eq("30"), eq("admin@example.org"));

        var reset = service.reset("pdf.retention-days", "admin@example.org");
        assertEquals(7, days.get());
        assertFalse(reset.overridden());
    }

    @Test
    void rejectsValuesOutsideTheRangeAndUnknownKeys() {
        AdminException tooBig = assertThrows(AdminException.class, () -> service.update("pdf.retention-days", 366, "a"));
        assertEquals("admin.valueOutOfRange", tooBig.toBody().get("code"));
        assertThrows(AdminException.class, () -> service.update("pdf.retention-days", 0, "a"));
        assertThrows(AdminException.class, () -> service.update("nope", 5, "a"));
        assertEquals(7, days.get());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void loadAppliesStoredValuesAndSkipsBrokenOnes() throws Exception {
        doAnswer(inv -> {
            RowCallbackHandler handler = inv.getArgument(1);
            handler.processRow(row("unknown.key", "5"));
            handler.processRow(row("pdf.retention-days", "999"));
            assertEquals(7, days.get());
            handler.processRow(row("pdf.retention-days", "45"));
            return null;
        }).when(jdbc).query(anyString(), any(RowCallbackHandler.class));

        service.load();

        assertEquals(45, days.get());
        assertTrue(service.list().get(0).overridden());
    }

    @Test
    void loadKeepsConfiguredValuesWhenTheTableIsMissing() {
        doThrow(new RuntimeException("relation does not exist")).when(jdbc).query(anyString(), any(RowCallbackHandler.class));
        service.load();
        assertEquals(7, days.get());
    }

    private static ResultSet row(String key, String value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(key);
        when(rs.getString(2)).thenReturn(value);
        return rs;
    }
}
