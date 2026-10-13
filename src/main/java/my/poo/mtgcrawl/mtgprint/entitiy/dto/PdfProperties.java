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
    /** Blatt im Querformat (A4 quer). NON_DEFAULT, damit der Request-Hash (PDF-Cache) ohne Querformat gleich bleibt. */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean pageLandscape;
    /** Karten quer aufs Blatt legen (um 90° gedreht); width/height bleiben die Maße der aufrechten Karte. */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean cardsLandscape;
    /**
     * Passmarken für Schneideplotter (Print and Cut); null = keine. Mit Passmarken bleiben die Ecken frei,
     * das Raster rückt dafür ein und Schnittlinien entfallen (sie würden durch die Marken laufen).
     * NON_NULL, damit der Request-Hash (PDF-Cache) ohne Passmarken gleich bleibt.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private RegistrationMarkStyle registrationMarks;
}
