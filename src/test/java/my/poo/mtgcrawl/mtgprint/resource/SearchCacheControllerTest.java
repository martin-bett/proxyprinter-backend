package my.poo.mtgcrawl.mtgprint.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Constructor;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import my.poo.mtgcrawl.mtgprint.service.searchcache.SearchResultCacheService;

class SearchCacheControllerTest {

    @TempDir
    Path dir;

    private MockMvc mvc;

    @BeforeEach
    void setUp() throws Exception {
        Constructor<SearchResultCacheService> ctor = SearchResultCacheService.class.getDeclaredConstructor(
                ObjectMapper.class, boolean.class, Path.class, int.class, int.class);
        ctor.setAccessible(true);
        SearchResultCacheService cache = ctor.newInstance(new ObjectMapper(), true, dir, 7, 2048);
        mvc = MockMvcBuilders.standaloneSetup(new SearchCacheController(cache)).build();
    }

    @Test
    void storeThenLookup() throws Exception {
        mvc.perform(put("/api/search-cache").contentType(MediaType.APPLICATION_JSON).content("""
                {"entries":[{"config":"7","configHash":"abc","term":"Lightning Bolt",
                  "result":{"results":[{"name":"Lightning Bolt","apiName":"scryfall","imageUrl":"https://x/1.png"}],"rawResponse":"{}"}}]}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stored").value(1));

        mvc.perform(post("/api/search-cache/lookup").contentType(MediaType.APPLICATION_JSON).content("""
                {"entries":[{"config":"7","configHash":"abc","term":"lightning bolt"},{"config":"7","configHash":"abc","term":"Shock"}]}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hits.length()").value(1))
                .andExpect(jsonPath("$.hits[0].term").value("lightning bolt"))
                .andExpect(jsonPath("$.hits[0].result.results[0].name").value("Lightning Bolt"));

        mvc.perform(get("/api/search-cache/info"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.retentionDays").value(7));
    }
}
