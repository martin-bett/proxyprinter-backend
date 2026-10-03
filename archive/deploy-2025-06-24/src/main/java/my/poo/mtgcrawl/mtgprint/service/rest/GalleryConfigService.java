/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.rest;

import jakarta.transaction.Transactional;
import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.repository.GalleryConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class GalleryConfigService {
    private GalleryConfigRepository galleryConfigRepository;
    private GalleryConfigMapper mapper;

    public Long createConfig(VisitorIdentifier visitorIdentifier) {
        return this.galleryConfigRepository.insertGalleryConfig("new gallery", ApiCategory.MISC.name(), visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
    }

    public Long copyConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        List<Long> idList = this.galleryConfigRepository.copyGalleryConfig(request.getId(), visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
        return idList != null && idList.size() > 0 ? idList.get(0) : null;
    }

    public void deleteConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        GalleryConfig old = this.galleryConfigRepository.findById(request.getId()).orElse(null);
        if (old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)) {
            this.galleryConfigRepository.delete(old);
        }
    }

    public boolean checkVisitorIdentifier(GalleryConfig config, VisitorIdentifier visitorIdentifier) {
        return visitorIdentifier != null && visitorIdentifier.equals(config.getVisitorIdentifier());
    }

    public Long editConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        GalleryConfig old = this.galleryConfigRepository.findById(request.getId()).orElse(null);
        if (old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)) {
            GalleryConfig config = this.mapper.toEntity(request);
            config.setVisitorIdentifier(visitorIdentifier);
            return this.galleryConfigRepository.save(config).getId();
        }
        return null;
    }

    public Long publishConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        GalleryConfig old = this.galleryConfigRepository.findById(request.getId()).orElse(null);
        if (old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)) {
            old.setPublished(request.isPublished());
            this.galleryConfigRepository.save(old);
            return old.getId();
        }
        return null;
    }

    public GalleryConfigService(GalleryConfigRepository galleryConfigRepository, GalleryConfigMapper mapper) {
        this.galleryConfigRepository = galleryConfigRepository;
        this.mapper = mapper;
    }
}

