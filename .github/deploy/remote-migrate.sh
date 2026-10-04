#!/usr/bin/env bash
# Läuft auf dem VPS vor dem Backend-Deploy (aus .github/workflows/deploy.yml hineingepipt).
# Führt jedes SQL-Skript aus src/main/resources/sql genau einmal aus und merkt sich das in
# proxymaster.schema_migration. Die Skripte müssen wiederholbar sein (IF NOT EXISTS, WHERE NOT EXISTS ...),
# denn beim ersten Lauf ist unbekannt, welche schon von Hand ausgeführt wurden.
# Erwartet DEPLOY_PATH und danach Aufrufe von run_migration <dateiname> <base64-inhalt>.
# Optional in der .env: DB_SERVICE (Compose-Service der Datenbank), sonst wird er am Image erkannt.
set -euo pipefail

cd "$DEPLOY_PATH"

DB_SERVICE="$(grep -E '^DB_SERVICE=' .env 2>/dev/null | tail -n1 | cut -d= -f2- || true)"
if [ -z "$DB_SERVICE" ]; then
  DB_SERVICE="$(docker compose ps --format '{{.Service}} {{.Image}}' | awk '$2 ~ /postgres/ {print $1; exit}')"
fi
[ -n "$DB_SERVICE" ] || { echo "Kein laufender Postgres-Service gefunden (DB_SERVICE in .env setzen)" >&2; exit 1; }
echo "Datenbank-Service: $DB_SERVICE"

# psql im Container über den lokalen Socket, Benutzer und Datenbank aus dessen Umgebung
db_psql() {
  docker compose exec -T "$DB_SERVICE" sh -c \
    'psql -X -q -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "${POSTGRES_DB:-$POSTGRES_USER}" "$@"' psql "$@"
}

db_psql -c "CREATE TABLE IF NOT EXISTS proxymaster.schema_migration (
  filename varchar(255) PRIMARY KEY,
  applied_at timestamp NOT NULL DEFAULT now()
);"

APPLIED=0
run_migration() {
  local name="$1" content="$2"
  if [ "$(db_psql -tA -v name="$name" <<< "SELECT count(*) FROM proxymaster.schema_migration WHERE filename = :'name';")" != "0" ]; then
    return 0
  fi
  echo "Migration $name"
  if ! printf '%s' "$content" | base64 -d | db_psql >/dev/null; then
    echo "Migration $name fehlgeschlagen, Deploy abgebrochen (Backend läuft unverändert weiter)" >&2
    exit 1
  fi
  db_psql -v name="$name" <<< "INSERT INTO proxymaster.schema_migration (filename) VALUES (:'name');"
  APPLIED=$((APPLIED + 1))
}
