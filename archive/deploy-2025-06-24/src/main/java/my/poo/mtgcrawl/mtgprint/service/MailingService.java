/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class MailingService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public void sendPdfMail(String email, byte[] pdf, String progressId) {
        System.out.println("W\u00fcrde PDF an " + email + " senden (Dateiname: " + progressId + ")");
    }
}

