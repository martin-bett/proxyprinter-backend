package my.poo.mtgcrawl.mtgprint.service.stats;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Erkennt, ob ein Besucher heute schon gezählt wurde, ohne IP oder User-Agent zu speichern: im Speicher liegt nur ein
 * HMAC aus beidem mit einem zufälligen Schlüssel, der jeden Tag neu erzeugt und mit der Liste verworfen wird.
 * Nichts davon landet in der DB, dort steht nur die Anzahl. Nach einem Neustart zählt ein Besucher am selben Tag erneut.
 */
class VisitorCounter {

    /** Obergrenze gegen Speicherverbrauch, darüber zählen neue Besucher an dem Tag nicht mehr */
    static final int MAX_PER_DAY = 200_000;

    private final Supplier<LocalDate> today;
    private final SecureRandom random = new SecureRandom();

    private LocalDate day;
    private Mac mac;
    private final Set<String> seen = new HashSet<>();

    VisitorCounter(Supplier<LocalDate> today) {
        this.today = today;
    }

    /** true beim ersten Aufruf heute für diese Kombination aus Adresse und Browser. */
    synchronized boolean firstVisitToday(String address, String userAgent) {
        LocalDate now = today.get();
        if (!now.equals(day)) {
            day = now;
            seen.clear();
            mac = newMac();
        }
        if (seen.size() >= MAX_PER_DAY) {
            return false;
        }
        byte[] digest = mac.doFinal((address + "\n" + (userAgent == null ? "" : userAgent)).getBytes(StandardCharsets.UTF_8));
        return seen.add(java.util.HexFormat.of().formatHex(digest, 0, 16));
    }

    private Mac newMac() {
        byte[] key = new byte[32];
        random.nextBytes(key);
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(key, "HmacSHA256"));
            return m;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }
}
