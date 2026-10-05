package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Data;

@Data
public class DeckFinderConfigDto {
    private Long id;
    private String category;
    private boolean published;
    private String officialWebsite;
    private String name;

    private String linkPattern;
    private String fetchUrl;
    private String dataFormat;

    private String deckNameField;
    private String entryListFields;
    private String quantityField;
    private String nameField;
    private String setField;
    private String numberField;
    private String boardField;
    private String optionalBoards;

    private String searchUrl;
    private String searchListField;
    private String searchIdField;
    private String searchNameField;
    private String searchOwnerField;
    private String searchImageField;
    private String searchInfoField;
}
