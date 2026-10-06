# Testumgebungen

- **https://test.proxyprinter.eu**: immer der aktuelle Stand von `develop` (Backend, Frontend und eine eigene Datenbank, beim ersten Start eine Kopie der Produktion ohne Log-Einträge).
- **https://&lt;kürzel&gt;.test.proxyprinter.eu**: eine Umgebung pro PR nach `develop`. Das Kürzel sind die ersten 6 Zeichen eines Hashes des Branch-Namens (z. B. `153d5e.test.proxyprinter.eu` für `claude/project-thread-2o4t9m`). Es ist in beiden Repos gleich und steht im PR-Kommentar.
  - Haben ein Backend- und ein Frontend-PR denselben Branch-Namen, landen beide in derselben Umgebung.
  - Was der PR nicht ändert, kommt von develop. Ein reiner Frontend-PR benutzt das develop-Backend und dessen Datenbank.
  - Ein Backend-PR bekommt eine eigene Datenbank (Kopie der test-Datenbank), seine SQL-Skripte laufen nur dort.
  - Der Link steht als Kommentar im PR. Jeder Push rollt neu aus.
  - Ändert ein PR nur Doku (`*.md`, `docs/`, `.claude/`, `archive/`, `LICENSE`, `TODO`, `.gitignore`), wird nichts ausgerollt. Dasselbe gilt für einen Push auf develop.
  - Nach Merge oder Schließen wird der Teil entfernt; sind beide PRs zu, wird die ganze Umgebung samt Datenbank, nginx-Eintrag und Zertifikat gelöscht.
- Alle Testumgebungen sind mit Benutzer und Passwort geschützt, E-Mail-Versand ist aus.
- PR-Backends schalten den gemeinsamen Such-Cache ab (`SEARCHCACHE_ENABLED=false`) und behalten PDFs, Uploads und DB-Logs nur kurz. Neue speicherintensive Funktionen bekommen einen Schalter und werden in `write_compose` in `.github/deploy/preview.sh` für PRs ausgeschaltet. Alles, was ein PR-Backend auf die Platte schreibt (auch der Bild-Cache), liegt im Container und verschwindet mit der Umgebung.

Technik: `.github/deploy/preview.sh` (in beiden Repos identisch), aufgerufen aus `ci.yml` (Job `preview`) und `preview-close.yml`. Auf dem Server liegt alles unter `/root/mnt/proxyprinter-test`, die Produktion wird nur gelesen (Datenbank-Kopie beim ersten Start).

## Einmalige Einrichtung

### 1. DNS bei netcup

netcup CCP → Domains → `proxyprinter.eu` → DNS. Zwei neue Einträge, Ziel ist dieselbe IP wie beim Eintrag `@` (Typ A):

| Host | Typ | Ziel |
|---|---|---|
| `test` | A | IP des VPS |
| `*.test` | A | IP des VPS |

Speichern. Es kann bis zu einer Stunde dauern, bis die Namen erreichbar sind.

### 2. Server

Per SSH als root auf dem VPS einloggen und diesen Block komplett einfügen.
Am Ende fragt `htpasswd` zweimal nach einem Passwort: Das ist das Passwort für die Testseiten (Benutzer `martin`), nicht das Root-Passwort.

```bash
# ===== START: auf dem VPS (als root) =====
mkdir -p /root/mnt/proxyprinter-test /var/www/proxyprinter-test-acme
cd /root/mnt/proxyprinter-test
[ -f .env ] || echo "DB_PASSWORD=$(openssl rand -hex 16)" > .env
chmod 600 .env
grep -q 'conf.d/\*.conf' /etc/nginx/nginx.conf && echo "OK: nginx liest conf.d" || echo "ACHTUNG: nginx liest conf.d nicht, bitte Ausgabe an Claude schicken"
cat > /etc/nginx/conf.d/proxyprinter-test.conf <<'EOF'
# Testumgebungen: Zertifikats-Abfrage und Umleitung auf https (die https-Teile legt preview.sh an)
server {
    listen 80;
    server_name test.proxyprinter.eu *.test.proxyprinter.eu;
    location /.well-known/acme-challenge/ { root /var/www/proxyprinter-test-acme; }
    location / { return 301 https://$host$request_uri; }
}
EOF
nginx -t && systemctl reload nginx
command -v htpasswd >/dev/null || apt-get install -y apache2-utils
htpasswd -c /etc/nginx/proxyprinter-test.htpasswd martin
echo "FERTIG"
# ===== ENDE =====
```

Weitere Personen bekommen Zugang mit `htpasswd /etc/nginx/proxyprinter-test.htpasswd NAME` (ohne `-c`).

### 3. Erster Start von test.proxyprinter.eu

Sobald die PRs mit dieser Funktion in `develop` gemergt sind, startet die develop-Umgebung von selbst. Falls nicht (oder ein Lauf rot war):

1. GitHub → `mtgprint-front` → Actions → **CI** → "Run workflow" → Branch `develop` → Run.
2. Danach dasselbe in `mtgprint`.

Erst Frontend, dann Backend: Beim ersten Backend-Lauf wird die Produktions-Datenbank kopiert, das dauert etwas länger.

## Nützliche Befehle (auf dem VPS)

```bash
# ===== START: auf dem VPS =====
ls /root/mnt/proxyprinter-test/envs                    # offene PR-Umgebungen
docker ps --filter name=ppt- --format "table {{.Names}}\t{{.Image}}\t{{.Status}}"
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}" | grep ppt-
# ===== ENDE =====
```

Logs einer Umgebung: `cd /root/mnt/proxyprinter-test/envs/<name> && docker compose logs --tail 100` (bzw. `.../develop`).

Datenbank von test neu aus der Produktion kopieren: `rm /root/mnt/proxyprinter-test/develop/.seeded`, dann CI auf `develop` in `mtgprint` neu ausführen.

## Testumgebung und Tests pro PR überspringen (Labels)

Zwei Labels am PR, getrennt entscheidbar (gelten nur für PRs nach `develop`; Pushes auf develop/master und Release-PRs nach master ignorieren sie):

- `skip-preview`: keine Testumgebung. Das Image wird nur gebaut (außer zusammen mit `skip-tests`), nicht gepusht und nicht ausgerollt. Eine schon laufende Umgebung bleibt bis Merge/Schließen auf dem alten Stand.
- `skip-tests`: der Job `test` (Typecheck/Tests/Build bzw. `mvn verify`) wird übersprungen. Ein übersprungener Job gilt bei required checks als erfüllt, der PR bleibt mergebar. Die Testumgebung läuft trotzdem, wenn `skip-preview` fehlt.

Label setzen oder entfernen startet CI neu und wertet neu aus. Fehlende Labels legt der Job `labels` in `ci.yml` selbst an.

Regel für Claude-Threads: Ist eine Änderung winzig oder hat keine Funktionsänderung (Tippfehler, Texte, Kommentare, Config ohne Wirkung), schlägt der Thread `skip-preview` und/oder `skip-tests` in der PR-Beschreibung mit einer Zeile Begründung vor. Das Label wird erst gesetzt, wenn martin zustimmt; martin kann auch selbst Labels setzen oder einen Vorschlag ablehnen.
