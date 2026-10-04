package my.poo.mtgcrawl.mtgprint.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.annotation.Nonnull;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;

@Repository
public interface ApiConfigRepository extends JpaRepository<ApiConfig, Long> {

    @Query(value=" SELECT api FROM ApiConfig api LEFT JOIN FETCH api.parameterMap pM WHERE api.published = true OR api.visitorIdentifier = :vi")
    @Nonnull List<ApiConfig> findAllVisible(@Param("vi") VisitorIdentifier vi);


    @Query(value = """
        INSERT INTO proxymaster.api_config (name, category, ip_address, browser_fingerprint, last_used) 
        VALUES (:name, CAST(:cate AS proxymaster.api_category), :ip, :footprint, now())
        RETURNING id
    """, nativeQuery = true)
    Long insertApiConfig(
        @Param("name") String name,
        @Param("cate") String cate,
        @Param("ip") String ip,
        @Param("footprint") String footprint
    );


    @Query(
        value = """
            WITH new_config AS (
                INSERT INTO proxymaster.api_config (
                    category, official_website, name, custom_arts, method, request_format, base_url, concurrent_limit,
                    data_format,pagination_max_page_field, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy,
                    data_list_field, name_field, name_split_character, image_url_field, language_field, set_field,
                    set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field,
                    follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field,
                    nested_language_language_field, ip_address, browser_fingerprint, published, proxy_images, last_used,
                    card_faces_data_list_field, card_faces_image_url_field, card_faces_name_field, default_language
                )
                SELECT category, official_website, name || '_copy', custom_arts, method, request_format, base_url,
                       concurrent_limit, data_format,pagination_max_page_field, pagination_link_field, csrf_token_variable_name, requires_csrf_token,
                       use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field,
                       language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field,
                       follow_link_image_field, follow_link_language_field, nested_language_data_list_field,
                       nested_language_image_url_field, nested_language_language_field, :ipAddress, :browserFingerprint, FALSE, proxy_images, now(),
                       card_faces_data_list_field, card_faces_image_url_field, card_faces_name_field, default_language
                FROM proxymaster.api_config
                WHERE id = :id
                RETURNING id
            )
            , new_parameters AS (
                INSERT INTO proxymaster.api_config_parameter (config_id, name,category, value)
                SELECT (SELECT id FROM new_config), name,category, value
                FROM proxymaster.api_config_parameter
                WHERE config_id = :id
            )
            -- Id auch dann liefern, wenn die Quelle keine Parameter hat
            SELECT id FROM new_config
            """,
        nativeQuery = true
    )
    List<Long> copyApiConfig(
        @Param("id") Long id,
        @Param("ipAddress") String ipAddress,
        @Param("browserFingerprint") String browserFingerprint
    );

    @Modifying
    @Query(value = "UPDATE proxymaster.api_config SET last_used = now() WHERE id IN (:ids)", nativeQuery = true)
    int markUsed(@Param("ids") Collection<Long> ids);

    // Nur Configs mit Ersteller: mitgelieferte Standard-Configs haben keine ip_address und bleiben immer.
    // last_used NULL (Spalte noch nicht befüllt) zählt nicht als abgelaufen.
    @Query(value = """
        SELECT id FROM proxymaster.api_config
        WHERE ip_address IS NOT NULL AND last_used IS NOT NULL AND last_used < :cutoff
        """, nativeQuery = true)
    List<Long> findUnusedSince(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query(value = "DELETE FROM proxymaster.api_config_parameter WHERE config_id IN (:ids)", nativeQuery = true)
    int deleteParametersOf(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query(value = "DELETE FROM proxymaster.ratings WHERE api_config_id IN (:ids)", nativeQuery = true)
    int deleteRatingsOf(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query(value = "DELETE FROM proxymaster.api_config WHERE id IN (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") Collection<Long> ids);
}
