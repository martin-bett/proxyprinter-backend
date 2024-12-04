package my.poo.mtgcrawl.mtgprint.service.rest;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.repository.RatingRepository;

// RatingService.java
@Service
@Transactional
public class RatingService {
    @Autowired
    private RatingRepository ratingRepository;
    
    public Rating saveOrUpdateRating(VisitorIdentifier visitorIdentifier, ApiConfig item, int score, String comment, String action) {
        Optional<Rating> existingRating = ratingRepository.findByVisitorIdentifierAndRatedConfig(visitorIdentifier, item);
        
        if (existingRating.isPresent()) {
            Rating rating = existingRating.get();
            if("availability".equals(action)){
                rating.setScoreAvailablity(score);
            }else {
                rating.setScoreQuality(score);
            }
            rating.setComment(comment);
            return ratingRepository.save(rating);
        }
        
        Rating newRating = new Rating();
        newRating.setVisitorIdentifier(visitorIdentifier);
        newRating.setRatedConfig(item);
        if("availability".equals(action)){
            newRating.setScoreAvailablity(score);
        }else {
            newRating.setScoreQuality(score);
        }
        newRating.setComment(comment);
        newRating.setCreatedAt(LocalDateTime.now());
        
        return ratingRepository.save(newRating);
    }

    public List<Rating> getRatingsForVisitorId(VisitorIdentifier visitorIdentifier){
        return ratingRepository.findAllByVisitorIdentifier(visitorIdentifier);
    }

    public void addRatingsToConfigDto(ApiConfigDto d) {
        List<Rating> ratings  = ratingRepository.findAllByConfigId(d.getId());
        d.setRatingCount(ratings.size());
        List<Integer> qualityValues = new ArrayList<>();
        List<Integer> availableValues = new ArrayList<>();
        for(Rating r : ratings){
            if(r.getScoreAvailablity() != null){
                availableValues.add(r.getScoreAvailablity());
            }
            if(r.getScoreQuality() != null){
                qualityValues.add(r.getScoreQuality());
            }
        }
        double valQuality = qualityValues.size() > 0 ? qualityValues.stream().mapToInt(Integer::intValue).sum() / qualityValues.size() : -1;
        double valAvailability = availableValues.size() > 0 ? availableValues.stream().mapToInt(Integer::intValue).sum() / availableValues.size() : -1;
        if(valAvailability >= 0 || valQuality >= 0){
            d.setRatingValue(((valAvailability >=0 ? valAvailability : 0) + (valQuality >= 0 ? valQuality : 0)) /((valAvailability >= 0 ? 1 : 0)+ (valQuality >= 0 ? 1 : 0)));
        }
    }
}