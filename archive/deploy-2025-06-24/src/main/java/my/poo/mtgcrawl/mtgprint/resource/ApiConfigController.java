/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 */
package my.poo.mtgcrawl.mtgprint.resource;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.RequestUtils;
import my.poo.mtgcrawl.mtgprint.service.mapper.ApiConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.ApiConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/configs"})
public class ApiConfigController {
    private final ApiConfigRepository apiConfigRepository;
    private final ApiConfigService apiConfigService;
    private final RatingService ratingService;
    private final ApiConfigMapper mapper;

    @GetMapping
    public List<ApiConfigDto> getAllConfigs(HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = RequestUtils.extractVisitorIdentifier(httpRequest);
        return this.apiConfigRepository.findAllVisible(visitorId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping(value={"/create"})
    public ResponseEntity<Long> createConfig(HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.apiConfigService.createApiConfig(visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value={"/copy"})
    public ResponseEntity<Long> copyConfig(@RequestBody ApiConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.apiConfigService.copyConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value={"/delete"})
    public ResponseEntity<?> deleteConfig(@RequestBody ApiConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        this.apiConfigService.deleteConfig(request, visitorIdentifier);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value={"/publish"})
    public ResponseEntity<Long> publishConfig(@RequestBody ApiConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.apiConfigService.publishConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value={"/edit"})
    public ResponseEntity<Long> editConfig(@RequestBody ApiConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.apiConfigService.editApiConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value={"/{configId}/check"})
    public ResponseEntity<Boolean> createConfig(@PathVariable ApiConfig configId, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        boolean res = this.apiConfigService.checkVisitorIdentifier(configId, visitorIdentifier);
        return ResponseEntity.ok(res);
    }

    @PostMapping(value={"/{configId}/rate"})
    public ResponseEntity<?> rateConfig(@PathVariable ApiConfig configId, @RequestBody RatingRequest request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = RequestUtils.extractVisitorIdentifier(httpRequest);
        this.ratingService.saveOrUpdateRating(visitorId, configId, request.getScore(), request.getComment(), request.getAction());
        return ResponseEntity.ok().build();
    }

    private ApiConfigDto toDTO(ApiConfig apiconfig1) {
        ApiConfigDto d = this.mapper.toDTO(apiconfig1);
        this.ratingService.addRatingsToConfigDto(d);
        return d;
    }

    public ApiConfigController(ApiConfigRepository apiConfigRepository, ApiConfigService apiConfigService, RatingService ratingService, ApiConfigMapper mapper) {
        this.apiConfigRepository = apiConfigRepository;
        this.apiConfigService = apiConfigService;
        this.ratingService = ratingService;
        this.mapper = mapper;
    }
}

