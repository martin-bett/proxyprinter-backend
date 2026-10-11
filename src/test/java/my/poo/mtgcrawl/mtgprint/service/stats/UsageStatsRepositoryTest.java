package my.poo.mtgcrawl.mtgprint.service.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import my.poo.mtgcrawl.mtgprint.service.stats.UsageStatsRepository.UsageDay;

class UsageStatsRepositoryTest {

    @Test
    void missingTableNeverBreaksRequests() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        BadSqlGrammarException missing = new BadSqlGrammarException("stats", "SELECT", new java.sql.SQLException("no table"));
        when(jdbc.update(anyString(), any(Object[].class))).thenThrow(missing);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenThrow(missing);
        doThrow(missing).when(jdbc).query(anyString(), any(RowCallbackHandler.class));
        doThrow(missing).when(jdbc).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
        UsageStatsRepository repo = new UsageStatsRepository(jdbc);

        repo.count(UsageMetric.DECKS);
        assertEquals(0, repo.today(UsageMetric.DECKS));
        assertEquals(0, repo.total(UsageMetric.DECKS));
        assertEquals(0L, repo.totals().get("decks"));
        List<UsageDay> days = repo.recentDays(30);
        assertEquals(30, days.size());
        assertEquals(UsageStatsRepository.today().toString(), days.get(0).day());
        assertEquals(List.copyOf(repo.totals().keySet()), List.copyOf(days.get(0).counts().keySet()));
    }

    @Test
    void zeroAmountWritesNothing() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        new UsageStatsRepository(jdbc).count(UsageMetric.SEARCH_TERMS, 0);
        verifyNoInteractions(jdbc);
    }
}
