# Pre-work: get green before the workshop

Please do this before the session. It takes about 15 minutes, most of it waiting
for downloads. If anything is red, bring it to the session and we will fix it in
the first ten minutes.

## 1. Pick an environment

You need one of these three. The first is the one we will use on stage.

**Option A: local install**

- Node 18 or newer (`node --version`)
- Java 17 or newer (`java -version`)
- git
- Claude Code: `npm install -g @anthropic-ai/claude-code`
- Docker Desktop (or any `docker compose`) for the database. Optional: without it
  the app runs on an in-memory database, see check 2.

Maven is not needed; the repo ships the Maven wrapper (`./mvnw`).

**Option B: GitHub Codespaces (nothing to install)**

Open https://github.com/charlottebellet-ant/working-with-claude-lab on GitHub, press *Use this template*
→ *Create a new repository* into your own account, then on your copy click
*Code*, then *Codespaces*, then *Create codespace on main*. The container in `.devcontainer/` has Java 17, Node 20,
Claude Code and a running PostgreSQL preinstalled. Give it two minutes the first time.

**Option C: Claude Code on the web**

Create your own copy of the repository as in Option B, then connect it from
claude.ai/code. The cloud environment installs the
same tools and runs the app on the `demo` profile (no database needed). You can
still follow along; a few local-only steps (hooks, the browser refresh) will be
shown on stage.

## 2. Clone and install

```bash
git clone https://github.com/charlottebellet-ant/working-with-claude-lab.git ops-dashboard
cd ops-dashboard
npm install
```

## 3. Are you green? Three checks

**Check 1: Claude Code starts**

```bash
claude --version
```

Prints a version number. If it asks you to log in, do that now.

**Check 2: the database connection**

With Docker:

```bash
docker compose up -d db
claude
/mcp
```

Shows `postgres` as connected. Part 4 of the workshop uses it to ask Claude Code a
question straight from the database. Type `/exit` to leave. Codespaces already has
the database running, so only the `/mcp` part applies there.

Without Docker: run the app with `SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run`
(in-memory database, same data) and use the CSV in `docs/data/` for the Part 4
question. `/mcp` will show `postgres` as failed; that is expected.

**Check 3: the test suites**

```bash
git pull && npm test
```

Ends with `Tests: 45 passed, 45 total`.

Optionally, also run `./mvnw test` once now so the Java dependencies are cached
before the session (it prints `Tests run: 25, Failures: 0, Errors: 0`). The tests
do not need the database.

## 4. Two blocks you will paste during the workshop

Keep these somewhere handy. You will paste them into Claude Code in Part 3.

### The brief for TODO-231

```
Read docs/tickets/TODO-231.md and plan the change. Do not write code yet.

Goal: a light/dark theme toggle in the header, as described in the ticket.

Fences:
- Frontend only. No Java changes.
- No new dependencies.
- The ticket file wins over anything I say in chat. If they disagree, say so.

Ask:
- Name the files you will touch and why.
- Tests first: list the tests you will add or change before any implementation.
- Flag anything the ticket leaves open and ask me before deciding.
```

### The definition of done

```
Before you say you are done:
- Run ./mvnw test and npm test and report both counts.
- Register any new element id in src/test/javascript/setup/loadApp.js.
- Restart the app so I can click it at http://localhost:8080.
```

## Note on pom.xml

Dependencies in `pom.xml` are frozen. Changes require a CHG ticket. In the
workshop we will install a hook that enforces this (see `docs/examples/`).
