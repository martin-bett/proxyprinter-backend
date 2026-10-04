/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.rest;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class RatingService {
    @Autowired
    private RatingRepository ratingRepository;

    public Rating saveOrUpdateRating(VisitorIdentifier visitorIdentifier, ApiConfig item, int score, String comment, String action) {
        Optional<Rating> existingRating = this.ratingRepository.findByVisitorIdentifierAndRatedConfig(visitorIdentifier, item);
        if (existingRating.isPresent()) {
            Rating rating = existingRating.get();
            if ("availability".equals(action)) {
                rating.setScoreAvailablity(score);
            } else {
                rating.setScoreQuality(score);
            }
            rating.setComment(comment);
            return this.ratingRepository.save(rating);
        }
        Rating newRating = new Rating();
        newRating.setVisitorIdentifier(visitorIdentifier);
        newRating.setRatedConfig(item);
        if ("availability".equals(action)) {
            newRating.setScoreAvailablity(score);
        } else {
            newRating.setScoreQuality(score);
        }
        newRating.setComment(comment);
        newRating.setCreatedAt(LocalDateTime.now());
        return this.ratingRepository.save(newRating);
    }

    public List<Rating> getRatingsForVisitorId(VisitorIdentifier visitorIdentifier) {
        return this.ratingRepository.findAllByVisitorIdentifier(visitorIdentifier);
    }

    public void addRatingsToConfigDto(ApiConfigDto d) {
        double valAvailability;
        List<Rating> ratings = this.ratingRepository.findAllByConfigId(d.getId());
        d.setRatingCount(ratings.size());
        ArrayList<Integer> qualityValues = new ArrayList<Integer>();
        ArrayList<Integer> availableValues = new ArrayList<Integer>();
        for (Rating r : ratings) {
            if (r.getScoreAvailablity() != null) {
                availableValues.add(r.getScoreAvailablity());
            }
            if (r.getScoreQuality() == null) continue;
            qualityValues.add(r.getScoreQuality());
        }
        double valQuality = qualityValues.size() > 0 ? (double)(qualityValues.stream().mapToInt(Integer::intValue).sum() / qualityValues.size()) : -1.0;
        double d2 = valAvailability = availableValues.size() > 0 ? (double)(availableValues.stream().mapToInt(Integer::intValue).sum() / availableValues.size()) : -1.0;
        if (valAvailability >= 0.0 || valQuality >= 0.0) {
            d.setRatingValue(((valAvailability >= 0.0 ? valAvailability : 0.0) + (valQuality >= 0.0 ? valQuality : 0.0)) / (double)((valAvailability >= 0.0 ? 1 : 0) + (valQuality >= 0.0 ? 1 : 0)));
        }
    }
}

