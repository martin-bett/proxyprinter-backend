package my.poo.mtgcrawl.mtgprint.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.RatingDto;

@Mapper(componentModel = "spring")
public interface RatingMapper {
  RatingMapper INSTANCE = Mappers.getMapper(RatingMapper.class);

  @Mapping(source = "ratedConfig.id", target = "configId")
  @Mapping(source = "scoreAvailablity", target = "scoreAvailability")
  @Mapping(source = "scoreQuality", target = "scoreQuality")
  RatingDto toDto(Rating rating);

  @Mapping(source = "configId", target = "ratedConfig.id")
  @Mapping(source = "scoreAvailability", target = "scoreAvailablity")
  @Mapping(source = "scoreQuality", target = "scoreQuality")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "visitorIdentifier", ignore = true)
  @Mapping(target = "comment", ignore = true)
  @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
  Rating toEntity(RatingDto dto);
}
