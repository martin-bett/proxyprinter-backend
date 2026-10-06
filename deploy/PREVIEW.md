# Testumgebungen

- **https://test.proxyprinter.eu**: immer der aktuelle Stand von `develop` (Backend, Frontend und eine eigene Datenbank, beim ersten Start eine Kopie der Produktion ohne Log-Einträge).
- **https://&lt;branch&gt;.test.proxyprinter.eu**: eine Umgebung pro PR nach `develop`. Der Name kommt aus dem Branch, ohne `claude/` (z. B. `claude/project-thread-2o4t9m` wird zu `project-thread-2o4t9m.test.proxyprinter.eu`).
  - Haben ein Backend- und ein Frontend-PR denselben Branch-Namen, landen beide in derselben Umgebung.
  - Was der PR nicht ändert, kommt von develop. Ein reiner Frontend-PR benutzt das develop-Backend und dessen Datenbank.
  - Ein Backend-PR bekommt eine eigene Datenbank (Kopie der test-Datenbank), seine SQL-Skripte laufen nur dort.
  - Der Link steht als Kommentar im PR. Jeder Push rollt neu aus.
  - Nach Merge oder Schließen wird der Teil entfernt; sind beide PRs zu, wird die ganze Umgebung samt Datenbank, nginx-Eintrag und Zertifikat gelöscht.
- Alle Testumgebungen sind mit Benutzer und Passwort geschützt, E-Mail-Versand ist aus.

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
