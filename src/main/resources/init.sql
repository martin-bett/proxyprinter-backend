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
	default_language varchar(16) NULL,
	link_pattern varchar(500) NULL,
	follow_input_redirects bool DEFAULT false NOT NULL,
	credential varchar(255) NULL,
	image_url_pattern varchar(500) NULL,
	image_url_replacement varchar(500) NULL,
	name_pattern varchar(500) NULL,
	name_replacement varchar(500) NULL,
	hint varchar(1000) NULL,
	hint_translations text NULL,
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
	last_used timestamp NULL,
	card_faces_data_list_field varchar(255) NULL,
	card_faces_image_url_field varchar(255) NULL,
	card_faces_name_field varchar(255) NULL,
	default_language varchar(16) NULL,
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

-- Verbundene Karten (z.B. Scryfall all_parts), je Liste eine Zeile
CREATE TABLE proxymaster.api_config_connected_card (
	id serial4 NOT NULL,
	config_id int8 NOT NULL,
	"position" int4 NULL,
	data_list_field varchar(255) NULL,
	name_field varchar(255) NULL,
	link_field varchar(255) NULL,
	type_field varchar(255) NULL,
	CONSTRAINT api_config_connected_card_pkey PRIMARY KEY (id),
	CONSTRAINT api_config_connected_card_config_fk FOREIGN KEY (config_id) REFERENCES proxymaster.api_config(id) ON DELETE CASCADE
);
CREATE INDEX api_config_connected_card_config_idx ON proxymaster.api_config_connected_card (config_id);

CREATE TABLE proxymaster.gallery_config_parameter (
	id serial4 NOT NULL,
	gallery_id int8 NULL,
	"name" varchar(255) NULL,
	category varchar(255) NULL,
	value varchar(255) NULL,
	CONSTRAINT gallery_config_parameter_gallery_id_name_key UNIQUE (gallery_id, name),
	CONSTRAINT gallery_config_parameter_pkey PRIMARY KEY (id)
);

CREATE TABLE proxymaster.deck_finder_config (
	id serial4 NOT NULL,
	category proxymaster."api_category" NULL,
	"name" varchar(255) NULL,
	official_website varchar(255) NULL,
	ip_address varchar(255) NULL,
	browser_fingerprint varchar(255) NULL,
	published bool DEFAULT false NOT NULL,
	link_pattern varchar(500) NULL,
	fetch_url varchar(500) NULL,
	data_format varchar(255) NULL,
	deck_name_field varchar(255) NULL,
	entry_list_fields varchar(500) NULL,
	quantity_field varchar(255) NULL,
	name_field varchar(255) NULL,
	set_field varchar(255) NULL,
	number_field varchar(255) NULL,
	board_field varchar(255) NULL,
	optional_boards varchar(255) NULL,
	search_url varchar(500) NULL,
	search_list_field varchar(255) NULL,
	search_id_field varchar(255) NULL,
	search_name_field varchar(255) NULL,
	search_owner_field varchar(255) NULL,
	search_image_field varchar(255) NULL,
	search_info_field varchar(255) NULL,
	CONSTRAINT deck_finder_config_pkey PRIMARY KEY (id)
);

-- Backend-Logs ab INFO (DbLogService), Einträge älter als 30 Tage werden täglich gelöscht
CREATE TABLE proxymaster.app_log (
	id bigserial PRIMARY KEY,
	logged_at timestamptz NOT NULL,
	level varchar(5) NOT NULL,
	logger varchar(255),
	thread varchar(100),
	message text,
	exception text
);
CREATE INDEX app_log_logged_at_idx ON proxymaster.app_log (logged_at);

-- Konten (Login per E-Mail-Link/Code)
CREATE TABLE proxymaster.app_user (
    id bigserial PRIMARY KEY,
    email varchar(254) NOT NULL UNIQUE,
    created_at timestamp NOT NULL DEFAULT now(),
    last_login timestamp
);

CREATE TABLE proxymaster.login_token (
    token_hash varchar(64) PRIMARY KEY,
    email varchar(254) NOT NULL,
    code_hash varchar(64) NOT NULL,
    attempts int NOT NULL DEFAULT 0,
    expires_at timestamp NOT NULL
);
CREATE INDEX login_token_email_idx ON proxymaster.login_token (email);

