/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigParameter;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel="spring", imports={Collectors.class, HashMap.class})
public interface ApiConfigMapper {
    public static final ApiConfigMapper INSTANCE = Mappers.getMapper(ApiConfigMapper.class);

    @Mappings(value={@Mapping(target="parameterMap", expression="java(mapParametersToMap(apiConfig.getParameterMap()))"), @Mapping(target="category", expression="java(mapEnumToString(apiConfig.getCategory()))")})
    public ApiConfigDto toDTO(ApiConfig var1);

    @Mappings(value={@Mapping(target="parameterMap", expression="java(mapMapToParameters(dto.getParameterMap()))"), @Mapping(target="category", expression="java(mapStringToEnum(dto.getCategory()))")})
    public ApiConfig toEntity(ApiConfigDto var1);

    default public Map<String, Map<String, String>> mapParametersToMap(List<ApiConfigParameter> parameters) {
        if (parameters == null) {
            return new HashMap<String, Map<String, String>>();
        }
        return parameters.stream().filter(param -> param.getCategory() != null && param.getName() != null && param.getValue() != null).collect(Collectors.groupingBy(ApiConfigParameter::getCategory, Collectors.toMap(ApiConfigParameter::getName, ApiConfigParameter::getValue, (existing, replacement) -> replacement, HashMap::new)));
    }

    default public List<ApiConfigParameter> mapMapToParameters(Map<String, Map<String, String>> parameterMap) {
        if (parameterMap == null) {
            return new ArrayList<ApiConfigParameter>();
        }
        return parameterMap.entrySet().stream().flatMap(categoryEntry -> {
            String category = (String)categoryEntry.getKey();
            Map params = (Map)categoryEntry.getValue();
            return params.entrySet().stream().map(paramEntry -> {
                ApiConfigParameter parameter = new ApiConfigParameter();
                parameter.setCategory(category);
                parameter.setName((String)paramEntry.getKey());
                parameter.setValue((String)paramEntry.getValue());
                return parameter;
            });
        }).collect(Collectors.toList());
    }

    default public String mapEnumToString(ApiCategory category) {
        return category != null ? category.name() : null;
    }

    default public ApiCategory mapStringToEnum(String category) {
        return category != null ? ApiCategory.byName(category) : null;
    }
}

