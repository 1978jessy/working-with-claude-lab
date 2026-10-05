# TODO-232: Server-side validation of query parameters

**Type:** Hardening
**Area:** Backend
**Priority:** High

## Story

The API runs whatever it is given. `from=next-tuesday` blows up inside
`LocalDate.parse` and comes back as `500 Internal Server Error`; `from` after `to`
silently returns an empty result that looks like a quiet week; `limit=-1` or
`limit=99999999` goes straight into the SQL `LIMIT`. The dashboard itself always
sends sensible values, so this is defence in depth: anything that talks to `/api`
directly (the wall-screen script, the finance export, curl) should get a clear
error rather than a stack trace or misleading zeros.

There is a test in `DashboardControllerTest` (`malformedFromCurrentlyProducesA5xx`)
that documents the current behaviour. It will need to change.

## Acceptance criteria

- **AC-1** `from` and `to`, when present, must be valid ISO dates (`YYYY-MM-DD`).
- **AC-2** `from` must be on or before `to`, and the range may span at most 366 days.
  The defaults (last 30 days ending today) still apply when a parameter is missing.
- **AC-3** `limit`, when present, must be an integer between 1 and 500. The default
  stays 20.
- **AC-4** Invalid input returns `400 Bad Request` with a JSON body of the form
  `{ "errors": ["from must be an ISO date (YYYY-MM-DD)"] }`. Several problems in one
  request produce several entries. The rules apply to every endpoint that takes the
  parameter (`/api/kpis`, `/api/deliveries/on-time`, `/api/deliveries/late`,
  `/api/tickets/by-category`).
- **AC-5** Existing tests are updated, new tests are added, both suites are green.

Fences:

- `pom.xml` is frozen (see the comment at the top). Do not add
  `spring-boot-starter-validation` or any other dependency; write the checks by hand.
- No frontend changes are needed.

## Suggested split for three subagents

- **auditor** (read-only): find every request parameter that reaches a repository
  query (controller methods, `DateRange.resolve`, the `limit` parameter) and list,
  per endpoint, which parameters are unchecked and what happens today with bad input.
  Output: a short table.
- **fixer**: implement the parsing and range checks in one place (a small validator
  the controllers call, or inside `DateRange`) and the 400 response shape in a
  controller advice, following the auditor's table. No test changes.
- **verifier**: rewrite the "currently produces a 5xx" test, add tests for each AC
  (malformed date, `from` after `to`, a 367-day range, `limit` 0, `limit` 501, several
  errors at once, and a valid request still working), and run `./mvnw test` and
  `npm test`. Report both counts.

## Definition of done

- Run `./mvnw test` and `npm test` and report both counts.
- The test that documented the old behaviour has been rewritten, not deleted.
- Restart the app and show one `curl` with `from=next-tuesday` returning 400.
