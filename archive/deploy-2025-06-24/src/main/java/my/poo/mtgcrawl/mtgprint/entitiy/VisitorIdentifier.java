/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class VisitorIdentifier {
    private String ipAddress;
    private String browserFingerprint;

    public VisitorIdentifier() {
    }

    public VisitorIdentifier(String ipAddress, String browserFingerprint) {
        this.ipAddress = ipAddress;
        this.browserFingerprint = browserFingerprint;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        VisitorIdentifier that = (VisitorIdentifier)o;
        return Objects.equals(this.browserFingerprint, that.browserFingerprint);
    }

    public int hashCode() {
        return Objects.hash(this.ipAddress, this.browserFingerprint);
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public String getBrowserFingerprint() {
        return this.browserFingerprint;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setBrowserFingerprint(String browserFingerprint) {
        this.browserFingerprint = browserFingerprint;
    }
}

