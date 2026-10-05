package my.poo.mtgcrawl.mtgprint.entitiy;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Eine Liste verbundener Karten in einem Treffer (z.B. Scryfall all_parts: Tokens, Emblems, Dungeons).
 * Jeder Eintrag der Liste wird eine eigene Karte, gesucht über alle aktiven APIs mit dem Namen.
 * Eine Config kann mehrere Listen haben, position gibt die Reihenfolge.
 */
@Entity
@Getter
@Setter
@Table(name = "apiConfigConnectedCard")
public class ApiConfigConnectedCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "config_id", nullable = false)
    private ApiConfig config;

    @Column
    private Integer position;

    @Column
    private String dataListField;

    @Column
    private String nameField;

    @Column
    private String linkField;

    @Column
    private String typeField;
}
