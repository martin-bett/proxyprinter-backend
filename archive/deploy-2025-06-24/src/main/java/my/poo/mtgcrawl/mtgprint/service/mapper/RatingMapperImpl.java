/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.mapper;

import java.time.LocalDateTime;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingDto;
import my.poo.mtgcrawl.mtgprint.service.mapper.RatingMapper;
import org.springframework.stereotype.Component;

@Component
public class RatingMapperImpl
implements RatingMapper {
    @Override
    public RatingDto toDto(Rating rating) {
        if (rating == null) {
            return null;
        }
        RatingDto ratingDto = new RatingDto();
        ratingDto.setConfigId(this.ratingRatedConfigId(rating));
        if (rating.getScoreAvailablity() != null) {
            ratingDto.setScoreAvailability(rating.getScoreAvailablity().doubleValue());
        }
        if (rating.getScoreQuality() != null) {
            ratingDto.setScoreQuality(rating.getScoreQuality().doubleValue());
        }
        return ratingDto;
    }

    @Override
    public Rating toEntity(RatingDto dto) {
        if (dto == null) {
            return null;
        }
        Rating rating = new Rating();
        rating.setRatedConfig(this.ratingDtoToApiConfig(dto));
        if (dto.getScoreAvailability() != null) {
            rating.setScoreAvailablity(dto.getScoreAvailability().intValue());
        }
        if (dto.getScoreQuality() != null) {
            rating.setScoreQuality(dto.getScoreQuality().intValue());
        }
        rating.setCreatedAt(LocalDateTime.now());
        return rating;
    }

    private Long ratingRatedConfigId(Rating rating) {
        if (rating == null) {
            return null;
        }
        ApiConfig ratedConfig = rating.getRatedConfig();
        if (ratedConfig == null) {
            return null;
        }
        Long id = ratedConfig.getId();
        if (id == null) {
            return null;
        }
        return id;
    }

    protected ApiConfig ratingDtoToApiConfig(RatingDto ratingDto) {
        if (ratingDto == null) {
            return null;
        }
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setId(ratingDto.getConfigId());
        return apiConfig;
    }
}

