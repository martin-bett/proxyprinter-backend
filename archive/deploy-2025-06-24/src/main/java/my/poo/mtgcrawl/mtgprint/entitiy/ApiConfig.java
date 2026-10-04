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
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigParameter;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name="apiConfig")
public class ApiConfig {
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
    @Column
    private String csrfTokenVariableName;
    @Column
    private Boolean requiresCsrfToken;
    @Column
    private Boolean useBackendProxy;
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
    @Column
    private String followLinkField;
    @Column
    private String followLinkDataListField;
    @Column
    private String followLinkImageField;
    @Column
    private String followLinkLanguageField;
    @Column
    private String nestedLanguageDataListField;
    @Column
    private String nestedLanguageImageUrlField;
    @Column
    private String nestedLanguageLanguageField;

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

    public String getMethod() {
        return this.method;
    }

    public String getRequestFormat() {
        return this.requestFormat;
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public String getPaginationLinkField() {
        return this.paginationLinkField;
    }

    public String getPaginationMaxPageField() {
        return this.paginationMaxPageField;
    }

    public Boolean getCustomArts() {
        return this.customArts;
    }

    public List<ApiConfigParameter> getParameterMap() {
        return this.parameterMap;
    }

    public Integer getConcurrentLimit() {
        return this.concurrentLimit;
    }

    public String getDataFormat() {
        return this.dataFormat;
    }

    public String getCsrfTokenVariableName() {
        return this.csrfTokenVariableName;
    }

    public Boolean getRequiresCsrfToken() {
        return this.requiresCsrfToken;
    }

    public Boolean getUseBackendProxy() {
        return this.useBackendProxy;
    }

    public String getDataListField() {
        return this.dataListField;
    }

    public String getNameField() {
        return this.nameField;
    }

    public String getNameSplitCharacter() {
        return this.nameSplitCharacter;
    }

    public String getImageUrlField() {
        return this.imageUrlField;
    }

    public String getLanguageField() {
        return this.languageField;
    }

    public String getSetField() {
        return this.setField;
    }

    public String getSetNumberField() {
        return this.setNumberField;
    }

    public String getFollowLinkField() {
        return this.followLinkField;
    }

    public String getFollowLinkDataListField() {
        return this.followLinkDataListField;
    }

    public String getFollowLinkImageField() {
        return this.followLinkImageField;
    }

    public String getFollowLinkLanguageField() {
        return this.followLinkLanguageField;
    }

    public String getNestedLanguageDataListField() {
        return this.nestedLanguageDataListField;
    }

    public String getNestedLanguageImageUrlField() {
        return this.nestedLanguageImageUrlField;
    }

    public String getNestedLanguageLanguageField() {
        return this.nestedLanguageLanguageField;
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

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRequestFormat(String requestFormat) {
        this.requestFormat = requestFormat;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setPaginationLinkField(String paginationLinkField) {
        this.paginationLinkField = paginationLinkField;
    }

    public void setPaginationMaxPageField(String paginationMaxPageField) {
        this.paginationMaxPageField = paginationMaxPageField;
    }

    public void setCustomArts(Boolean customArts) {
        this.customArts = customArts;
    }

    public void setParameterMap(List<ApiConfigParameter> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public void setConcurrentLimit(Integer concurrentLimit) {
        this.concurrentLimit = concurrentLimit;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public void setCsrfTokenVariableName(String csrfTokenVariableName) {
        this.csrfTokenVariableName = csrfTokenVariableName;
    }

    public void setRequiresCsrfToken(Boolean requiresCsrfToken) {
        this.requiresCsrfToken = requiresCsrfToken;
    }

    public void setUseBackendProxy(Boolean useBackendProxy) {
        this.useBackendProxy = useBackendProxy;
    }

    public void setDataListField(String dataListField) {
        this.dataListField = dataListField;
    }

    public void setNameField(String nameField) {
        this.nameField = nameField;
    }

    public void setNameSplitCharacter(String nameSplitCharacter) {
        this.nameSplitCharacter = nameSplitCharacter;
    }

    public void setImageUrlField(String imageUrlField) {
        this.imageUrlField = imageUrlField;
    }

    public void setLanguageField(String languageField) {
        this.languageField = languageField;
    }

    public void setSetField(String setField) {
        this.setField = setField;
    }

    public void setSetNumberField(String setNumberField) {
        this.setNumberField = setNumberField;
    }

    public void setFollowLinkField(String followLinkField) {
        this.followLinkField = followLinkField;
    }

    public void setFollowLinkDataListField(String followLinkDataListField) {
        this.followLinkDataListField = followLinkDataListField;
    }

    public void setFollowLinkImageField(String followLinkImageField) {
        this.followLinkImageField = followLinkImageField;
    }

    public void setFollowLinkLanguageField(String followLinkLanguageField) {
        this.followLinkLanguageField = followLinkLanguageField;
    }

    public void setNestedLanguageDataListField(String nestedLanguageDataListField) {
        this.nestedLanguageDataListField = nestedLanguageDataListField;
    }

    public void setNestedLanguageImageUrlField(String nestedLanguageImageUrlField) {
        this.nestedLanguageImageUrlField = nestedLanguageImageUrlField;
    }

    public void setNestedLanguageLanguageField(String nestedLanguageLanguageField) {
        this.nestedLanguageLanguageField = nestedLanguageLanguageField;
    }
}

