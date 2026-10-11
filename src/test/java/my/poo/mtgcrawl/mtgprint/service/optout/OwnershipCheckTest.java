package my.poo.mtgcrawl.mtgprint.service.optout;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import my.poo.mtgcrawl.mtgprint.service.net.OutboundUrlGuard;

class OwnershipCheckTest {

    private HttpServer server;
    private String port;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(OwnershipCheck.FILE_PATH, exchange -> {
            byte[] body = "  token-123\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/redirect" + OwnershipCheck.FILE_PATH, exchange -> {
            exchange.getResponseHeaders().add("Location", OwnershipCheck.FILE_PATH);
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        port = ":" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void findetDenTokenInDerDateiPerHttp() {
        OwnershipCheck check = new OwnershipCheck(new OutboundUrlGuard(true));
        assertTrue(check.fileHasToken("127.0.0.1", port, "token-123"));
        assertFalse(check.fileHasToken("127.0.0.1", port, "other"));
    }

    @Test
    void interneAdressenNurWennErlaubt() {
        assertFalse(new OwnershipCheck(new OutboundUrlGuard(false)).fileHasToken("127.0.0.1", port, "token-123"));
    }

    @Test
    void unbekannteDomainHatKeinenDnsEintrag() {
        assertFalse(new OwnershipCheck(new OutboundUrlGuard(false)).dnsHasToken("does-not-exist.invalid", "x"));
    }
}
