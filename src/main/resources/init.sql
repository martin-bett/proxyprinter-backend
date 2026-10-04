DROP SCHEMA IF EXISTS proxymaster;

CREATE SCHEMA proxymaster;

ALTER ROLE mtguser SET search_path TO proxymaster, public;


-- DROP TYPE proxymaster."api_category";

CREATE TYPE proxymaster."api_category" AS ENUM (
	'MTG',
	'YUGIOH',
	'POKE',
	'MISC');
	
CREATE TABLE proxymaster.ratings (
	id serial4 NOT NULL,
	ip_address varchar(255) NULL,
	browser_fingerprint varchar(255) NULL,
	api_config_id int8 NULL,
	score_quality int4 NULL,
	score_availablity int4 NULL,
	"comment" varchar(255) NULL,
	created_at timestamp NULL,
	CONSTRAINT ratings_pkey PRIMARY KEY (id)
);

CREATE TABLE proxymaster.gallery_config (
	id serial4 NOT NULL,
	category proxymaster."api_category" NULL,
	"name" varchar(255) NULL,
	base_url varchar(255) NULL,
	data_format varchar(255) NULL,
	image_url_field varchar(255) NULL,
	name_field varchar(255) NULL,
	data_list_field varchar(255) NULL,
	pagination_field varchar(255) NULL,
	official_website varchar(255) NULL,
	ip_address varchar(255) NULL,
	browser_fingerprint varchar(255) NULL,
	published bool DEFAULT false NOT NULL,
	"method" varchar(255) NULL,
	request_format varchar(255) NULL,
	use_backend_proxy bool NULL,
	CONSTRAINT gallery_config_pkey PRIMARY KEY (id)
);

CREATE TABLE proxymaster.api_config (
	id serial4 NOT NULL,
	category proxymaster."api_category" NULL,
	official_website varchar(255) NULL,
	"name" varchar(255) NULL,
	custom_arts bool DEFAULT false NULL,
	"method" varchar(255) NULL,
	request_format varchar(255) NULL,
	base_url varchar(255) NULL,
	concurrent_limit int4 NULL,
	data_format varchar(255) NULL,
	pagination_link_field varchar(255) NULL,
	csrf_token_variable_name varchar(255) NULL,
	requires_csrf_token bool NULL,
	use_backend_proxy bool NULL,
	data_list_field varchar(255) NULL,
	name_field varchar(255) NULL,
	name_split_character varchar(255) NULL,
	image_url_field varchar(255) NULL,
	language_field varchar(255) NULL,
	set_field varchar(255) NULL,
	set_number_field varchar(255) NULL,
	follow_link_field varchar(255) NULL,
	follow_link_data_list_field varchar(255) NULL,
	follow_link_image_field varchar(255) NULL,
	follow_link_language_field varchar(255) NULL,
	nested_language_data_list_field varchar(255) NULL,
	nested_language_image_url_field varchar(255) NULL,
	nested_language_language_field varchar(255) NULL,
	ip_address varchar(255) NULL,
	browser_fingerprint varchar(255) NULL,
	published bool DEFAULT false NOT NULL,
	pagination_max_page_field varchar(255) NULL,
	proxy_images bool NULL,
	card_faces_data_list_field varchar(255) NULL,
	card_faces_image_url_field varchar(255) NULL,
	card_faces_name_field varchar(255) NULL,
	CONSTRAINT api_config_pkey PRIMARY KEY (id)
);

CREATE TABLE proxymaster.api_config_parameter (
	id serial4 NOT NULL,
	config_id int8 NULL,
	"name" varchar(255) NULL,
	value varchar(255) NULL,
	category varchar(255) NULL,
	CONSTRAINT api_config_parameter_config_id_name_key UNIQUE (config_id, name),
	CONSTRAINT api_config_parameter_pkey PRIMARY KEY (id)
);

