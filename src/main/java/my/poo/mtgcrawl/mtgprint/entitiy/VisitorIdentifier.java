package my.poo.mtgcrawl.mtgprint.entitiy;

import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

// VisitorIdentifier.java
@Embeddable
@Getter
@Setter
public class VisitorIdentifier {
    private String ipAddress;
    private String browserFingerprint;
    
    public VisitorIdentifier() {}
    
    public VisitorIdentifier(String ipAddress, String browserFingerprint) {
        this.ipAddress = ipAddress;
        this.browserFingerprint = browserFingerprint;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisitorIdentifier that = (VisitorIdentifier) o;
        return Objects.equals(browserFingerprint, that.browserFingerprint);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(ipAddress, browserFingerprint);
    }
}