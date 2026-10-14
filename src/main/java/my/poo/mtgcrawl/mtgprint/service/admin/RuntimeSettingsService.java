package my.poo.mtgcrawl.mtgprint.service.admin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import my.poo.mtgcrawl.mtgprint.service.log.DbLogService;
import my.poo.mtgcrawl.mtgprint.service.pdf.PdfCacheService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileUploadService;
import my.poo.mtgcrawl.mtgprint.service.searchcache.SearchResultCacheService;

/**
 * Einstellungen, die der Admin zur Laufzeit ändern kann (Tabelle proxymaster.app_setting).
 * Ein gespeicherter Wert geht dem aus application.properties / Umgebungsvariablen vor; ohne Zeile gilt der
 * Konfigurationswert (der "Standard" auf der Admin-Seite). Werte wirken sofort, ohne Neustart.
 */
@Service
public class RuntimeSettingsService {

    private static final Logger log = LoggerFactory.getLogger(RuntimeSettingsService.class);

    /** Eine änderbare Zahl mit erlaubtem Bereich. */
    public record Setting(String key, int min, int max, IntSupplier getter, IntConsumer setter) {
    }

    /** Eine Einstellung, wie die Admin-Seite sie anzeigt. */
    public record SettingView(String key, int value, int defaultValue, int min, int max, boolean overridden) {
    }

    private final JdbcTemplate jdbc;
    private final Map<String, Setting> settings = new LinkedHashMap<>();
    private final Map<String, Integer> defaults = new LinkedHashMap<>();
    private final Map<String, Integer> overrides = new LinkedHashMap<>();

    @Autowired
    public RuntimeSettingsService(JdbcTemplate jdbc, SearchResultCacheService searchCache, PdfCacheService pdfCache,
            FileUploadService uploads, DbLogService dbLog) {
        this(jdbc, List.of(
                new Setting("searchcache.retention-days", 1, 3650, searchCache::getRetentionDays, searchCache::setRetentionDays),
                new Setting("searchcache.max-entry-kb", 16, 65536, searchCache::getMaxEntryKb, searchCache::setMaxEntryKb),
                new Setting("pdf.retention-days", 1, 365, pdfCache::getRetentionDays, pdfCache::setRetentionDays),
                new Setting("upload.retention-days", 1, 365, uploads::getRetentionDays, uploads::setRetentionDays),
                new Setting("log.db.retention-days", 1, 3650, dbLog::getRetentionDays, dbLog::setRetentionDays)));
    }

    RuntimeSettingsService(JdbcTemplate jdbc, List<Setting> settings) {
        this.jdbc = jdbc;
        for (Setting s : settings) {
            this.settings.put(s.key(), s);
            this.defaults.put(s.key(), s.getter().getAsInt());
        }
    }

    /** Beim Start die gespeicherten Werte übernehmen. Fehlt die Tabelle noch, gelten die Konfigurationswerte. */
    @EventListener(ApplicationReadyEvent.class)
    public synchronized void load() {
        try {
            jdbc.query("SELECT setting_key, setting_value FROM proxymaster.app_setting", rs -> {
                String key = rs.getString(1);
                Setting setting = settings.get(key);
                if (setting == null) {
                    log.warn("Ignoring unknown setting {}", key);
                    return;
                }
                try {
                    int value = Integer.parseInt(rs.getString(2).trim());
                    if (value < setting.min() || value > setting.max()) {
                        log.warn("Ignoring setting {}={} outside {}..{}", key, value, setting.min(), setting.max());
                        return;
                    }
                    overrides.put(key, value);
                    setting.setter().accept(value);
                } catch (NumberFormatException e) {
                    log.warn("Ignoring setting {} with non-numeric value", key);
                }
            });
            if (!overrides.isEmpty()) {
                log.info("Admin settings in effect: {}", overrides);
            }
        } catch (Exception e) {
            log.warn("Could not load admin settings, using configured values: {}", e.getMessage());
        }
    }

    public synchronized List<SettingView> list() {
        List<SettingView> result = new ArrayList<>();
        for (Setting s : settings.values()) {
            result.add(new SettingView(s.key(), s.getter().getAsInt(), defaults.get(s.key()), s.min(), s.max(),
                    overrides.containsKey(s.key())));
        }
        return result;
    }

    public synchronized SettingView update(String key, int value, String changedBy) {
        Setting setting = find(key);
        if (value < setting.min() || value > setting.max()) {
            throw new AdminException(HttpStatus.BAD_REQUEST, "admin.valueOutOfRange",
                    "Value must be between " + setting.min() + " and " + setting.max() + ".",
                    Map.of("min", setting.min(), "max", setting.max()));
        }
        jdbc.update("INSERT INTO proxymaster.app_setting (setting_key, setting_value, updated_at, updated_by) VALUES (?, ?, now(), ?)"
                + " ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value,"
                + " updated_at = EXCLUDED.updated_at, updated_by = EXCLUDED.updated_by",
                key, String.valueOf(value), changedBy);
        overrides.put(key, value);
        setting.setter().accept(value);
        log.info("Admin {} set {} = {}", changedBy, key, value);
        return view(setting);
    }

    /** Zurück auf den Wert aus der Konfiguration. */
    public synchronized SettingView reset(String key, String changedBy) {
        Setting setting = find(key);
        jdbc.update("DELETE FROM proxymaster.app_setting WHERE setting_key = ?", key);
        overrides.remove(key);
        setting.setter().accept(defaults.get(key));
        log.info("Admin {} reset {} to {}", changedBy, key, defaults.get(key));
        return view(setting);
    }

    private SettingView view(Setting s) {
        return new SettingView(s.key(), s.getter().getAsInt(), defaults.get(s.key()), s.min(), s.max(),
                overrides.containsKey(s.key()));
    }

    private Setting find(String key) {
        Setting setting = settings.get(key);
        if (setting == null) {
            throw new AdminException(HttpStatus.NOT_FOUND, "admin.unknownSetting", "Unknown setting.", Map.of("key", String.valueOf(key)));
        }
        return setting;
    }
}
