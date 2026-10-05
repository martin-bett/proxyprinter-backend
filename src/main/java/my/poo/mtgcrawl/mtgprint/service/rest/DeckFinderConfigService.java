package my.poo.mtgcrawl.mtgprint.service.rest;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.DeckFinderConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.DeckFinderConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.repository.DeckFinderConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.DeckFinderConfigMapper;

@Service
@Transactional
@AllArgsConstructor
public class DeckFinderConfigService {
    private DeckFinderConfigRepository repository;
    private DeckFinderConfigMapper mapper;

    public List<DeckFinderConfigDto> findAllVisible(VisitorIdentifier visitorIdentifier) {
        return repository.findAllVisible(visitorIdentifier).stream().map(mapper::toDTO).toList();
    }

    /** Config nur benutzen, wenn sie veröffentlicht ist oder dem Besucher gehört. */
    public DeckFinderConfig findVisible(Long id, VisitorIdentifier visitorIdentifier) {
        DeckFinderConfig config = repository.findById(id).orElse(null);
        if (config == null || !(config.isPublished() || isOwner(config, visitorIdentifier))) {
            return null;
        }
        return config;
    }

    public Long createConfig(VisitorIdentifier visitorIdentifier) {
        return repository.insertDeckFinderConfig("new deckfinder", ApiCategory.MTG.name(),
                visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
    }

    public Long copyConfig(DeckFinderConfigDto request, VisitorIdentifier visitorIdentifier) {
        List<Long> ids = repository.copyDeckFinderConfig(request.getId(), visitorIdentifier.getIpAddress(),
                visitorIdentifier.getBrowserFingerprint());
        return ids != null && !ids.isEmpty() ? ids.get(0) : null;
    }

    public void deleteConfig(DeckFinderConfigDto request, VisitorIdentifier visitorIdentifier) {
        repository.findById(request.getId())
                .filter(old -> isOwner(old, visitorIdentifier))
                .ifPresent(repository::delete);
    }

    public boolean isOwner(DeckFinderConfig config, VisitorIdentifier visitorIdentifier) {
        return visitorIdentifier != null && config.getVisitorIdentifier() != null
                && config.getVisitorIdentifier().equals(visitorIdentifier);
    }

    public Long editConfig(DeckFinderConfigDto request, VisitorIdentifier visitorIdentifier) {
        DeckFinderConfig old = repository.findById(request.getId()).orElse(null);
        if (old == null || !isOwner(old, visitorIdentifier)) {
            return null;
        }
        DeckFinderConfig config = mapper.toEntity(request);
        config.setVisitorIdentifier(old.getVisitorIdentifier());
        // Veröffentlichen geht nur über publish
        config.setPublished(old.isPublished());
        return repository.save(config).getId();
    }

    public Long publishConfig(DeckFinderConfigDto request, VisitorIdentifier visitorIdentifier) {
        DeckFinderConfig old = repository.findById(request.getId()).orElse(null);
        if (old == null || !isOwner(old, visitorIdentifier)) {
            return null;
        }
        old.setPublished(request.isPublished());
        return repository.save(old).getId();
    }
}
