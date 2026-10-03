/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import java.util.Map;

public class GalleryConfigDto {
    private Long id;
    private String category;
    private boolean published;
    private String officialWebsite;
    private String name;
    private String baseUrl;
    private Map<String, Map<String, String>> parameterMap;
    private String dataFormat;
    private String paginationField;
    private String dataListField;
    private String nameField;
    private String imageUrlField;
    private String method;
    private String requestFormat;
    private Boolean useBackendProxy;

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GalleryConfigDto)) {
            return false;
        }
        GalleryConfigDto other = (GalleryConfigDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isPublished() != other.isPublished()) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Boolean this$useBackendProxy = this.getUseBackendProxy();
        Boolean other$useBackendProxy = other.getUseBackendProxy();
        if (this$useBackendProxy == null ? other$useBackendProxy != null : !((Object)this$useBackendProxy).equals(other$useBackendProxy)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        String this$officialWebsite = this.getOfficialWebsite();
        String other$officialWebsite = other.getOfficialWebsite();
        if (this$officialWebsite == null ? other$officialWebsite != null : !this$officialWebsite.equals(other$officialWebsite)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$baseUrl = this.getBaseUrl();
        String other$baseUrl = other.getBaseUrl();
        if (this$baseUrl == null ? other$baseUrl != null : !this$baseUrl.equals(other$baseUrl)) {
            return false;
        }
        Map<String, Map<String, String>> this$parameterMap = this.getParameterMap();
        Map<String, Map<String, String>> other$parameterMap = other.getParameterMap();
        if (this$parameterMap == null ? other$parameterMap != null : !((Object)this$parameterMap).equals(other$parameterMap)) {
            return false;
        }
        String this$dataFormat = this.getDataFormat();
        String other$dataFormat = other.getDataFormat();
        if (this$dataFormat == null ? other$dataFormat != null : !this$dataFormat.equals(other$dataFormat)) {
            return false;
        }
        String this$paginationField = this.getPaginationField();
        String other$paginationField = other.getPaginationField();
        if (this$paginationField == null ? other$paginationField != null : !this$paginationField.equals(other$paginationField)) {
            return false;
        }
        String this$dataListField = this.getDataListField();
        String other$dataListField = other.getDataListField();
        if (this$dataListField == null ? other$dataListField != null : !this$dataListField.equals(other$dataListField)) {
            return false;
        }
        String this$nameField = this.getNameField();
        String other$nameField = other.getNameField();
        if (this$nameField == null ? other$nameField != null : !this$nameField.equals(other$nameField)) {
            return false;
        }
        String this$imageUrlField = this.getImageUrlField();
        String other$imageUrlField = other.getImageUrlField();
        if (this$imageUrlField == null ? other$imageUrlField != null : !this$imageUrlField.equals(other$imageUrlField)) {
            return false;
        }
        String this$method = this.getMethod();
        String other$method = other.getMethod();
        if (this$method == null ? other$method != null : !this$method.equals(other$method)) {
            return false;
        }
        String this$requestFormat = this.getRequestFormat();
        String other$requestFormat = other.getRequestFormat();
        return !(this$requestFormat == null ? other$requestFormat != null : !this$requestFormat.equals(other$requestFormat));
    }

    protected boolean canEqual(Object other) {
        return other instanceof GalleryConfigDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isPublished() ? 79 : 97);
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Boolean $useBackendProxy = this.getUseBackendProxy();
        result = result * 59 + ($useBackendProxy == null ? 43 : ((Object)$useBackendProxy).hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $officialWebsite = this.getOfficialWebsite();
        result = result * 59 + ($officialWebsite == null ? 43 : $officialWebsite.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $baseUrl = this.getBaseUrl();
        result = result * 59 + ($baseUrl == null ? 43 : $baseUrl.hashCode());
        Map<String, Map<String, String>> $parameterMap = this.getParameterMap();
        result = result * 59 + ($parameterMap == null ? 43 : ((Object)$parameterMap).hashCode());
        String $dataFormat = this.getDataFormat();
        result = result * 59 + ($dataFormat == null ? 43 : $dataFormat.hashCode());
        String $paginationField = this.getPaginationField();
        result = result * 59 + ($paginationField == null ? 43 : $paginationField.hashCode());
        String $dataListField = this.getDataListField();
        result = result * 59 + ($dataListField == null ? 43 : $dataListField.hashCode());
        String $nameField = this.getNameField();
        result = result * 59 + ($nameField == null ? 43 : $nameField.hashCode());
        String $imageUrlField = this.getImageUrlField();
        result = result * 59 + ($imageUrlField == null ? 43 : $imageUrlField.hashCode());
        String $method = this.getMethod();
        result = result * 59 + ($method == null ? 43 : $method.hashCode());
        String $requestFormat = this.getRequestFormat();
        result = result * 59 + ($requestFormat == null ? 43 : $requestFormat.hashCode());
        return result;
    }

    public String toString() {
        return "GalleryConfigDto(id=" + this.getId() + ", category=" + this.getCategory() + ", published=" + this.isPublished() + ", officialWebsite=" + this.getOfficialWebsite() + ", name=" + this.getName() + ", baseUrl=" + this.getBaseUrl() + ", parameterMap=" + String.valueOf(this.getParameterMap()) + ", dataFormat=" + this.getDataFormat() + ", paginationField=" + this.getPaginationField() + ", dataListField=" + this.getDataListField() + ", nameField=" + this.getNameField() + ", imageUrlField=" + this.getImageUrlField() + ", method=" + this.getMethod() + ", requestFormat=" + this.getRequestFormat() + ", useBackendProxy=" + this.getUseBackendProxy() + ")";
    }

    public Long getId() {
        return this.id;
    }

    public String getCategory() {
        return this.category;
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

    public Map<String, Map<String, String>> getParameterMap() {
        return this.parameterMap;
    }

    public String getDataFormat() {
        return this.dataFormat;
    }

    public String getPaginationField() {
        return this.paginationField;
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

    public String getMethod() {
        return this.method;
    }

    public String getRequestFormat() {
        return this.requestFormat;
    }

    public Boolean getUseBackendProxy() {
        return this.useBackendProxy;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public void setParameterMap(Map<String, Map<String, String>> parameterMap) {
        this.parameterMap = parameterMap;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public void setPaginationField(String paginationField) {
        this.paginationField = paginationField;
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

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRequestFormat(String requestFormat) {
        this.requestFormat = requestFormat;
    }

    public void setUseBackendProxy(Boolean useBackendProxy) {
        this.useBackendProxy = useBackendProxy;
    }
}

