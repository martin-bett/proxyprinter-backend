package my.poo.mtgcrawl.mtgprint.service.mail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class MailServiceTest {

    @SuppressWarnings("unchecked")
    private static MailService service(JavaMailSender sender, String from, int maxPerHour) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return new MailService(provider, from, maxPerHour);
    }

    @Test
    void validatesAddresses() {
        assertTrue(MailService.isValidAddress("martin@example.com"));
        assertTrue(MailService.isValidAddress("a.b+c@sub.example.eu"));
        assertFalse(MailService.isValidAddress(null));
        assertFalse(MailService.isValidAddress(""));
        assertFalse(MailService.isValidAddress("martin"));
        assertFalse(MailService.isValidAddress("martin@example"));
        assertFalse(MailService.isValidAddress("a b@example.com"));
        assertFalse(MailService.isValidAddress("a@example.com\nBcc: x@example.com"));
        assertFalse(MailService.isValidAddress("a".repeat(250) + "@example.com"));
    }

    @Test
    void isDisabledWithoutSenderOrFrom() {
        assertFalse(service(null, "noreply@example.com", 10).isEnabled());
        assertFalse(service(mock(JavaMailSender.class), "", 10).isEnabled());
        MailService disabled = service(null, "", 10);
        assertFalse(disabled.send("a@example.com", "s", "t"));
    }

    @Test
    void sendsAndLimitsMailsPerRecipient() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MailService mail = service(sender, "noreply@example.com", 2);
        try {
            assertTrue(mail.isEnabled());
            assertTrue(mail.send("a@example.com", "s", "t"));
            assertTrue(mail.send("A@example.com", "s", "t"));
            assertFalse(mail.send("a@example.com", "s", "t"), "third mail within the hour is dropped");
            assertTrue(mail.send("b@example.com", "s", "t"));
            assertFalse(mail.send("not-an-address", "s", "t"));
            verify(sender, timeout(5000).times(3)).send(any(SimpleMailMessage.class));
        } finally {
            mail.shutdown();
        }
    }
}
