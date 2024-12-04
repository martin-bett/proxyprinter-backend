CREATE SCHEMA proxymaster;
ALTER ROLE mtguser SET search_path TO proxymaster, public;
-- Erst den Enum-Typ erstellen
CREATE TYPE proxymaster.api_category AS ENUM ('MTG', 'YUGIOH', 'POKE', 'MISC');

CREATE TABLE proxymaster.ratings (
  id SERIAL PRIMARY KEY,
  ip_address VARCHAR(255),
  browser_fingerprint VARCHAR(255),
  api_config_id BIGINT,
  score_quality INT,
  score_availablity INT,
  comment VARCHAR(255),
  created_at TIMESTAMP
);



create table proxymaster.gallery_config (
  id SERIAL PRIMARY KEY,
  category proxymaster.api_category,
  name VARCHAR(255),
  base_url VARCHAR(255),
  data_format VARCHAR(255),
  image_url_field VARCHAR(255),
  method VARCHAR(255),
  name_field VARCHAR(255),
  data_list_field VARCHAR(255),
  use_backend_proxy BOOLEAN,
  pagination_field VARCHAR(255),
  request_format VARCHAR(255),
  official_website VARCHAR(255),
  ip_address VARCHAR(255),
  browser_fingerprint VARCHAR(255),
  published BOOLEAN not null default false
);

CREATE TABLE proxymaster.gallery_config_parameter (
  id SERIAL PRIMARY KEY, 
  category VARCHAR(255),
  gallery_id BIGINT,
  name VARCHAR(255),
  value VARCHAR(255)
  UNIQUE (gallery_id, name)
);

CREATE TABLE proxymaster.api_config (
  id SERIAL PRIMARY KEY,
  category proxymaster.api_category,
  official_website VARCHAR(255),
  name VARCHAR(255),
  custom_arts BOOLEAN default false,
  method VARCHAR(255),
  request_format VARCHAR(255),
  base_url VARCHAR(255),
  concurrent_limit INT,
  data_format VARCHAR(255),
  pagination_field VARCHAR(255),
  csrf_token_variable_name VARCHAR(255),
  requires_csrf_token BOOLEAN,
  use_backend_proxy BOOLEAN,
  data_list_field VARCHAR(255),
  name_field VARCHAR(255),
  name_split_character VARCHAR(255),
  image_url_field VARCHAR(255),
  language_field VARCHAR(255),
  set_field VARCHAR(255),
  set_number_field VARCHAR(255),
  follow_link_field VARCHAR(255),
  follow_link_data_list_field VARCHAR(255),
  follow_link_image_field VARCHAR(255),
  follow_link_language_field VARCHAR(255),
  nested_language_data_list_field VARCHAR(255),
  nested_language_image_url_field VARCHAR(255),
  nested_language_language_field VARCHAR(255),
  ip_address VARCHAR(255),
  browser_fingerprint VARCHAR(255),
  published BOOLEAN not null default false
);

CREATE TABLE proxymaster.api_config_parameter (
  id SERIAL PRIMARY KEY, 
  config_id BIGINT,
  name VARCHAR(255),
  category VARCHAR(255),
  value VARCHAR(255),
  UNIQUE (config_id, name)
);


