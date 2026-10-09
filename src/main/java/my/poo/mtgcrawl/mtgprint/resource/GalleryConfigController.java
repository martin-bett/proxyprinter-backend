package my.poo.mtgcrawl.mtgprint.resource;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.repository.GalleryConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.gallery.GalleryFetchService;
import my.poo.mtgcrawl.mtgprint.service.gallery.GallerySourceException;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.GalleryConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;

@RestController
@RequestMapping("/api/gallery")
@AllArgsConstructor
public class GalleryConfigController {

    private final GalleryConfigRepository galleryConfigRepository;
    private final GalleryConfigService galleryConfigService;
    private final RatingService ratingService;
    private final GalleryFetchService galleryFetchService;

    private final GalleryConfigMapper mapper;
    @GetMapping
    public List<GalleryConfigDto> getAllConfigs(HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = extractVisitorIdentifier(httpRequest);
        return galleryConfigRepository.findAllVisible(visitorId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    @GetMapping("/create")
    public ResponseEntity<Long> createConfig(
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = galleryConfigService.createConfig(visitorIdentifier);
        return ResponseEntity.ok(result);
    }   

    @PostMapping("/copy")
    public ResponseEntity<Long> copyConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = galleryConfigService.copyConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        galleryConfigService.deleteConfig(request, visitorIdentifier);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/publish")
    public ResponseEntity<Long> publishConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = galleryConfigService.publishConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/edit")
    public ResponseEntity<Long> editConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        Long result = galleryConfigService.editConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{configId}/check")
    public ResponseEntity<Boolean> createConfig(
            @PathVariable GalleryConfig configId,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        boolean res = galleryConfigService.checkVisitorIdentifier(configId, visitorIdentifier);
        return ResponseEntity.ok(res);
    }
    
    /**
     * Führt die Config serverseitig aus (Link-Pattern, Zugang) und liefert die Rohantwort der Quelle;
     * die Felder liest das Frontend. Nur für veröffentlichte oder eigene Configs.
     */
    @GetMapping("/{configId}/fetch")
    public ResponseEntity<String> fetch(
            @PathVariable GalleryConfig configId,
            @RequestParam(value = "input", required = false) String input,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = extractVisitorIdentifier(httpRequest);
        if (!configId.isPublished() && !galleryConfigService.checkVisitorIdentifier(configId, visitorIdentifier)) {
            throw new GallerySourceException(HttpStatus.NOT_FOUND, "gallery.configNotFound", "Gallery config not found.", Map.of());
        }
        GalleryFetchService.GalleryResponse response = galleryFetchService.fetch(configId, input);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(response.contentType())).body(response.body());
    }

    @ExceptionHandler(GallerySourceException.class)
    public ResponseEntity<Map<String, Object>> handle(GallerySourceException e) {
        return ResponseEntity.status(e.getStatus()).body(e.toBody());
    }

    @PostMapping("/{configId}/rate")
    public ResponseEntity<?> rateConfig(
            @PathVariable GalleryConfig configId,
            @RequestBody RatingRequest request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = extractVisitorIdentifier(httpRequest);
        // ratingService.saveOrUpdateRating(visitorId, configId, request.getScore(), request.getComment(), request.getAction());
        return ResponseEntity.ok().build();
    }

    private VisitorIdentifier extractVisitorIdentifier(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String fingerprint = request.getHeader("X-Fingerprint");
        return new VisitorIdentifier(ip, fingerprint);
    }

    private GalleryConfigDto toDTO(GalleryConfig apiconfig1) {
        GalleryConfigDto d = mapper.toDTO(apiconfig1);
        // ratingService.addRatingsToConfigDto(d);
        return d;
    }
}
