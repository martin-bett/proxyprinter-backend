package my.poo.mtgcrawl.mtgprint.service.rest;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;
import my.poo.mtgcrawl.mtgprint.repository.RatingRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.ApiConfigMapper;

@Service
@Transactional
@AllArgsConstructor
public class ApiConfigService {
    private ApiConfigRepository apiConfigRepository;
    private RatingRepository ratingRepository;
    private ApiConfigMapper mapper;
    
    public Long createApiConfig(VisitorIdentifier visitorIdentifier) {
        return apiConfigRepository.insertApiConfig("new config", ApiCategory.MISC.name(),visitorIdentifier.getIpAddress(),visitorIdentifier.getBrowserFingerprint());
    }

    public Long copyConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        List<Long> idList = apiConfigRepository.copyApiConfig(request.getId(), visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
        return idList != null && idList.size() > 0 ? idList.get(0) : null; 
    }

    public void deleteConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            apiConfigRepository.delete(old);
        }
    }

    public boolean checkVisitorIdentifier(ApiConfig config, VisitorIdentifier visitorIdentifier) {
        return visitorIdentifier != null && visitorIdentifier.equals(config.getVisitorIdentifier());
    }

    public Long editApiConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            ApiConfig config = mapper.toEntity(request);
            config.setVisitorIdentifier(visitorIdentifier);
            return apiConfigRepository.save(config).getId();
        }
        return null;
    }
    
    public void updateAverageRating(ApiConfig config) {
        // ApiConfig config = apiConfigRepository.findById(configId)
        //     .orElseThrow(() -> new RuntimeException("Config not found"));
            
        // List<Rating> ratings = ratingRepository.findByApiConfigId(configId);
        // double average = ratings.stream()
        //     .mapToInt(Rating::getScore)
        //     .average()
        //     .orDefault(0.0);
            
        // config.setAverageRating(average);
        // config.setRatingCount(ratings.size());
        apiConfigRepository.save(config);
    }

    public Long publishConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            old.setPublished(request.getPublished());
            apiConfigRepository.save(old);
            return old.getId();
        } else {
            return null;
        }
    }
}