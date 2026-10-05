---
name: release-check
description: Run before opening a PR. Verifies both test suites against the baseline.
---

# Release check

Copy this file to `.claude/skills/release-check/SKILL.md` and invoke it with
`/release-check`.

## Preconditions

- You are at the root of the ops-dashboard repository (`pom.xml` and `package.json`
  are both present).
- `node_modules/` exists. If not, run `npm install` first and say so.
- The working tree has the change you intend to ship. Do not stash or discard
  anything.

## Steps

1. Run `./mvnw test`. Capture the line that starts with `Tests run:` from the
   summary at the end (the one after all the per-class lines).
2. Run `npm test`. Capture the line that starts with `Tests:`.
3. Compare both counts with the baseline below.
4. Report using the output format.

## Constraints

- Baseline: **Java 25**, **Jest 45**.
- A count BELOW the baseline is a **FAIL**, even if every test that ran passed.
  Fewer tests means something was deleted or skipped; find out what before
  reporting.
- A count above the baseline is fine when the change adds tests. In that case,
  raise the baseline in this file as part of the same change so the next run
  checks against the new number.
- Never edit, skip or delete a test to make the check pass.
- Do not open the PR from this skill; it only reports.

## Output format

```
Release check
  Java:  <n> run, <f> failures, <e> errors   (baseline 25)  PASS|FAIL
  Jest:  <n> passed, <t> total               (baseline 45)  PASS|FAIL
  Verdict: READY | NOT READY
  Notes: <one line per problem, or "none">
```
