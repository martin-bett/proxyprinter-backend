package my.poo.mtgcrawl.mtgprint.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.Rating;

// RatingRepository.java
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByUserIdAndRatedConfig(Long userId, ApiConfig item);

    @Query(value = "SELECT r FROM Rating r WHERE r.ratedConfig.id = :configId")
    List<Rating> findAllByConfigId(@Param("configId")Long configId);

    List<Rating> findAllByUserId(Long userId);
}