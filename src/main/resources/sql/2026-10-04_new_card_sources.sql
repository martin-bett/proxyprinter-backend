-- Neue Kartenquellen (Recherche 2026-10-03) für bestehende DBs, die nicht neu aus init.sql aufgebaut werden.
-- Kann mehrfach laufen: Configs, deren Name schon existiert, werden übersprungen.
-- Hinweis: init.sql enthält die gleichen Quellen; mtgio entspricht dort der vorhandenen Config mtggatherer (id 3).
-- IDs vergibt die Sequenz. Deshalb zuerst die Sequenzen nachziehen (wie 2026-10-03_fix_sequences.sql).
-- In DBeaver als Script ausführen (Alt+X), nicht nur das markierte Statement.

SELECT setval(pg_get_serial_sequence('proxymaster.api_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.api_config_parameter', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config_parameter), 0) + 1, false);

-- Verwaiste Parameter ohne Config entfernen. init.sql legt z.B. Parameter für config_id 18-21 an,
-- deren Configs fehlen; neue Configs mit diesen IDs würden sie sonst erben.
DELETE FROM proxymaster.api_config_parameter p
WHERE NOT EXISTS (SELECT 1 FROM proxymaster.api_config c WHERE c.id = p.config_id);

-- 1) magicthegathering.io (MTG, offizielle Gatherer-Scans, klein 223x310, live geprüft)
--    Bild-URLs sind http://gatherer..., deshalb proxy_images = true.
WITH c AS (
  INSERT INTO proxymaster.api_config
  (category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format,
   use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, set_field, set_number_field,
   nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, published, proxy_images)
  SELECT 'MTG'::proxymaster."api_category", 'https://gatherer.wizards.com/', 'mtgio', false, 'GET', NULL,
         'https://api.magicthegathering.io/v1/cards', 3, 'json',
         false, 'cards', 'name', '//', 'imageUrl', 'setName', 'number',
         'foreignNames', 'imageUrl', 'language', true, true
  WHERE NOT EXISTS (SELECT 1 FROM proxymaster.api_config WHERE "name" = 'mtgio')
  RETURNING id
)
INSERT INTO proxymaster.api_config_parameter (config_id, "name", value, category)
SELECT c.id, p.name, p.value, 'request'
FROM c, (VALUES ('name', '"{cardname}"'), ('pageSize', '100')) AS p(name, value);

-- 2) YGOPRODeck (Yu-Gi-Oh!, offizielle Bilder 421x614, live geprüft)
--    Limit 20 Anfragen/s (sonst 1 h Sperre), Hotlinking unerwünscht -> proxy_images = true.
WITH c AS (
  INSERT INTO proxymaster.api_config
  (category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format,
   use_backend_proxy, data_list_field, name_field, image_url_field, set_field, set_number_field, published, proxy_images)
  SELECT 'YUGIOH'::proxymaster."api_category", 'https://ygoprodeck.com/', 'ygoprodeck', false, 'GET', NULL,
         'https://db.ygoprodeck.com/api/v7/cardinfo.php', 2, 'json',
         false, 'data', 'name', 'card_images.0.image_url', 'card_sets.0.set_name', 'card_sets.0.set_code', true, true
  WHERE NOT EXISTS (SELECT 1 FROM proxymaster.api_config WHERE "name" = 'ygoprodeck')
  RETURNING id
)
INSERT INTO proxymaster.api_config_parameter (config_id, "name", value, category)
SELECT c.id, p.name, p.value, 'request'
FROM c, (VALUES ('fname', '{cardname}')) AS p(name, value);

-- 3) Lorcast (Disney Lorcana, offizielle Bilder als AVIF, live geprüft)
WITH c AS (
  INSERT INTO proxymaster.api_config
  (category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format,
   use_backend_proxy, data_list_field, name_field, image_url_field, language_field, set_field, set_number_field, published, proxy_images)
  SELECT 'MISC'::proxymaster."api_category", 'https://lorcast.com/', 'lorcast', false, 'GET', NULL,
         'https://api.lorcast.com/v0/cards/search', 2, 'json',
         false, 'results', 'name', 'image_uris.digital.large', 'lang', 'set.name', 'collector_number', true, false
  WHERE NOT EXISTS (SELECT 1 FROM proxymaster.api_config WHERE "name" = 'lorcast')
  RETURNING id
)
INSERT INTO proxymaster.api_config_parameter (config_id, "name", value, category)
SELECT c.id, p.name, p.value, 'request'
FROM c, (VALUES ('q', '{cardname}')) AS p(name, value);

-- 4) SWU-DB (Star Wars Unlimited) - NICHT live geprüft, deshalb published = false.
--    Nach erfolgreichem Test: UPDATE proxymaster.api_config SET published = true WHERE "name" = 'swudb';
WITH c AS (
  INSERT INTO proxymaster.api_config
  (category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format,
   use_backend_proxy, data_list_field, name_field, image_url_field, set_field, set_number_field, published, proxy_images)
  SELECT 'MISC'::proxymaster."api_category", 'https://www.swu-db.com/', 'swudb', false, 'GET', NULL,
         'https://api.swu-db.com/cards/search', 2, 'json',
         false, 'data', 'Name', 'FrontArt', 'Set', 'Number', false, true
  WHERE NOT EXISTS (SELECT 1 FROM proxymaster.api_config WHERE "name" = 'swudb')
  RETURNING id
)
INSERT INTO proxymaster.api_config_parameter (config_id, "name", value, category)
SELECT c.id, p.name, p.value, 'request'
FROM c, (VALUES ('q', '{cardname}')) AS p(name, value);

-- Kontrolle
SELECT c.id, c."name", c.category, c.published, p."name" AS param, p.value
FROM proxymaster.api_config c
LEFT JOIN proxymaster.api_config_parameter p ON p.config_id = c.id
WHERE c."name" IN ('mtgio', 'ygoprodeck', 'lorcast', 'swudb')
ORDER BY c.id, p.id;
