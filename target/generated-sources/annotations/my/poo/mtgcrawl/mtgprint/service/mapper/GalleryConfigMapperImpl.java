package my.poo.mtgcrawl.mtgprint.service.mapper;

import java.util.HashMap;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.GalleryConfigDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-04T20:24:54+0100",
    comments = "version: 1.4.2.Final, compiler: Eclipse JDT (IDE) 3.40.0.v20240919-1711, environment: Java 17.0.12 (Eclipse Adoptium)"
)
@Component
public class GalleryConfigMapperImpl implements GalleryConfigMapper {

    @Override
    public GalleryConfig toEntity(GalleryConfigDto dto) {
        if ( dto == null ) {
            return null;
        }

        GalleryConfig galleryConfig = new GalleryConfig();

        galleryConfig.setBaseUrl( dto.getBaseUrl() );
        galleryConfig.setDataFormat( dto.getDataFormat() );
        galleryConfig.setDataListField( dto.getDataListField() );
        galleryConfig.setId( dto.getId() );
        galleryConfig.setImageUrlField( dto.getImageUrlField() );
        galleryConfig.setMethod( dto.getMethod() );
        galleryConfig.setName( dto.getName() );
        galleryConfig.setNameField( dto.getNameField() );
        galleryConfig.setOfficialWebsite( dto.getOfficialWebsite() );
        galleryConfig.setPaginationField( dto.getPaginationField() );
        galleryConfig.setPublished( dto.isPublished() );
        galleryConfig.setRequestFormat( dto.getRequestFormat() );
        galleryConfig.setUseBackendProxy( dto.getUseBackendProxy() );

        galleryConfig.setParameters( mapMapToParameters(dto.getParameterMap()) );
        galleryConfig.setCategory( mapStringToEnum(dto.getCategory()) );

        return galleryConfig;
    }

    @Override
    public GalleryConfigDto toDTO(GalleryConfig apiConfig) {
        if ( apiConfig == null ) {
            return null;
        }

        GalleryConfigDto galleryConfigDto = new GalleryConfigDto();

        galleryConfigDto.setBaseUrl( apiConfig.getBaseUrl() );
        galleryConfigDto.setDataFormat( apiConfig.getDataFormat() );
        galleryConfigDto.setDataListField( apiConfig.getDataListField() );
        galleryConfigDto.setId( apiConfig.getId() );
        galleryConfigDto.setImageUrlField( apiConfig.getImageUrlField() );
        galleryConfigDto.setMethod( apiConfig.getMethod() );
        galleryConfigDto.setName( apiConfig.getName() );
        galleryConfigDto.setNameField( apiConfig.getNameField() );
        galleryConfigDto.setOfficialWebsite( apiConfig.getOfficialWebsite() );
        galleryConfigDto.setPaginationField( apiConfig.getPaginationField() );
        galleryConfigDto.setPublished( apiConfig.isPublished() );
        galleryConfigDto.setRequestFormat( apiConfig.getRequestFormat() );
        galleryConfigDto.setUseBackendProxy( apiConfig.getUseBackendProxy() );

        galleryConfigDto.setParameterMap( mapParametersToMap(apiConfig.getParameters()) );
        galleryConfigDto.setCategory( mapEnumToString(apiConfig.getCategory()) );

        return galleryConfigDto;
    }
}
