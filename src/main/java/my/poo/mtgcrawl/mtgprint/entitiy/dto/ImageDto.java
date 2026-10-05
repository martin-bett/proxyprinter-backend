package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class ImageDto {
    private String imageUrl;
    private CropSettingsDto cropSettings;
    /** Original-URL einer heruntergeladenen Datei, nur intern für Fehlermeldungen und den Download-Cache. */
    @JsonIgnore
    private String sourceUrl;
    
}
