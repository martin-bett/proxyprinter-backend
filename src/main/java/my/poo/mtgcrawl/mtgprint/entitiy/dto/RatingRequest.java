package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;

@Data
public class RatingRequest {
    private ApiConfig configId;
    private int score;
    private String comment;
    private String action;
}
