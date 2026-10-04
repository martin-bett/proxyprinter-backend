/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.repository;

import jakarta.annotation.Nonnull;
import java.util.List;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiConfigRepository
extends JpaRepository<ApiConfig, Long> {
    @Query(value=" SELECT api FROM ApiConfig api LEFT JOIN FETCH api.parameterMap pM WHERE api.published = true OR api.visitorIdentifier = :vi")
    @Nonnull
    public List<ApiConfig> findAllVisible(@Param(value="vi") VisitorIdentifier var1);

    @Query(value="    INSERT INTO proxymaster.api_config (name, category, ip_address, browser_fingerprint)\n    VALUES (:name, CAST(:cate AS proxymaster.api_category), :ip, :footprint)\n    RETURNING id\n", nativeQuery=true)
    public Long insertApiConfig(@Param(value="name") String var1, @Param(value="cate") String var2, @Param(value="ip") String var3, @Param(value="footprint") String var4);

    @Query(value="WITH new_config AS (\n    INSERT INTO proxymaster.api_config (\n        category, official_website, name, custom_arts, method, request_format, base_url, concurrent_limit,\n        data_format,pagination_max_page_field, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy,\n        data_list_field, name_field, name_split_character, image_url_field, language_field, set_field,\n        set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field,\n        follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field,\n        nested_language_language_field, ip_address, browser_fingerprint, published\n    )\n    SELECT category, official_website, name || '_copy', custom_arts, method, request_format, base_url,\n           concurrent_limit, data_format,pagination_max_page_field, pagination_link_field, csrf_token_variable_name, requires_csrf_token,\n           use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field,\n           language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field,\n           follow_link_image_field, follow_link_language_field, nested_language_data_list_field,\n           nested_language_image_url_field, nested_language_language_field, :ipAddress, :browserFingerprint, FALSE\n    FROM proxymaster.api_config\n    WHERE id = :id\n    RETURNING id\n)\nINSERT INTO proxymaster.api_config_parameter (config_id, name,category, value)\nSELECT (SELECT id FROM new_config), name,category, value\nFROM proxymaster.api_config_parameter\nWHERE config_id = :id\nRETURNING (SELECT id FROM new_config)\n", nativeQuery=true)
    public List<Long> copyApiConfig(@Param(value="id") Long var1, @Param(value="ipAddress") String var2, @Param(value="browserFingerprint") String var3);
}

