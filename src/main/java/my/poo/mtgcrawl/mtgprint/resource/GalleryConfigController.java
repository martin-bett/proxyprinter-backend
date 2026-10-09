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
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.repository.GalleryConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.gallery.GalleryFetchService;
import my.poo.mtgcrawl.mtgprint.service.gallery.GallerySourceException;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.GalleryConfigService;

/** Galerie-Configs: benutzen geht ohne Konto (veröffentlichte), anlegen, kopieren und bearbeiten nur mit Konto. */
@RestController
@RequestMapping("/api/gallery")
@AllArgsConstructor
public class GalleryConfigController {

    private final GalleryConfigRepository galleryConfigRepository;
    private final GalleryConfigService galleryConfigService;
    private final GalleryFetchService galleryFetchService;
    private final CurrentUser currentUser;

    private final GalleryConfigMapper mapper;
    @GetMapping
    public List<GalleryConfigDto> getAllConfigs(HttpServletRequest httpRequest) {
        return galleryConfigRepository.findAllVisible(currentUser.idOrNull(httpRequest))
            .stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
    }
    
    @GetMapping("/create")
    public ResponseEntity<Long> createConfig(HttpServletRequest httpRequest) {
        long userId = currentUser.require(httpRequest);
        return ResponseEntity.ok(galleryConfigService.createConfig(userId, httpRequest.getRemoteAddr()));
    }   

    @PostMapping("/copy")
    public ResponseEntity<Long> copyConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        long userId = currentUser.require(httpRequest);
        return ResponseEntity.ok(galleryConfigService.copyConfig(request, userId, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        galleryConfigService.deleteConfig(request, currentUser.require(httpRequest));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/publish")
    public ResponseEntity<Long> publishConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(galleryConfigService.publishConfig(request, currentUser.require(httpRequest)));
    }

    @PostMapping("/edit")
    public ResponseEntity<Long> editConfig(
            @RequestBody GalleryConfigDto request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(galleryConfigService.editConfig(request, currentUser.require(httpRequest)));
    }

    /** Darf der Besucher die Config bearbeiten? Ohne Konto immer false. */
    @GetMapping("/{configId}/check")
    public ResponseEntity<Boolean> checkOwner(
            @PathVariable GalleryConfig configId,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(GalleryConfigService.isOwner(configId, currentUser.idOrNull(httpRequest)));
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
        if (!GalleryConfigService.isVisible(configId, currentUser.idOrNull(httpRequest))) {
            throw new GallerySourceException(HttpStatus.NOT_FOUND, "gallery.configNotFound", "Gallery config not found.", Map.of());
        }
        GalleryFetchService.GalleryResponse response = galleryFetchService.fetch(configId, input);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(response.contentType())).body(response.body());
    }

    @ExceptionHandler(GallerySourceException.class)
    public ResponseEntity<Map<String, Object>> handle(GallerySourceException e) {
        return ResponseEntity.status(e.getStatus()).body(e.toBody());
    }
}
