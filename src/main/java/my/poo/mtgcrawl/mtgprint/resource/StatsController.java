package my.poo.mtgcrawl.mtgprint.resource;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import my.poo.mtgcrawl.mtgprint.service.stats.StatsService;

/** Aggregierte Cache- und Deck-Zahlen für die Anzeige unter der Build-Nummer, ohne Login lesbar. */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public Map<String, Long> stats() {
        return statsService.stats();
    }
}
