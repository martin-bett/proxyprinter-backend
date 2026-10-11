package my.poo.mtgcrawl.mtgprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

import jakarta.annotation.Nonnull;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;

@Repository
public interface GalleryConfigRepository extends JpaRepository<GalleryConfig, Long> {

    // userId null (nicht angemeldet): nur veröffentlichte
    @Query(value=" SELECT gal FROM GalleryConfig gal LEFT JOIN FETCH gal.parameters pM WHERE gal.published = true OR gal.userId = :userId")
    @Nonnull List<GalleryConfig> findAllVisible(@Param("userId") Long userId);

    @Query(value = """
        INSERT INTO proxymaster.gallery_config (name, category, ip_address, user_id)
        VALUES (:name, CAST(:cate AS proxymaster.api_category), :ip, :userId)
        RETURNING id
    """, nativeQuery = true)
    Long insertGalleryConfig(
        @Param("name") String name,
        @Param("cate") String cate,
        @Param("ip") String ip,
        @Param("userId") Long userId
    );

    @Query(
        value = """
            WITH new_config AS (
                INSERT INTO proxymaster.gallery_config (
                    category, official_website, name,  method, request_format, base_url, 
                    data_format, pagination_field, use_backend_proxy, data_list_field, name_field,  image_url_field,
                     ip_address, user_id, published, default_language,
                     link_pattern, follow_input_redirects, credential, image_url_pattern, image_url_replacement, name_pattern, name_replacement,
                     hint, hint_translations
                )
                SELECT category, official_website, name || '_copy',  method, request_format, base_url,
                        data_format, pagination_field, use_backend_proxy, data_list_field, name_field,  image_url_field,
                        :ipAddress, :userId, FALSE, default_language,
                        link_pattern, follow_input_redirects, credential, image_url_pattern, image_url_replacement, name_pattern, name_replacement,
                        hint, hint_translations
                FROM proxymaster.gallery_config
                WHERE id = :id
                RETURNING id
            )
            INSERT INTO proxymaster.gallery_config_parameter (gallery_id, name,category, value)
            SELECT (SELECT id FROM new_config), name,category, value
            FROM proxymaster.gallery_config_parameter
            WHERE gallery_id = :id
            RETURNING (SELECT id FROM new_config)
            """,
        nativeQuery = true
    )
    List<Long> copyGalleryConfig(@Param("id") Long id, @Param("ipAddress") String ipAddress, @Param("userId") Long userId);

    @Query(value = "SELECT id FROM proxymaster.gallery_config WHERE user_id = :userId AND published = false", nativeQuery = true)
    List<Long> findUnpublishedIdsOf(@Param("userId") Long userId);

    @Modifying
    @Query(value = "DELETE FROM proxymaster.gallery_config_parameter WHERE gallery_id IN (:ids)", nativeQuery = true)
    int deleteParametersOf(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query(value = "DELETE FROM proxymaster.gallery_config WHERE id IN (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") Collection<Long> ids);
}