CREATE TABLE proxymaster.gallery_config_parameter (
	id serial4 NOT NULL,
	gallery_id int8 NULL,
	"name" varchar(255) NULL,
	category varchar(255) NULL,
	value varchar(255) NULL,
	CONSTRAINT gallery_config_parameter_gallery_id_name_key UNIQUE (gallery_id, name),
	CONSTRAINT gallery_config_parameter_pkey PRIMARY KEY (id)
);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(1, 'MTG'::proxymaster."api_category", 'https://aetherhub.com/', 'aetherhub', false, 'GET', NULL, 'https://aetherhub.com/Card/Search', 2, 'html', NULL, NULL, NULL, true, 'div#cards>div', 'div.item-hidden-text | innerText', NULL, 'img | attr:data-src', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(2, 'MTG'::proxymaster."api_category", 'https://www.mtgdatabase.com/de/search/', 'manala', false, 'POST', 'multipart', 'https://www.mtgdatabase.com/en/search/', 5, 'html', NULL, 'search[csrf_token]', false, true, 'div#list_cards_layout_2 table.dataTable tbody tr', 'td.details-col div:nth-child(1) a | innerText', NULL, 'td.img-col img | attr:src', NULL, 'td.details-col div:nth-child(2) a | innerText', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field, proxy_images)
VALUES(3, 'MTG'::proxymaster."api_category", 'https://gatherer.wizards.com/', 'mtggatherer', false, 'GET', NULL, 'https://api.magicthegathering.io/v1/cards', 3, 'json', NULL, NULL, NULL, false, 'cards', 'name', '//', 'imageUrl', NULL, 'setName', 'number', NULL, NULL, NULL, NULL, 'foreignNames', 'imageUrl', 'language', NULL, NULL, true, NULL, true);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(4, 'MTG'::proxymaster."api_category", 'https://mtgcardbuilder.com/', 'mtgbuilder', true, 'POST', NULL, 'https://mtgcardbuilder.com/wp-admin/admin-ajax.php', 5, 'json', NULL, NULL, NULL, false, 'data', 'search_card_name', NULL, 'image_url', NULL, 'card_edition', 'id', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, 'total');
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(6, 'MTG'::proxymaster."api_category", 'https://www.printingproxies.com/mtg-proxy-custom-card/?index=1', 'printingproxies', true, 'POST', NULL, 'https://www.printingproxies.com/wp-content/plugins/card-demo/ajax/gallery.php', 5, 'json', NULL, NULL, NULL, false, 'data', 'search_card_name', NULL, 'image_url', NULL, 'card_edition', 'id', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, 'total');
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(7, 'POKE'::proxymaster."api_category", NULL, 'pokemontcg', false, 'GET', NULL, 'https://api.pokemontcg.io/v2/cards', 2, 'json', NULL, NULL, NULL, false, 'data', 'name', NULL, 'images.large', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(10, 'MTG'::proxymaster."api_category", 'https://mtgcardbuilder.com/', 'mtgbuilder_copy', false, 'POST', NULL, 'https://mtgcardbuilder.com/wp-admin/admin-ajax.php', 5, 'json', NULL, NULL, NULL, false, 'data', 'search_card_name', NULL, 'image_url', NULL, 'card_edition', 'id', NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0:0:0:0:0:0:0:1', '30746e66fa767fc38efb57b1d778e2ab', false, NULL);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(5, 'POKE'::proxymaster."api_category", 'https://pkmncards.com/', 'pkmcards', false, 'GET', NULL, 'https://pkmncards.com/', 2, 'html', 'div.navigation li.next-link a | attr:href', NULL, NULL, true, 'main#genesis-content>article', 'div.card-text-area span.name a | innerText', NULL, '.card-image-area img | attr:src', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(11, 'MTG'::proxymaster."api_category", '', 'new mmoikk', false, '', '', '', 0, '', '', '', NULL, NULL, '', '', '', '', '', '', '', '', '', '', '', '', '', '', '0:0:0:0:0:0:0:1', '3e3feded8292e89f0b12e11e52f8c64f', false, NULL);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(8, 'MTG'::proxymaster."api_category", 'https://scryfall.com/', 'scryfall', false, 'GET', NULL, 'https://api.scryfall.com/cards/search', 5, 'json', 'next_page', NULL, NULL, false, 'data', 'name', '//', 'image_uris.png', 'lang', 'set_name', 'collector_number', NULL, NULL, NULL, NULL, NULL, NULL, 'lang', NULL, NULL, true, NULL);
-- Doppelseitige Karten (Transform, MDFC, ...) haben kein image_uris oben, nur je Seite in card_faces
UPDATE proxymaster.api_config SET card_faces_data_list_field = 'card_faces', card_faces_image_url_field = 'image_uris.png', card_faces_name_field = 'name' WHERE id = 8;
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field)
VALUES(17, 'MISC'::proxymaster."api_category", NULL, 'new config', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '0:0:0:0:0:0:0:1', '976130e0482b5dd311d9992109824fba', false, NULL);

INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(1, 1, 'name', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(2, 1, 'uniq', 'false', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(3, 6, 'search', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(4, 6, 'type', 'search', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(5, 2, 'search[name]', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(6, 2, 'search[showduplicates]', 'on', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(7, 2, 'search[name_begins]', 'on', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(8, 3, 'name', '"{cardname}"', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(9, 4, 'search', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(10, 4, 'method', 'search_gallery_cards', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(11, 4, 'action', 'builder_ajax', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(12, 4, 'other', '0', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(13, 4, 'nsfw', '0', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(14, 4, 'order', 'top', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(18, 8, 'include_multilingual', 'true', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(19, 8, 'include_variations', 'true', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(20, 8, 'unique', 'prints', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(21, 10, 'action', 'builder_ajax', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(22, 10, 'method', 'search_gallery_cards', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(23, 10, 'nsfw', '0', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(24, 10, 'order', 'top', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(25, 10, 'other', '0', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(26, 10, 'search', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(34, 6, 'include_variations', 'true', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(35, 6, 'ajax_action', 'searchGalleryCards', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(36, 6, 'ajax_mod', 'card', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(37, 6, 'other', '0', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(38, 6, 'nsfw', '0', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(39, 6, 'order', 'top', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(40, 4, 'cpage', '{pageCount}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(41, 6, 'cpage', '{pageCount}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(17, 8, 'q', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(16, 7, 'q', '!name:"{cardname}"', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(52, 5, 'sort', 'date', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(53, 5, 'ord', 'auto', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(54, 5, 'display', 'full', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(15, 5, 's', '"{cardname}"', 'request');

-- Weitere Kartenquellen (Recherche 2026-10-03). swudb ist ungeprüft und deshalb nicht veröffentlicht.
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field, proxy_images)
VALUES(18, 'YUGIOH'::proxymaster."api_category", 'https://ygoprodeck.com/', 'ygoprodeck', false, 'GET', NULL, 'https://db.ygoprodeck.com/api/v7/cardinfo.php', 2, 'json', NULL, NULL, NULL, false, 'data', 'name', NULL, 'card_images.0.image_url', NULL, 'card_sets.0.set_name', 'card_sets.0.set_code', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, true);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field, proxy_images)
VALUES(19, 'MISC'::proxymaster."api_category", 'https://lorcast.com/', 'lorcast', false, 'GET', NULL, 'https://api.lorcast.com/v0/cards/search', 2, 'json', NULL, NULL, NULL, false, 'results', 'name', NULL, 'image_uris.digital.large', 'lang', 'set.name', 'collector_number', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, false);
INSERT INTO proxymaster.api_config
(id, category, official_website, "name", custom_arts, "method", request_format, base_url, concurrent_limit, data_format, pagination_link_field, csrf_token_variable_name, requires_csrf_token, use_backend_proxy, data_list_field, name_field, name_split_character, image_url_field, language_field, set_field, set_number_field, follow_link_field, follow_link_data_list_field, follow_link_image_field, follow_link_language_field, nested_language_data_list_field, nested_language_image_url_field, nested_language_language_field, ip_address, browser_fingerprint, published, pagination_max_page_field, proxy_images)
VALUES(20, 'MISC'::proxymaster."api_category", 'https://www.swu-db.com/', 'swudb', false, 'GET', NULL, 'https://api.swu-db.com/cards/search', 2, 'json', NULL, NULL, NULL, false, 'data', 'Name', NULL, 'FrontArt', NULL, 'Set', 'Number', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, NULL, true);
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(55, 3, 'pageSize', '100', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(56, 18, 'fname', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(57, 19, 'q', '{cardname}', 'request');
INSERT INTO proxymaster.api_config_parameter
(id, config_id, "name", value, category)
VALUES(58, 20, 'q', '{cardname}', 'request');

-- Galerie-Quellen laufen über das Backend (/api/gallery/source/...), das liefert {images:[{name,imageUrl}]}.
-- Parameter "url" ohne Wert wird im Frontend zum Eingabefeld ($GALLERY_INPUT$).
INSERT INTO proxymaster.gallery_config
(id, category, "name", base_url, data_format, image_url_field, name_field, data_list_field, pagination_field, official_website, ip_address, browser_fingerprint, published, "method", request_format, use_backend_proxy)
VALUES(1, 'MISC'::proxymaster."api_category", 'reddit', '/api/gallery/source/reddit', 'json', 'imageUrl', 'name', 'images', NULL, 'https://www.reddit.com/r/magicproxies/', NULL, NULL, true, 'GET', NULL, false);
INSERT INTO proxymaster.gallery_config
(id, category, "name", base_url, data_format, image_url_field, name_field, data_list_field, pagination_field, official_website, ip_address, browser_fingerprint, published, "method", request_format, use_backend_proxy)
VALUES(2, 'MISC'::proxymaster."api_category", 'google_drive', '/api/gallery/source/gdrive', 'json', 'imageUrl', 'name', 'images', NULL, 'https://drive.google.com/', NULL, NULL, true, 'GET', NULL, false);

INSERT INTO proxymaster.gallery_config_parameter
(id, gallery_id, "name", category, value)
VALUES(1, 1, 'url', 'request', NULL);
INSERT INTO proxymaster.gallery_config_parameter
(id, gallery_id, "name", category, value)
VALUES(2, 2, 'url', 'request', NULL);


-- Sequenzen hinter die festen IDs oben setzen, sonst scheitern Create/Copy
SELECT setval(pg_get_serial_sequence('proxymaster.api_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.api_config_parameter', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config_parameter), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.gallery_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.gallery_config), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.gallery_config_parameter', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.gallery_config_parameter), 0) + 1, false);
