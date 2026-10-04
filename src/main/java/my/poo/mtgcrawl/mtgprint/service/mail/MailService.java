package my.poo.mtgcrawl.mtgprint.service.mail;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * Verschickt Text-Mails über SMTP. Eingeschaltet ist der Versand nur, wenn
 * {@code spring.mail.host} und ein Absender ({@code mail.from}) gesetzt sind,
 * beides kommt aus Umgebungsvariablen auf dem Server (SPRING_MAIL_HOST, MAIL_FROM, ...).
 * Gesendet wird in einem eigenen Thread, damit ein langsamer SMTP-Server nichts blockiert.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_LENGTH = 254;
    private static final long HOUR_MS = 60 * 60 * 1000L;

    private final JavaMailSender sender;
    private final String from;
    private final int maxPerRecipientPerHour;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Map<String, Deque<Long>> sentPerRecipient = new ConcurrentHashMap<>();

    public MailService(ObjectProvider<JavaMailSender> senderProvider,
            @Value("${mail.from:${spring.mail.username:}}") String from,
            @Value("${mail.max-per-recipient-per-hour:10}") int maxPerRecipientPerHour) {
        this.sender = senderProvider.getIfAvailable();
        this.from = from == null ? "" : from.trim();
        this.maxPerRecipientPerHour = maxPerRecipientPerHour;
        if (!isEnabled()) {
            log.info("Mail delivery is disabled (spring.mail.host or mail.from not set)");
        }
    }

    public boolean isEnabled() {
        return sender != null && !from.isEmpty();
    }

    /** Einfache Prüfung, die auch Zeilenumbrüche (Header-Injection) ausschließt. */
    public static boolean isValidAddress(String address) {
        return address != null && address.length() <= MAX_LENGTH && EMAIL.matcher(address).matches();
    }

    /**
     * Stellt die Mail in die Sendewarteschlange. Gibt false zurück, wenn der Versand
     * abgeschaltet ist, die Adresse ungültig ist oder die Adresse in der letzten Stunde
     * schon zu viele Mails bekommen hat.
     */
    public boolean send(String to, String subject, String text) {
        if (!isEnabled()) {
            log.warn("Mail to " + to + " not sent, mail delivery is disabled");
            return false;
        }
        if (!isValidAddress(to)) {
            log.warn("Mail not sent, invalid address: " + to);
            return false;
        }
        if (!allowSend(to.toLowerCase())) {
            log.warn("Mail to " + to + " not sent, more than " + maxPerRecipientPerHour + " mails in the last hour");
            return false;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        executor.submit(() -> {
            try {
                sender.send(message);
                log.info("Mail sent to " + to);
            } catch (Exception e) {
                log.error("Could not send mail to " + to, e);
            }
        });
        return true;
    }

    private boolean allowSend(String recipient) {
        long now = System.currentTimeMillis();
        Deque<Long> sent = sentPerRecipient.computeIfAbsent(recipient, k -> new ArrayDeque<>());
        synchronized (sent) {
            while (!sent.isEmpty() && now - sent.peekFirst() > HOUR_MS) {
                sent.pollFirst();
            }
            if (maxPerRecipientPerHour > 0 && sent.size() >= maxPerRecipientPerHour) {
                return false;
            }
            sent.addLast(now);
            return true;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
