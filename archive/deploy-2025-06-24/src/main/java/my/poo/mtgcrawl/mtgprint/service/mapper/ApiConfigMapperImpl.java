/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.mapper;

import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import my.poo.mtgcrawl.mtgprint.service.mapper.ApiConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class ApiConfigMapperImpl
implements ApiConfigMapper {
    @Override
    public ApiConfigDto toDTO(ApiConfig apiConfig) {
        if (apiConfig == null) {
            return null;
        }
        ApiConfigDto apiConfigDto = new ApiConfigDto();
        apiConfigDto.setId(apiConfig.getId());
        apiConfigDto.setOfficialWebsite(apiConfig.getOfficialWebsite());
        apiConfigDto.setCustomArts(apiConfig.getCustomArts());
        apiConfigDto.setPublished(apiConfig.isPublished());
        apiConfigDto.setName(apiConfig.getName());
        apiConfigDto.setMethod(apiConfig.getMethod());
        apiConfigDto.setRequestFormat(apiConfig.getRequestFormat());
        apiConfigDto.setBaseUrl(apiConfig.getBaseUrl());
        if (apiConfig.getConcurrentLimit() != null) {
            apiConfigDto.setConcurrentLimit(apiConfig.getConcurrentLimit());
        }
        apiConfigDto.setDataFormat(apiConfig.getDataFormat());
        apiConfigDto.setCsrfTokenVariableName(apiConfig.getCsrfTokenVariableName());
        apiConfigDto.setRequiresCsrfToken(apiConfig.getRequiresCsrfToken());
        apiConfigDto.setUseBackendProxy(apiConfig.getUseBackendProxy());
        apiConfigDto.setDataListField(apiConfig.getDataListField());
        apiConfigDto.setNameField(apiConfig.getNameField());
        apiConfigDto.setNameSplitCharacter(apiConfig.getNameSplitCharacter());
        apiConfigDto.setImageUrlField(apiConfig.getImageUrlField());
        apiConfigDto.setLanguageField(apiConfig.getLanguageField());
        apiConfigDto.setSetField(apiConfig.getSetField());
        apiConfigDto.setSetNumberField(apiConfig.getSetNumberField());
        apiConfigDto.setPaginationLinkField(apiConfig.getPaginationLinkField());
        apiConfigDto.setPaginationMaxPageField(apiConfig.getPaginationMaxPageField());
        apiConfigDto.setFollowLinkField(apiConfig.getFollowLinkField());
        apiConfigDto.setFollowLinkDataListField(apiConfig.getFollowLinkDataListField());
        apiConfigDto.setFollowLinkImageField(apiConfig.getFollowLinkImageField());
        apiConfigDto.setFollowLinkLanguageField(apiConfig.getFollowLinkLanguageField());
        apiConfigDto.setNestedLanguageDataListField(apiConfig.getNestedLanguageDataListField());
        apiConfigDto.setNestedLanguageImageUrlField(apiConfig.getNestedLanguageImageUrlField());
        apiConfigDto.setNestedLanguageLanguageField(apiConfig.getNestedLanguageLanguageField());
        apiConfigDto.setParameterMap(this.mapParametersToMap(apiConfig.getParameterMap()));
        apiConfigDto.setCategory(this.mapEnumToString(apiConfig.getCategory()));
        return apiConfigDto;
    }

    @Override
    public ApiConfig toEntity(ApiConfigDto dto) {
        if (dto == null) {
            return null;
        }
        ApiConfig apiConfig = new ApiConfig();
        apiConfig.setId(dto.getId());
        if (dto.getPublished() != null) {
            apiConfig.setPublished(dto.getPublished());
        }
        apiConfig.setOfficialWebsite(dto.getOfficialWebsite());
        apiConfig.setName(dto.getName());
        apiConfig.setMethod(dto.getMethod());
        apiConfig.setRequestFormat(dto.getRequestFormat());
        apiConfig.setBaseUrl(dto.getBaseUrl());
        apiConfig.setPaginationLinkField(dto.getPaginationLinkField());
        apiConfig.setPaginationMaxPageField(dto.getPaginationMaxPageField());
        apiConfig.setCustomArts(dto.getCustomArts());
        apiConfig.setConcurrentLimit(dto.getConcurrentLimit());
        apiConfig.setDataFormat(dto.getDataFormat());
        apiConfig.setCsrfTokenVariableName(dto.getCsrfTokenVariableName());
        apiConfig.setRequiresCsrfToken(dto.getRequiresCsrfToken());
        apiConfig.setUseBackendProxy(dto.getUseBackendProxy());
        apiConfig.setDataListField(dto.getDataListField());
        apiConfig.setNameField(dto.getNameField());
        apiConfig.setNameSplitCharacter(dto.getNameSplitCharacter());
        apiConfig.setImageUrlField(dto.getImageUrlField());
        apiConfig.setLanguageField(dto.getLanguageField());
        apiConfig.setSetField(dto.getSetField());
        apiConfig.setSetNumberField(dto.getSetNumberField());
        apiConfig.setFollowLinkField(dto.getFollowLinkField());
        apiConfig.setFollowLinkDataListField(dto.getFollowLinkDataListField());
        apiConfig.setFollowLinkImageField(dto.getFollowLinkImageField());
        apiConfig.setFollowLinkLanguageField(dto.getFollowLinkLanguageField());
        apiConfig.setNestedLanguageDataListField(dto.getNestedLanguageDataListField());
        apiConfig.setNestedLanguageImageUrlField(dto.getNestedLanguageImageUrlField());
        apiConfig.setNestedLanguageLanguageField(dto.getNestedLanguageLanguageField());
        apiConfig.setParameterMap(this.mapMapToParameters(dto.getParameterMap()));
        apiConfig.setCategory(this.mapStringToEnum(dto.getCategory()));
        return apiConfig;
    }
}

