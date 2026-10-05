package my.poo.mtgcrawl.mtgprint.service.rest;


import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigConnectedCard;
import my.poo.mtgcrawl.mtgprint.repository.ApiConfigConnectedCardRepository;
import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.ApiConfigMapper;

@Service
@Transactional
@AllArgsConstructor
public class ApiConfigService {
    private ApiConfigRepository apiConfigRepository;
    private ApiConfigConnectedCardRepository connectedCardRepository;
    private ApiConfigMapper mapper;
    
    public Long createApiConfig(VisitorIdentifier visitorIdentifier) {
        return apiConfigRepository.insertApiConfig("new config", ApiCategory.MISC.name(),visitorIdentifier.getIpAddress(),visitorIdentifier.getBrowserFingerprint());
    }

    public Long copyConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        List<Long> idList = apiConfigRepository.copyApiConfig(request.getId(), visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
        Long newId = idList != null && idList.size() > 0 ? idList.get(0) : null;
        if (newId != null) {
            apiConfigRepository.copyConnectedCards(request.getId(), newId);
        }
        return newId;
    }

    public void deleteConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            connectedCardRepository.deleteByConfigIds(List.of(old.getId()));
            apiConfigRepository.delete(old);
        }
    }

    // Frontend meldet die Configs einer Suche, damit sie nicht als unbenutzt gelöscht werden
    public void markUsed(List<Long> ids) {
        if (ids == null) return;
        List<Long> valid = ids.stream().filter(Objects::nonNull).distinct().limit(200).toList();
        if (!valid.isEmpty()) {
            apiConfigRepository.markUsed(valid);
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
            config.setLastUsed(LocalDateTime.now());
            List<ApiConfigConnectedCard> connectedCards = config.getConnectedCards();
            config.setConnectedCards(null);
            ApiConfig saved = apiConfigRepository.save(config);
            saveConnectedCards(saved, connectedCards);
            return saved.getId();
        }
        return null;
    }
    
    // Listen werden komplett ersetzt: alte Zeilen löschen, neue in der übergebenen Reihenfolge anlegen
    private void saveConnectedCards(ApiConfig config, List<ApiConfigConnectedCard> connectedCards) {
        connectedCardRepository.deleteByConfigIds(List.of(config.getId()));
        if (connectedCards == null || connectedCards.isEmpty()) {
            return;
        }
        connectedCards.forEach(card -> card.setConfig(config));
        connectedCardRepository.saveAll(connectedCards);
    }

    public Long publishConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if(old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)){
            old.setPublished(request.getPublished());
            old.setLastUsed(LocalDateTime.now());
            apiConfigRepository.save(old);
            return old.getId();
        } else {
            return null;
        }
    }
}