# Example: scheduling the release check as a nightly routine

This is a prompt you give Claude Code, not a configuration file. It asks Claude
to set up a scheduled run of the release check that only reports.

```
Schedule a routine that runs every weekday at 02:00 (Europe/London) on the main
branch of this repository.

What it does:
1. Run /release-check (the skill in .claude/skills/release-check/SKILL.md).
2. Post the "Release check" block it produces as the routine's summary.

Constraints:
- Report only. Never commit, push, open a PR, or change any file.
- If either suite is below baseline, say NOT READY and list the failing or
  missing tests. Do not try to fix them.
- If npm install or the Maven download fails, say so; that is also a NOT READY.

Name it "Nightly release check".
```

What to expect: Claude confirms the schedule and the prompt it stored. The next
morning you read one short block. If it says NOT READY, that is the first thing
to look at before anyone opens a pull request.

Variations to try:
- Add "also run the app on the demo profile for 30 seconds and curl /api/health" to catch startup
  failures that the tests would not.
- Change "report only" to "open a draft PR with the fix if a single test fails"
  once the team trusts the routine.
