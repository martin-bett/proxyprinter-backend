package my.poo.mtgcrawl.mtgprint.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import my.poo.mtgcrawl.mtgprint.entitiy.DeckFinderConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.DeckFinderConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

@Mapper(componentModel = "spring")
public interface DeckFinderConfigMapper {

    @Mapping(target = "category", expression = "java(mapStringToEnum(dto.getCategory()))")
    @Mapping(target = "visitorIdentifier", ignore = true)
    DeckFinderConfig toEntity(DeckFinderConfigDto dto);

    @Mapping(target = "category", expression = "java(mapEnumToString(config.getCategory()))")
    DeckFinderConfigDto toDTO(DeckFinderConfig config);

    default String mapEnumToString(ApiCategory category) {
        return category != null ? category.name() : null;
    }

    default ApiCategory mapStringToEnum(String category) {
        return category != null ? ApiCategory.byName(category) : null;
    }
}
