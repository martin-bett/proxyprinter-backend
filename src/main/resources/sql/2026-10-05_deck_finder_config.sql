-- Deckfinder-Configs: Decks per Link, ID oder Name von Deckbuilder-Seiten laden.
-- Wiederholbar, läuft beim Deploy automatisch.
CREATE TABLE IF NOT EXISTS proxymaster.deck_finder_config (
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

INSERT INTO proxymaster.deck_finder_config
(category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
SELECT 'MTG'::proxymaster."api_category", 'archidekt', 'https://archidekt.com/', true, 'archidekt\.com/(?:api/)?decks/(?<id>\d+)', 'https://archidekt.com/api/decks/{id}/', 'json', 'name', 'cards', 'quantity', 'card.oracleCard.name', 'card.edition.editioncode', 'card.collectorNumber', 'categories', 'Sideboard, Maybeboard', 'https://archidekt.com/api/decks/v3/?name={query}&page={page}', 'results', 'id', 'name', 'owner.username', 'featured', 'size'
WHERE NOT EXISTS (SELECT 1 FROM proxymaster.deck_finder_config WHERE "name" = 'archidekt' AND ip_address IS NULL);
INSERT INTO proxymaster.deck_finder_config
(category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
SELECT 'MTG'::proxymaster."api_category", 'tappedout', 'https://tappedout.net/', true, 'tappedout\.net/mtg-decks/(?<id>[\w-]+)', 'https://tappedout.net/mtg-decks/{id}/?fmt=txt', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Sideboard, Maybeboard', NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM proxymaster.deck_finder_config WHERE "name" = 'tappedout' AND ip_address IS NULL);
INSERT INTO proxymaster.deck_finder_config
(category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
SELECT 'MTG'::proxymaster."api_category", 'edhrec_average', 'https://edhrec.com/', true, 'edhrec\.com/(?:average-decks|commanders)/(?<id>[a-z0-9-]+)', 'https://json.edhrec.com/pages/average-decks/{id}.json', 'json', 'header', 'deck.commander_v2, deck.cards.*', '1', '0', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM proxymaster.deck_finder_config WHERE "name" = 'edhrec_average' AND ip_address IS NULL);
INSERT INTO proxymaster.deck_finder_config
(category, "name", official_website, published, link_pattern, fetch_url, data_format, deck_name_field, entry_list_fields, quantity_field, name_field, set_field, number_field, board_field, optional_boards, search_url, search_list_field, search_id_field, search_name_field, search_owner_field, search_image_field, search_info_field)
SELECT 'MTG'::proxymaster."api_category", 'scryfall_deck', 'https://scryfall.com/', true, 'scryfall\.com/@[^/]+/decks/(?<id>[0-9a-f-]{36})', 'https://api.scryfall.com/decks/{id}/export/text', 'text', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Sideboard, Maybeboard', NULL, NULL, NULL, NULL, NULL, NULL, NULL
WHERE NOT EXISTS (SELECT 1 FROM proxymaster.deck_finder_config WHERE "name" = 'scryfall_deck' AND ip_address IS NULL);
