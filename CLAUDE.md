# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

ops-dashboard for Marlowe & Finch (fictional): a read-only Spring Boot 3.2 / Java 17 JSON API under `/api` over PostgreSQL, plus a vanilla-JS wall-screen page in `src/main/resources/static/`. The repo is also the material for a Claude Code workshop (`workshop/`, `docs/`); the tickets the labs implement are in `docs/tickets/`.

## Commands

```bash
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run   # run on in-memory H2, no Docker (use this in the cloud)
docker compose up -d db && ./mvnw spring-boot:run    # run on PostgreSQL 16 (default "postgres" profile)
./mvnw test                                          # Java suite (JUnit 5, H2, MockMvc)
./mvnw test -Dtest='DashboardControllerTest#healthReportsUpAndTheFixedToday'
npm install && npm test                              # frontend suite (Jest + jsdom)
npx jest src/test/javascript/api.test.js -t "at most 20"
python3 tools/make_seed.py                           # regenerates V2__seed.sql, the CSV and docs/data/ANSWER-KEY.md (git-ignored)
```

There is no linter or frontend build step. The app serves on port 8080.

## Rules

- `pom.xml` dependencies are frozen: any change needs a CHG ticket. Don't add npm packages either unless asked.
- "Today" is pinned to **2026-09-21** (`ops.today` → `ClockConfig` `Clock` bean). Always get dates from the injected `Clock`, i.e. `LocalDate.now(clock)`, never the no-arg `LocalDate.now()`. Default ranges are the 30 days ending today (`DateRange.resolve`), and test expectations depend on it.
- Persistence is plain SQL with Spring JDBC (`NamedParameterJdbcTemplate` / `JdbcTemplate`) in `DashboardRepository`/`VendorRepository`; no JPA. Schema changes go in new Flyway migrations under `src/main/resources/db/migration/`. SQL must run on both PostgreSQL and H2 in PostgreSQL mode.
- Responses are Java records; controllers stay thin (parse params → `DateRange` → repository).

## Architecture notes

- **Frontend ↔ backend**: `app.js` is one IIFE. `initApp(document, fetchImpl)` takes `fetch` by injection. `createApi(fetchImpl)` is the only network layer and throws `Request failed: <status> <url>` on non-2xx. Startup calls `/api/health` to get "today" from the server, then `loadVendors()` and `load(from, to)`. `load` fetches kpis, on-time, late (`limit=20`) and tickets-by-category with `Promise.all`, and any failure goes to `#status-line`. Charts are inline SVG built in JS. The file exports via `module.exports` under Jest and `window.OpsDashboard` in the browser.
- **Java tests** all use `@SpringBootTest` + `@ActiveProfiles("demo")`: the full app on H2 with the real migrations and seed, no mocks. Assertions use exact seed numbers, so editing the seed or the clock breaks them. `DashboardControllerTest.malformedFromCurrentlyProducesA5xx` documents the current lack of validation and is expected to change with TODO-232.
- **JS tests** go through `src/test/javascript/setup/loadApp.js`. It injects the `<body>` of the real `index.html` into jsdom and replaces `fetch` with `createFakeApi()`, an in-memory fake of every `/api` endpoint built from `FIXTURES` that records `calls`. Overrides look like `loadApp({ kpis: {...}, failing: ['/api/kpis'] })`. **Every new element `id` in `index.html` must be added to `REGISTERED_IDS`**, or `harness.test.js` fails. When an API response shape changes, update `FIXTURES` and the fake routes too.
- Query params (`from`, `to`, `limit`) are not validated yet: a malformed date gives a 500, and `from > to` gives an empty result (TODO-232).
- `.mcp.json` registers a read-only `postgres` MCP server against `postgresql://ops:ops@localhost:5432/ops` (only works with the compose database running).
