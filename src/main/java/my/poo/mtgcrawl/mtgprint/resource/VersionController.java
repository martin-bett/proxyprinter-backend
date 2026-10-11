package my.poo.mtgcrawl.mtgprint.resource;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import my.poo.mtgcrawl.mtgprint.service.stats.UsageTracker;

/**
 * Build-Nummer des Backends für die Anzeige im Frontend.
 * Kommt aus META-INF/build-info.properties (Maven-Build, UTC-Zeitstempel yyyyMMdd-HHmm).
 * Fehlt die Datei (z. B. Start aus der IDE ohne Maven-Build), gilt der Startzeitpunkt.
 * Das Frontend fragt genau einmal pro Laden der Seite, deshalb zählt jeder Aufruf als Seitenaufruf.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    static final DateTimeFormatter BUILD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneOffset.UTC);

    private final String build;
    private final UsageTracker usage;

    public VersionController(ObjectProvider<BuildProperties> buildProperties, UsageTracker usage) {
        this.usage = usage;
        BuildProperties props = buildProperties.getIfAvailable();
        String number = props != null ? props.get("number") : null;
        this.build = number != null && !number.isBlank() ? number : BUILD_FORMAT.format(Instant.now());
    }

    @GetMapping
    public Map<String, String> version(HttpServletRequest request) {
        usage.pageView(request);
        return Map.of("build", build);
    }
}
