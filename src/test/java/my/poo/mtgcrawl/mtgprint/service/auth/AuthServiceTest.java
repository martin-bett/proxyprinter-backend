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
    private final AuthService service = new AuthService(repository, mail, "https://example.test/", false);

    private String sentText() {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("a@b.de"), anyString(), text.capture());
        return text.getValue();
    }

    @Test
    void loginMailContainsLinkAndCodeAndStoresOnlyHashes() {
        when(mail.isEnabled()).thenReturn(true);

        assertEquals(RequestResult.SENT, service.requestLogin("  A@B.de ", "1.1.1.1"));

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
        assertEquals(RequestResult.INVALID_EMAIL, service.requestLogin("nonsense", "1.1.1.1"));
        assertEquals(RequestResult.INVALID_EMAIL, service.requestLogin("a@b.de\nBcc: x@y.de", "1.1.1.1"));

        when(mail.isEnabled()).thenReturn(false);
        assertEquals(RequestResult.DISABLED, service.requestLogin("a@b.de", "1.1.1.1"));
        verify(repository, never()).replaceLoginToken(any());
    }

    @Test
    void localLinkLoggingEnablesLoginWithoutMail() {
        when(mail.isEnabled()).thenReturn(false);
        AuthService local = new AuthService(repository, mail, "http://localhost:3000", true);
        assertTrue(local.isLoginEnabled());
        assertEquals(RequestResult.SENT, local.requestLogin("a@b.de", "1.1.1.1"));
        verify(mail, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void limitsRequestsPerAddress() {
        when(mail.isEnabled()).thenReturn(true);
        for (int i = 0; i < AuthService.MAX_REQUESTS_PER_EMAIL_PER_HOUR; i++) {
            assertEquals(RequestResult.SENT, service.requestLogin("a@b.de", "1.1.1." + i));
        }
        assertEquals(RequestResult.RATE_LIMITED, service.requestLogin("a@b.de", "9.9.9.9"));
        assertEquals(RequestResult.SENT, service.requestLogin("other@b.de", "9.9.9.9"));
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
}
