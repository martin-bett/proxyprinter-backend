package my.poo.mtgcrawl.mtgprint.entitiy;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CreatePdfDto {
    private List<String> imageUrls = new ArrayList<>(); 
    private PdfProperties settings;
}