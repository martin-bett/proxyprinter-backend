package my.poo.mtgcrawl.mtgprint.service.rest;


import org.springframework.stereotype.Service;
import java.util.List;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.repository.GalleryConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;

@Service
@Transactional
@AllArgsConstructor
public class GalleryConfigService {
    private GalleryConfigRepository galleryConfigRepository;
    private GalleryConfigMapper mapper;

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
        config.setVisitorIdentifier(old.getVisitorIdentifier());
        config.setUserId(old.getUserId());
        // Veröffentlichen geht nur über publish
        config.setPublished(old.isPublished());
        return galleryConfigRepository.save(config).getId();
    }
    
    public Long publishConfig(GalleryConfigDto request, long userId) {
        GalleryConfig old = galleryConfigRepository.findById(request.getId()).orElse(null);
        if (!isOwner(old, userId)) {
            return null;
        }
        old.setPublished(request.isPublished());
        galleryConfigRepository.save(old);
        return old.getId();
    }
}
