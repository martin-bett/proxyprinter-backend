package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class PdfProperties {
    /** Kartenbreite in mm, Kommawerte erlaubt (echte Karten: 63,5). */
    private double width;
    /** Kartenhöhe in mm, Kommawerte erlaubt (echte Karten: 88,9). */
    private double height;
    /** Abstand zwischen den Karten in mm. */
    private double hmargin;
    private double vmargin;
    private Integer maxDpi;
    private boolean cutlines;
    private Integer cutlineThickness;
    private String cutlineColor;
    private boolean corners;
    private boolean cornerColorAuto;
    private String cornerColor;
    /**
     * Radius der Eckmarken in mm. Leer = automatisch (1/8 Zoll bei echter Kartengröße, wächst mit der Kartenhöhe).
     * NON_NULL, damit der Request-Hash (PDF-Cache) ohne den Wert gleich bleibt.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double cornerSize;
}
