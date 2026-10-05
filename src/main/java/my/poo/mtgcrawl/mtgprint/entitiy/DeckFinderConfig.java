package my.poo.mtgcrawl.mtgprint.entitiy;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

/**
 * Beschreibt, wie ein Deck von einer Deckbuilder-Seite (Archidekt, TappedOut ...) geladen wird:
 * Link erkennen, Abruf-URL bauen, Felder der Antwort lesen. Das Auslesen passiert im Frontend.
 */
@Entity
@Table(name = "deckFinderConfig")
@Getter
@Setter
public class DeckFinderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Info
    @Column(columnDefinition = "api_category")
    @Enumerated(EnumType.STRING)
    @JdbcType(value = PostgreSQLEnumJdbcType.class)
    private ApiCategory category;

    @Embedded
    private VisitorIdentifier visitorIdentifier;

    @Column
    private boolean published;

    @Column
    private String officialWebsite;
    @Column
    private String name;

    // Abruf: Regex mit Gruppe "id" erkennt Links, fetchUrl enthält {id}
    @Column
    private String linkPattern;
    @Column
    private String fetchUrl;
    // json oder text
    @Column
    private String dataFormat;

    // Datalocator (nur json)
    @Column
    private String deckNameField;
    // Komma-getrennte Pfade, "*" = alle Werte eines Objekts, optional "Board=pfad"
    @Column
    private String entryListFields;
    @Column
    private String quantityField;
    @Column
    private String nameField;
    @Column
    private String setField;
    @Column
    private String numberField;
    @Column
    private String boardField;
    // Boards, die nur auf Wunsch übernommen werden, z. B. "Sideboard, Maybeboard"
    @Column
    private String optionalBoards;

    // Suche per Name (optional), searchUrl enthält {query} und optional {page}
    @Column
    private String searchUrl;
    @Column
    private String searchListField;
    @Column
    private String searchIdField;
    @Column
    private String searchNameField;
    @Column
    private String searchOwnerField;
    @Column
    private String searchImageField;
    @Column
    private String searchInfoField;
}
