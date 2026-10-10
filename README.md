# ProxyPrinter – Backend

Backend of [proxyprinter.eu](https://proxyprinter.eu): stores API, gallery and deckfinder configs, fetches images
from configured sources, builds the printable proxy PDF and can send it by mail.

The React frontend lives in [martin-bett/mtgprint-front](https://github.com/martin-bett/mtgprint-front).

## Stack

Spring Boot 3.5, Java 25, PostgreSQL (schema `proxymaster`), JPA/Hibernate, MapStruct, Lombok, PDFBox, jsoup, Tika.

## Run locally

Requirements: JDK 25, Maven (or `./mvnw`), Docker for Postgres.

```bash
# Postgres with the dev defaults from application.properties (local only)
docker run --name proxymaster -p 5432:5432 -e POSTGRES_DB=proxymaster \
  -e POSTGRES_USER=mtguser -e POSTGRES_PASSWORD=mtgpassword -d postgres:16
psql -h localhost -U mtguser -d proxymaster -f src/main/resources/init.sql

./mvnw spring-boot:run      # http://localhost:8080, the frontend dev server proxies /api here
./mvnw verify               # build and tests (CI runs the same against Postgres)
```

Settings live in `src/main/resources/application.properties` and can be overridden with environment variables
(`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_MAIL_*`, `GALLERY_CREDENTIALS_*`, …).
`deploy/.env.example` lists what a server installation needs. Never commit real credentials.

## Database changes

There is no `ddl-auto`. Schema changes go into a new idempotent script `src/main/resources/sql/YYYY-MM-DD_<topic>.sql`;
keep `init.sql` (full schema for a fresh database) in sync.

## Project layout (`src/main/java/my/poo/mtgcrawl/mtgprint/`)

- `resource/` – REST controllers (`/api/...`)
- `entitiy/` – JPA entities and DTOs
- `service/` – config services, image fetching, PDF generation, gallery fetch, deckfinder, search cache, mail
- `CLAUDE.md` – short developer notes (also used by AI coding agents)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security issues: see [SECURITY.md](SECURITY.md).

## Disclaimer

ProxyPrinter is an unofficial, non-commercial fan project. It is not affiliated with, endorsed or sponsored by
Wizards of the Coast or any other publisher. Magic: The Gathering and all related names are trademarks of their
respective owners. This repository contains no card images.
