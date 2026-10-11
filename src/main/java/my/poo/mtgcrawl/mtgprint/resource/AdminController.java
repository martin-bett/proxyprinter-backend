package my.poo.mtgcrawl.mtgprint.resource;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import my.poo.mtgcrawl.mtgprint.service.admin.AdminAccess;
import my.poo.mtgcrawl.mtgprint.service.admin.AdminException;
import my.poo.mtgcrawl.mtgprint.service.admin.CacheAdminService;
import my.poo.mtgcrawl.mtgprint.service.admin.RuntimeSettingsService;
import my.poo.mtgcrawl.mtgprint.service.admin.RuntimeSettingsService.SettingView;

/** Admin-Seite: Laufzeiten der Caches einstellen und Caches leeren. Nur für Konten aus admin.emails. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAccess access;
    private final RuntimeSettingsService settings;
    private final CacheAdminService caches;

    public AdminController(AdminAccess access, RuntimeSettingsService settings, CacheAdminService caches) {
        this.access = access;
        this.settings = settings;
        this.caches = caches;
    }

    /** Einstellungen und Cache-Übersicht. 401 ohne Login, 403 für andere Konten. */
    @GetMapping
    public Map<String, Object> overview(HttpServletRequest request) {
        access.require(request);
        return Map.of("settings", settings.list(), "caches", caches.list());
    }

    /** Body: {"value": 30}. */
    @PutMapping("/settings/{key}")
    public SettingView update(@PathVariable String key, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        String admin = access.require(request).email();
        Object value = body == null ? null : body.get("value");
        if (!(value instanceof Number number) || number.doubleValue() != Math.rint(number.doubleValue())
                || Math.abs(number.doubleValue()) > Integer.MAX_VALUE) {
            throw new AdminException(HttpStatus.BAD_REQUEST, "admin.invalidValue", "Value must be a whole number.", Map.of());
        }
        return settings.update(key, number.intValue(), admin);
    }

    /** Zurück auf den Wert aus der Server-Konfiguration. */
    @DeleteMapping("/settings/{key}")
    public SettingView reset(@PathVariable String key, HttpServletRequest request) {
        return settings.reset(key, access.require(request).email());
    }

    @PostMapping("/caches/{name}/clear")
    public Map<String, Object> clear(@PathVariable String name, HttpServletRequest request) {
        String admin = access.require(request).email();
        return Map.of("deleted", caches.clear(name, admin));
    }
}
