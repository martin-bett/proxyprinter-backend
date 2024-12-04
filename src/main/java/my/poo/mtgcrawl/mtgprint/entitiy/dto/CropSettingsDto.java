package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;

@Data
public class CropSettingsDto {
    private Integer width;
    private Integer height;
    private Integer offSetX;
    private Integer offSetY;
    private Integer padding;
}
