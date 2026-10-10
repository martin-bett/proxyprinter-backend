package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

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
    /**
     * Radius der Eckmarken in mm. Leer = automatisch (3 % der Kartenhöhe, wie bisher).
     * NON_NULL, damit der Request-Hash (PDF-Cache) ohne den Wert gleich bleibt.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double cornerSize;
}
