package my.poo.mtgcrawl.mtgprint.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Nonnull;
import my.poo.mtgcrawl.mtgprint.entitiy.DeckFinderConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;

@Repository
public interface DeckFinderConfigRepository extends JpaRepository<DeckFinderConfig, Long> {

    @Query("SELECT d FROM DeckFinderConfig d WHERE d.published = true OR d.visitorIdentifier = :vi")
    @Nonnull List<DeckFinderConfig> findAllVisible(@Param("vi") VisitorIdentifier vi);

    @Query(value = """
        INSERT INTO proxymaster.deck_finder_config (name, category, data_format, ip_address, browser_fingerprint)
        VALUES (:name, CAST(:cate AS proxymaster.api_category), 'json', :ip, :footprint)
        RETURNING id
    """, nativeQuery = true)
    Long insertDeckFinderConfig(
        @Param("name") String name,
        @Param("cate") String cate,
        @Param("ip") String ip,
        @Param("footprint") String footprint
    );

    @Query(value = """
        INSERT INTO proxymaster.deck_finder_config (
            category, official_website, name, link_pattern, fetch_url, data_format,
            deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field,
            board_field, optional_boards, search_url, search_list_field, search_id_field,
            search_name_field, search_owner_field, search_image_field, search_info_field,
            ip_address, browser_fingerprint, published
        )
        SELECT category, official_website, name || '_copy', link_pattern, fetch_url, data_format,
            deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field,
            board_field, optional_boards, search_url, search_list_field, search_id_field,
            search_name_field, search_owner_field, search_image_field, search_info_field,
            :ip, :footprint, FALSE
        FROM proxymaster.deck_finder_config
        WHERE id = :id
        RETURNING id
    """, nativeQuery = true)
    List<Long> copyDeckFinderConfig(@Param("id") Long id, @Param("ip") String ip, @Param("footprint") String footprint);
}
