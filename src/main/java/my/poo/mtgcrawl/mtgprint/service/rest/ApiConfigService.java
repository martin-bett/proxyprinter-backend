package my.poo.mtgcrawl.mtgprint.service.rest;


import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
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

    /** Besitzt das Konto die Config? Nicht angemeldet (userId null) besitzt niemand etwas. */
    public static boolean isOwner(ApiConfig config, Long userId) {
        return config != null && userId != null && userId.equals(config.getUserId());
    }

    /** Veröffentlicht oder eigene: darf benutzt und kopiert werden. */
    public static boolean isVisible(ApiConfig config, Long userId) {
        return config != null && (config.isPublished() || isOwner(config, userId));
    }

    // ip_address markiert weiterhin „von Benutzern angelegt“ (Standard-Configs haben keine, die SQL-Skripte unter sql/ verlassen sich darauf)
    public Long createApiConfig(long userId, String ip) {
        return apiConfigRepository.insertApiConfig("new config", ApiCategory.MISC.name(), ip, userId);
    }

    public Long copyConfig(ApiConfigDto request, long userId, String ip) {
        ApiConfig source = apiConfigRepository.findById(request.getId()).orElse(null);
        if (!isVisible(source, userId)) {
            return null;
        }
        List<Long> idList = apiConfigRepository.copyApiConfig(source.getId(), ip, userId);
        Long newId = idList != null && idList.size() > 0 ? idList.get(0) : null;
        if (newId != null) {
            apiConfigRepository.copyConnectedCards(source.getId(), newId);
        }
        return newId;
    }

    public void deleteConfig(ApiConfigDto request, long userId) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if (isOwner(old, userId)) {
            deleteByIds(List.of(old.getId()));
        }
    }

    /** Beim Löschen eines Kontos: unveröffentlichte Configs weg, veröffentlichte bleiben ohne Besitzer (FK ON DELETE SET NULL). */
    public int deleteUnpublishedOf(long userId) {
        return deleteByIds(apiConfigRepository.findUnpublishedIdsOf(userId));
    }

    private int deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        apiConfigRepository.deleteParametersOf(ids);
        apiConfigRepository.deleteConnectedCardsOf(ids);
        apiConfigRepository.deleteRatingsOf(ids);
        return apiConfigRepository.deleteByIds(ids);
    }

    // Frontend meldet die Configs einer Suche, damit sie nicht als unbenutzt gelöscht werden
    public void markUsed(List<Long> ids) {
        if (ids == null) return;
        List<Long> valid = ids.stream().filter(Objects::nonNull).distinct().limit(200).toList();
        if (!valid.isEmpty()) {
            apiConfigRepository.markUsed(valid);
        }
    }

    public Long editApiConfig(ApiConfigDto request, long userId) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if (!isOwner(old, userId)) {
            return null;
        }
        ApiConfig config = mapper.toEntity(request);
        config.setVisitorIdentifier(old.getVisitorIdentifier());
        config.setUserId(old.getUserId());
        // Veröffentlichen geht nur über publish
        config.setPublished(old.isPublished());
        config.setLastUsed(LocalDateTime.now());
        List<ApiConfigConnectedCard> connectedCards = config.getConnectedCards();
        config.setConnectedCards(null);
        ApiConfig saved = apiConfigRepository.save(config);
        saveConnectedCards(saved, connectedCards);
        return saved.getId();
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

    public Long publishConfig(ApiConfigDto request, long userId) {
        ApiConfig old = apiConfigRepository.findById(request.getId()).orElse(null);
        if (!isOwner(old, userId)) {
            return null;
        }
        old.setPublished(Boolean.TRUE.equals(request.getPublished()));
        old.setLastUsed(LocalDateTime.now());
        apiConfigRepository.save(old);
        return old.getId();
    }
}
