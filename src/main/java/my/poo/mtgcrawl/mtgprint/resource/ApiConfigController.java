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
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.repository.ApiConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.mapper.ApiConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.ApiConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;


@RestController
@RequestMapping("/api/configs")
@AllArgsConstructor
public class ApiConfigController{
    
    private final ApiConfigRepository apiConfigRepository;
    private final ApiConfigService apiConfigService;
    private final RatingService ratingService;

    private final ApiConfigMapper mapper;
    
    @GetMapping
    public List<ApiConfigDto> getAllConfigs(HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = extractVisitorIdentifier(httpRequest);
        return apiConfigRepository.findAllVisible(visitorId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @GetMapping("/create")
    public ResponseEntity<Long> createConfig(
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = apiConfigService.createApiConfig(visitorIdentifier);
        return ResponseEntity.ok(result);
    }   

    @PostMapping("/copy")
    public ResponseEntity<Long> copyConfig(
            @RequestBody ApiConfigDto request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = apiConfigService.copyConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteConfig(
            @RequestBody ApiConfigDto request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        apiConfigService.deleteConfig(request, visitorIdentifier);
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
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = apiConfigService.publishConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/edit")
    public ResponseEntity<Long> editConfig(
            @RequestBody ApiConfigDto request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = apiConfigService.editApiConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{configId}/check")
    public ResponseEntity<Boolean> createConfig(
            @PathVariable ApiConfig configId,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        boolean res = apiConfigService.checkVisitorIdentifier(configId, visitorIdentifier);
        return ResponseEntity.ok(res);
    }
    
    @PostMapping("/{configId}/rate")
    public ResponseEntity<?> rateConfig(
            @PathVariable ApiConfig configId,
            @RequestBody RatingRequest request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = extractVisitorIdentifier(httpRequest);
        ratingService.saveOrUpdateRating(visitorId, configId, request.getScore(), request.getComment(), request.getAction());
        return ResponseEntity.ok().build();
    }

    private VisitorIdentifier extractVisitorIdentifier(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String fingerprint = request.getHeader("X-Fingerprint");
        return new VisitorIdentifier(ip, fingerprint);
    }

    // private boolean isValidVisitorIdentifier(VisitorIdentifier identifier) {
    //     return identifier.getIpAddress() != null && !identifier.getIpAddress().isEmpty() &&
    //            identifier.getBrowserFingerprint() != null && !identifier.getBrowserFingerprint().isEmpty();
    // }

    private ApiConfigDto toDTO(ApiConfig apiconfig1) {
        ApiConfigDto d = mapper.toDTO(apiconfig1);
        ratingService.addRatingsToConfigDto(d);
        return d;
    }
}