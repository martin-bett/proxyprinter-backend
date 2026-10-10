package my.poo.mtgcrawl.mtgprint.resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import my.poo.mtgcrawl.mtgprint.service.mail.MailService;
import my.poo.mtgcrawl.mtgprint.service.optout.OwnershipCheck;

/** Selbst-Sperre über /api/blocklist mit gemocktem Mailversand und DNS/Datei-Prüfung. Braucht die Test-DB wie contextLoads. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OptOutControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoBean
    private MailService mailService;
    @MockitoBean
    private OwnershipCheck ownershipCheck;

    private final ObjectMapper json = new ObjectMapper();

    private ResultActions post(String path, String body, String ip) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/blocklist" + path)
                .with(r -> { r.setRemoteAddr(ip); return r; })
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String ip() {
        return "203.0.113." + (1 + (int) (Math.random() * 250));
    }

    private boolean blocked(String host) {
        return jdbc.queryForObject("SELECT count(*) FROM proxymaster.blocked_host WHERE host = ?", Integer.class, host) == 1;
    }

    @Test
    void dnsOderDateiNachweisSperrtDenHost() throws Exception {
        String host = "ops-" + UUID.randomUUID().toString().substring(0, 8) + ".example.com";
        String response = post("/request", "{\"host\":\"https://" + host.toUpperCase() + "/x\",\"method\":\"challenge\"}", ip())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("challenge"))
                .andExpect(jsonPath("$.dnsName").value("_proxyprinter-verify." + host))
                .andExpect(jsonPath("$.fileUrl").value("https://" + host + "/.well-known/proxyprinter-verify.txt"))
                .andReturn().getResponse().getContentAsString();
        String token = json.readTree(response).get("token").asText();
        assertEquals("proxyprinter-verify=" + token, json.readTree(response).get("dnsValue").asText());

        // Noch nicht hinterlegt
        post("/verify", "{\"token\":\"" + token + "\"}", ip()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("blocklist.notVerified"));
        assertFalse(blocked(host));

        when(ownershipCheck.fileHasToken(host, token)).thenReturn(true);
        post("/verify", "{\"token\":\"" + token + "\"}", ip()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("blocked")).andExpect(jsonPath("$.host").value(host));
        assertTrue(blocked(host));
        // Token ist verbraucht, neue Anfrage meldet gleich "blocked"
        post("/verify", "{\"token\":\"" + token + "\"}", ip()).andExpect(status().isNotFound());
        post("/request", "{\"host\":\"www." + host + "\",\"method\":\"challenge\"}", ip())
                .andExpect(jsonPath("$.status").value("blocked"));
    }

    @Test
    void mailLinkSperrtDenHost() throws Exception {
        String host = "mail-" + UUID.randomUUID().toString().substring(0, 8) + ".example.org";
        when(mailService.isEnabled()).thenReturn(true);
        when(mailService.send(anyString(), anyString(), anyString())).thenReturn(true);

        post("/request", "{\"host\":\"" + host + "\",\"method\":\"mail\",\"mailbox\":\"webmaster\",\"language\":\"de\"}", ip())
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.email").value("webmaster@" + host)).andExpect(jsonPath("$.token").doesNotExist());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("webmaster@" + host), contains(host), text.capture());
        assertTrue(text.getValue().contains("Bestätigen"), text.getValue());
        String token = text.getValue().replaceAll("(?s).*\\?blockverify=(\\S+).*", "$1");

        post("/verify", "{\"token\":\"" + token + "\"}", ip()).andExpect(status().isOk())
                .andExpect(jsonPath("$.host").value(host));
        assertTrue(blocked(host));
        verify(ownershipCheck, never()).dnsHasToken(any(), any());
    }

    @Test
    void ungueltigeEingaben() throws Exception {
        post("/request", "{\"host\":\"localhost\",\"method\":\"challenge\"}", ip()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("blocklist.invalidHost"));
        post("/request", "{\"host\":\"\",\"method\":\"challenge\"}", ip()).andExpect(jsonPath("$.code").value("blocklist.invalidHost"));
        when(mailService.isEnabled()).thenReturn(true);
        post("/request", "{\"host\":\"198.51.100.7\",\"method\":\"mail\",\"mailbox\":\"admin\"}", ip())
                .andExpect(jsonPath("$.code").value("blocklist.mailNeedsDomain"));
        post("/request", "{\"host\":\"example.net\",\"method\":\"mail\",\"mailbox\":\"info\"}", ip())
                .andExpect(jsonPath("$.code").value("blocklist.invalidMailbox"));
        when(mailService.isEnabled()).thenReturn(false);
        post("/request", "{\"host\":\"example.net\",\"method\":\"mail\",\"mailbox\":\"admin\"}", ip())
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("blocklist.mailDisabled"));
        post("/verify", "{\"token\":\"nope\"}", ip()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("blocklist.tokenInvalid"));
        // IP: nur Datei, kein DNS-Name
        post("/request", "{\"host\":\"198.51.100.7\",\"method\":\"challenge\"}", ip())
                .andExpect(jsonPath("$.dnsName").doesNotExist())
                .andExpect(jsonPath("$.fileUrl").value("https://198.51.100.7/.well-known/proxyprinter-verify.txt"));
    }

    @Test
    void begrenztAnfragenProIp() throws Exception {
        String ip = "198.51.100.200";
        for (int i = 0; i < 10; i++) {
            post("/request", "{\"host\":\"rate" + i + ".example.com\",\"method\":\"challenge\"}", ip).andExpect(status().isOk());
        }
        post("/request", "{\"host\":\"rate-x.example.com\",\"method\":\"challenge\"}", ip).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("blocklist.rateLimited"));
    }
}
