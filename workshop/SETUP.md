# Set-up: get green before Part 3, including on a laptop where you cannot install anything

Working with Claude, London. Afternoon session: Claude Code, zero to expert.

Read this before Part 3. Every hands-on lab this afternoon runs in the same small codebase, `ops-dashboard` (a Spring Boot REST API over PostgreSQL with a vanilla JavaScript dashboard, 25 Java tests and 45 Jest tests). You need it open in a Claude Code session before Part 3 starts.

## Which path are you on?

| You have | Use | Labs work? |
|---|---|---|
| A laptop where you can install software and run a terminal | **Path A, local** | All three, fully |
| Only a browser, and a GitHub account (or you can create one) | **Path B, GitHub Codespaces** | All three, fully. The terminal is in the browser |
| Only a browser, and Claude Code on the web is enabled for your organisation | **Path C, Claude Code on the web** | Labs 1 and 2 fully; Lab 3 step 3 is already true (you are remote) |
| None of the above today | **Path D, pair up** | You drive, your neighbour types. Same badges |

## Path A: local install

1. Prerequisites: git, Node 18 or later, Java 17 or later. Check with `git --version`, `node --version`, `java -version`.
2. Install Claude Code: `npm install -g @anthropic-ai/claude-code`, then run `claude` once and sign in with your work account.
3. Get the code:
   ```
   git clone https://github.com/charlottebellet-ant/working-with-claude-lab.git ops-dashboard
   ```
   No git on the laptop? Download the zip from the green **Code** button on GitHub, unzip it, and run `git init && git add -A && git commit -m "Initial import"` inside the folder so you have a repository to work in.
4. Inside the folder: `npm install`, then `npm test` (expect 45 passed) and `./mvnw test` (expect 25 tests, 0 failures; the first run downloads Maven).
5. The database: `docker compose up -d db` starts PostgreSQL 16 on port 5432, and `./mvnw spring-boot:run` connects to it. No Docker? `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run` runs the same app on an in-memory database, and Lab 2 has a no-MCP fallback.
6. Used in Lab 2: start `claude`, type `/mcp`, and check `postgres` is connected (it reads the compose database through `.mcp.json`).

## Path B: GitHub Codespaces (browser only, closest to the local experience)

The repository contains a dev container with Java, Node, Claude Code and a running PostgreSQL pre-installed. Codespaces gives you VS Code and a terminal in a browser tab.

1. Open the workshop repository on GitHub: https://github.com/charlottebellet-ant/working-with-claude-lab.
2. Press **Use this template → Create a new repository** into your own GitHub account (or **Fork**). Name it `ops-dashboard`.
3. On your copy, press **Code → Codespaces → Create codespace on main**. Wait two or three minutes for the container to build.
4. In the terminal at the bottom of the window: `npm test` (45 passed) and `./mvnw test` (25 tests).
5. Type `claude`. It prints a sign-in URL: open it in a new tab, sign in with your work Claude account, paste the code back. You are in.
6. The app runs on port 8080. When Claude Code restarts it, Codespaces shows a **Open in browser** toast for the forwarded port.
7. The database is already running: the dev container starts PostgreSQL next to the app container, so `./mvnw spring-boot:run` and the `postgres` MCP server both work with nothing to install.

Everything in the labs is identical from here. `/mcp` works the same way.

## Path C: Claude Code on the web

Claude Code on the web runs sessions in a cloud sandbox against a GitHub repository, and you type in a browser tab at claude.ai. Your organisation needs it enabled and the GitHub app connected.

1. As in Path B, create your own copy of the workshop repository on GitHub.
2. Open claude.ai, then **Code**, and start a session on your `ops-dashboard` repository.
3. There is no PostgreSQL in the cloud sandbox: the app runs with `SPRING_PROFILES_ACTIVE=demo` (in-memory, same data) and Lab 2 uses the CSV fallback instead of the `postgres` MCP server.
4. Lab 1 and Lab 2 prompts work unchanged; tests run inside the session and Claude reports the counts. You cannot click the running app, so skip "restart the app so I can click it" in the definition of done and ask for a screenshot description instead.
5. Lab 3 step 3 (a remote session) is what you are already doing. Show the mentor your session on the phone app instead.
6. Connectors (Lab 2, `/mcp`) come from your claude.ai connectors rather than a local config.

## Path D: pair up

Sit with someone on Path A or B. You read the ticket, write the brief, decide what to push back on, and review the diff. They type. Tell the mentor you are a pair; both of you can earn the badges.

## Are you green? (three checks, everyone)

1. `claude --version` prints a version.
2. `docker compose up -d db`, then `claude`, then `/mcp` shows `postgres` connected (Codespaces: already running; no Docker: Lab 2 has a fallback).
3. `git pull` (or nothing, if you unzipped) and `npm test` prints `45 passed, 45 total`.

Not green? Hand up. Mentors are walking the room. All three green: leave the session open.

## The badge game

| Lab | Badge | What to show a mentor |
|---|---|---|
| 1 | Shipper | A PR for TODO-231: the plan you pushed back on once, and both suites green |
| 2 | Toolmaker | Your `/release-check` skill running by name and holding the baseline |
| 3 | Orchestrator | Three subagents on TODO-232, and only their summaries coming back |

## Before the day (mentors)

- The repository participants copy from is https://github.com/charlottebellet-ant/working-with-claude-lab. Check it is public and marked as a template.
- Check `docker compose up -d db` and `/mcp` on your own laptop, and have `docs/data/deliveries-last-30-days.csv` ready to point at for anyone without Docker (the lab says how).
- Enable Codespaces for the organisation if participants will use Path B, and check the Claude Code on the web toggle for Path C.
- Do Lab 1 once yourself in a fresh Codespace, so you know how long the container build takes on the venue Wi-Fi.
