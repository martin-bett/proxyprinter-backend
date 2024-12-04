package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;

@Data
public class ImageDto {
    private String imageUrl;
    private CropSettingsDto cropSettings;
    
}
