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
    /** Blatt im Querformat (A4 quer). */
    private boolean pageLandscape;
    /** Karten quer aufs Blatt legen (um 90° gedreht); width/height bleiben die Maße der aufrechten Karte. */
    private boolean cardsLandscape;
}
