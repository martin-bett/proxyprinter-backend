/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 */
package my.poo.mtgcrawl.mtgprint.resource;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.service.mapper.RatingMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/ratings"})
public class RatingController {
    private final RatingService ratingService;
    private final RatingMapper ratingMapper;

    @PostMapping
    public ResponseEntity<Rating> createOrUpdateRating(@RequestBody RatingRequest request, HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = this.extractVisitorIdentifier(httpRequest);
        if (!this.isValidVisitorIdentifier(visitorId)) {
            return ResponseEntity.badRequest().build();
        }
        Rating rating = this.ratingService.saveOrUpdateRating(visitorId, request.getConfigId(), request.getScore(), request.getComment(), request.getAction());
        return ResponseEntity.ok(rating);
    }

    @GetMapping(value={"/get"})
    public List<RatingDto> getRatingsForUser(HttpServletRequest httpRequest) {
        return this.ratingService.getRatingsForVisitorId(this.extractVisitorIdentifier(httpRequest)).stream().map(this.ratingMapper::toDto).toList();
    }

    public String getMethodName(@RequestParam String param) {
        return new String();
    }

    private VisitorIdentifier extractVisitorIdentifier(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String fingerprint = request.getHeader("X-Fingerprint");
        return new VisitorIdentifier(ip, fingerprint);
    }

    private boolean isValidVisitorIdentifier(VisitorIdentifier identifier) {
        return identifier.getIpAddress() != null && !identifier.getIpAddress().isEmpty() && identifier.getBrowserFingerprint() != null && !identifier.getBrowserFingerprint().isEmpty();
    }

    public RatingController(RatingService ratingService, RatingMapper ratingMapper) {
        this.ratingService = ratingService;
        this.ratingMapper = ratingMapper;
    }
}

