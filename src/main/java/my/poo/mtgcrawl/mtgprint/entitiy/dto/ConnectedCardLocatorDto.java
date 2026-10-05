package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import lombok.Getter;
import lombok.Setter;

/** Locator für eine Liste verbundener Karten (siehe ApiConfigConnectedCard). */
@Getter
@Setter
public class ConnectedCardLocatorDto {
    private String dataListField;
    private String nameField;
    private String linkField;
    private String typeField;
}
