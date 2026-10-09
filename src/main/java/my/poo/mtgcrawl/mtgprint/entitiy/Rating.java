package my.poo.mtgcrawl.mtgprint.entitiy;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ratings",
uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "api_config_id"}
    ))
@Getter
@Setter
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Embedded
    private VisitorIdentifier visitorIdentifier; // nur alte, anonyme Ratings

    // Konto, das bewertet hat; alte Ratings aus der Fingerprint-Zeit haben keins
    @Column(name = "user_id")
    private Long userId;
    
    @ManyToOne
    @JoinColumn(name = "api_config_id")
    private ApiConfig ratedConfig;
    
    @Column
    private Integer scoreQuality;
    @Column
    private Integer scoreAvailablity;
    @Column
    private String comment;
    @Column
    private LocalDateTime createdAt;
    
}