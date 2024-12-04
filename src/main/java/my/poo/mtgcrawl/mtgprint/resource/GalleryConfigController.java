package my.poo.mtgcrawl.mtgprint.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
