package my.poo.mtgcrawl.mtgprint.resource;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.ApiConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.ApiConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;


/** API-Configs: benutzen geht ohne Konto (veröffentlichte), anlegen, kopieren, bearbeiten und bewerten nur mit Konto. */
@RestController
@RequestMapping("/api/configs")
@AllArgsConstructor
public class ApiConfigController{
    
    private final ApiConfigRepository apiConfigRepository;
    private final ApiConfigService apiConfigService;
    private final RatingService ratingService;
    private final CurrentUser currentUser;

    private final ApiConfigMapper mapper;
    
    @GetMapping
    public List<ApiConfigDto> getAllConfigs(HttpServletRequest httpRequest) {
        return apiConfigRepository.findAllVisible(currentUser.idOrNull(httpRequest))
            .stream()
            .filter(config -> !apiConfigService.isBlocked(config))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @GetMapping("/create")
    public ResponseEntity<Long> createConfig(HttpServletRequest httpRequest) {
        long userId = currentUser.require(httpRequest);
        return ResponseEntity.ok(apiConfigService.createApiConfig(userId, httpRequest.getRemoteAddr()));
    }   

    @PostMapping("/copy")
    public ResponseEntity<Long> copyConfig(
            @RequestBody ApiConfigDto request,
            HttpServletRequest httpRequest) {
        long userId = currentUser.require(httpRequest);
        return ResponseEntity.ok(apiConfigService.copyConfig(request, userId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteConfig(
            @RequestBody ApiConfigDto request,
            HttpServletRequest httpRequest) {
        apiConfigService.deleteConfig(request, currentUser.require(httpRequest));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/used")
    public ResponseEntity<?> markUsed(@RequestBody List<Long> configIds) {
        apiConfigService.markUsed(configIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/publish")
    public ResponseEntity<Long> publishConfig(
            @RequestBody ApiConfigDto request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(apiConfigService.publishConfig(request, currentUser.require(httpRequest)));
    }

    @PostMapping("/edit")
    public ResponseEntity<Long> editConfig(
            @RequestBody ApiConfigDto request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(apiConfigService.editApiConfig(request, currentUser.require(httpRequest)));
    }

    /** Darf der Besucher die Config bearbeiten? Ohne Konto immer false. */
    @GetMapping("/{configId}/check")
    public ResponseEntity<Boolean> checkOwner(
            @PathVariable ApiConfig configId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiConfigService.isOwner(configId, currentUser.idOrNull(httpRequest)));
    }
    
    @PostMapping("/{configId}/rate")
    public ResponseEntity<?> rateConfig(
            @PathVariable ApiConfig configId,
            @RequestBody RatingRequest request,
            HttpServletRequest httpRequest) {
        long userId = currentUser.require(httpRequest);
        if (!ApiConfigService.isVisible(configId, userId)) {
            return ResponseEntity.notFound().build();
        }
        ratingService.saveOrUpdateRating(userId, configId, request.getScore(), request.getComment(), request.getAction());
        return ResponseEntity.ok().build();
    }

    private ApiConfigDto toDTO(ApiConfig apiconfig1) {
        ApiConfigDto d = mapper.toDTO(apiconfig1);
        ratingService.addRatingsToConfigDto(d);
        return d;
    }
}
