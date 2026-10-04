/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfigParameter;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel="spring", imports={Collectors.class, HashMap.class})
public interface GalleryConfigMapper {
    @Mappings(value={@Mapping(target="parameters", expression="java(mapMapToParameters(dto.getParameterMap()))"), @Mapping(target="category", expression="java(mapStringToEnum(dto.getCategory()))")})
    public GalleryConfig toEntity(GalleryConfigDto var1);

    @Mappings(value={@Mapping(target="parameterMap", expression="java(mapParametersToMap(apiConfig.getParameters()))"), @Mapping(target="category", expression="java(mapEnumToString(apiConfig.getCategory()))")})
    public GalleryConfigDto toDTO(GalleryConfig var1);

    default public Map<String, Map<String, String>> mapParametersToMap(List<GalleryConfigParameter> parameters) {
        if (parameters == null) {
            return new HashMap<String, Map<String, String>>();
        }
        return parameters.stream().filter(param -> param.getCategory() != null && param.getName() != null).collect(Collectors.groupingBy(GalleryConfigParameter::getCategory, Collectors.toMap(GalleryConfigParameter::getName, param -> "$GALLERY_INPUT$", (existing, replacement) -> replacement, HashMap::new)));
    }

    default public List<GalleryConfigParameter> mapMapToParameters(Map<String, Map<String, String>> parameterMap) {
        if (parameterMap == null) {
            return new ArrayList<GalleryConfigParameter>();
        }
        return parameterMap.entrySet().stream().flatMap(categoryEntry -> {
            String category = (String)categoryEntry.getKey();
            Map params = (Map)categoryEntry.getValue();
            return params.entrySet().stream().map(paramEntry -> {
                GalleryConfigParameter parameter = new GalleryConfigParameter();
                parameter.setCategory(category);
                parameter.setName((String)paramEntry.getKey());
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

