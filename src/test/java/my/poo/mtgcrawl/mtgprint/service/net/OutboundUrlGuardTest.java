package my.poo.mtgcrawl.mtgprint.service.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.URI;

import org.junit.jupiter.api.Test;

class OutboundUrlGuardTest {

    private final OutboundUrlGuard guard = new OutboundUrlGuard(false);

    private static String code(Runnable r) {
        return assertThrows(BlockedUrlException.class, r::run).getCode();
    }

    @Test
    void erlaubtOeffentlicheAdressen() throws Exception {
        assertEquals(URI.create("https://93.184.215.14/x"), guard.check("https://93.184.215.14/x"));
        assertFalse(OutboundUrlGuard.isInternal(InetAddress.getByName("8.8.8.8")));
        assertFalse(OutboundUrlGuard.isInternal(InetAddress.getByName("2a00:1450:4001:80b::200e")));
    }

    @Test
    void lehntNichtHttpAb() {
        assertEquals("proxy.httpOnly", code(() -> guard.check("file:///etc/passwd")));
        assertEquals("proxy.httpOnly", code(() -> guard.check("gopher://example.com/")));
        assertEquals("proxy.invalidUrl", code(() -> guard.check("http://")));
        assertEquals("proxy.invalidUrl", code(() -> guard.check("not a url")));
    }

    @Test
    void lehntInterneAdressenAb() {
        for (String url : new String[] {
                "http://localhost:8080/actuator", "http://127.0.0.1/", "http://0.0.0.0/",
                "http://10.0.0.5/", "http://172.18.0.2:5432/", "http://192.168.1.5/", "http://169.254.169.254/latest/meta-data/",
                "http://100.64.0.1/", "http://[::1]/", "http://[fd00::1]/", "http://[fe80::1]/", "http://[::ffff:127.0.0.1]/",
                "http://2130706433/" }) {
            assertEquals("proxy.internalAddress", code(() -> guard.check(url)), url);
        }
        // Kurzform: URI erkennt keinen Host, abgelehnt wird sie trotzdem
        assertThrows(BlockedUrlException.class, () -> guard.check("http://127.1/"));
    }

    @Test
    void unbekannterHost() {
        assertEquals("proxy.hostNotFound", code(() -> guard.check("http://does-not-exist.invalid/")));
    }

    @Test
    void erlaubtInternFuerTests() {
        assertEquals(URI.create("http://127.0.0.1:1/"), new OutboundUrlGuard(true).check("http://127.0.0.1:1/"));
        assertEquals("proxy.httpOnly", code(() -> new OutboundUrlGuard(true).check("file:///etc/passwd")));
    }
}
