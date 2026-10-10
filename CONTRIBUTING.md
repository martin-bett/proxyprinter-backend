# Contributing

Thanks for helping out!

- Fork the repository and open a pull request against `develop` (the default branch). `master` is the release branch.
- Keep pull requests small and focused. Describe what changes for the user.
- Run `./mvnw verify` before pushing. CI runs the same build against Postgres.
- User-visible texts are not hardcoded: API errors carry a `code` and `params` (the frontend translates them), mail texts live in `src/main/resources/i18n/mail_<lang>.properties`.
- Schema changes need a new idempotent script in `src/main/resources/sql/` and an update of `init.sql`.
- Prefer generic solutions driven by API/gallery configs over special cases for a single source.
- Pull requests from forks are built and tested, but get no preview environment.

Changes that need the frontend go into [martin-bett/mtgprint-front](https://github.com/martin-bett/mtgprint-front) as a separate pull request.
