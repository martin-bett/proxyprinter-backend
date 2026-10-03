/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

public class CropSettingsDto {
    private Integer width;
    private Integer height;
    private Integer offSetX;
    private Integer offSetY;
    private Integer padding;

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }

    public Integer getOffSetX() {
        return this.offSetX;
    }

    public Integer getOffSetY() {
        return this.offSetY;
    }

    public Integer getPadding() {
        return this.padding;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public void setOffSetX(Integer offSetX) {
        this.offSetX = offSetX;
    }

    public void setOffSetY(Integer offSetY) {
        this.offSetY = offSetY;
    }

    public void setPadding(Integer padding) {
        this.padding = padding;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof CropSettingsDto)) {
            return false;
        }
        CropSettingsDto other = (CropSettingsDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$width = this.getWidth();
        Integer other$width = other.getWidth();
        if (this$width == null ? other$width != null : !((Object)this$width).equals(other$width)) {
            return false;
        }
        Integer this$height = this.getHeight();
        Integer other$height = other.getHeight();
        if (this$height == null ? other$height != null : !((Object)this$height).equals(other$height)) {
            return false;
        }
        Integer this$offSetX = this.getOffSetX();
        Integer other$offSetX = other.getOffSetX();
        if (this$offSetX == null ? other$offSetX != null : !((Object)this$offSetX).equals(other$offSetX)) {
            return false;
        }
        Integer this$offSetY = this.getOffSetY();
        Integer other$offSetY = other.getOffSetY();
        if (this$offSetY == null ? other$offSetY != null : !((Object)this$offSetY).equals(other$offSetY)) {
            return false;
        }
        Integer this$padding = this.getPadding();
        Integer other$padding = other.getPadding();
        return !(this$padding == null ? other$padding != null : !((Object)this$padding).equals(other$padding));
    }

    protected boolean canEqual(Object other) {
        return other instanceof CropSettingsDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $width = this.getWidth();
        result = result * 59 + ($width == null ? 43 : ((Object)$width).hashCode());
        Integer $height = this.getHeight();
        result = result * 59 + ($height == null ? 43 : ((Object)$height).hashCode());
        Integer $offSetX = this.getOffSetX();
        result = result * 59 + ($offSetX == null ? 43 : ((Object)$offSetX).hashCode());
        Integer $offSetY = this.getOffSetY();
        result = result * 59 + ($offSetY == null ? 43 : ((Object)$offSetY).hashCode());
        Integer $padding = this.getPadding();
        result = result * 59 + ($padding == null ? 43 : ((Object)$padding).hashCode());
        return result;
    }

    public String toString() {
        return "CropSettingsDto(width=" + this.getWidth() + ", height=" + this.getHeight() + ", offSetX=" + this.getOffSetX() + ", offSetY=" + this.getOffSetY() + ", padding=" + this.getPadding() + ")";
    }
}

