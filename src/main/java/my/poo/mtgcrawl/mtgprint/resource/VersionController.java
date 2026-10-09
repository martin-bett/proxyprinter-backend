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

/**
 * Build-Nummer des Backends für die Anzeige im Frontend.
 * Kommt aus META-INF/build-info.properties (Maven-Build, UTC-Zeitstempel yyyyMMdd-HHmm).
 * Fehlt die Datei (z. B. Start aus der IDE ohne Maven-Build), gilt der Startzeitpunkt.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    static final DateTimeFormatter BUILD_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(ZoneOffset.UTC);

    private final String build;

    public VersionController(ObjectProvider<BuildProperties> buildProperties) {
        BuildProperties props = buildProperties.getIfAvailable();
        String number = props != null ? props.get("number") : null;
        this.build = number != null && !number.isBlank() ? number : BUILD_FORMAT.format(Instant.now());
    }

    @GetMapping
    public Map<String, String> version() {
        return Map.of("build", build);
    }
}