CREATE TABLE proxymaster.user_session (
    token_hash varchar(64) PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES proxymaster.app_user (id) ON DELETE CASCADE,
    created_at timestamp NOT NULL DEFAULT now(),
    last_seen timestamp NOT NULL DEFAULT now(),
    user_agent varchar(255)
);
CREATE INDEX user_session_user_idx ON proxymaster.user_session (user_id);

-- Besitz von Configs und Ratings (sql/2026-10-09_config_owner.sql)
ALTER TABLE proxymaster.api_config ADD COLUMN user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;
ALTER TABLE proxymaster.gallery_config ADD COLUMN user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;
ALTER TABLE proxymaster.deck_finder_config ADD COLUMN user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;
ALTER TABLE proxymaster.ratings ADD COLUMN user_id bigint REFERENCES proxymaster.app_user (id) ON DELETE SET NULL;
CREATE INDEX api_config_user_idx ON proxymaster.api_config (user_id);
CREATE INDEX gallery_config_user_idx ON proxymaster.gallery_config (user_id);
CREATE INDEX deck_finder_config_user_idx ON proxymaster.deck_finder_config (user_id);
CREATE UNIQUE INDEX ratings_user_config_idx ON proxymaster.ratings (user_id, api_config_id) WHERE user_id IS NOT NULL;

-- Fertig erzeugte PDFs pro Tag (UTC), für /api/stats
CREATE TABLE proxymaster.deck_stats (
    day date PRIMARY KEY,
    created int NOT NULL DEFAULT 0
);

-- Nutzungszähler pro Tag und Kennzahl (löst deck_stats ab, siehe sql/2026-10-11_usage_stats.sql)
CREATE TABLE proxymaster.usage_stats (
    day date NOT NULL,
    metric varchar(40) NOT NULL,
    count bigint NOT NULL DEFAULT 0,
    PRIMARY KEY (day, metric)
);

-- Gesperrte Quellen (sql/2026-10-10_blocked_host.sql): Domain inkl. Subdomains oder IP/Netz
CREATE TABLE proxymaster.blocked_host (
    id bigserial PRIMARY KEY,
    host varchar(255) NOT NULL UNIQUE,
    reason text,
    created_at timestamp NOT NULL DEFAULT now()
);

-- Offene Anfragen der Selbst-Sperre (sql/2026-10-10_blocklist_request.sql)
CREATE TABLE proxymaster.blocklist_request (
    token_hash varchar(64) PRIMARY KEY,
    host varchar(255) NOT NULL,
    method varchar(10) NOT NULL,
    email varchar(320),
    ip varchar(64),
    created_at timestamp NOT NULL DEFAULT now(),
    expires_at timestamp NOT NULL
);
CREATE INDEX blocklist_request_host_idx ON proxymaster.blocklist_request (host);

