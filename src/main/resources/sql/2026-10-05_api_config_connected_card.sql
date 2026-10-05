-- Verbundene Karten (z.B. Scryfall all_parts: Tokens, Emblems, Dungeons): je Liste eine Zeile.
-- Jeder Eintrag einer Liste wird eine eigene Karte (Anzahl 1) und über alle aktiven APIs per Name gesucht.
-- Einmal auf der bestehenden Datenbank ausführen (ddl-auto ist "none"), mehrfach ausführbar.
BEGIN;

CREATE TABLE IF NOT EXISTS proxymaster.api_config_connected_card (
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
CREATE INDEX IF NOT EXISTS api_config_connected_card_config_idx ON proxymaster.api_config_connected_card (config_id);

-- Mitgelieferte Scryfall-Config (ohne Besitzer): all_parts
INSERT INTO proxymaster.api_config_connected_card (config_id, "position", data_list_field, name_field, link_field, type_field)
SELECT c.id, 0, 'all_parts', 'name', 'uri', 'type_line'
FROM proxymaster.api_config c
WHERE c."name" = 'scryfall' AND c.ip_address IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM proxymaster.api_config_connected_card cc
    WHERE cc.config_id = c.id AND cc.data_list_field = 'all_parts'
  );

COMMIT;
