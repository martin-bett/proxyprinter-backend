package my.poo.mtgcrawl.mtgprint.entitiy;

import lombok.Data;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

@Data
public class ApiConfigDto {
    // Info
    private ApiCategory category;
    private String officialWebsite;

    // Request
    private String name;
    private String method;
    private String requestFormat;
    private String baseUrl;
    private java.util.Map<String, Object> parameterMap;
    private int concurrentLimit;
    private String dataFormat;

    // Custom Request
    private String csrfTokenVariableName;
    private Boolean requiresCsrfToken;
    private Boolean useBackendProxy;

    // Datalocator
    private String dataListField;
    private String nameField;
    private String nameSplitCharacter;
    private String imageUrlField;
    private String languageField;
    private String setField;
    private String setNumberField;

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