package my.poo.mtgcrawl.mtgprint.entitiy;

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
@Table(name = "galleryConfig")
@Getter
@Setter
public class GalleryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Info
    @Column(columnDefinition = "api_category")
    @Enumerated(EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    private ApiCategory category;

    @Embedded
    private VisitorIdentifier visitorIdentifier;

    @Column
    private boolean published;

    @Column
    private String officialWebsite;
    // Request
    @Column
    private String name;
    @Column
    private String baseUrl;
    @Column
    private String method;
    @Column
    private String paginationField;
    @Column
    private String requestFormat;
    @Column
    private Boolean useBackendProxy;

    @OneToMany(mappedBy="gallery")
    private List<GalleryConfigParameter> parameters;
    @Column
    private String dataFormat;

    // Datalocator
    @Column
    private String dataListField;
    @Column
    private String nameField;
    @Column
    private String imageUrlField;
    // Sprache (ISO-639-1) für alle Bilder dieser Gallery, z. B. "de"
    @Column
    private String defaultLanguage;

}
