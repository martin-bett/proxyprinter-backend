package my.poo.mtgcrawl.mtgprint.service.net;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Prüft Ziele von Abrufen, die das Backend im Auftrag eines Besuchers macht (Proxy, Probe, Bilder, PDF-Bilder):
 * nur http(s), keine Adressen im eigenen Netz (localhost, Docker-Netz, private Netze, Link-Local, Cloud-Metadaten)
 * und keine Hosts der HostBlocklist.
 * Jede Weiterleitung wird erneut geprüft (GuardedHttp, GuardedRequestFactory).
 * Die JVM cached DNS-Antworten (30 s), Prüfung und Verbindung sehen also dieselbe Adresse.
 */
@Component
public class OutboundUrlGuard {

    private final boolean allowInternal;
    private final HostBlocklist blocklist;

    public OutboundUrlGuard(boolean allowInternal) {
        this(allowInternal, HostBlocklist.NONE);
    }

    @Autowired
    public OutboundUrlGuard(@Value("${outbound.allow-internal-addresses:false}") boolean allowInternal, HostBlocklist blocklist) {
        this.allowInternal = allowInternal;
        this.blocklist = blocklist;
    }

    public URI check(String url) {
        if (url == null || url.isBlank()) {
            throw new BlockedUrlException("proxy.invalidUrl", "Invalid URL.", Map.of("url", ""));
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new BlockedUrlException("proxy.invalidUrl", "Invalid URL: " + url, Map.of("url", url));
        }
        return check(uri);
    }

    public URI check(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BlockedUrlException("proxy.httpOnly", "Only http(s) URLs are allowed: " + uri, Map.of("url", uri.toString()));
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new BlockedUrlException("proxy.invalidUrl", "Invalid URL: " + uri, Map.of("url", uri.toString()));
        }
        // Auch bei erlaubten internen Adressen: gesperrte Betreiber bleiben gesperrt
        blocklist.check(uri);
        if (allowInternal) {
            return uri;
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isInternal(address)) {
                    throw new BlockedUrlException("proxy.internalAddress", "Internal addresses are not allowed.", Map.of("host", host));
                }
            }
        } catch (UnknownHostException e) {
            throw new BlockedUrlException("proxy.hostNotFound", host + " not found.", Map.of("host", host));
        }
        return uri;
    }

    /** Alles, was nicht ins öffentliche Internet zeigt. */
    public static boolean isInternal(InetAddress address) {
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || address.isAnyLocalAddress() || address.isMulticastAddress()) {
            return true;
        }
        byte[] b = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = b[0] & 0xff;
            int second = b[1] & 0xff;
            return first == 0                                     // 0.0.0.0/8
                    || (first == 100 && second >= 64 && second < 128) // 100.64.0.0/10 (Carrier-NAT)
                    || (first == 192 && second == 0 && (b[2] & 0xff) == 0) // 192.0.0.0/24
                    || (first == 198 && (second == 18 || second == 19))   // 198.18.0.0/15
                    || first >= 240;                               // reserviert, Broadcast
        }
        if (address instanceof Inet6Address) {
            int first = b[0] & 0xff;
            if ((first & 0xfe) == 0xfc) {
                return true; // fc00::/7 (Unique Local)
            }
            boolean mappedOrCompatible = true;
            for (int i = 0; i < 10; i++) {
                mappedOrCompatible &= b[i] == 0;
            }
            if (mappedOrCompatible && ((b[10] == 0 && b[11] == 0) || (b[10] == (byte) 0xff && b[11] == (byte) 0xff))) {
                // ::a.b.c.d und ::ffff:a.b.c.d: die eingebettete IPv4-Adresse prüfen
                try {
                    return isInternal(InetAddress.getByAddress(new byte[] { b[12], b[13], b[14], b[15] }));
                } catch (UnknownHostException e) {
                    return true;
                }
            }
        }
        return false;
    }
}
