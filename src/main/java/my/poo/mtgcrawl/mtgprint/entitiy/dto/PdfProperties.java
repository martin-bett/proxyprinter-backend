package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;

@Data
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
    /** Seite im Querformat; die Karten bleiben aufrecht, nur das Raster passt sich an. */
    private boolean landscape;
}
