package my.poo.mtgcrawl.mtgprint.service.rest;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;

/**
 * Löscht einmal am Tag API-Configs von Usern, die länger als
 * apiconfig.cleanup.max-unused-days weder gesucht noch bearbeitet wurden.
 * Mitgelieferte Standard-Configs (ohne Ersteller) bleiben immer.
 */
@Service
public class ApiConfigCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ApiConfigCleanupService.class);

    private final ApiConfigRepository apiConfigRepository;
    private final int maxUnusedDays;

    public ApiConfigCleanupService(
            ApiConfigRepository apiConfigRepository,
            @Value("${apiconfig.cleanup.max-unused-days:180}") int maxUnusedDays) {
        this.apiConfigRepository = apiConfigRepository;
        this.maxUnusedDays = maxUnusedDays;
    }

    @Scheduled(cron = "${apiconfig.cleanup.cron:0 30 3 * * *}")
    @Transactional
    public int deleteUnusedConfigs() {
        if (maxUnusedDays <= 0) {
            return 0;
        }
        List<Long> ids = apiConfigRepository.findUnusedSince(LocalDateTime.now().minusDays(maxUnusedDays));
        if (ids.isEmpty()) {
            return 0;
        }
        apiConfigRepository.deleteParametersOf(ids);
        apiConfigRepository.deleteRatingsOf(ids);
        int deleted = apiConfigRepository.deleteByIds(ids);
        log.info("Deleted {} API configs unused for more than {} days: {}", deleted, maxUnusedDays, ids);
        return deleted;
    }
}
