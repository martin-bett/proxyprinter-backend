package my.poo.mtgcrawl.mtgprint.entitiy;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;


import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

@Entity
@Table(name = "apiConfig")
@Getter
@Setter
public class ApiConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Info
    @Column(columnDefinition = "api_category")
    @Enumerated(EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    // @Convert(converter = ApiCategoryConverter.class)
    private ApiCategory category;

    @Embedded
    private VisitorIdentifier visitorIdentifier;

    @Column
    private boolean published;

    @Column
    private String officialWebsite;

    // Letzte Suche oder Bearbeitung. ApiConfigCleanupService löscht User-Configs, die zu lange unbenutzt sind
    @Column
    private LocalDateTime lastUsed;

    // Request
    @Column
    private String name;
    @Column
    private String method;
    @Column
    private String requestFormat;
    @Column
    private String baseUrl;
    @Column
    private String paginationLinkField;    
    @Column
    private String paginationMaxPageField;

    @Column
    private Boolean customArts;

    @OneToMany(mappedBy="config")
    private List<ApiConfigParameter> parameterMap;
    @Column
    private Integer concurrentLimit;
    @Column
    private String dataFormat;

    // Custom Request
    @Column
    private String csrfTokenVariableName;
    @Column
    private Boolean requiresCsrfToken;
    @Column
    private Boolean useBackendProxy;
    // Bilder dieser API über /api/proxy/image laden (CORS, Cache im Backend)
    @Column
    private Boolean proxyImages;

    // Datalocator
    @Column
    private String dataListField;
    @Column
    private String nameField;
    @Column
    private String nameSplitCharacter;
    @Column
    private String imageUrlField;
    @Column
    private String languageField;
    @Column
    private String setField;
    @Column
    private String setNumberField;

    // Extended Datalocator
    @Column
    private String followLinkField;
    @Column
    private String followLinkDataListField;
    @Column
    private String followLinkImageField;
    @Column
    private String followLinkLanguageField;

    // Language Support
    @Column
    private String nestedLanguageDataListField;
    @Column
    private String nestedLanguageImageUrlField;
    @Column
    private String nestedLanguageLanguageField;
}
