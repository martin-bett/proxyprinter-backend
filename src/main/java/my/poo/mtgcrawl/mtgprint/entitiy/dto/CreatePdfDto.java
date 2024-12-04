package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CreatePdfDto {
    private List<ImageDto> images = new ArrayList<>(); 
    private PdfProperties settings;

}