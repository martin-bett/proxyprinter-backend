/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.rest;

import jakarta.transaction.Transactional;
import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.ApiConfigMapper;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ApiConfigService {
    private ApiConfigRepository apiConfigRepository;
    private ApiConfigMapper mapper;

    public Long createApiConfig(VisitorIdentifier visitorIdentifier) {
        return this.apiConfigRepository.insertApiConfig("new config", ApiCategory.MISC.name(), visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
    }

    public Long copyConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        List<Long> idList = this.apiConfigRepository.copyApiConfig(request.getId(), visitorIdentifier.getIpAddress(), visitorIdentifier.getBrowserFingerprint());
        return idList != null && idList.size() > 0 ? idList.get(0) : null;
    }

    public void deleteConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = this.apiConfigRepository.findById(request.getId()).orElse(null);
        if (old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)) {
            this.apiConfigRepository.delete(old);
        }
    }

    public boolean checkVisitorIdentifier(ApiConfig config, VisitorIdentifier visitorIdentifier) {
        return visitorIdentifier != null && visitorIdentifier.equals(config.getVisitorIdentifier());
    }

    public Long editApiConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = this.apiConfigRepository.findById(request.getId()).orElse(null);
        if (old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)) {
            ApiConfig config = this.mapper.toEntity(request);
            config.setVisitorIdentifier(visitorIdentifier);
            return this.apiConfigRepository.save(config).getId();
        }
        return null;
    }

    public Long publishConfig(ApiConfigDto request, VisitorIdentifier visitorIdentifier) {
        ApiConfig old = this.apiConfigRepository.findById(request.getId()).orElse(null);
        if (old != null && old.getVisitorIdentifier() != null && old.getVisitorIdentifier().equals(visitorIdentifier)) {
            old.setPublished(request.getPublished());
            this.apiConfigRepository.save(old);
            return old.getId();
        }
        return null;
    }

    public ApiConfigService(ApiConfigRepository apiConfigRepository, ApiConfigMapper mapper) {
        this.apiConfigRepository = apiConfigRepository;
        this.mapper = mapper;
    }
}

