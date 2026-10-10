package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Data
@Getter
@Setter
public class GalleryConfigDto {
    private Long id;
    private String category;

    private boolean published;

    private String officialWebsite;

    private String name;
    private String baseUrl;
    private Map<String,Map<String, String>> parameterMap;
    
    
    private String dataFormat;
    private String paginationField;

    private String dataListField;
    private String nameField;
    private String imageUrlField;
    private String defaultLanguage;

    private String linkPattern;
    private boolean followInputRedirects;
    private String credential;
    private String imageUrlPattern;
    private String imageUrlReplacement;
    private String namePattern;
    private String nameReplacement;

    private String hint;
    // Sprachcode -> Hinweis; das Frontend zeigt den Eintrag der UI-Sprache, sonst hint
    private Map<String, String> hintTranslations;

    private String method;
    private String requestFormat;

    // private String csrfTokenVariableName;
    // private Boolean requiresCsrfToken;
    private Boolean useBackendProxy;

}
