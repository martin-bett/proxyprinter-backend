/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy;

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
import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfigParameter;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name="galleryConfig")
public class GalleryConfig {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition="api_category")
    @Enumerated(value=EnumType.STRING)
    @JdbcType(value=PostgreSQLEnumJdbcType.class)
    private ApiCategory category;
    @Embedded
    private VisitorIdentifier visitorIdentifier;
    @Column
    private boolean published;
    @Column
    private String officialWebsite;
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
    @Column
    private String dataListField;
    @Column
    private String nameField;
    @Column
    private String imageUrlField;

    public Long getId() {
        return this.id;
    }

    public ApiCategory getCategory() {
        return this.category;
    }

    public VisitorIdentifier getVisitorIdentifier() {
        return this.visitorIdentifier;
    }

    public boolean isPublished() {
        return this.published;
    }

    public String getOfficialWebsite() {
        return this.officialWebsite;
    }

    public String getName() {
        return this.name;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public String getMethod() {
        return this.method;
    }

    public String getPaginationField() {
        return this.paginationField;
    }

    public String getRequestFormat() {
        return this.requestFormat;
    }

    public Boolean getUseBackendProxy() {
        return this.useBackendProxy;
    }

    public List<GalleryConfigParameter> getParameters() {
        return this.parameters;
    }

    public String getDataFormat() {
        return this.dataFormat;
    }

    public String getDataListField() {
        return this.dataListField;
    }

    public String getNameField() {
        return this.nameField;
    }

    public String getImageUrlField() {
        return this.imageUrlField;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCategory(ApiCategory category) {
        this.category = category;
    }

    public void setVisitorIdentifier(VisitorIdentifier visitorIdentifier) {
        this.visitorIdentifier = visitorIdentifier;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public void setOfficialWebsite(String officialWebsite) {
        this.officialWebsite = officialWebsite;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPaginationField(String paginationField) {
        this.paginationField = paginationField;
    }

    public void setRequestFormat(String requestFormat) {
        this.requestFormat = requestFormat;
    }

    public void setUseBackendProxy(Boolean useBackendProxy) {
        this.useBackendProxy = useBackendProxy;
    }

    public void setParameters(List<GalleryConfigParameter> parameters) {
        this.parameters = parameters;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public void setDataListField(String dataListField) {
        this.dataListField = dataListField;
    }

    public void setNameField(String nameField) {
        this.nameField = nameField;
    }

    public void setImageUrlField(String imageUrlField) {
        this.imageUrlField = imageUrlField;
    }
}

