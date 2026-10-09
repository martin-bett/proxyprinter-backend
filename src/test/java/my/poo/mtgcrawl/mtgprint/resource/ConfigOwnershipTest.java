package my.poo.mtgcrawl.mtgprint.resource;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.Cookie;
import my.poo.mtgcrawl.mtgprint.service.auth.AppUser;
import my.poo.mtgcrawl.mtgprint.service.auth.AuthRepository;

/** Configs und Ratings gehören einem Konto: ohne Login nur lesen, fremde Configs nicht ändern. Braucht die Test-DB wie contextLoads. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfigOwnershipTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuthRepository authRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Cookie alice;
    private Cookie bob;
    private long aliceId;

    @BeforeEach
    void users() throws Exception {
        String suffix = UUID.randomUUID().toString();
        AppUser a = authRepository.findOrCreateUser("alice-" + suffix + "@example.com");
        AppUser b = authRepository.findOrCreateUser("bob-" + suffix + "@example.com");
        aliceId = a.id();
        alice = session(a);
        bob = session(b);
    }

    private Cookie session(AppUser user) throws Exception {
        String token = UUID.randomUUID().toString();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        authRepository.insertSession(hash, user.id(), "test");
        return new Cookie(AuthController.COOKIE, token);
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, Cookie cookie) {
        return cookie == null ? request : request.cookie(cookie);
    }

    private long create(String base, Cookie cookie) throws Exception {
        String body = mvc.perform(as(get(base + "/create"), cookie)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body);
    }

    private String postJson(String url, String json, Cookie cookie) throws Exception {
        return mvc.perform(as(post(url), cookie).contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse().getContentAsString();
    }

    private boolean listed(String base, long id, Cookie cookie) throws Exception {
        String list = mvc.perform(as(get(base), cookie)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return list.contains("\"id\":" + id + ",");
    }

    @Test
    void ohneLoginNurLesen() throws Exception {
        for (String base : new String[] { "/api/configs", "/api/gallery", "/api/deckfinder" }) {
            mvc.perform(get(base + "/create")).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("auth.loginRequired"));
            for (String action : new String[] { "/copy", "/edit", "/publish", "/delete" }) {
                mvc.perform(post(base + action).contentType(MediaType.APPLICATION_JSON).content("{\"id\":1}"))
                        .andExpect(status().isUnauthorized());
            }
            mvc.perform(get(base)).andExpect(status().isOk());
        }
        mvc.perform(post("/api/configs/1/rate").contentType(MediaType.APPLICATION_JSON).content("{\"score\":5,\"action\":\"quality\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eigeneConfigsSiehtNurDerBesitzer() throws Exception {
        for (String base : new String[] { "/api/configs", "/api/gallery", "/api/deckfinder" }) {
            long id = create(base, alice);
            assertTrue(listed(base, id, alice), base);
            assertFalse(listed(base, id, bob), base);
            assertFalse(listed(base, id, null), base);
            assertEquals("true", mvc.perform(get(base + "/" + id + "/check").cookie(alice)).andReturn().getResponse().getContentAsString());
            assertEquals("false", mvc.perform(get(base + "/" + id + "/check").cookie(bob)).andReturn().getResponse().getContentAsString());
            assertEquals("false", mvc.perform(get(base + "/" + id + "/check")).andReturn().getResponse().getContentAsString());

            // fremde Configs: nicht bearbeiten, nicht veröffentlichen, nicht kopieren, nicht löschen
            assertEquals("", postJson(base + "/edit", "{\"id\":" + id + ",\"name\":\"bob\"}", bob));
            assertEquals("", postJson(base + "/publish", "{\"id\":" + id + ",\"published\":true}", bob));
            assertEquals("", postJson(base + "/copy", "{\"id\":" + id + "}", bob));
            postJson(base + "/delete", "{\"id\":" + id + "}", bob);
            assertTrue(listed(base, id, alice), base);
            assertFalse(listed(base, id, null), base);

            // Veröffentlichen geht nur über publish, nicht über edit
            assertEquals(String.valueOf(id), postJson(base + "/edit", "{\"id\":" + id + ",\"name\":\"alice\",\"published\":true}", alice));
            assertFalse(listed(base, id, null), base);
            assertEquals(String.valueOf(id), postJson(base + "/publish", "{\"id\":" + id + ",\"published\":true}", alice));
            assertTrue(listed(base, id, null), base);
            assertTrue(listed(base, id, bob), base);
        }
    }

    @Test
    void kontoLoeschenEntferntUnveroeffentlichte() throws Exception {
        long kept = create("/api/configs", alice);
        long dropped = create("/api/configs", alice);
        long gallery = create("/api/gallery", alice);
        long deckfinder = create("/api/deckfinder", alice);
        postJson("/api/configs/publish", "{\"id\":" + kept + ",\"published\":true}", alice);
        mvc.perform(post("/api/configs/" + kept + "/rate").cookie(bob).contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":4,\"action\":\"quality\"}")).andExpect(status().isOk());

        mvc.perform(delete("/api/auth/account").cookie(alice)).andExpect(status().isNoContent());

        assertNull(jdbc.queryForObject("SELECT user_id FROM proxymaster.api_config WHERE id = ?", Long.class, kept));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM proxymaster.api_config WHERE id = ?", Integer.class, dropped));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM proxymaster.gallery_config WHERE id = ?", Integer.class, gallery));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM proxymaster.deck_finder_config WHERE id = ?", Integer.class, deckfinder));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM proxymaster.api_config WHERE user_id = ?", Integer.class, aliceId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM proxymaster.ratings WHERE api_config_id = ?", Integer.class, kept));
    }

    @Test
    void einRatingProKonto() throws Exception {
        long id = create("/api/configs", alice);
        postJson("/api/configs/publish", "{\"id\":" + id + ",\"published\":true}", alice);
        for (int score : new int[] { 2, 5 }) {
            mvc.perform(post("/api/configs/" + id + "/rate").cookie(bob).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"score\":" + score + ",\"action\":\"quality\"}")).andExpect(status().isOk());
        }
        mvc.perform(post("/api/configs/" + id + "/rate").cookie(alice).contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":4,\"action\":\"quality\"}")).andExpect(status().isOk());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM proxymaster.ratings WHERE api_config_id = ?", Integer.class, id));
        mvc.perform(get("/api/ratings/get").cookie(bob)).andExpect(jsonPath("$[0].scoreQuality").value(5.0));
        mvc.perform(get("/api/ratings/get")).andExpect(jsonPath("$.length()").value(0));
        // Durchschnitt 4,5 statt Ganzzahl-Division
        String list = mvc.perform(get("/api/configs")).andReturn().getResponse().getContentAsString();
        assertTrue(list.contains("\"ratingValue\":4.5"), list);
    }

    @Test
    void fremdeUnveroeffentlichteNichtBewerten() throws Exception {
        long id = create("/api/configs", alice);
        mvc.perform(post("/api/configs/" + id + "/rate").cookie(bob).contentType(MediaType.APPLICATION_JSON)
                .content("{\"score\":1,\"action\":\"quality\"}")).andExpect(status().isNotFound());
    }
}
