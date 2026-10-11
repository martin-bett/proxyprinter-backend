package my.poo.mtgcrawl.mtgprint.service.optout;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.service.net.BlockedUrlException;
import my.poo.mtgcrawl.mtgprint.service.net.OutboundUrlGuard;

/**
 * Prüft, ob der Betreiber eines Hosts den Prüf-Token hinterlegt hat: als DNS-TXT-Eintrag
 * {@value #DNS_PREFIX}&lt;host&gt; oder als Datei {@value #FILE_PATH} auf dem Host (https, sonst http).
 * Weiterleitungen werden nicht verfolgt, sonst könnte ein fremder Host den Nachweis liefern.
 */
@Slf4j
@Component
public class OwnershipCheck {

    public static final String DNS_PREFIX = "_proxyprinter-verify.";
    public static final String DNS_VALUE_PREFIX = "proxyprinter-verify=";
    public static final String FILE_PATH = "/.well-known/proxyprinter-verify.txt";
    private static final int MAX_FILE_BYTES = 4096;

    private final OutboundUrlGuard guard;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .proxy(ProxySelector.getDefault())
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public OwnershipCheck(OutboundUrlGuard guard) {
        this.guard = guard;
    }

    public boolean dnsHasToken(String host, String token) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("com.sun.jndi.dns.timeout.initial", "3000");
        env.put("com.sun.jndi.dns.timeout.retries", "2");
        try {
            InitialDirContext context = new InitialDirContext(env);
            try {
                Attributes attributes = context.getAttributes("dns:/" + DNS_PREFIX + host, new String[] { "TXT" });
                Attribute txt = attributes.get("TXT");
                if (txt == null) {
                    return false;
                }
                NamingEnumeration<?> values = txt.getAll();
                while (values.hasMore()) {
                    // Lange TXT-Werte kommen in Stücken ("a" "b"), Anführungszeichen und Leerzeichen dazwischen entfernen
                    String value = String.valueOf(values.next()).replace("\" \"", "").replace("\"", "").trim();
                    if (value.equals(DNS_VALUE_PREFIX + token)) {
                        return true;
                    }
                }
                return false;
            } finally {
                context.close();
            }
        } catch (NamingException e) {
            log.debug("TXT lookup for {} failed: {}", host, e.getMessage());
            return false;
        }
    }

    public boolean fileHasToken(String host, String token) {
        return fileHasToken(host, "", token);
    }

    /** port mit Doppelpunkt (":8080") oder leer, nur für Tests. */
    boolean fileHasToken(String host, String port, String token) {
        String authority = (host.contains(":") ? "[" + host + "]" : host) + port;
        for (String scheme : new String[] { "https", "http" }) {
            try {
                URI uri = guard.check(URI.create(scheme + "://" + authority + FILE_PATH));
                HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "proxyprinter.eu ownership check (+https://proxyprinter.eu)").GET().build();
                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    if (response.statusCode() == 200
                            && new String(body.readNBytes(MAX_FILE_BYTES), StandardCharsets.UTF_8).trim().equals(token)) {
                        return true;
                    }
                }
            } catch (BlockedUrlException | IOException | IllegalArgumentException e) {
                log.debug("Ownership file on {} via {} not readable: {}", host, scheme, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
