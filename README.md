# AI Test Healing Framework

A monorepo demonstrating an AI-agent-driven **Plan → Generate → Execute → Heal** test automation
loop across two independent stacks:

| Stack | Path | Language | Engine |
|---|---|---|---|
| UI (browser) | [`ui-tests/`](./ui-tests) | TypeScript | Playwright + Playwright MCP server |
| API | [`api-tests-java/`](./api-tests-java) | Java | Playwright Java (`APIRequestContext`) + TestNG |

Both stacks use **the same test engine family (Playwright)** and **the same healing
methodology** — a Planner writes a numbered Markdown plan, a Generator turns a scenario into
runnable code following strict framework conventions, and a Healer fixes failures without ever
weakening what a test actually verifies.

## AI provider support

| Priority | Provider | Rulebook it reads | Agent definitions |
|---|---|---|---|
| **Primary** | Claude Code | `CLAUDE.md` | `.claude/agents/*.md` |
| Backup | GitHub Copilot (VS Code) | `.github/copilot-instructions.md` (symlink → `CLAUDE.md`) | `.github/agents/*.agent.md` + `.vscode/mcp.json` |
| Backup | OpenAI Codex CLI | `AGENTS.md` (symlink → `CLAUDE.md`) | none needed — Codex works from the rulebook directly |

All three read **one set of rules** — edit `CLAUDE.md` and every provider picks up the change via
the symlinks. See `CLAUDE.md` for the full chain of authority and both stacks' conventions.

## Setup

### Prerequisites
- Node.js 20+, npm
- Java 11+, Maven 3.9+
- Claude Code (primary) — or VS Code 1.105+ with GitHub Copilot, or OpenAI Codex CLI, as a backup

### UI stack

```bash
cd ui-tests
npm install
npx playwright install --with-deps chromium
npx playwright test tests/seed.spec.ts   # verify the baseline before anything else
```

### API stack

```bash
cd api-tests-java
mvn test   # runs against https://jsonplaceholder.typicode.com — no auth, no setup required
```

No `.env` or credentials are required for either stack out of the box — both target free, public
demo services (SauceDemo for UI, JSONPlaceholder for API). Swap the target in
`ui-tests/playwright.config.ts` (`baseURL`) and `api-tests-java/src/test/resources/application.properties`
(`base.url`) for your own app/API; the conventions in `CLAUDE.md` stay the same.

## Running the agent loop

Full walkthrough with example prompts for both stacks: **[`docs/testing-workflow-guide.md`](./docs/testing-workflow-guide.md)**.

Short version, using Claude Code:

1. **Plan** — `playwright-test-planner` (UI) / `api-test-planner` (API) explores the target and
   writes a numbered scenario plan to `ui-tests/specs/*.md` / `api-tests-java/specs/*.md`.
2. **Generate** — `playwright-test-generator` / `api-test-generator` turns one scenario into a
   Page Object + spec (UI) or a Service Object + TestNG test (API), reusing existing infra.
3. **Execute** — `npx playwright test` (UI) or `mvn test` (API), or the MCP `test_run` tool (UI).
4. **Heal** — on failure, `playwright-test-healer` / `api-test-healer` classifies the root cause,
   applies a minimal fix that preserves the test's original intent, and re-runs. Both escalate to
   a human after 2 failed attempts instead of silently skipping/disabling the test.

## Reporting

- **UI:** Playwright HTML report (`npx playwright show-report` from `ui-tests/`) and Trace Viewer
  for a screenshot-per-action timeline on both pass and fail.
- **API:** JUnit-style XML (`api-tests-java/target/surefire-reports/`, produced by Surefire for
  the TestNG suite) plus Allure (`allure-testng` is wired in `pom.xml`) for a rich pass/fail
  dashboard, and per-request/response
  debug logs at `api-tests-java/target/req-resp.log`.

## Project structure

```
CLAUDE.md                     Primary rulebook (Claude Code) — both stacks, chain of authority
AGENTS.md -> CLAUDE.md        Backup: OpenAI Codex CLI reads this automatically
.claude/agents/               Primary subagents (Claude Code): 3 UI + 3 API roles
.github/copilot-instructions.md -> CLAUDE.md   Backup: GitHub Copilot reads this automatically
.github/agents/               Backup subagents (Copilot .agent.md format): 3 UI + 3 API roles
.github/workflows/            CI: ui-tests.yml, api-tests.yml
.vscode/mcp.json              Backup: MCP server registration for Copilot in VS Code
.mcp.json                     MCP server registration for Claude Code
ui-tests/                     Playwright TypeScript UI suite (Page Object Model)
api-tests-java/               Playwright Java API suite (Service Object Model)
docs/                         Guides
```

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
