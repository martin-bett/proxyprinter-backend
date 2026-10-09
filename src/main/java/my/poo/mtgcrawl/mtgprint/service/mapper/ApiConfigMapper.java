package my.poo.mtgcrawl.mtgprint.service.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigConnectedCard;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigParameter;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ConnectedCardLocatorDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

@Mapper(componentModel = "spring", imports = {Collectors.class, HashMap.class})
public interface ApiConfigMapper {
    ApiConfigMapper INSTANCE = Mappers.getMapper(ApiConfigMapper.class);

    @Mapping(target = "parameterMap", expression = "java(mapParametersToMap(apiConfig.getParameterMap()))")
    @Mapping(target = "category", expression = "java(mapEnumToString(apiConfig.getCategory()))")
    @Mapping(target = "connectedCards", expression = "java(mapConnectedCardsToLocators(apiConfig.getConnectedCards()))")
    ApiConfigDto toDTO(ApiConfig apiConfig);

    @Mapping(target = "parameterMap", expression = "java(mapMapToParameters(dto.getParameterMap()))")
    @Mapping(target = "category", expression = "java(mapStringToEnum(dto.getCategory()))")
    @Mapping(target = "lastUsed", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "connectedCards", expression = "java(mapLocatorsToConnectedCards(dto.getConnectedCards()))")
    ApiConfig toEntity(ApiConfigDto dto);

    default Map<String, Map<String, String>> mapParametersToMap(List<ApiConfigParameter> parameters) {
        if (parameters == null) {
            return new HashMap<>();
        }

        return parameters.stream()
            .filter(param -> param.getCategory() != null && param.getName() != null && param.getValue() != null)
            .collect(Collectors.groupingBy(
                ApiConfigParameter::getCategory,
                Collectors.toMap(
                    ApiConfigParameter::getName,
                    ApiConfigParameter::getValue,
                    (existing, replacement) -> replacement,
                    HashMap::new
                )
            ));
    }


    default List<ApiConfigParameter> mapMapToParameters(Map<String, Map<String, String>> parameterMap) {
        if (parameterMap == null) {
            return new ArrayList<>();
        }

        return parameterMap.entrySet().stream()
            .flatMap(categoryEntry -> {
                String category = categoryEntry.getKey();
                Map<String, String> params = categoryEntry.getValue();
                
                return params.entrySet().stream()
                    .map(paramEntry -> {
                        ApiConfigParameter parameter = new ApiConfigParameter();
                        parameter.setCategory(category);
                        parameter.setName(paramEntry.getKey());
                        parameter.setValue(paramEntry.getValue());
                        return parameter;
                    });
            })
            .collect(Collectors.toList());
    }

    default List<ConnectedCardLocatorDto> mapConnectedCardsToLocators(List<ApiConfigConnectedCard> connectedCards) {
        if (connectedCards == null) {
            return new ArrayList<>();
        }
        return connectedCards.stream()
            .map(card -> {
                ConnectedCardLocatorDto dto = new ConnectedCardLocatorDto();
                dto.setDataListField(card.getDataListField());
                dto.setNameField(card.getNameField());
                dto.setLinkField(card.getLinkField());
                dto.setTypeField(card.getTypeField());
                return dto;
            })
            .collect(Collectors.toList());
    }

    // Leere Zeilen (ohne Liste oder Name) fallen weg, position = Reihenfolge im Request
    default List<ApiConfigConnectedCard> mapLocatorsToConnectedCards(List<ConnectedCardLocatorDto> locators) {
        List<ApiConfigConnectedCard> result = new ArrayList<>();
        if (locators == null) {
            return result;
        }
        for (ConnectedCardLocatorDto locator : locators) {
            if (locator == null || isBlank(locator.getDataListField()) || isBlank(locator.getNameField())) {
                continue;
            }
            ApiConfigConnectedCard card = new ApiConfigConnectedCard();
            card.setPosition(result.size());
            card.setDataListField(locator.getDataListField().trim());
            card.setNameField(locator.getNameField().trim());
            card.setLinkField(isBlank(locator.getLinkField()) ? null : locator.getLinkField().trim());
            card.setTypeField(isBlank(locator.getTypeField()) ? null : locator.getTypeField().trim());
            result.add(card);
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
