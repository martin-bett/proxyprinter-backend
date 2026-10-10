package my.poo.mtgcrawl.mtgprint.service.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import my.poo.mtgcrawl.mtgprint.service.auth.AuthService.RequestResult;
import my.poo.mtgcrawl.mtgprint.service.mail.MailService;

class AuthServiceTest {

    private final AuthRepository repository = mock(AuthRepository.class);
    private final MailService mail = mock(MailService.class);
    private final AuthService service = new AuthService(repository, mail, "https://example.test/", false, false, "");

    private String sentText() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("a@b.de"), anyString(), text.capture());
        return text.getValue();
    }

    @Test
    void loginMailContainsLinkAndCodeAndStoresOnlyHashes() {
        when(mail.isEnabled()).thenReturn(true);

        assertEquals(RequestResult.SENT, service.requestLogin("  A@B.de ", "1.1.1.1").result());

        String text = sentText();
        String token = text.substring(text.indexOf("/?login=") + 8, text.indexOf("\n\n", text.indexOf("/?login=")));
        String code = text.replaceAll("(?s).*code in the login window: (\\d{6}).*", "$1");
        ArgumentCaptor<LoginToken> stored = ArgumentCaptor.forClass(LoginToken.class);
        verify(repository).replaceLoginToken(stored.capture());
        assertEquals("a@b.de", stored.getValue().email());
        assertEquals(AuthService.hash(token), stored.getValue().tokenHash());
        assertEquals(AuthService.hash("a@b.de:" + code), stored.getValue().codeHash());
        assertTrue(text.contains("https://example.test/?login="));
    }

    @Test
    void invalidAddressAndDisabledMailAreRejected() {
        when(mail.isEnabled()).thenReturn(true);
        assertEquals(RequestResult.INVALID_EMAIL, service.requestLogin("nonsense", "1.1.1.1").result());
        assertEquals(RequestResult.INVALID_EMAIL, service.requestLogin("a@b.de\nBcc: x@y.de", "1.1.1.1").result());

        when(mail.isEnabled()).thenReturn(false);
        assertEquals(RequestResult.DISABLED, service.requestLogin("a@b.de", "1.1.1.1").result());
        verify(repository, never()).replaceLoginToken(any());
    }

    @Test
    void localLinkLoggingEnablesLoginWithoutMail() {
        when(mail.isEnabled()).thenReturn(false);
        AuthService local = new AuthService(repository, mail, "http://localhost:3000", true, false, "");
        assertTrue(local.isLoginEnabled());
        assertEquals(RequestResult.SENT, local.requestLogin("a@b.de", "1.1.1.1").result());
        verify(mail, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void testEnvironmentReturnsTheCodeInsteadOfMailing() {
        when(mail.isEnabled()).thenReturn(false);
        AuthService test = new AuthService(repository, mail, "https://x.test", false, true, "");
        assertTrue(test.isLoginEnabled());

        AuthService.LoginRequest result = test.requestLogin("a@b.de", "1.1.1.1");

        assertEquals(RequestResult.SENT, result.result());
        assertTrue(result.devCode().matches("\\d{6}"));
        ArgumentCaptor<LoginToken> stored = ArgumentCaptor.forClass(LoginToken.class);
        verify(repository).replaceLoginToken(stored.capture());
        assertEquals(AuthService.hash("a@b.de:" + result.devCode()), stored.getValue().codeHash());
        verify(mail, never()).send(anyString(), anyString(), anyString());
        assertNull(service.requestLogin("a@b.de", "2.2.2.2").devCode());
    }

    @Test
    void limitsRequestsPerAddress() {
        when(mail.isEnabled()).thenReturn(true);
        for (int i = 0; i < AuthService.MAX_REQUESTS_PER_EMAIL_PER_HOUR; i++) {
            assertEquals(RequestResult.SENT, service.requestLogin("a@b.de", "1.1.1." + i).result());
        }
        assertEquals(RequestResult.RATE_LIMITED, service.requestLogin("a@b.de", "9.9.9.9").result());
        assertEquals(RequestResult.SENT, service.requestLogin("other@b.de", "9.9.9.9").result());
    }

    @Test
    void validTokenStartsSessionAndConsumesLogin() {
        when(repository.findLoginTokenByHash(AuthService.hash("tok"))).thenReturn(Optional.of(
                new LoginToken("a@b.de", AuthService.hash("tok"), "x", 0, LocalDateTime.now().plusMinutes(5))));
        when(repository.findOrCreateUser("a@b.de")).thenReturn(new AppUser(7, "a@b.de", LocalDateTime.now()));

        Optional<String> session = service.verifyToken("tok", "UA");

        assertTrue(session.isPresent());
        verify(repository).deleteLoginToken("a@b.de");
        verify(repository).insertSession(AuthService.hash(session.get()), 7, "UA");
    }

    @Test
    void expiredOrUnknownTokenFails() {
        when(repository.findLoginTokenByHash(anyString())).thenReturn(Optional.empty());
        assertTrue(service.verifyToken("nope", "UA").isEmpty());

        when(repository.findLoginTokenByHash(AuthService.hash("old"))).thenReturn(Optional.of(
                new LoginToken("a@b.de", AuthService.hash("old"), "x", 0, LocalDateTime.now().minusMinutes(1))));
        assertTrue(service.verifyToken("old", "UA").isEmpty());
        verify(repository, never()).insertSession(anyString(), anyLong(), any());
    }

    @Test
    void codeMustMatchAndWrongTriesAreCounted() {
        LoginToken token = new LoginToken("a@b.de", "t", AuthService.hash("a@b.de:123456"), 0, LocalDateTime.now().plusMinutes(5));
        when(repository.findLoginTokenByEmail("a@b.de")).thenReturn(Optional.of(token));
        when(repository.findOrCreateUser("a@b.de")).thenReturn(new AppUser(7, "a@b.de", LocalDateTime.now()));

        assertTrue(service.verifyCode("A@b.de", "000000", "UA").isEmpty());
        verify(repository).incrementAttempts("a@b.de");
        assertTrue(service.verifyCode("A@b.de", " 123456 ", "UA").isPresent());
    }

    @Test
    void tooManyWrongCodesKillTheLogin() {
        LoginToken token = new LoginToken("a@b.de", "t", AuthService.hash("a@b.de:123456"),
                AuthService.MAX_CODE_ATTEMPTS, LocalDateTime.now().plusMinutes(5));
        when(repository.findLoginTokenByEmail("a@b.de")).thenReturn(Optional.of(token));

        assertTrue(service.verifyCode("a@b.de", "123456", "UA").isEmpty());
        verify(repository).deleteLoginToken("a@b.de");
    }

    @Test
    void sessionLookupHashesTheCookieValue() {
        AppUser user = new AppUser(7, "a@b.de", LocalDateTime.now());
        when(repository.findUserBySession(AuthService.hash("sess"))).thenReturn(Optional.of(user));

        assertEquals(user, service.findUser("sess").orElseThrow());
        verify(repository).touchSession(AuthService.hash("sess"));
        assertTrue(service.findUser(null).isEmpty());
        assertTrue(service.findUser("other").isEmpty());
    }

    @Test
    void autoLoginStartsASessionForTheTestAccount() {
        when(mail.isEnabled()).thenReturn(false);
        when(repository.findOrCreateUser("test@proxyprinter.eu")).thenReturn(new AppUser(7, "test@proxyprinter.eu", LocalDateTime.now()));
        AuthService test = new AuthService(repository, mail, "https://abc123.test.proxyprinter.eu", false, false, " Test@Proxyprinter.eu ");

        assertTrue(test.isAutoLoginEnabled());
        assertTrue(test.isLoginEnabled());
        String session = test.autoLogin("Browser").orElseThrow();

        verify(repository).insertSession(AuthService.hash(session), 7L, "Browser");
        assertFalse(service.isAutoLoginEnabled());
        assertTrue(service.autoLogin("Browser").isEmpty());
    }

    @Test
    void autoLoginIsIgnoredOnTheProductionDomainAndForInvalidAddresses() {
        assertNull(AuthService.autoLoginEmail("test@proxyprinter.eu", "https://proxyprinter.eu"));
        assertNull(AuthService.autoLoginEmail("test@proxyprinter.eu", "https://www.proxyprinter.eu/"));
        assertNull(AuthService.autoLoginEmail("test@proxyprinter.eu", "HTTPS://ProxyPrinter.EU:443"));
        assertNull(AuthService.autoLoginEmail("nonsense", "https://test.proxyprinter.eu"));
        assertNull(AuthService.autoLoginEmail("", "https://test.proxyprinter.eu"));
        assertEquals("test@proxyprinter.eu", AuthService.autoLoginEmail("test@proxyprinter.eu", "https://test.proxyprinter.eu"));
    }
}
