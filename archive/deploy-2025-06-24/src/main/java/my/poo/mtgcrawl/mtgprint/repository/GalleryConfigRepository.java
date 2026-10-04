/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.repository;

import jakarta.annotation.Nonnull;
import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.GalleryConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GalleryConfigRepository
extends JpaRepository<GalleryConfig, Long> {
    @Query(value=" SELECT gal FROM GalleryConfig gal LEFT JOIN FETCH gal.parameters pM WHERE gal.published = true OR gal.visitorIdentifier = :vi")
    @Nonnull
    public List<GalleryConfig> findAllVisible(@Param(value="vi") VisitorIdentifier var1);

    @Query(value="    INSERT INTO proxymaster.gallery_config (name, category, ip_address, browser_fingerprint)\n    VALUES (:name, CAST(:cate AS proxymaster.api_category), :ip, :footprint)\n    RETURNING id\n", nativeQuery=true)
    public Long insertGalleryConfig(@Param(value="name") String var1, @Param(value="cate") String var2, @Param(value="ip") String var3, @Param(value="footprint") String var4);

    @Query(value="WITH new_config AS (\n    INSERT INTO proxymaster.gallery_config (\n        category, official_website, name,  method, request_format, base_url,\n        data_format, pagination_field, use_backend_proxy, data_list_field, name_field,  image_url_field,\n         ip_address, browser_fingerprint, published\n    )\n    SELECT category, official_website, name || '_copy',  method, request_format, base_url,\n            data_format, pagination_field, use_backend_proxy, data_list_field, name_field,  image_url_field,\n            :ipAddress, :browserFingerprint, FALSE\n    FROM proxymaster.gallery_config\n    WHERE id = :id\n    RETURNING id\n)\nINSERT INTO proxymaster.gallery_config_parameter (gallery_id, name,category, value)\nSELECT (SELECT id FROM new_config), name,category, value\nFROM proxymaster.gallery_config_parameter\nWHERE gallery_id = :id\nRETURNING (SELECT id FROM new_config)\n", nativeQuery=true)
    public List<Long> copyGalleryConfig(Long var1, String var2, String var3);
}

