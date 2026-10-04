package my.poo.mtgcrawl.mtgprint.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import jakarta.annotation.Nonnull;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;

@Repository
public interface GalleryConfigRepository extends JpaRepository<GalleryConfig, Long> {

    @Query(value=" SELECT gal FROM GalleryConfig gal LEFT JOIN FETCH gal.parameters pM WHERE gal.published = true OR gal.visitorIdentifier = :vi")
    @Nonnull List<GalleryConfig> findAllVisible(@Param("vi") VisitorIdentifier vi);

    @Query(value = """
        INSERT INTO proxymaster.gallery_config (name, category, ip_address, browser_fingerprint) 
        VALUES (:name, CAST(:cate AS proxymaster.api_category), :ip, :footprint)
        RETURNING id
    """, nativeQuery = true)
    Long insertGalleryConfig(
        @Param("name") String name,
        @Param("cate") String cate,
        @Param("ip") String ip,
        @Param("footprint") String footprint
    );

    @Query(
        value = """
            WITH new_config AS (
                INSERT INTO proxymaster.gallery_config (
                    category, official_website, name,  method, request_format, base_url, 
                    data_format, pagination_field, use_backend_proxy, data_list_field, name_field,  image_url_field,
                     ip_address, browser_fingerprint, published, default_language
                )
                SELECT category, official_website, name || '_copy',  method, request_format, base_url,
                        data_format, pagination_field, use_backend_proxy, data_list_field, name_field,  image_url_field,
                        :ipAddress, :browserFingerprint, FALSE, default_language
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
    List<Long> copyGalleryConfig(Long id, String ipAddress, String browserFingerprint);
}
