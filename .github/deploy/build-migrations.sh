#!/usr/bin/env bash
# Erzeugt auf dem Runner die Aufrufe für remote-migrate.sh: ein run_migration je SQL-Datei,
# sortiert nach Dateiname (Datum vorne), Inhalt base64-kodiert.
set -euo pipefail
dir="${1:-src/main/resources/sql}"
for f in $(ls "$dir"/*.sql | sort); do
  printf 'run_migration %q %q\n' "$(basename "$f")" "$(base64 -w0 < "$f")"
done
echo 'echo "Migrationen fertig, neu ausgeführt: $APPLIED"'
