package my.poo.mtgcrawl.mtgprint.resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthRepository;

/** Gesperrte Hosts (proxymaster.blocked_host): nicht speichern, nicht ausliefern, nicht abrufen. Braucht die Test-DB wie contextLoads. */
@SpringBootTest(properties = "blocklist.refresh-seconds=0")
@AutoConfigureMockMvc
@Transactional
class BlocklistTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Cookie alice;

    @BeforeEach
    void user() throws Exception {
        AppUser a = authRepository.findOrCreateUser("alice-" + UUID.randomUUID() + "@example.com");
        String token = UUID.randomUUID().toString();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        authRepository.insertSession(hash, a.id(), "test");
        alice = new Cookie(AuthController.COOKIE, token);
        jdbc.update("INSERT INTO proxymaster.blocked_host (host, reason) VALUES ('blocked.test', 'test')");
    }

    private long create(String base) throws Exception {
        return Long.parseLong(mvc.perform(get(base + "/create").cookie(alice)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mvc.perform(post(url).cookie(alice).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private boolean listed(String base, long id) throws Exception {
        String list = mvc.perform(get(base).cookie(alice)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return list.contains("\"id\":" + id + ",");
    }

    private static void rejected(ResultActions result, String host) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("blocklist.blockedHost"))
                .andExpect(jsonPath("$.params.host").value(host));
    }

    @Test
    void speichernKopierenUndVeroeffentlichenAbgelehnt() throws Exception {
        String[][] cases = {
                { "/api/configs", "baseUrl" }, { "/api/configs", "officialWebsite" },
                { "/api/gallery", "baseUrl" }, { "/api/deckfinder", "fetchUrl" }, { "/api/deckfinder", "searchUrl" } };
        for (String[] c : cases) {
            long id = create(c[0]);
            rejected(postJson(c[0] + "/edit", "{\"id\":" + id + ",\"name\":\"x\",\"" + c[1] + "\":\"https://api.Blocked.test/{id}\"}"),
                    "api.blocked.test");
            postJson(c[0] + "/edit", "{\"id\":" + id + ",\"name\":\"x\",\"" + c[1] + "\":\"https://later.test/{id}\"}")
                    .andExpect(status().isOk());
            assertTrue(listed(c[0], id), c[0]);

            // Später gesperrt: nicht mehr ausgeliefert, nicht veröffentlichen, nicht kopieren
            jdbc.update("INSERT INTO proxymaster.blocked_host (host) VALUES ('later.test')");
            assertFalse(listed(c[0], id), c[0]);
            rejected(postJson(c[0] + "/publish", "{\"id\":" + id + ",\"published\":true}"), "later.test");
            rejected(postJson(c[0] + "/copy", "{\"id\":" + id + "}"), "later.test");
            // Zurückziehen und auf eine andere Quelle umstellen geht weiter
            postJson(c[0] + "/publish", "{\"id\":" + id + ",\"published\":false}").andExpect(status().isOk());
            postJson(c[0] + "/edit", "{\"id\":" + id + ",\"name\":\"x\",\"" + c[1] + "\":\"https://other.test/\"}")
                    .andExpect(status().isOk());
            assertTrue(listed(c[0], id), c[0]);
            jdbc.update("DELETE FROM proxymaster.blocked_host WHERE host = 'later.test'");
        }
    }

    @Test
    void proxyRuftGesperrteHostsNichtAb() throws Exception {
        rejected(mvc.perform(post("/api/proxy").header("X-Target-URL", "https://cdn.blocked.test/search")
                .header("X-Request-Format", "json").header("X-Request-Method", "get")), "cdn.blocked.test");
        mvc.perform(post("/api/proxy/probe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://blocked.test/x\",\"method\":\"GET\"}"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("blocked at the request of its operator")));
    }
}
