package my.poo.mtgcrawl.mtgprint.entitiy;

import lombok.Data;

@Data
public class PdfProperties {
    private Integer width;
    private Integer height;
    private Integer hmargin;
    private Integer vmargin;
    private boolean cutlines;
    private String cutlineColor;
    private boolean corners;
    private String cornerColor;
}
