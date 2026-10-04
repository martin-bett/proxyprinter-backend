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
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.repository.GalleryConfigRepository;
import my.poo.mtgcrawl.mtgprint.service.RequestUtils;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.GalleryConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/gallery"})
public class GalleryConfigController {
    private final GalleryConfigRepository galleryConfigRepository;
    private final GalleryConfigService galleryConfigService;
    private final RatingService ratingService;
    private final GalleryConfigMapper mapper;

    @GetMapping
    public List<GalleryConfigDto> getAllConfigs(HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = RequestUtils.extractVisitorIdentifier(httpRequest);
        return this.galleryConfigRepository.findAllVisible(visitorId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @GetMapping(value={"/create"})
    public ResponseEntity<Long> createConfig(HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.galleryConfigService.createConfig(visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value={"/copy"})
    public ResponseEntity<Long> copyConfig(@RequestBody GalleryConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.galleryConfigService.copyConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value={"/delete"})
    public ResponseEntity<?> deleteConfig(@RequestBody GalleryConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        this.galleryConfigService.deleteConfig(request, visitorIdentifier);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value={"/publish"})
    public ResponseEntity<Long> publishConfig(@RequestBody GalleryConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.galleryConfigService.publishConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value={"/edit"})
    public ResponseEntity<Long> editConfig(@RequestBody GalleryConfigDto request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        Long result = this.galleryConfigService.editConfig(request, visitorIdentifier);
        return ResponseEntity.ok(result);
    }

    @GetMapping(value={"/{configId}/check"})
    public ResponseEntity<Boolean> createConfig(@PathVariable GalleryConfig configId, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorIdentifier = RequestUtils.extractVisitorIdentifier(httpRequest);
        boolean res = this.galleryConfigService.checkVisitorIdentifier(configId, visitorIdentifier);
        return ResponseEntity.ok(res);
    }

    @PostMapping(value={"/{configId}/rate"})
    public ResponseEntity<?> rateConfig(@PathVariable GalleryConfig configId, @RequestBody RatingRequest request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = RequestUtils.extractVisitorIdentifier(httpRequest);
        return ResponseEntity.ok().build();
    }

    private GalleryConfigDto toDTO(GalleryConfig apiconfig1) {
        GalleryConfigDto d = this.mapper.toDTO(apiconfig1);
        return d;
    }

    public GalleryConfigController(GalleryConfigRepository galleryConfigRepository, GalleryConfigService galleryConfigService, RatingService ratingService, GalleryConfigMapper mapper) {
        this.galleryConfigRepository = galleryConfigRepository;
        this.galleryConfigService = galleryConfigService;
        this.ratingService = ratingService;
        this.mapper = mapper;
    }
}

