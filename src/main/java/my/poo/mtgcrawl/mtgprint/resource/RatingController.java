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
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingRequest;
import my.poo.mtgcrawl.mtgprint.service.mapper.RatingMapper;
import my.poo.mtgcrawl.mtgprint.service.rest.ApiConfigService;
import my.poo.mtgcrawl.mtgprint.service.rest.RatingService;
import org.springframework.web.bind.annotation.GetMapping;


/** Ratings nur mit Konto, eins pro Konto und Config. */
@RestController
@RequestMapping("/api/ratings")
@AllArgsConstructor
public class RatingController {
     
    private final RatingService ratingService;
    private final RatingMapper ratingMapper;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<Rating> createOrUpdateRating(
            @RequestBody RatingRequest request,
            HttpServletRequest httpRequest) {
        long userId = currentUser.require(httpRequest);
        if (!ApiConfigService.isVisible(request.getConfigId(), userId)) {
            return ResponseEntity.notFound().build();
        }
        Rating rating = ratingService.saveOrUpdateRating(
                userId,
                request.getConfigId(),
                request.getScore(),
                request.getComment(),
                request.getAction());
        return ResponseEntity.ok(rating);
    }

    /** Eigene Ratings; ohne Konto leer. */
    @GetMapping("/get")
    public List<RatingDto> getRatingsForUser(HttpServletRequest httpRequest){
        Long userId = currentUser.idOrNull(httpRequest);
        if (userId == null) {
            return List.of();
        }
        return ratingService.getRatingsOf(userId).stream().map(ratingMapper::toDto).toList();
    }
}
