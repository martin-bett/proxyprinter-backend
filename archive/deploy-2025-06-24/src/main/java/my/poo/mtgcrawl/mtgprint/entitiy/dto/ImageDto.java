/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import my.poo.mtgcrawl.mtgprint.entitiy.dto.CropSettingsDto;

public class ImageDto {
    private String imageUrl;
    private CropSettingsDto cropSettings;

    public String getImageUrl() {
        return this.imageUrl;
    }

    public CropSettingsDto getCropSettings() {
        return this.cropSettings;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCropSettings(CropSettingsDto cropSettings) {
        this.cropSettings = cropSettings;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ImageDto)) {
            return false;
        }
        ImageDto other = (ImageDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        String this$imageUrl = this.getImageUrl();
        String other$imageUrl = other.getImageUrl();
        if (this$imageUrl == null ? other$imageUrl != null : !this$imageUrl.equals(other$imageUrl)) {
            return false;
        }
        CropSettingsDto this$cropSettings = this.getCropSettings();
        CropSettingsDto other$cropSettings = other.getCropSettings();
        return !(this$cropSettings == null ? other$cropSettings != null : !((Object)this$cropSettings).equals(other$cropSettings));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ImageDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $imageUrl = this.getImageUrl();
        result = result * 59 + ($imageUrl == null ? 43 : $imageUrl.hashCode());
        CropSettingsDto $cropSettings = this.getCropSettings();
        result = result * 59 + ($cropSettings == null ? 43 : ((Object)$cropSettings).hashCode());
        return result;
    }

    public String toString() {
        return "ImageDto(imageUrl=" + this.getImageUrl() + ", cropSettings=" + String.valueOf(this.getCropSettings()) + ")";
    }
}

