/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.mapper;

import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import my.poo.mtgcrawl.mtgprint.service.mapper.GalleryConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class GalleryConfigMapperImpl
implements GalleryConfigMapper {
    @Override
    public GalleryConfig toEntity(GalleryConfigDto dto) {
        if (dto == null) {
            return null;
        }
        GalleryConfig galleryConfig = new GalleryConfig();
        galleryConfig.setId(dto.getId());
        galleryConfig.setPublished(dto.isPublished());
        galleryConfig.setOfficialWebsite(dto.getOfficialWebsite());
        galleryConfig.setName(dto.getName());
        galleryConfig.setBaseUrl(dto.getBaseUrl());
        galleryConfig.setMethod(dto.getMethod());
        galleryConfig.setPaginationField(dto.getPaginationField());
        galleryConfig.setRequestFormat(dto.getRequestFormat());
        galleryConfig.setUseBackendProxy(dto.getUseBackendProxy());
        galleryConfig.setDataFormat(dto.getDataFormat());
        galleryConfig.setDataListField(dto.getDataListField());
        galleryConfig.setNameField(dto.getNameField());
        galleryConfig.setImageUrlField(dto.getImageUrlField());
        galleryConfig.setParameters(this.mapMapToParameters(dto.getParameterMap()));
        galleryConfig.setCategory(this.mapStringToEnum(dto.getCategory()));
        return galleryConfig;
    }

    @Override
    public GalleryConfigDto toDTO(GalleryConfig apiConfig) {
        if (apiConfig == null) {
            return null;
        }
        GalleryConfigDto galleryConfigDto = new GalleryConfigDto();
        galleryConfigDto.setId(apiConfig.getId());
        galleryConfigDto.setPublished(apiConfig.isPublished());
        galleryConfigDto.setOfficialWebsite(apiConfig.getOfficialWebsite());
        galleryConfigDto.setName(apiConfig.getName());
        galleryConfigDto.setBaseUrl(apiConfig.getBaseUrl());
        galleryConfigDto.setDataFormat(apiConfig.getDataFormat());
        galleryConfigDto.setPaginationField(apiConfig.getPaginationField());
        galleryConfigDto.setDataListField(apiConfig.getDataListField());
        galleryConfigDto.setNameField(apiConfig.getNameField());
        galleryConfigDto.setImageUrlField(apiConfig.getImageUrlField());
        galleryConfigDto.setMethod(apiConfig.getMethod());
        galleryConfigDto.setRequestFormat(apiConfig.getRequestFormat());
        galleryConfigDto.setUseBackendProxy(apiConfig.getUseBackendProxy());
        galleryConfigDto.setParameterMap(this.mapParametersToMap(apiConfig.getParameters()));
        galleryConfigDto.setCategory(this.mapEnumToString(apiConfig.getCategory()));
        return galleryConfigDto;
    }
}

