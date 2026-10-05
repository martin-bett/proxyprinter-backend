package my.poo.mtgcrawl.mtgprint.service.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;

class ApiConfigCleanupServiceTest {

    private final ApiConfigRepository repository = mock(ApiConfigRepository.class);

    @Test
    void deletesConfigsOlderThanLimitWithParametersAndRatings() {
        when(repository.findUnusedSince(any())).thenReturn(List.of(12L, 13L));
        when(repository.deleteByIds(List.of(12L, 13L))).thenReturn(2);

        int deleted = new ApiConfigCleanupService(repository, 180).deleteUnusedConfigs();

        assertEquals(2, deleted);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findUnusedSince(cutoff.capture());
        LocalDateTime expected = LocalDateTime.now().minusDays(180);
        assertTrue(Math.abs(java.time.Duration.between(expected, cutoff.getValue()).toSeconds()) < 5);
        verify(repository).deleteParametersOf(List.of(12L, 13L));
        verify(repository).deleteConnectedCardsOf(List.of(12L, 13L));
        verify(repository).deleteRatingsOf(List.of(12L, 13L));
    }

    @Test
    void nothingToDelete() {
        when(repository.findUnusedSince(any())).thenReturn(List.of());

        assertEquals(0, new ApiConfigCleanupService(repository, 180).deleteUnusedConfigs());
        verify(repository, never()).deleteByIds(any());
    }

    @Test
    void zeroDaysDisablesCleanup() {
        assertEquals(0, new ApiConfigCleanupService(repository, 0).deleteUnusedConfigs());
        verifyNoInteractions(repository);
    }
}