-- Vom Admin geänderte Einstellungen (sql/2026-10-11_app_setting.sql)
CREATE TABLE proxymaster.app_setting (
    setting_key varchar(100) PRIMARY KEY,
    setting_value varchar(200) NOT NULL,
    updated_at timestamp NOT NULL DEFAULT now(),
    updated_by varchar(320)
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
-- Tokens, Emblems, Dungeons usw. stehen in all_parts und werden eigene Karten
INSERT INTO proxymaster.api_config_connected_card (config_id, "position", data_list_field, name_field, link_field, type_field) VALUES(8, 0, 'all_parts', 'name', 'uri', 'type_line');
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

-- Gallery-Configs ohne eigenen Code: link_pattern zerlegt den Link in {id}, base_url wird damit gefüllt und
-- (bei link_pattern oder credential) vom Backend abgerufen; Felder und image_url_pattern liest das Frontend.
-- Parameter "url" ohne Wert wird im Frontend zum Eingabefeld ($GALLERY_INPUT$).
INSERT INTO proxymaster.gallery_config
(id, category, "name", base_url, data_format, image_url_field, name_field, data_list_field, pagination_field, official_website, ip_address, browser_fingerprint, published, "method", request_format, use_backend_proxy, link_pattern, follow_input_redirects, credential, image_url_pattern, image_url_replacement, name_pattern, name_replacement)
VALUES(1, 'MISC'::proxymaster."api_category", 'reddit', 'https://www.reddit.com/comments/{id}/', 'html', 'zoomable-img img | attr:src', 'zoomable-img img | attr:src', 'gallery-carousel li', NULL, 'https://www.reddit.com/r/magicproxies/', NULL, NULL, true, 'GET', NULL, false, '(?:/comments/|/gallery/|redd\.it/)(?<id>[a-z0-9]{4,12})(?:[/?#]|$)', true, 'reddit', NULL, NULL, '^.*/([^/.]+)\.[a-z0-9]+$', '$1');
INSERT INTO proxymaster.gallery_config
(id, category, "name", base_url, data_format, image_url_field, name_field, data_list_field, pagination_field, official_website, ip_address, browser_fingerprint, published, "method", request_format, use_backend_proxy, link_pattern, follow_input_redirects, credential, image_url_pattern, image_url_replacement, name_pattern, name_replacement)
VALUES(2, 'MISC'::proxymaster."api_category", 'google_drive', 'https://drive.google.com/embeddedfolderview?id={id}', 'html', 'a[href] | attr:href', '.flip-entry-title | innerText', 'div.flip-entry:not(:has(a[href*="/folders/"]))', NULL, 'https://drive.google.com/', NULL, NULL, true, 'GET', NULL, false, '(?:/folders/|[?&]id=)(?<id>[A-Za-z0-9_-]{10,})', false, NULL, '^.*/file/d/([A-Za-z0-9_-]+).*$', 'https://drive.google.com/uc?export=download&id=$1', '\.[A-Za-z0-9]{2,5}$', '');

INSERT INTO proxymaster.gallery_config_parameter
(id, gallery_id, "name", category, value)
VALUES(1, 1, 'url', 'request', NULL);
INSERT INTO proxymaster.gallery_config_parameter
(id, gallery_id, "name", category, value)
VALUES(2, 2, 'url', 'request', NULL);


-- Deckfinder: Decks per Link/ID von Deckbuilder-Seiten laden (Antwort liest das Frontend)
INSERT INTO proxymaster.deck_finder_config
(id, category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
VALUES(1, 'MTG'::proxymaster."api_category", 'archidekt', 'https://archidekt.com/', true, 'archidekt\.com/(?:api/)?decks/(?<id>\d+)', 'https://archidekt.com/api/decks/{id}/', 'json', 'name', 'cards', 'quantity', 'card.oracleCard.name', 'card.edition.editioncode', 'card.collectorNumber', 'categories', 'Sideboard, Maybeboard', 'https://archidekt.com/api/decks/v3/?name={query}&page={page}', 'results', 'id', 'name', 'owner.username', 'featured', 'size');
INSERT INTO proxymaster.deck_finder_config
(id, category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
VALUES(2, 'MTG'::proxymaster."api_category", 'tappedout', 'https://tappedout.net/', true, 'tappedout\.net/mtg-decks/(?<id>[\w-]+)', 'https://tappedout.net/mtg-decks/{id}/?fmt=txt', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Sideboard, Maybeboard', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO proxymaster.deck_finder_config
(id, category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
VALUES(3, 'MTG'::proxymaster."api_category", 'edhrec_average', 'https://edhrec.com/', true, 'edhrec\.com/(?:average-decks|commanders)/(?<id>[a-z0-9-]+)', 'https://json.edhrec.com/pages/average-decks/{id}.json', 'json', 'header', 'deck.commander_v2, deck.cards.*', '1', '0', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO proxymaster.deck_finder_config
(id, category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
VALUES(4, 'MTG'::proxymaster."api_category", 'scryfall_deck', 'https://scryfall.com/', true, 'scryfall\.com/@[^/]+/decks/(?<id>[0-9a-f-]{36})', 'https://api.scryfall.com/decks/{id}/export/text', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Sideboard, Maybeboard', NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- Sequenzen hinter die festen IDs oben setzen, sonst scheitern Create/Copy
SELECT setval(pg_get_serial_sequence('proxymaster.api_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.api_config_parameter', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config_parameter), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.gallery_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.gallery_config), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.gallery_config_parameter', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.gallery_config_parameter), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.deck_finder_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.deck_finder_config), 0) + 1, false);
