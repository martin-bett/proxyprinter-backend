package my.poo.mtgcrawl.mtgprint.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.stream.Collectors;
import java.util.*;

import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfigParameter;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

@Mapper(componentModel = "spring", imports = {Collectors.class, HashMap.class})
public interface GalleryConfigMapper {

    @Mapping(target = "parameters", expression = "java(mapMapToParameters(dto.getParameterMap()))")
    @Mapping(target = "category", expression = "java(mapStringToEnum(dto.getCategory()))")
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "hintTranslations", expression = "java(translationsToJson(dto.getHintTranslations()))")
    GalleryConfig toEntity(GalleryConfigDto dto); 

    @Mapping(target = "parameterMap", expression = "java(mapParametersToMap(apiConfig.getParameters()))")
    @Mapping(target = "category", expression = "java(mapEnumToString(apiConfig.getCategory()))")
    @Mapping(target = "hintTranslations", expression = "java(translationsFromJson(apiConfig.getHintTranslations()))")
    GalleryConfigDto toDTO(GalleryConfig apiConfig);
    
  default Map<String, Map<String, String>> mapParametersToMap(List<GalleryConfigParameter> parameters) {
        if (parameters == null) {
            return new HashMap<>();
        }

        return parameters.stream()
            .filter(param -> param.getCategory() != null && param.getName() != null)
            .collect(Collectors.groupingBy(
                GalleryConfigParameter::getCategory,
                Collectors.toMap(
                    GalleryConfigParameter::getName,
                    param -> "$GALLERY_INPUT$",
                    (existing, replacement) -> replacement,
                    HashMap::new
                )
            ));
    }


    default List<GalleryConfigParameter> mapMapToParameters(Map<String, Map<String, String>> parameterMap) {
        if (parameterMap == null) {
            return new ArrayList<>();
        }

        return parameterMap.entrySet().stream()
            .flatMap(categoryEntry -> {
                String category = categoryEntry.getKey();
                Map<String, String> params = categoryEntry.getValue();
                return params.entrySet().stream()
                    .map(paramEntry -> {
                        GalleryConfigParameter parameter = new GalleryConfigParameter();
                        parameter.setCategory(category);
                        parameter.setName(paramEntry.getKey());
                        return parameter;
                    });
            })
            .collect(Collectors.toList());
    }

    ObjectMapper JSON = new ObjectMapper();

    default String translationsToJson(Map<String, String> translations) {
        if (translations == null || translations.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(translations);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    default Map<String, String> translationsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSON.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (JsonProcessingException e) {
            return null;
        }
    }

        // Enum zu String
    default String mapEnumToString(ApiCategory category) {
        return category != null ? category.name() : null;
    }

    // String zu Enum
    default ApiCategory mapStringToEnum(String category) {
        return category != null ? ApiCategory.byName(category) : null;
    }

}
