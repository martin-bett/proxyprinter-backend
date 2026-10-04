/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.mapper;

import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel="spring")
public interface RatingMapper {
    public static final RatingMapper INSTANCE = Mappers.getMapper(RatingMapper.class);

    @Mappings(value={@Mapping(source="ratedConfig.id", target="configId"), @Mapping(source="scoreAvailablity", target="scoreAvailability"), @Mapping(source="scoreQuality", target="scoreQuality")})
    public RatingDto toDto(Rating var1);

    @Mappings(value={@Mapping(source="configId", target="ratedConfig.id"), @Mapping(source="scoreAvailability", target="scoreAvailablity"), @Mapping(source="scoreQuality", target="scoreQuality"), @Mapping(target="id", ignore=true), @Mapping(target="visitorIdentifier", ignore=true), @Mapping(target="comment", ignore=true), @Mapping(target="createdAt", expression="java(java.time.LocalDateTime.now())")})
    public Rating toEntity(RatingDto var1);
}

