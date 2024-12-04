package my.poo.mtgcrawl.mtgprint.service.rest;


import org.springframework.stereotype.Service;
import java.util.List;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
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
    
    public Long createConfig(VisitorIdentifier visitorIdentifier) {
        return galleryConfigRepository.insertGalleryConfig("new gallery", ApiCategory.MISC.name(),visitorIdentifier.getIpAddress(),visitorIdentifier.getBrowserFingerprint());
    }

    public Long copyConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        List<Long> idList = galleryConfigRepository.copyGalleryConfig(request.getId(), visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
        return idList != null && idList.size() > 0 ? idList.get(0) : null; 
    }

    public void deleteConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        GalleryConfig old = galleryConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            galleryConfigRepository.delete(old);
        }
    }

    public boolean checkVisitorIdentifier(GalleryConfig config, VisitorIdentifier visitorIdentifier) {
        return visitorIdentifier != null && visitorIdentifier.equals(config.getVisitorIdentifier());
    }

    public Long editConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        GalleryConfig old = galleryConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            GalleryConfig config = mapper.toEntity(request);
            config.setVisitorIdentifier(visitorIdentifier);
            return galleryConfigRepository.save(config).getId();
        }
        return null;
    }
    
    public Long publishConfig(GalleryConfigDto request, VisitorIdentifier visitorIdentifier) {
        GalleryConfig old = galleryConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            old.setPublished(request.isPublished());
            galleryConfigRepository.save(old);
            return old.getId();
        } else {
            return null;
        }
    }
}