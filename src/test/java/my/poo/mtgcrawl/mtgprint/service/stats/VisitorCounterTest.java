package my.poo.mtgcrawl.mtgprint.service.stats;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class VisitorCounterTest {

    @Test
    void countsEachAddressAndBrowserOncePerDay() {
        AtomicReference<LocalDate> day = new AtomicReference<>(LocalDate.of(2026, 10, 11));
        VisitorCounter counter = new VisitorCounter(day::get);

        assertTrue(counter.firstVisitToday("1.2.3.4", "Firefox"));
        assertFalse(counter.firstVisitToday("1.2.3.4", "Firefox"));
        assertTrue(counter.firstVisitToday("1.2.3.4", "Chrome"));
        assertTrue(counter.firstVisitToday("5.6.7.8", "Firefox"));
        assertTrue(counter.firstVisitToday("9.9.9.9", null));
        assertFalse(counter.firstVisitToday("9.9.9.9", null));

        day.set(LocalDate.of(2026, 10, 12));
        assertTrue(counter.firstVisitToday("1.2.3.4", "Firefox"), "a new day starts a new list");
    }
}
