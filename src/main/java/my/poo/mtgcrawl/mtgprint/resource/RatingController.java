package my.poo.mtgcrawl.mtgprint.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.service.mapper.RatingMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


// RatingController.java
@RestController
@RequestMapping("/api/ratings")
@AllArgsConstructor
public class RatingController {
     
    private final RatingService ratingService;
    private final RatingMapper ratingMapper;

    @PostMapping
    public ResponseEntity<Rating> createOrUpdateRating(
            @RequestBody RatingRequest request,
            HttpServletRequest httpRequest) {
        VisitorIdentifier visitorId = extractVisitorIdentifier(httpRequest);

        if (!isValidVisitorIdentifier(visitorId)) {
            return ResponseEntity.badRequest().build();
        }

        Rating rating = ratingService.saveOrUpdateRating(
                visitorId,
                request.getConfigId(),
                request.getScore(),
                request.getComment(),
                request.getAction());
        return ResponseEntity.ok(rating);
    }

    @GetMapping("/get")
    public List<RatingDto> getRatingsForUser(HttpServletRequest httpRequest){
        return ratingService.getRatingsForVisitorId(extractVisitorIdentifier(httpRequest)).stream().map(ratingMapper::toDto).toList();
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
        return identifier.getIpAddress() != null && !identifier.getIpAddress().isEmpty() &&
                identifier.getBrowserFingerprint() != null && !identifier.getBrowserFingerprint().isEmpty();
    }
}
