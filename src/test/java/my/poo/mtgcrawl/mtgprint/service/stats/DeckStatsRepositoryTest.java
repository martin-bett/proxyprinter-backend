package my.poo.mtgcrawl.mtgprint.service.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

class DeckStatsRepositoryTest {

    @Test
    void missingTableNeverBreaksPdfGeneration() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        BadSqlGrammarException missing = new BadSqlGrammarException("stats", "SELECT", new java.sql.SQLException("no table"));
        when(jdbc.update(anyString(), any(Object[].class))).thenThrow(missing);
        when(jdbc.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenThrow(missing);
        DeckStatsRepository repo = new DeckStatsRepository(jdbc);

        repo.countCreated();
        assertEquals(0, repo.createdToday());
        assertEquals(0, repo.createdTotal());
    }
}
