# Lab 3: Ship TODO-231

**Part 3, Ship a feature. 30 minutes. Badge: Shipper.**

The whole loop once, in your own clone: onboard, plan, build, review, ship. The ticket is `docs/tickets/TODO-231.md`, a light/dark theme toggle in the header. Everyone ships.

## What you need

- `ops-dashboard` open in a Claude Code session, all three "are you green" checks passed (see `workshop/SETUP.md`).
- The ticket is `docs/tickets/TODO-231.md`; the brief and the definition of done are in `docs/PRE-WORK.md` and repeated below.

## Step 1: explain this codebase. Retell it in two lines

In `claude`:

```
Explain this codebase: what it does, how the frontend talks to the backend, how the two test suites work, and what a new joiner should read first.
```

Turn to your neighbour and retell it in two sentences. If you cannot, ask a follow-up question.

## Step 2: /init. Trim CLAUDE.md to ten lines, plus your rule

```
/init
```

Read what it wrote. It will be long. Cut `CLAUDE.md` to the ten lines that matter (how to run, how to test, the layout, the conventions). Then add the one rule only a human knows, by hand:

```
- pom.xml dependencies are frozen. Any change needs a CHG ticket.
```

Watch a later step obey it.

## Step 3: plan mode. Brief it. Reject the plan once

Press **Shift+Tab** until the status line says plan mode (Claude reads and proposes, but changes nothing). Paste the brief:

```
Read docs/tickets/TODO-231.md and plan the change.

Goal: the light/dark theme toggle described in the ticket, with both test suites still green.

Fences: frontend only. No Java changes. No new dependencies. Where the ticket and this chat disagree, the ticket file wins.

Ask: name every file you will touch and why, write the tests first, and flag anything the ticket leaves open before you start.
```

The plan will come back with an open question: the default theme. Reject the plan once, on purpose, and answer it:

```
Dark by default. Ignore the OS setting. Re-plan, and make that an explicit acceptance criterion (AC-4) with its own test.
```

Read the better plan. Then approve.

## Step 4: build to green. Your neighbour reviews. Open the PR

Approve with the definition of done attached:

```
Approved. When done: run ./mvnw test and npm test and report both counts. New element ids must be registered in src/test/javascript/setup/loadApp.js. Then restart the app so I can click it.
```

Approve the first file edit by hand and read the diff. Then turn the trust dial up (**Shift+Tab** to accept edits) so you are not asked for every file.

Expect one Jest test to fail: the new `theme-toggle` id is not in the harness. Watch Claude read the failure, register the id, and re-run. Both suites green: Jest 48, Java 25.

Then:

```
/code-review
```

Fix one finding, ticket the other with a reason. Re-run the suites. Have your neighbour read the diff. Then:

```
Open the PR with a summary a non-engineer can read: what changed, how to verify it, what was deliberately left out.
```

Path C (Claude Code on the web): skip "restart the app so I can click it"; ask for the `git diff --stat` instead.

## Done when

- [ ] You explained the codebase to a neighbour in two sentences
- [ ] `CLAUDE.md` is about ten lines and contains the pom.xml rule
- [ ] You rejected the first plan and got a better one with AC-4
- [ ] Both suites green: Jest 48, Java 25
- [ ] `/code-review` ran and you dealt with one finding
- [ ] A PR is open (or a branch is pushed) with a readable summary

**Badge: Shipper.** Show a mentor the open PR, the plan you pushed back on, and both suites green.

## If you are stuck

- Claude edits before you approved a plan? You are not in plan mode. Shift+Tab and check the status line.
- Drowning in permission prompts? Turn the dial up to accept edits after you have read the first diff.
- The Jest failure does not mention the harness? Read the message; it names the file to register the id in.
- No `gh` or no push rights? A local branch and a written PR summary earn the badge.
- Stuck five minutes: hand up.
