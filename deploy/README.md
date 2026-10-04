# Deployment von proxyprinter.eu

Beide Repos bauen bei jedem Push auf `master` ein Docker-Image, legen es in der GitHub Container Registry ab
und rollen es per SSH auf dem VPS aus. Der VPS braucht dafür nur Docker und diesen Ordner.

| Repo | Image | Compose-Service | Tag-Variable in `.env` |
|---|---|---|---|
| mtgprint | `ghcr.io/martin-bett/mtgprint-backend` | `backend` | `BACKEND_TAG` |
| mtgprint-front | `ghcr.io/martin-bett/mtgprint-frontend` | `frontend` | `FRONTEND_TAG` |

Ablauf eines Deploys (`.github/workflows/deploy.yml`, Skript `.github/deploy/remote-deploy.sh`):

1. Image bauen, Tags `sha-<12 Zeichen Commit>` und `latest` nach ghcr.io pushen.
2. Per SSH auf den VPS, in `DEPLOY_PATH` den neuen Tag in die `.env` schreiben, `docker compose pull` und `up -d` nur für diesen Service.
3. Bis zu 150 s prüfen, ob der Container läuft und antwortet (Backend: `/api/pdf/limits`, Frontend: `/`).
4. Gesund: der alte Tag wird als `*_PREVIOUS` gemerkt. Nicht gesund: automatisch zurück auf den alten Tag, der Workflow wird rot.

## Branch-Flow

- Feature-Branches gehen per PR nach `develop`. Auf jedem PR (nach `develop` oder `master`) laufen Build, Tests und ein Test-Build des Docker-Images (`ci.yml`).
- Release: PR `develop` → `master` mergen (Merge-Commit, kein Squash, damit die Branches nicht auseinanderlaufen). Der Push auf `master` startet den Deploy.
- Hotfix: Branch von `master`, PR nach `master`, danach `master` zurück nach `develop` mergen.
- Manuell: Actions → Deploy → Run workflow. Ohne Eingabe wird der aktuelle `master` gebaut und ausgerollt.

### Einmalig in GitHub einstellen (beide Repos)

1. Branch `develop` aus `master` anlegen (Code → Branches → New branch).
2. Settings → General → Default branch auf `develop` stellen, damit neue PRs automatisch dorthin zeigen.
3. Settings → Rules → Rulesets → New branch ruleset für `master` (empfohlen auch für `develop`):
   "Require a pull request before merging" und "Require status checks to pass" mit den Checks `test` und `docker`.
   Die Checks tauchen in der Auswahl erst auf, nachdem sie einmal gelaufen sind.
4. Optional: Settings → Environments → `production` → "Required reviewers" auf dich. Dann wartet jeder Deploy auf deinen Klick.

### Secrets (Settings → Secrets and variables → Actions, in beiden Repos)

| Name | Art | Inhalt |
|---|---|---|
| `VPS_HOST` | Secret | `proxyprinter.eu` oder die IP |
| `VPS_USER` | Secret | SSH-Benutzer auf dem VPS (muss in der Gruppe `docker` sein) |
| `VPS_SSH_KEY` | Secret | Privater Schlüssel des Deploy-Schlüsselpaars (ganzer Inhalt inkl. `-----BEGIN…`) |
| `VPS_KNOWN_HOSTS` | Secret | Ausgabe von `ssh-keyscan -H proxyprinter.eu` |
| `VPS_PORT` | Secret, optional | SSH-Port, wenn nicht 22 |
| `DEPLOY_PATH` | Variable, optional | Ordner auf dem VPS, Standard `/opt/proxyprinter` |

Schlüssel nur für den Deploy erzeugen (auf deinem PC):

```bash
ssh-keygen -t ed25519 -f proxyprinter-deploy -N "" -C "github-actions-deploy"
# proxyprinter-deploy.pub auf dem VPS an ~/.ssh/authorized_keys des Deploy-Benutzers anhängen
# Inhalt von proxyprinter-deploy (ohne .pub) als VPS_SSH_KEY eintragen, danach die Datei löschen
```

## VPS einrichten

Den bisherigen Aufbau auf dem VPS kenne ich nicht. Bitte vorher prüfen:

- [ ] `docker ps` und eine vorhandene `docker-compose.yml`: Wie heißen die Container, welche Ports, welche Volumes?
- [ ] Wo läuft Postgres? Die `application.properties` zeigt auf `localhost:5432`.
  - Postgres schon im Container: dessen Volume in der Vorlage bei `db` eintragen (statt `db-data`), damit die Daten bleiben.
  - Postgres direkt auf dem VPS installiert: den Service `db` löschen, beim Backend
    `SPRING_DATASOURCE_URL: jdbc:postgresql://host.docker.internal:5432/proxymaster` setzen,
    `extra_hosts: ["host.docker.internal:host-gateway"]` ergänzen und in Postgres `listen_addresses`
    sowie `pg_hba.conf` für das Docker-Netz (`172.16.0.0/12`) freigeben.
  - Oder die Daten mit `pg_dump` / `psql` in den neuen `db`-Container umziehen.
- [ ] Liegen die Zertifikate unter `/etc/letsencrypt/live/proxyprinter.eu/` und wie wird erneuert?
  Nach einer Erneuerung muss der Proxy neu laden: `docker compose exec proxy nginx -s reload` (z. B. als certbot `--deploy-hook`).
- [ ] Gecachte Bilder/PDFs: Das Backend schreibt nach `/app/img` und `/app/pdf`, die Vorlage legt dafür Volumes an.

Danach:

```bash
sudo mkdir -p /opt/proxyprinter && sudo chown "$USER" /opt/proxyprinter
cd /opt/proxyprinter
# docker-compose.yml, proxy-nginx.conf und .env.example aus diesem Ordner hierher kopieren
cp .env.example .env && chmod 600 .env    # DB_PASSWORD usw. eintragen
docker compose config -q                  # prüft die Datei
docker compose up -d db                   # nur wenn Postgres im Container laufen soll
```

Erster Deploy, wenn die Secrets gesetzt sind:

1. In **mtgprint-front** Actions → Deploy → Run workflow (startet `frontend`).
2. In **mtgprint** Actions → Deploy → Run workflow (startet `backend`; der Health-Check läuft über den Frontend-Container).
3. Alten Aufbau stoppen, damit 80/443 frei sind, dann `docker compose up -d proxy`.

Die Images sind privat. Der Workflow meldet sich auf dem VPS nur für die Dauer des Deploys mit seinem eigenen
Token an. Wer auf dem VPS selbst `docker compose pull` ausführen will, braucht einmal
`docker login ghcr.io` mit einem GitHub-Token mit `read:packages`, oder stellt die Pakete auf öffentlich.

## Rollback

- **Automatisch:** schlägt der Health-Check fehl, läuft wieder der vorherige Tag.
- **Per GitHub:** Actions → Deploy → Run workflow, bei `tag` entweder `previous` (vorheriger Stand)
  oder einen bestimmten Tag wie `sha-1a2b3c4d5e6f` eintragen (Liste unter Packages im Repo). Es wird dann nichts gebaut.
- **Direkt auf dem VPS:** in `.env` `BACKEND_TAG` bzw. `FRONTEND_TAG` auf den gewünschten Tag setzen,
  dann `docker compose up -d backend` (bzw. `frontend`).

Alte Images bleiben auf dem VPS liegen, damit ein Rollback ohne Download geht.
Gelegentlich aufräumen: `docker image prune -a --filter "until=720h"`.
