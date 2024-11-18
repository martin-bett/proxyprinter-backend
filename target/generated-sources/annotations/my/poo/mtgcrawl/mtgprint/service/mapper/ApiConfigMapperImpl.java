package my.poo.mtgcrawl.mtgprint.service.mapper;

import java.util.HashMap;
import java.util.stream.Collectors;
import javax.annotation.processing.Generated;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ApiConfigDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2024-11-18T18:47:12+0100",
    comments = "version: 1.4.2.Final, compiler: Eclipse JDT (IDE) 3.40.0.v20240919-1711, environment: Java 17.0.12 (Eclipse Adoptium)"
)
@Component
public class ApiConfigMapperImpl implements ApiConfigMapper {

    @Override
    public ApiConfigDto toDTO(ApiConfig apiConfig) {
        if ( apiConfig == null ) {
            return null;
        }

        ApiConfigDto apiConfigDto = new ApiConfigDto();

        apiConfigDto.setBaseUrl( apiConfig.getBaseUrl() );
        if ( apiConfig.getConcurrentLimit() != null ) {
            apiConfigDto.setConcurrentLimit( apiConfig.getConcurrentLimit() );
        }
        apiConfigDto.setCsrfTokenVariableName( apiConfig.getCsrfTokenVariableName() );
        apiConfigDto.setCustomArts( apiConfig.getCustomArts() );
        apiConfigDto.setDataFormat( apiConfig.getDataFormat() );
        apiConfigDto.setDataListField( apiConfig.getDataListField() );
        apiConfigDto.setFollowLinkDataListField( apiConfig.getFollowLinkDataListField() );
        apiConfigDto.setFollowLinkField( apiConfig.getFollowLinkField() );
        apiConfigDto.setFollowLinkImageField( apiConfig.getFollowLinkImageField() );
        apiConfigDto.setFollowLinkLanguageField( apiConfig.getFollowLinkLanguageField() );
        apiConfigDto.setId( apiConfig.getId() );
        apiConfigDto.setImageUrlField( apiConfig.getImageUrlField() );
        apiConfigDto.setLanguageField( apiConfig.getLanguageField() );
        apiConfigDto.setMethod( apiConfig.getMethod() );
        apiConfigDto.setName( apiConfig.getName() );
        apiConfigDto.setNameField( apiConfig.getNameField() );
        apiConfigDto.setNameSplitCharacter( apiConfig.getNameSplitCharacter() );
        apiConfigDto.setNestedLanguageDataListField( apiConfig.getNestedLanguageDataListField() );
        apiConfigDto.setNestedLanguageImageUrlField( apiConfig.getNestedLanguageImageUrlField() );
        apiConfigDto.setNestedLanguageLanguageField( apiConfig.getNestedLanguageLanguageField() );
        apiConfigDto.setOfficialWebsite( apiConfig.getOfficialWebsite() );
        apiConfigDto.setPaginationField( apiConfig.getPaginationField() );
        apiConfigDto.setPublished( apiConfig.isPublished() );
        apiConfigDto.setRequestFormat( apiConfig.getRequestFormat() );
        apiConfigDto.setRequiresCsrfToken( apiConfig.getRequiresCsrfToken() );
        apiConfigDto.setSetField( apiConfig.getSetField() );
        apiConfigDto.setSetNumberField( apiConfig.getSetNumberField() );
        apiConfigDto.setUseBackendProxy( apiConfig.getUseBackendProxy() );

        apiConfigDto.setParameterMap( mapParametersToMap(apiConfig.getParameterMap()) );
        apiConfigDto.setCategory( mapEnumToString(apiConfig.getCategory()) );

        return apiConfigDto;
    }

    @Override
    public ApiConfig toEntity(ApiConfigDto dto) {
        if ( dto == null ) {
            return null;
        }

        ApiConfig apiConfig = new ApiConfig();

        apiConfig.setBaseUrl( dto.getBaseUrl() );
        apiConfig.setConcurrentLimit( dto.getConcurrentLimit() );
        apiConfig.setCsrfTokenVariableName( dto.getCsrfTokenVariableName() );
        apiConfig.setCustomArts( dto.getCustomArts() );
        apiConfig.setDataFormat( dto.getDataFormat() );
        apiConfig.setDataListField( dto.getDataListField() );
        apiConfig.setFollowLinkDataListField( dto.getFollowLinkDataListField() );
        apiConfig.setFollowLinkField( dto.getFollowLinkField() );
        apiConfig.setFollowLinkImageField( dto.getFollowLinkImageField() );
        apiConfig.setFollowLinkLanguageField( dto.getFollowLinkLanguageField() );
        apiConfig.setId( dto.getId() );
        apiConfig.setImageUrlField( dto.getImageUrlField() );
        apiConfig.setLanguageField( dto.getLanguageField() );
        apiConfig.setMethod( dto.getMethod() );
        apiConfig.setName( dto.getName() );
        apiConfig.setNameField( dto.getNameField() );
        apiConfig.setNameSplitCharacter( dto.getNameSplitCharacter() );
        apiConfig.setNestedLanguageDataListField( dto.getNestedLanguageDataListField() );
        apiConfig.setNestedLanguageImageUrlField( dto.getNestedLanguageImageUrlField() );
        apiConfig.setNestedLanguageLanguageField( dto.getNestedLanguageLanguageField() );
        apiConfig.setOfficialWebsite( dto.getOfficialWebsite() );
        apiConfig.setPaginationField( dto.getPaginationField() );
        if ( dto.getPublished() != null ) {
            apiConfig.setPublished( dto.getPublished() );
        }
        apiConfig.setRequestFormat( dto.getRequestFormat() );
        apiConfig.setRequiresCsrfToken( dto.getRequiresCsrfToken() );
        apiConfig.setSetField( dto.getSetField() );
        apiConfig.setSetNumberField( dto.getSetNumberField() );
        apiConfig.setUseBackendProxy( dto.getUseBackendProxy() );

        apiConfig.setParameterMap( mapMapToParameters(dto.getParameterMap()) );
        apiConfig.setCategory( mapStringToEnum(dto.getCategory()) );

        return apiConfig;
    }
}
