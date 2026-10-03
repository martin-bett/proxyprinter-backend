-- init.sql legt die Beispiel-Configs mit festen IDs an, die Sequenzen stehen danach aber noch auf 1.
-- Dadurch scheitern "Create" und "Copy" mit "duplicate key value violates unique constraint ..._pkey".
-- Setzt jede Sequenz auf die höchste vorhandene ID. Kann gefahrlos mehrfach laufen.
SELECT setval(pg_get_serial_sequence('proxymaster.api_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.api_config_parameter', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.api_config_parameter), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.gallery_config', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.gallery_config), 0) + 1, false);
SELECT setval(pg_get_serial_sequence('proxymaster.gallery_config_parameter', 'id'), COALESCE((SELECT MAX(id) FROM proxymaster.gallery_config_parameter), 0) + 1, false);
