package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;

@Data
public class RatingDto {
    private Long configId;
    private Double scoreAvailability;
    private Double scoreQuality;
}
