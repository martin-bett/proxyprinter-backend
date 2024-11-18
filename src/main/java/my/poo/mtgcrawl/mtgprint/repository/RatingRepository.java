package my.poo.mtgcrawl.mtgprint.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;

// RatingRepository.java
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByVisitorIdentifierAndRatedConfig(VisitorIdentifier visitorIdentifier, ApiConfig item);

    @Query(value = "SELECT r FROM Rating r WHERE r.ratedConfig.id = :configId")
    List<Rating> findAllByConfigId(@Param("configId")Long configId);

    @Query(value = "SELECT r FROM Rating r WHERE r.visitorIdentifier = :vId")
    List<Rating> findAllByVisitorIdentifier(@Param("vId")VisitorIdentifier vId);
}