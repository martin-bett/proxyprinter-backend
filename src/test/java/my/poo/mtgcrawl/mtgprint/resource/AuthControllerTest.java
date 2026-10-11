package my.poo.mtgcrawl.mtgprint.resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthService;
import my.poo.mtgcrawl.mtgprint.service.rest.ApiConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.DeckFinderConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.GalleryConfigService;
import my.poo.mtgcrawl.mtgprint.service.stats.UsageTracker;

class AuthControllerTest {

    private static final AppUser TEST_USER = new AppUser(7, "test@proxyprinter.eu", LocalDateTime.now());

    private final AuthService authService = mock(AuthService.class);
    private final AuthController controller = new AuthController(authService, mock(ApiConfigService.class),
            mock(GalleryConfigService.class), mock(DeckFinderConfigService.class), mock(UsageTracker.class), "https://abc123.test.proxyprinter.eu");

    private static List<String> cookies(ResponseEntity<?> response) {
        List<String> values = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        return values == null ? List.of() : values;
    }

    @Test
    void meLogsInTheTestAccountWhenAutoLoginIsOn() {
        when(authService.isAutoLoginEnabled()).thenReturn(true);
        when(authService.autoLogin(any())).thenReturn(Optional.of("sess"));
        when(authService.findUser("sess")).thenReturn(Optional.of(TEST_USER));

        var response = controller.me(new MockHttpServletRequest());

        assertEquals("test@proxyprinter.eu", response.getBody().get("email"));
        assertTrue(cookies(response).stream().anyMatch(c -> c.startsWith("pp_session=sess;")));
    }

    @Test
    void meStaysLoggedOutWithoutAutoLoginOrAfterLogout() {
        var response = controller.me(new MockHttpServletRequest());
        assertNull(response.getBody().get("email"));
        verify(authService, never()).autoLogin(any());

        when(authService.isAutoLoginEnabled()).thenReturn(true);
        MockHttpServletRequest afterLogout = new MockHttpServletRequest();
        afterLogout.setCookies(new Cookie("pp_no_auto_login", "1"));
        assertNull(controller.me(afterLogout).getBody().get("email"));
        verify(authService, never()).autoLogin(any());
    }

    @Test
    void logoutSwitchesAutoLoginOffUntilTheNextLogin() {
        when(authService.isAutoLoginEnabled()).thenReturn(true);
        var logout = controller.logout(new MockHttpServletRequest());
        assertTrue(cookies(logout).stream().anyMatch(c -> c.startsWith("pp_no_auto_login=1;")));

        when(authService.verifyCode(any(), any(), any())).thenReturn(Optional.of("sess"));
        when(authService.findUser("sess")).thenReturn(Optional.of(TEST_USER));
        MockHttpServletRequest login = new MockHttpServletRequest();
        login.setCookies(new Cookie("pp_no_auto_login", "1"));
        var verify = controller.verify(java.util.Map.of("email", "a@b.de", "code", "123456"), login);
        assertTrue(cookies(verify).stream().anyMatch(c -> c.startsWith("pp_no_auto_login=;") && c.contains("Max-Age=0")));
    }

    @Test
    void logoutWithoutAutoLoginSetsNoExtraCookie() {
        var logout = controller.logout(new MockHttpServletRequest());
        assertEquals(1, cookies(logout).size());
    }
}
