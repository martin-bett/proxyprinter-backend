package my.poo.mtgcrawl.mtgprint.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;

class JsonConfigTest {

    private ObjectMapper mapper() {
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        new JsonConfig().emptyStringAsNullForMaps().customize(builder);
        return builder.build();
    }

    @Test
    void emptyStringForMapFieldIsNull() throws Exception {
        GalleryConfigDto dto = mapper().readValue("{\"id\":4,\"hint\":\"\",\"hintTranslations\":\"\"}", GalleryConfigDto.class);

        assertNull(dto.getHintTranslations());
        assertEquals("", dto.getHint(), "Textfelder bleiben unverändert");
    }

    @Test
    void realMapIsStillRead() throws Exception {
        GalleryConfigDto dto = mapper().readValue("{\"hintTranslations\":{\"de\":\"Hallo\"}}", GalleryConfigDto.class);

        assertEquals("Hallo", dto.getHintTranslations().get("de"));
    }
}
