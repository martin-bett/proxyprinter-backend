package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
public class CreatePdfDto {
    private List<ImageDto> images = new ArrayList<>(); 
    private PdfProperties settings;
    /** Optional: true schickt den Link an die Adresse des angemeldeten Kontos. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean sendMail;
    /** Wird nur vom Backend aus dem Konto gesetzt, nie vom Client übernommen. An diese Adresse geht ein Download-Link, sobald das PDF fertig ist. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String email;
    /** Optional: language of the mail (ISO 639-1, e.g. "de"); unknown or missing means English. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String language;

}
