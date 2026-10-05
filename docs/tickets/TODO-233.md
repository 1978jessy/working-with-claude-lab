# TODO-233: Add `GET /api/summary`

**Type:** Feature
**Area:** Backend
**Priority:** Low

Small and self-contained, a good candidate for a remote or cloud session that runs
on its own and comes back with a pull request.

## Story

The ops lead reads one line at the morning stand-up. Add an endpoint that returns
the four KPIs plus the two names that matter: the carrier with the lowest on-time
rate and the ticket category with the most tickets, for the same date range as the
rest of the API.

## Acceptance criteria

- **AC-1** `GET /api/summary?from&to` returns `200` with a JSON body of the form
  `{ "from": "2026-08-22", "to": "2026-09-21", "onTimeRate": 0.937, "openTickets": 114,
  "revenue": 360095.50, "orders": 624, "worstCarrier": "Kessler Logistics",
  "busiestTicketCategory": "Delivery delay" }`.
- **AC-2** `from` and `to` behave exactly as on `/api/kpis` (same defaults, same
  `DateRange`).
- **AC-3** `worstCarrier` is the carrier with the lowest on-time rate among carriers
  that delivered something in the range; `busiestTicketCategory` is the category
  with the most tickets opened in the range. Both are `null` when the range is empty.
- **AC-4** Computed from the repositories at request time, reusing the existing
  queries; no new SQL is needed.
- **AC-5** Tests: a controller test for the route and body shape with the default
  range, and one for an empty range. `./mvnw test` count goes up accordingly and
  `npm test` stays at its baseline.

Fences:

- No frontend changes.
- No new dependencies.

## Definition of done

- Run `./mvnw test` and `npm test` and report both counts.
- Open a pull request titled `TODO-233: add /api/summary` with the counts in the
  description.
