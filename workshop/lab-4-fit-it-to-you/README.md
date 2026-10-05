# Lab 4: Fit it to you

**Part 4, Tailor the loop. 25 minutes. Badge: Toolmaker.**

The receipt, the guardian, the live system, and your know-how saved as a skill. Stay in the session from Lab 3.

## Step 1: /usage. Write down what TODO-231 cost

```
/usage
```

Write the wall-clock time and the cost of TODO-231 on your badge card. That is the number for the meeting. Time left? Try the dials on something small:

```
/model sonnet
/effort medium
Add a favicon link to index.html.
```

Routine work gets the cheapest setting that reliably succeeds.

## Step 2: /security-review your change. Deal with one finding

```
/security-review
```

It reads your own change for security findings before the PR. Expect the stored theme value read from localStorage without validation. Deal with one finding: fix it, or ticket it with a reason.

Then try the rule that always fires. Copy `docs/examples/settings.hooks.json` into `.claude/settings.json` (and `docs/examples/hooks/pom-guard.sh` next to it), make the script executable, and ask:

```
Add lodash to pom.xml.
```

The hook blocks the edit with "pom.xml dependencies are frozen. A change needs a CHG ticket." CLAUDE.md is advice. A hook is a wall.

## Step 3: /mcp. Ask the database which carrier let you down

```
/mcp
```

Check `postgres` is connected (it is the server in `.mcp.json`, reading the compose database; `docker compose up -d db` if it is not). Then:

```
Which carrier was late most often last week, and how many deliveries was that? Use the database.
```

Watch it look at the schema, write the query and answer with a number. Ask a follow-up: "and over the last 30 days, per carrier?"

**No MCP?** The same deliveries are in the repo at `docs/data/deliveries-last-30-days.csv` (order, carrier, promised date, delivered date, days late). Ask the same question and point at that path. Compare with your neighbour who used the database: same numbers, different reach.

## Step 4: save your definition of done as a skill. Call it by name

Turn the definition of done from Lab 3 into a skill. A template is in `docs/examples/release-check.SKILL.md`.

```
Create .claude/skills/release-check/SKILL.md from docs/examples/release-check.SKILL.md. Baseline: Java 25, Jest 48 (the count after TODO-231). A count below baseline is a FAIL even if everything passes.
```

Then run it by name:

```
/release-check
```

It should run both suites, report both counts, and say "baseline held". Now delete one Jest test and run `/release-check` again. It must fail, even though everything that remains passes. Restore the test.

## Done when

- [ ] You wrote the cost and wall-clock of TODO-231 on your card
- [ ] `/security-review` ran and you fixed or ticketed one finding
- [ ] The pom.xml hook blocked an edit on your screen (or you read why it would)
- [ ] You answered the late-carrier question, from the database or from the CSV
- [ ] `/release-check` runs by name and fails when a test is removed

**Badge: Toolmaker.** Show a mentor `/release-check` running by name and holding the baseline. A hook that fired on your screen also counts.

## If you are stuck

- `/mcp` shows `postgres` failed? Is the database up (`docker compose ps`)? If you cannot run Docker, use the CSV in `docs/data/`. The lab is about the question, not the transport.
- The skill does not trigger by name? Check the folder name matches `name:` in the frontmatter, and start a new session so it is picked up.
- The skill passes with a test removed? It is not holding the baseline. Fix the skill, not the run.
