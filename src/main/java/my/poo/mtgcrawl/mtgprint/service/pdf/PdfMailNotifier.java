package my.poo.mtgcrawl.mtgprint.service.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import my.poo.mtgcrawl.mtgprint.service.mail.MailService;

/**
 * Schreibt die Mails rund um ein PDF. Verschickt wird ein Download-Link statt eines
 * Anhangs, weil PDFs mehrere hundert MB groß werden können. Der Link zeigt auf
 * GET /api/pdf/{progressId} und gilt so lange, wie das PDF im Cache liegt (pdf.retention-days).
 */
@Component
public class PdfMailNotifier {

    private final MailService mailService;
    private final String publicUrl;
    private final int retentionDays;

    public PdfMailNotifier(MailService mailService, @Value("${app.public-url:https://proxyprinter.eu}") String publicUrl,
            @Value("${pdf.retention-days:7}") int retentionDays) {
        this.mailService = mailService;
        this.retentionDays = Math.max(1, retentionDays);
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    public boolean isEnabled() {
        return mailService.isEnabled();
    }

    String downloadUrl(String progressId) {
        return publicUrl + "/api/pdf/" + progressId;
    }

    public void sendReady(String to, String language, String progressId, long fileSize) {
        String size = fileSize > 0 ? MailTexts.get(language, "ready.size", formatMb(fileSize)) : "";
        String valid = retentionDays == 1
                ? MailTexts.get(language, "ready.validOne")
                : MailTexts.get(language, "ready.validMany", retentionDays);
        String text = MailTexts.get(language, "greeting") + "\n\n"
                + MailTexts.get(language, "ready.body", size) + "\n\n"
                + downloadUrl(progressId) + "\n\n"
                + valid + "\n\n"
                + "Proxyprinter\n" + publicUrl + "\n";
        mailService.send(to, MailTexts.get(language, "ready.subject"), text);
    }

    public void sendFailed(String to, String language, String reason) {
        String text = MailTexts.get(language, "greeting") + "\n\n"
                + MailTexts.get(language, "failed.body") + "\n\n"
                + reason + "\n\n"
                + MailTexts.get(language, "failed.again", publicUrl) + "\n\n"
                + "Proxyprinter\n";
        mailService.send(to, MailTexts.get(language, "failed.subject"), text);
    }

    private static String formatMb(long bytes) {
        return Math.max(1, Math.round(bytes / (1024.0 * 1024.0))) + " MB";
    }
}
