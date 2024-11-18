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
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigParameter;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

@Mapper(componentModel = "spring", imports = {Collectors.class, HashMap.class})
public interface ApiConfigMapper {
    ApiConfigMapper INSTANCE = Mappers.getMapper(ApiConfigMapper.class);

    @Mapping(target = "parameterMap", expression = "java(mapParametersToMap(apiConfig.getParameterMap()))")
    @Mapping(target = "category", expression = "java(mapEnumToString(apiConfig.getCategory()))")
    ApiConfigDto toDTO(ApiConfig apiConfig);

    @Mapping(target = "parameterMap", expression = "java(mapMapToParameters(dto.getParameterMap()))")
    @Mapping(target = "category", expression = "java(mapStringToEnum(dto.getCategory()))")
    ApiConfig toEntity(ApiConfigDto dto);

    default Map<String, Object> mapParametersToMap(List<ApiConfigParameter> parameters) {
        if (parameters == null) {
            return new HashMap<>();
        }
        return parameters.stream()
            .collect(Collectors.toMap(
                ApiConfigParameter::getName,
                ApiConfigParameter::getValue,
                (existing, replacement) -> replacement
            ));
    }


    default List<ApiConfigParameter> mapMapToParameters(Map<String, Object> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return new ArrayList<>();
        }
        return parameterMap.entrySet().stream()
            .map(entry -> {
                ApiConfigParameter parameter = new ApiConfigParameter();
                parameter.setName(entry.getKey());
                parameter.setValue(String.valueOf(entry.getValue()));
                return parameter;
            })
            .collect(Collectors.toList());
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
