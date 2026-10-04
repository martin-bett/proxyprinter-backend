package my.poo.mtgcrawl.mtgprint.service.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import my.poo.mtgcrawl.mtgprint.service.mail.MailService;

/**
 * Schreibt die Mails rund um ein PDF. Verschickt wird ein Download-Link statt eines
 * Anhangs, weil PDFs mehrere hundert MB groß werden können. Der Link zeigt auf
 * GET /api/pdf/{progressId} und gilt so lange, wie das PDF im Cache liegt (24h).
 */
@Component
public class PdfMailNotifier {

    private final MailService mailService;
    private final String publicUrl;

    public PdfMailNotifier(MailService mailService, @Value("${app.public-url:https://proxyprinter.eu}") String publicUrl) {
        this.mailService = mailService;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    public boolean isEnabled() {
        return mailService.isEnabled();
    }

    String downloadUrl(String progressId) {
        return publicUrl + "/api/pdf/" + progressId;
    }

    public void sendReady(String to, String progressId, long fileSize) {
        long hours = PdfCacheService.MAX_AGE_MS / (60 * 60 * 1000L);
        String text = "Hi,\n\n"
                + "your proxy PDF is ready" + (fileSize > 0 ? " (" + formatMb(fileSize) + ")" : "") + ". Download it here:\n\n"
                + downloadUrl(progressId) + "\n\n"
                + "The link works for about " + hours + " hours.\n\n"
                + "Proxyprinter\n" + publicUrl + "\n";
        mailService.send(to, "Your proxy PDF is ready", text);
    }

    public void sendFailed(String to, String reason) {
        String text = "Hi,\n\n"
                + "unfortunately your proxy PDF could not be created:\n\n"
                + reason + "\n\n"
                + "Please try again on " + publicUrl + "\n\n"
                + "Proxyprinter\n";
        mailService.send(to, "Your proxy PDF could not be created", text);
    }

    private static String formatMb(long bytes) {
        return Math.max(1, Math.round(bytes / (1024.0 * 1024.0))) + " MB";
    }
}
