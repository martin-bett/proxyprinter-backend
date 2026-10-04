/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.ImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.PdfProperties;

public class CreatePdfDto
implements Serializable {
    private List<ImageDto> images = new ArrayList<ImageDto>();
    private PdfProperties settings;
    private String emailOnFinish;

    public List<ImageDto> getImages() {
        return this.images;
    }

    public PdfProperties getSettings() {
        return this.settings;
    }

    public String getEmailOnFinish() {
        return this.emailOnFinish;
    }

    public void setImages(List<ImageDto> images) {
        this.images = images;
    }

    public void setSettings(PdfProperties settings) {
        this.settings = settings;
    }

    public void setEmailOnFinish(String emailOnFinish) {
        this.emailOnFinish = emailOnFinish;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CreatePdfDto)) {
            return false;
        }
        CreatePdfDto other = (CreatePdfDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        List<ImageDto> this$images = this.getImages();
        List<ImageDto> other$images = other.getImages();
        if (this$images == null ? other$images != null : !((Object)this$images).equals(other$images)) {
            return false;
        }
        PdfProperties this$settings = this.getSettings();
        PdfProperties other$settings = other.getSettings();
        if (this$settings == null ? other$settings != null : !((Object)this$settings).equals(other$settings)) {
            return false;
        }
        String this$emailOnFinish = this.getEmailOnFinish();
        String other$emailOnFinish = other.getEmailOnFinish();
        return !(this$emailOnFinish == null ? other$emailOnFinish != null : !this$emailOnFinish.equals(other$emailOnFinish));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CreatePdfDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        List<ImageDto> $images = this.getImages();
        result = result * 59 + ($images == null ? 43 : ((Object)$images).hashCode());
        PdfProperties $settings = this.getSettings();
        result = result * 59 + ($settings == null ? 43 : ((Object)$settings).hashCode());
        String $emailOnFinish = this.getEmailOnFinish();
        result = result * 59 + ($emailOnFinish == null ? 43 : $emailOnFinish.hashCode());
        return result;
    }

    public String toString() {
        return "CreatePdfDto(images=" + String.valueOf(this.getImages()) + ", settings=" + String.valueOf(this.getSettings()) + ", emailOnFinish=" + this.getEmailOnFinish() + ")";
    }
}

