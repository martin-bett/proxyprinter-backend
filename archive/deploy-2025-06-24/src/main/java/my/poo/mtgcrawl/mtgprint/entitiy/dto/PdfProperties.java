/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

public class PdfProperties {
    private Integer width;
    private Integer height;
    private Integer hmargin;
    private Integer vmargin;
    private Integer maxDpi;
    private boolean cutlines;
    private Integer cutlineThickness;
    private String cutlineColor;
    private boolean corners;
    private boolean cornerColorAuto;
    private String cornerColor;

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }

    public Integer getHmargin() {
        return this.hmargin;
    }

    public Integer getVmargin() {
        return this.vmargin;
    }

    public Integer getMaxDpi() {
        return this.maxDpi;
    }

    public boolean isCutlines() {
        return this.cutlines;
    }

    public Integer getCutlineThickness() {
        return this.cutlineThickness;
    }

    public String getCutlineColor() {
        return this.cutlineColor;
    }

    public boolean isCorners() {
        return this.corners;
    }

    public boolean isCornerColorAuto() {
        return this.cornerColorAuto;
    }

    public String getCornerColor() {
        return this.cornerColor;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public void setHmargin(Integer hmargin) {
        this.hmargin = hmargin;
    }

    public void setVmargin(Integer vmargin) {
        this.vmargin = vmargin;
    }

    public void setMaxDpi(Integer maxDpi) {
        this.maxDpi = maxDpi;
    }

    public void setCutlines(boolean cutlines) {
        this.cutlines = cutlines;
    }

    public void setCutlineThickness(Integer cutlineThickness) {
        this.cutlineThickness = cutlineThickness;
    }

    public void setCutlineColor(String cutlineColor) {
        this.cutlineColor = cutlineColor;
    }

    public void setCorners(boolean corners) {
        this.corners = corners;
    }

    public void setCornerColorAuto(boolean cornerColorAuto) {
        this.cornerColorAuto = cornerColorAuto;
    }

    public void setCornerColor(String cornerColor) {
        this.cornerColor = cornerColor;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PdfProperties)) {
            return false;
        }
        PdfProperties other = (PdfProperties)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.isCutlines() != other.isCutlines()) {
            return false;
        }
        if (this.isCorners() != other.isCorners()) {
            return false;
        }
        if (this.isCornerColorAuto() != other.isCornerColorAuto()) {
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
        Integer this$hmargin = this.getHmargin();
        Integer other$hmargin = other.getHmargin();
        if (this$hmargin == null ? other$hmargin != null : !((Object)this$hmargin).equals(other$hmargin)) {
            return false;
        }
        Integer this$vmargin = this.getVmargin();
        Integer other$vmargin = other.getVmargin();
        if (this$vmargin == null ? other$vmargin != null : !((Object)this$vmargin).equals(other$vmargin)) {
            return false;
        }
        Integer this$maxDpi = this.getMaxDpi();
        Integer other$maxDpi = other.getMaxDpi();
        if (this$maxDpi == null ? other$maxDpi != null : !((Object)this$maxDpi).equals(other$maxDpi)) {
            return false;
        }
        Integer this$cutlineThickness = this.getCutlineThickness();
        Integer other$cutlineThickness = other.getCutlineThickness();
        if (this$cutlineThickness == null ? other$cutlineThickness != null : !((Object)this$cutlineThickness).equals(other$cutlineThickness)) {
            return false;
        }
        String this$cutlineColor = this.getCutlineColor();
        String other$cutlineColor = other.getCutlineColor();
        if (this$cutlineColor == null ? other$cutlineColor != null : !this$cutlineColor.equals(other$cutlineColor)) {
            return false;
        }
        String this$cornerColor = this.getCornerColor();
        String other$cornerColor = other.getCornerColor();
        return !(this$cornerColor == null ? other$cornerColor != null : !this$cornerColor.equals(other$cornerColor));
    }

    protected boolean canEqual(Object other) {
        return other instanceof PdfProperties;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isCutlines() ? 79 : 97);
        result = result * 59 + (this.isCorners() ? 79 : 97);
        result = result * 59 + (this.isCornerColorAuto() ? 79 : 97);
        Integer $width = this.getWidth();
        result = result * 59 + ($width == null ? 43 : ((Object)$width).hashCode());
        Integer $height = this.getHeight();
        result = result * 59 + ($height == null ? 43 : ((Object)$height).hashCode());
        Integer $hmargin = this.getHmargin();
        result = result * 59 + ($hmargin == null ? 43 : ((Object)$hmargin).hashCode());
        Integer $vmargin = this.getVmargin();
        result = result * 59 + ($vmargin == null ? 43 : ((Object)$vmargin).hashCode());
        Integer $maxDpi = this.getMaxDpi();
        result = result * 59 + ($maxDpi == null ? 43 : ((Object)$maxDpi).hashCode());
        Integer $cutlineThickness = this.getCutlineThickness();
        result = result * 59 + ($cutlineThickness == null ? 43 : ((Object)$cutlineThickness).hashCode());
        String $cutlineColor = this.getCutlineColor();
        result = result * 59 + ($cutlineColor == null ? 43 : $cutlineColor.hashCode());
        String $cornerColor = this.getCornerColor();
        result = result * 59 + ($cornerColor == null ? 43 : $cornerColor.hashCode());
        return result;
    }

    public String toString() {
        return "PdfProperties(width=" + this.getWidth() + ", height=" + this.getHeight() + ", hmargin=" + this.getHmargin() + ", vmargin=" + this.getVmargin() + ", maxDpi=" + this.getMaxDpi() + ", cutlines=" + this.isCutlines() + ", cutlineThickness=" + this.getCutlineThickness() + ", cutlineColor=" + this.getCutlineColor() + ", corners=" + this.isCorners() + ", cornerColorAuto=" + this.isCornerColorAuto() + ", cornerColor=" + this.getCornerColor() + ")";
    }
}

