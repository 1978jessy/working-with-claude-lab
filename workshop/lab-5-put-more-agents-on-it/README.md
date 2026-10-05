# Lab 5: Put more agents on it

**Part 5, Scale it. 20 minutes. Badge: Orchestrator.**

From one developer to the organisation: subagents, a routine on a clock, a session that follows you, and a swap with another table.

## Step 1: TODO-232. Ask for three subagents

The ticket is `docs/tickets/TODO-232.md`, server-side validation. Ask for three subagents by name and watch only the summaries come back:

```
Read docs/tickets/TODO-232.md. Do it with three subagents, in this order:
- auditor: find every request parameter that reaches a database query and list what is currently unchecked. Report a list, no code.
- fixer: implement the validation the ticket asks for, following the auditor's list. Frontend untouched.
- verifier: write the tests, run ./mvnw test and npm test, and report both counts against the baseline (Java 25, Jest 48).
Give me each agent's summary, then the combined result. Do not paste their full transcripts into this session.
```

Optional: the three agent definitions are in `docs/examples/agents/`. Copy them into `.claude/agents/` if you want them reusable.

## Step 2: schedule the release check. Nightly, report only

```
Schedule the release check to run nightly at 02:00, report only: run /release-check on main and post the result as a note. Never push or merge. Read the routine back to me before you save it.
```

Read the routine back. Report only, nothing that pushes. Then find it in your list of scheduled runs.

## Step 3: start a remote session. Close the lid

Hand a small task to a session that runs somewhere else. The ticket is `docs/tickets/TODO-233.md`, a summary endpoint:

```
Start a remote session on this repository for docs/tickets/TODO-233.md: add GET /api/summary with tests, run both suites, open a PR. I will check on my phone.
```

Close the lid. Open the Claude app on your phone if you have it and watch the session. Path C (Claude Code on the web): you are already remote; open the same session on your phone and show the mentor.

## Step 4: time left? Swap a recipe with another table

Swap with another table: run their skill, brief, hook or routine on your repo, or lend them yours. Both tables earn the Borrower badge, and it counts double.

## Done when

- [ ] TODO-232 was done by three named subagents and only summaries came back
- [ ] Both suites green with the new validation tests (Java above 25, Jest 48)
- [ ] A nightly, report-only routine exists and you read it back
- [ ] A remote session ran a task without you at the keyboard
- [ ] Bonus: you ran another table's recipe

**Badge: Orchestrator.** Show a mentor the three subagents on TODO-232 and the summaries that came back.

## If you are stuck

- Subagents dump their whole transcript? Say: `Summaries only. Under ten lines each.`
- Scheduling is not available on your plan or environment? Write the routine as a prompt in `docs/examples/routine.md` and show the mentor; ask what would need to be true to enable it.
- No phone app? A mentor can show you theirs. The point is the session survived the lid closing.
- Finished early: `/permissions` and read what is locked from above. That is managed settings.
