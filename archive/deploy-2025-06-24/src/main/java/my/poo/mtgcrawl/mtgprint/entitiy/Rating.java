/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy;

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
import java.time.LocalDateTime;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;

@Entity
@Table(name="ratings", uniqueConstraints={@UniqueConstraint(columnNames={"ip_address", "browser_fingerprint", "api_config_id"})})
public class Rating {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Embedded
    private VisitorIdentifier visitorIdentifier;
    @ManyToOne
    @JoinColumn(name="api_config_id")
    private ApiConfig ratedConfig;
    @Column
    private Integer scoreQuality;
    @Column
    private Integer scoreAvailablity;
    @Column
    private String comment;
    @Column
    private LocalDateTime createdAt;

    public Long getId() {
        return this.id;
    }

    public VisitorIdentifier getVisitorIdentifier() {
        return this.visitorIdentifier;
    }

    public ApiConfig getRatedConfig() {
        return this.ratedConfig;
    }

    public Integer getScoreQuality() {
        return this.scoreQuality;
    }

    public Integer getScoreAvailablity() {
        return this.scoreAvailablity;
    }

    public String getComment() {
        return this.comment;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVisitorIdentifier(VisitorIdentifier visitorIdentifier) {
        this.visitorIdentifier = visitorIdentifier;
    }

    public void setRatedConfig(ApiConfig ratedConfig) {
        this.ratedConfig = ratedConfig;
    }

    public void setScoreQuality(Integer scoreQuality) {
        this.scoreQuality = scoreQuality;
    }

    public void setScoreAvailablity(Integer scoreAvailablity) {
        this.scoreAvailablity = scoreAvailablity;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

