package my.poo.mtgcrawl.mtgprint.service.gallery;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.env.MapPropertySource;

/** Zugänge kommen per Umgebungsvariable und Properties zusammen in dieselbe Map. */
class GalleryCredentialsBindingTest {

    @Test
    void bindsHostFromPropertiesAndSecretsFromEnvironment() {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("props", Map.of("gallery.credentials.reddit.host", "reddit.com")));
        env.getPropertySources().addFirst(new SystemEnvironmentPropertySource("env",
                Map.of("GALLERY_CREDENTIALS_REDDIT_COOKIE", "session=abc", "GALLERY_CREDENTIALS_REDDIT_AUTHORIZATION", "")));

        GalleryCredentials bound = Binder.get(env).bind("gallery", GalleryCredentials.class).get();

        assertTrue(bound.exists("reddit"));
        Map<String, String> headers = bound.headersFor("reddit", "www.reddit.com");
        assertEquals("session=abc", headers.get("Cookie"));
        assertFalse(headers.containsKey("Authorization"), "leere Werte werden nicht gesendet");
        assertTrue(bound.headersFor("reddit", "example.com").isEmpty());
    }
}
