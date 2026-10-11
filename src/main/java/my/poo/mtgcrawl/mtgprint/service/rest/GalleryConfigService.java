package my.poo.mtgcrawl.mtgprint.service.rest;


import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.repository.GalleryConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.net.HostBlocklist;

@Service
@Transactional
@AllArgsConstructor
public class GalleryConfigService {
    private GalleryConfigRepository galleryConfigRepository;
    private GalleryConfigMapper mapper;
    private HintTranslationService hintTranslationService;
    private HostBlocklist blocklist;

    /** Zeigt eine URL der Config auf einen gesperrten Betreiber? Solche Configs werden nicht ausgeliefert. */
    public boolean isBlocked(GalleryConfig config) {
        return blocklist.blockedHostIn(config.getBaseUrl(), config.getOfficialWebsite()).isPresent();
    }

    /** Besitzt das Konto die Config? Nicht angemeldet (userId null) besitzt niemand etwas. */
    public static boolean isOwner(GalleryConfig config, Long userId) {
        return config != null && userId != null && userId.equals(config.getUserId());
    }

    /** Veröffentlicht oder eigene: darf benutzt und kopiert werden. */
    public static boolean isVisible(GalleryConfig config, Long userId) {
        return config != null && (config.isPublished() || isOwner(config, userId));
    }

    public Long createConfig(long userId, String ip) {
        return galleryConfigRepository.insertGalleryConfig("new gallery", ApiCategory.MISC.name(), ip, userId);
    }

    public Long copyConfig(GalleryConfigDto request, long userId, String ip) {
        GalleryConfig source = galleryConfigRepository.findById(request.getId()).orElse(null);
        if (!isVisible(source, userId)) {
            return null;
        }
        blocklist.checkConfigUrls(source.getBaseUrl(), source.getOfficialWebsite());
        List<Long> idList = galleryConfigRepository.copyGalleryConfig(source.getId(), ip, userId);
        return idList != null && idList.size() > 0 ? idList.get(0) : null; 
    }

    public void deleteConfig(GalleryConfigDto request, long userId) {
        GalleryConfig old = galleryConfigRepository.findById(request.getId()).orElse(null);
        if (isOwner(old, userId)) {
            deleteByIds(List.of(old.getId()));
        }
    }

    /** Beim Löschen eines Kontos: unveröffentlichte Configs weg, veröffentlichte bleiben ohne Besitzer. */
    public int deleteUnpublishedOf(long userId) {
        return deleteByIds(galleryConfigRepository.findUnpublishedIdsOf(userId));
    }

    private int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        galleryConfigRepository.deleteParametersOf(ids);
        return galleryConfigRepository.deleteByIds(ids);
    }

    public Long editConfig(GalleryConfigDto request, long userId) {
        GalleryConfig old = galleryConfigRepository.findById(request.getId()).orElse(null);
        if (!isOwner(old, userId)) {
            return null;
        }
        GalleryConfig config = mapper.toEntity(request);
        blocklist.checkConfigUrls(config.getBaseUrl(), config.getOfficialWebsite());
        config.setVisitorIdentifier(old.getVisitorIdentifier());
        config.setUserId(old.getUserId());
        // Veröffentlichen geht nur über publish
        config.setPublished(old.isPublished());
        applyHint(config, old, request);
        return galleryConfigRepository.save(config).getId();
    }
    
    static final int MAX_HINT_LENGTH = 1000;

    /**
     * Hinweis säubern und Übersetzungen setzen: Ändert sich der Hinweis, wird neu übersetzt (DeepL);
     * bleibt er gleich, gelten die mitgeschickten Übersetzungen, sonst die gespeicherten.
     */
    private void applyHint(GalleryConfig config, GalleryConfig old, GalleryConfigDto request) {
        String hint = cleanText(request.getHint());
        config.setHint(hint);
        if (hint == null) {
            config.setHintTranslations(null);
            return;
        }
        Map<String, String> translations;
        if (!Objects.equals(hint, old.getHint())) {
            translations = hintTranslationService.translate(hint);
        } else if (request.getHintTranslations() != null) {
            translations = request.getHintTranslations();
        } else {
            translations = mapper.translationsFromJson(old.getHintTranslations());
        }
        config.setHintTranslations(mapper.translationsToJson(cleanTranslations(translations)));
    }

    private static String cleanText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() > MAX_HINT_LENGTH ? trimmed.substring(0, MAX_HINT_LENGTH) : trimmed;
    }

    /** Nur bekannte UI-Sprachen und nicht leere, begrenzte Texte. */
    private static Map<String, String> cleanTranslations(Map<String, String> translations) {
        Map<String, String> clean = new java.util.TreeMap<>();
        if (translations != null) {
            translations.forEach((lang, text) -> {
                String value = cleanText(text);
                if (lang != null && HintTranslationService.TARGETS.containsKey(lang) && value != null) {
                    clean.put(lang, value);
                }
            });
        }
        return clean;
    }

    public Long publishConfig(GalleryConfigDto request, long userId) {
        GalleryConfig old = galleryConfigRepository.findById(request.getId()).orElse(null);
        if (!isOwner(old, userId)) {
            return null;
        }
        if (request.isPublished()) {
            blocklist.checkConfigUrls(old.getBaseUrl(), old.getOfficialWebsite());
        }
        old.setPublished(request.isPublished());
        galleryConfigRepository.save(old);
        return old.getId();
    }
}
