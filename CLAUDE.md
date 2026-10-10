# CLAUDE.md – mtgprint (Backend)

Spring Boot backend for proxyprinter.eu: stores API/gallery/deckfinder configs in Postgres, proxies images,
builds the proxy PDF (PDFBox) and sends it by mail. Frontend lives in `martin-bett/proxyprinter-frontend`.

## Stack
- Spring Boot 3.5.6, Java 25 (`java.version` in `pom.xml`), packaged as WAR (`target/mtgprint-0.0.1-SNAPSHOT.war`)
- Postgres, schema `proxymaster`; JPA (Hibernate), MapStruct, Lombok, PDFBox 2, jsoup, Tika
- Config: `src/main/resources/application.properties` (keys like `pdf.*`, `searchcache.*`, `log.db.*`, `apiconfig.cleanup.*`)

## Where things live (`src/main/java/my/poo/mtgcrawl/mtgprint/`)
- `resource/` – REST controllers (`/api/configs`, `/api/gallery…`, `/api/pdf`, `/api/proxy`, `/api/files`, `/api/ratings`, deckfinder, search cache)
- `entitiy/` (sic) – JPA entities; `entitiy/dto/` – DTOs
- `repository/` – Spring Data repositories
- `service/rest/` – config services, image fetch/upload/download, API probe, nightly config cleanup
- `service/pdf/` – PDF generation, job/progress handling, PDF and image caches, mail notifier
- `service/gallery/` – generic server-side fetch for gallery configs (link pattern, credentials); no per-source code
- `service/deckfinder/` – deck import from deckbuilder sites
- `service/searchcache/` – shared search result cache (JSON files)
- `service/mapper/` – MapStruct mappers; `service/log/` – DB log appender; `service/mail/` – mail
- Tests: `src/test/java/...` mirror the packages

## Build, test, run
- Cloud container has only JDK 21. Use:
  `mvn test -Djava.version=21 -Dmaven.compiler.release=21`
- CI (`.github/workflows/ci.yml`) runs `mvn -B verify` on Java 25 against a Postgres service initialised with
  `src/main/resources/init.sql`, plus a multi-arch Docker test build (`docker.dockerfile`).
- Locally: VS Code launch config in `.vscode/`; `/api` is called by the frontend dev server via proxy to `localhost:8080`.

## Database migrations
- No `ddl-auto`. Schema changes go into a new script in `src/main/resources/sql/`, named `YYYY-MM-DD_<topic>.sql`.
- Scripts must be idempotent (`CREATE TABLE IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, …): they can run any number of times.
- The Deploy workflow runs them in filename order before restarting the backend
  (`.github/deploy/build-migrations.sh`, `.github/deploy/remote-migrate.sh`).
- Locally: `db-update-local.cmd` (or VS Code task "DB lokal aktualisieren") pipes all scripts into the Docker container `proxymaster`.
- `init.sql` is the full schema for a fresh DB (used by CI); keep it in sync when adding tables/columns.

## Branches and deploy
- `develop` is the default branch. All PRs target `develop` (CI only, no deploy).
- Release = PR `develop` → `master` (merge commit). Only a push to `master` deploys (`.github/workflows/deploy.yml`, see `deploy/README.md`).
- Release order: backend first, wait for green Deploy, then frontend.

## Working conventions
- Answer martin in German.
- No user-visible text hardcoded in German or English only: API errors carry a `code` + `params` (plus an English `error` fallback; the frontend translates), mail texts live in `src/main/resources/i18n/mail_<lang>.properties` (`MailTexts`, all languages need all keys).
- Commit and push early and often; open a draft PR early (unpushed work has been lost before).
- Prefer generic solutions driven by API/gallery configs over source-specific special cases in code.
- Blocked sources: `proxymaster.blocked_host` (domain incl. subdomains, IP or CIDR; see `sql/2026-10-10_blocked_host.sql`) is checked by `service/net/HostBlocklist`
  on saving/copying/publishing configs, on delivering configs and in `OutboundUrlGuard`/`GalleryHttp` for every outbound fetch.
  New config URL fields or new outbound HTTP clients must go through it. Maintained via SQL, reloaded every `blocklist.refresh-seconds`.
  Operators can also block their own host via `/api/blocklist` (`service/optout/`): mail link to admin@/webmaster@/… of the domain,
  or a token as DNS TXT `_proxyprinter-verify.<host>` / file `/.well-known/proxyprinter-verify.txt` (no redirects followed).
- `archive/` holds old recovered code; don't edit it.
- Skip labels: PR labels `skip-preview` (no test deployment) and `skip-tests` (skip the `test` job) are evaluated by `ci.yml`
  for PRs to `develop` only. If a change is tiny or has no functional effect, propose them in the PR description with a one-line
  reason; apply a label only after martin agrees (see `deploy/PREVIEW.md` in the backend repo).
