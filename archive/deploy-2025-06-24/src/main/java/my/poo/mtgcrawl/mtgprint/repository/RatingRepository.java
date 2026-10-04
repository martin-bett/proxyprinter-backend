/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.repository;

import java.util.List;
import java.util.Optional;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RatingRepository
extends JpaRepository<Rating, Long> {
    public Optional<Rating> findByVisitorIdentifierAndRatedConfig(VisitorIdentifier var1, ApiConfig var2);

    @Query(value="SELECT r FROM Rating r WHERE r.ratedConfig.id = :configId")
    public List<Rating> findAllByConfigId(@Param(value="configId") Long var1);

    @Query(value="SELECT r FROM Rating r WHERE r.visitorIdentifier = :vId")
    public List<Rating> findAllByVisitorIdentifier(@Param(value="vId") VisitorIdentifier var1);
}