WITH inserted_configs AS (
INSERT INTO proxymaster.api_config (
    category, official_website, name, method, request_format, base_url, 
    concurrent_limit, data_format, csrf_token_variable_name, requires_csrf_token,
    use_backend_proxy, data_list_field, name_field, name_split_character,
    image_url_field, set_field, set_number_field, nested_language_data_list_field,
    nested_language_image_url_field, nested_language_language_field,published 
) VALUES
-- aetherhub
('MTG'::proxymaster.api_category, 'https://aetherhub.com/', 'aetherhub', 'GET', NULL, 'https://aetherhub.com/Card/Search',
 2, 'html', NULL, NULL, 
 TRUE, 'div#cards>div', 'div.item-hidden-text | innerText', NULL,
 'img | attr:data-src', NULL, NULL, NULL, NULL, null,true),

-- manala
('MTG'::proxymaster.api_category, 'https://www.mtgdatabase.com/de/search/', 'manala', 'POST', 'multipart', 'https://www.mtgdatabase.com/en/search/',
 5, 'html', 'search[csrf_token]', FALSE,
 TRUE, 'div#list_cards_layout_2 table.dataTable tbody tr', 'td.details-col div:nth-child(1) a | innerText', NULL,
 'td.img-col img | attr:src', 'td.details-col div:nth-child(2) a | innerText', NULL, NULL, NULL, NULL,true),

-- weitere Eintr�ge folgen mit dem gleichen Pattern
('MTG'::proxymaster.api_category, 'https://gatherer.wizards.com/Pages/Default.aspx', 'mtggatherer', 'GET', NULL, 'https://api.magicthegathering.io/v1/cards',
 5, 'json', NULL, NULL,
 FALSE, 'cards', 'name', NULL,
 'imageUrl', NULL, NULL, 'foreignNames', 'imageUrl', 'language',true),

('MTG'::proxymaster.api_category, 'https://mtgcardbuilder.com/', 'mtgbuilder', 'POST', NULL, 'https://mtgcardbuilder.com/wp-admin/admin-ajax.php',
 5, 'json', NULL, NULL,
 FALSE, 'data', 'search_card_name', NULL,
 'image_url', 'card_edition', 'id', NULL, NULL, NULL,true),

('POKE'::proxymaster.api_category, NULL, 'pkmcards', 'GET', NULL, 'https://pkmncards.com/',
 2, 'html', NULL, NULL,
 TRUE, 'main#genesis-content>article', 'a | attr:title', '�',
 'img | attr:src', NULL, NULL, NULL, NULL, NULL,true),

('MTG'::proxymaster.api_category, 'https://www.printingproxies.com/mtg-proxy-custom-card/?index=1', 'printingproxies', 'POST', NULL, 'https://www.printingproxies.com/wp-content/plugins/card-demo/ajax/gallery.php',
 5, 'json', NULL, NULL,
 FALSE, 'data', 'search_card_name', NULL,
 'image_url', 'card_edition', 'id', NULL, NULL, NULL,true),

('POKE'::proxymaster.api_category, NULL, 'pokemontcg', 'GET', NULL, 'https://api.pokemontcg.io/v2/cards',
 2, 'json', NULL, NULL,
 FALSE, 'data', 'name', NULL,
 'images.large', NULL, NULL, NULL, NULL, NULL,true),

('MTG'::proxymaster.api_category, 'https://scryfall.com/', 'scryfall', 'GET', NULL, 'https://api.scryfall.com/cards/search',
 5, 'json', NULL, NULL,
 FALSE, 'data', 'name', '//',
 'image_uris.png', 'set', NULL, NULL, NULL, 'lang',true)
 returning id, name
 )
 INSERT INTO proxymaster.api_config_parameter (config_id, "name", category, value)
 SELECT
    id,
    param_name,
    'request',
    param_value
FROM (
	VALUES
    -- Parameters for aetherhub
        ('aetherhub', 'name', '{cardname}'),
        ('aetherhub', 'uniq', 'false'),

        -- Parameters for printingproxies
        ('printingproxies', 'search', '{cardname}'),
        ('printingproxies', 'type', 'search'),
        ('printingproxies', 'include_variations', 'true'),
        ('printingproxies', 'ajax_action', 'searchGalleryCards'),
        ('printingproxies', 'ajax_mod', 'card'),
        ('printingproxies', 'other', '0'),
        ('printingproxies', 'nsfw', '0'),
        ('printingproxies', 'order', 'top'),

        -- Parameters for manala
        ('manala', 'search[name]', '{cardname}'),
        ('manala', 'search[showduplicates]', 'on'),
        ('manala', 'search[name_begins]', 'on'),

        -- Parameters for mtggatherer
        ('mtggatherer', 'name', '{cardname}'),

        -- Parameters for mtgbuilder
        ('mtgbuilder', 'search', '{cardname}'),
        ('mtgbuilder', 'method', 'search_gallery_cards'),
        ('mtgbuilder', 'action', 'builder_ajax'),
        ('mtgbuilder', 'other', '0'),
        ('mtgbuilder', 'nsfw', '0'),
        ('mtgbuilder', 'order', 'top'),

        -- Parameters for pkmcards
        ('pkmcards', 's', '{cardname}'),

        -- Parameters for pokemontcg
        ('pokemontcg', 'q', '!name:{cardname}'),

        -- Parameters for scryfall
        ('scryfall', 'q', '{cardname}'),
        ('scryfall', 'include_multilingual', 'true'),
        ('scryfall', 'include_variations', 'true'),
        ('scryfall', 'unique', 'prints')
) AS params(config_key, param_name, param_value)
JOIN inserted_configs ON params.config_key = inserted_configs.name;
 