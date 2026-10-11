package my.poo.mtgcrawl.mtgprint.service.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.repository.GalleryConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.net.HostBlocklist;

class GalleryConfigServiceHintTest {

    private static final long OWNER = 7L;

    private GalleryConfigRepository repository;
    private HintTranslationService translator;
    private GalleryConfigService service;
    private GalleryConfig old;

    @BeforeEach
    void setUp() {
        repository = mock(GalleryConfigRepository.class);
        translator = mock(HintTranslationService.class);
        GalleryConfigMapper mapper = Mappers.getMapper(GalleryConfigMapper.class);
        service = new GalleryConfigService(repository, mapper, translator, HostBlocklist.NONE);
        old = new GalleryConfig();
        old.setId(1L);
        old.setUserId(OWNER);
        when(repository.findById(1L)).thenReturn(Optional.of(old));
        when(repository.save(any())).thenAnswer(i -> {
            GalleryConfig saved = i.getArgument(0);
            saved.setId(1L);
            return saved;
        });
    }

    private GalleryConfigDto request(String hint, Map<String, String> translations) {
        GalleryConfigDto dto = new GalleryConfigDto();
        dto.setId(1L);
        dto.setName("reddit");
        dto.setHint(hint);
        dto.setHintTranslations(translations);
        return dto;
    }

    private GalleryConfig saved() {
        ArgumentCaptor<GalleryConfig> captor = ArgumentCaptor.forClass(GalleryConfig.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void changedHintIsTranslatedAndStored() {
        when(translator.translate("Paste the link")).thenReturn(Map.of("de", "Link einfügen"));

        service.editConfig(request("  Paste the link  ", null), OWNER);

        GalleryConfig config = saved();
        assertEquals("Paste the link", config.getHint());
        assertEquals("{\"de\":\"Link einfügen\"}", config.getHintTranslations());
    }

    @Test
    void unchangedHintKeepsStoredTranslationsAndDoesNotCallTheService() {
        old.setHint("Paste the link");
        old.setHintTranslations("{\"de\":\"Link einfügen\"}");

        service.editConfig(request("Paste the link", null), OWNER);

        assertEquals("{\"de\":\"Link einfügen\"}", saved().getHintTranslations());
        verify(translator, never()).translate(anyString());
    }

    @Test
    void unchangedHintTakesManuallyEditedTranslationsAndDropsUnknownLanguages() {
        old.setHint("Paste the link");
        old.setHintTranslations("{\"de\":\"alt\"}");

        service.editConfig(request("Paste the link", Map.of("de", "Link hier einfügen", "xx", "nope", "fr", " ")), OWNER);

        assertEquals("{\"de\":\"Link hier einfügen\"}", saved().getHintTranslations());
        verify(translator, never()).translate(anyString());
    }

    @Test
    void removedHintRemovesTranslations() {
        old.setHint("Paste the link");
        old.setHintTranslations("{\"de\":\"Link einfügen\"}");

        service.editConfig(request("  ", Map.of("de", "x")), OWNER);

        GalleryConfig config = saved();
        assertNull(config.getHint());
        assertNull(config.getHintTranslations());
    }

    @Test
    void failedTranslationStillSavesTheHint() {
        when(translator.translate(anyString())).thenReturn(Map.of());

        service.editConfig(request("Paste the link", null), OWNER);

        GalleryConfig config = saved();
        assertEquals("Paste the link", config.getHint());
        assertNull(config.getHintTranslations());
    }

    @Test
    void overlongHintIsCut() {
        when(translator.translate(anyString())).thenReturn(Map.of());

        service.editConfig(request("x".repeat(1500), null), OWNER);

        assertEquals(GalleryConfigService.MAX_HINT_LENGTH, saved().getHint().length());
    }
}
