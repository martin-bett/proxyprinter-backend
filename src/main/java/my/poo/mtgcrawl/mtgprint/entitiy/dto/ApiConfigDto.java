package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import java.util.Map;

import lombok.Data;

@Data
public class ApiConfigDto {
    //Rating
    private Double ratingValue;
    private Integer ratingCount;

    private Long id;
    // Info
    private String category;
    private String officialWebsite;
    private Boolean customArts;
    private Boolean published;
    

    // Request
    private String name;
    private String method;
    private String requestFormat;
    private String baseUrl;
    private Map<String,Map<String, String>> parameterMap;
    private int concurrentLimit;
    private String dataFormat;
    

    // Custom Request
    private String csrfTokenVariableName;
    private Boolean requiresCsrfToken;
    private Boolean useBackendProxy;
    private Boolean proxyImages;

    // Datalocator
    private String dataListField;
    private String nameField;
    private String nameSplitCharacter;
    private String imageUrlField;
    private String languageField;
    private String setField;
    private String setNumberField;
    private String paginationLinkField;
    private String paginationMaxPageField;

    // Extended Datalocator
    private String followLinkField;
    private String followLinkDataListField;
    private String followLinkImageField;
    private String followLinkLanguageField;

    // Language Support
    private String nestedLanguageDataListField;
    private String nestedLanguageImageUrlField;
    private String nestedLanguageLanguageField;
}