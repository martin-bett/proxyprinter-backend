package my.poo.mtgcrawl.mtgprint.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;
import my.poo.mtgcrawl.mtgprint.config.ApiErrorHandler;
import my.poo.mtgcrawl.mtgprint.service.admin.AdminAccess;
import my.poo.mtgcrawl.mtgprint.service.admin.CacheAdminService;
import my.poo.mtgcrawl.mtgprint.service.admin.RuntimeSettingsService;
import my.poo.mtgcrawl.mtgprint.service.admin.RuntimeSettingsService.SettingView;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthService;

class AdminControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final RuntimeSettingsService settings = mock(RuntimeSettingsService.class);
    private final CacheAdminService caches = mock(CacheAdminService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminController(new AdminAccess(authService, " Admin@Example.org , other@example.org"), settings, caches))
            .setControllerAdvice(new ApiErrorHandler())
            .build();

    {
        when(authService.findUser("admin")).thenReturn(Optional.of(new AppUser(1, "admin@example.org", LocalDateTime.now())));
        when(authService.findUser("user")).thenReturn(Optional.of(new AppUser(2, "user@example.org", LocalDateTime.now())));
        when(settings.list()).thenReturn(List.of(new SettingView("pdf.retention-days", 30, 7, 1, 365, true)));
        when(caches.list()).thenReturn(List.of());
    }

    @Test
    void onlyListedAccountsGetIn() throws Exception {
        mvc.perform(get("/api/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("auth.loginRequired"));
        mvc.perform(get("/api/admin").cookie(new Cookie("pp_session", "user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("admin.forbidden"));
        mvc.perform(post("/api/admin/caches/searchCache/clear").cookie(new Cookie("pp_session", "user")))
                .andExpect(status().isForbidden());
        verify(caches, never()).clear(any(), any());

        mvc.perform(get("/api/admin").cookie(new Cookie("pp_session", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings[0].key").value("pdf.retention-days"))
                .andExpect(jsonPath("$.settings[0].defaultValue").value(7));
    }

    @Test
    void updateNeedsAWholeNumber() throws Exception {
        Cookie admin = new Cookie("pp_session", "admin");
        mvc.perform(put("/api/admin/settings/pdf.retention-days").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("admin.invalidValue"));
        mvc.perform(put("/api/admin/settings/pdf.retention-days").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":1.5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/settings/pdf.retention-days").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":1e12}"))
                .andExpect(status().isBadRequest());
        verify(settings, never()).update(any(), anyInt(), any());

        when(settings.update("pdf.retention-days", 60, "admin@example.org"))
                .thenReturn(new SettingView("pdf.retention-days", 60, 7, 1, 365, true));
        mvc.perform(put("/api/admin/settings/pdf.retention-days").cookie(admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":60}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(60));
    }

    @Test
    void clearReportsTheDeletedCount() throws Exception {
        when(caches.clear(eq("searchCache"), eq("admin@example.org"))).thenReturn(12);
        mvc.perform(post("/api/admin/caches/searchCache/clear").cookie(new Cookie("pp_session", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(12));
    }
}
