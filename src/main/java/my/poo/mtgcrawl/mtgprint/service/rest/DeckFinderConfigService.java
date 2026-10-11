package my.poo.mtgcrawl.mtgprint.service.rest;

import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.DeckFinderConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.DeckFinderConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.repository.DeckFinderConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.DeckFinderConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.net.HostBlocklist;

@Service
@Transactional
@AllArgsConstructor
public class DeckFinderConfigService {
    private DeckFinderConfigRepository repository;
    private DeckFinderConfigMapper mapper;
    private HostBlocklist blocklist;

    /** Zeigt eine URL der Config auf einen gesperrten Betreiber? Solche Configs werden nicht ausgeliefert. */
    public boolean isBlocked(DeckFinderConfig config) {
        return blocklist.blockedHostIn(urlsOf(config)).isPresent();
    }

    private static String[] urlsOf(DeckFinderConfig config) {
        return new String[] { config.getFetchUrl(), config.getSearchUrl(), config.getOfficialWebsite() };
    }

    public List<DeckFinderConfigDto> findAllVisible(Long userId) {
        return repository.findAllVisible(userId).stream().filter(c -> !isBlocked(c)).map(mapper::toDTO).toList();
    }

    /** Config nur benutzen, wenn sie veröffentlicht ist oder dem Konto gehört und nicht gesperrt ist. */
    public DeckFinderConfig findVisible(Long id, Long userId) {
        DeckFinderConfig config = repository.findById(id).orElse(null);
        if (config == null || !(config.isPublished() || isOwner(config, userId)) || isBlocked(config)) {
            return null;
        }
        return config;
    }

    public Long createConfig(long userId, String ip) {
        return repository.insertDeckFinderConfig("new deckfinder", ApiCategory.MTG.name(), ip, userId);
    }

    public Long copyConfig(DeckFinderConfigDto request, long userId, String ip) {
        DeckFinderConfig source = repository.findById(request.getId()).orElse(null);
        if (source == null || !(source.isPublished() || isOwner(source, userId))) {
            return null;
        }
        blocklist.checkConfigUrls(urlsOf(source));
        List<Long> ids = repository.copyDeckFinderConfig(request.getId(), ip, userId);
        return ids != null && !ids.isEmpty() ? ids.get(0) : null;
    }

    public void deleteConfig(DeckFinderConfigDto request, long userId) {
        repository.findById(request.getId())
                .filter(old -> isOwner(old, userId))
                .ifPresent(repository::delete);
    }

    /** Beim Löschen eines Kontos: unveröffentlichte Configs weg, veröffentlichte bleiben ohne Besitzer. */
    public int deleteUnpublishedOf(long userId) {
        return repository.deleteUnpublishedOf(userId);
    }

    /** Nicht angemeldet (userId null) besitzt niemand etwas. */
    public boolean isOwner(DeckFinderConfig config, Long userId) {
        return userId != null && userId.equals(config.getUserId());
    }

    public Long editConfig(DeckFinderConfigDto request, long userId) {
        DeckFinderConfig old = repository.findById(request.getId()).orElse(null);
        if (old == null || !isOwner(old, userId)) {
            return null;
        }
        DeckFinderConfig config = mapper.toEntity(request);
        blocklist.checkConfigUrls(urlsOf(config));
        config.setVisitorIdentifier(old.getVisitorIdentifier());
        config.setUserId(old.getUserId());
        // Veröffentlichen geht nur über publish
        config.setPublished(old.isPublished());
        return repository.save(config).getId();
    }

    public Long publishConfig(DeckFinderConfigDto request, long userId) {
        DeckFinderConfig old = repository.findById(request.getId()).orElse(null);
        if (old == null || !isOwner(old, userId)) {
            return null;
        }
        if (request.isPublished()) {
            blocklist.checkConfigUrls(urlsOf(old));
        }
        old.setPublished(request.isPublished());
        return repository.save(old).getId();
    }
}
