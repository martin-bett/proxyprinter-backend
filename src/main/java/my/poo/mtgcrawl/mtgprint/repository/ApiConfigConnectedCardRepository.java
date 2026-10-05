package my.poo.mtgcrawl.mtgprint.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigConnectedCard;

@Repository
public interface ApiConfigConnectedCardRepository extends JpaRepository<ApiConfigConnectedCard, Long> {

    @Modifying
    @Query(value = "DELETE FROM proxymaster.api_config_connected_card WHERE config_id IN (:ids)", nativeQuery = true)
    int deleteByConfigIds(@Param("ids") Collection<Long> ids);
}
