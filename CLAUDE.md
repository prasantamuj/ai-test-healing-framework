# Project rules for AI agents

This is a monorepo with two independent test automation stacks, each with its own
Plan → Generate → Execute → Heal loop:

| Stack | Path | Language | Drives |
|---|---|---|---|
| UI (browser) | `ui-tests/` | TypeScript | Playwright + Playwright MCP server |
| API | `api-tests-java/` | Java | Playwright Java (`APIRequestContext`) + JUnit 5 + Maven |

## AI provider priority

**Primary: Claude Code.** This file (`CLAUDE.md`) is the rulebook Claude Code loads
automatically, and the subagents in `.claude/agents/` are the primary implementation of every
role below (Planner, Generator, Healer — for both stacks).

**Backup: GitHub Copilot (VS Code).** Equivalent `.agent.md` files live in `.github/agents/` and
an MCP config at `.vscode/mcp.json`, so the same workflow runs if Claude Code isn't available.
`.github/copilot-instructions.md` is a symlink to this file — same rules, no drift.

**Backup: OpenAI Codex CLI.** Codex CLI auto-loads `AGENTS.md`, which is a symlink to this file.
No separate agent-role files exist for Codex — it works from this rulebook plus your own prompts.

All three read the **same rules** from this one file. Never fork the rules — edit here only; the
symlinks propagate the change everywhere.

## Chain of authority

1. The user's prompt in the current conversation
2. This file (`CLAUDE.md` / `AGENTS.md` / `.github/copilot-instructions.md` — one file, three names)
3. The specific subagent's definition (`.claude/agents/*.md` or `.github/agents/*.agent.md`)
4. Playwright / Playwright-Java / MCP tool defaults

When an agent's behavior surprises you, walk down this list to find where the instruction is
coming from.

---

## UI stack (`ui-tests/`)

### Stack
- Playwright 1.62+ with TypeScript, Node 20+
- Test runner: `@playwright/test`
- MCP server: `playwright-test` (`npx playwright run-test-mcp-server`, see `ui-tests/.mcp.json` /
  root `.mcp.json`)
- Target app: https://www.saucedemo.com (swap for your own app; keep the same conventions)

### Folder structure
- `ui-tests/src/pages/` — Page Object classes (one file per page)
- `ui-tests/src/fixtures/` — Custom fixtures extending base test
- `ui-tests/src/utils/` — Pure helpers, no test logic
- `ui-tests/tests/` — Spec files, mirror app URL structure
- `ui-tests/tests/data/` — JSON/CSV test data
- `ui-tests/specs/` — Planner output (Markdown plans)

### Coding conventions
- Import `test` from `ui-tests/src/fixtures/base.ts`, never from `@playwright/test` directly
- Use `test.describe` per feature area
- One logical assertion group per test
- Use `test.step` for readability when a flow has more than 3 actions
- File names: kebab-case (`add-to-cart.spec.ts`)

### Locator priority (STRICT — do not deviate)
1. `getByRole` with accessible name
2. `getByLabel` for form fields
3. `getByTestId` (attribute is `data-test-id`)
4. `getByText` only for genuinely static UI text
5. CSS / XPath — forbidden unless explicitly approved, and only with a comment justifying it

### Page Object contract
- One class per page, extends `BasePage`
- Constructor takes `page: Page` only
- All locators declared as `readonly` in constructor
- Action methods return `Promise<void>` OR the next page object
- No `expect()` calls inside page objects — assertions belong in tests
- No business logic in tests — put it in page objects or helpers

### Assertion rules
- Web-first assertions only (`expect(locator).toBeVisible()`)
- No `page.waitForTimeout` — ever
- No `waitForSelector` — use locator auto-waiting
- No `networkidle` waits
- Custom timeouts only when justified in a code comment

### Seed file
`ui-tests/tests/seed.spec.ts` is the baseline every UI agent references for base URL and
environment setup. If it is broken, fix it before anything else.

---

## API stack (`api-tests-java/`)

### Stack
- Java 11+, Maven 3.9+
- `com.microsoft.playwright:playwright` — `APIRequestContext` for HTTP calls (chosen over
  REST Assured so both stacks share one automation engine and one healing methodology)
- JUnit 5 (`@Test`, `@Tag`) as the runner
- Allure (`allure-junit5`) + Surefire JUnit XML for reporting
- SLF4J + Logback for request/response logging
- Target API: https://jsonplaceholder.typicode.com (free, unauthenticated, stable — swap for
  your own API; keep the same conventions)

### Folder structure
- `api-tests-java/src/main/java/.../config/` — base configuration (base URL, timeouts, headers,
  auth) loaded from `application.properties` / `env-*.properties`
- `api-tests-java/src/main/java/.../clients/` — "Service Objects" (the API equivalent of a Page
  Object): one class per resource/endpoint group
- `api-tests-java/src/main/java/.../model/` — POJOs for request/response bodies
- `api-tests-java/src/test/java/.../tests/` — JUnit 5 test classes, mirror the API's resource
  structure
- `api-tests-java/src/test/resources/` — `application.properties`, `logback.xml`
- `api-tests-java/specs/` — Planner output (Markdown plans), same numbering convention as UI

### Coding conventions
- Every test class extends `BaseApiTest` (creates/tears down the shared `APIRequestContext`)
- Never hardcode the base URL, headers, or credentials in a test — load from `ApiConfig`
- One logical assertion group per test method
- File/class names: PascalCase for Java (`PostsApiTest.java`), matching the resource under test
- Tag every test with JUnit `@Tag("smoke")`, `@Tag("regression")`, or `@Tag("critical")`

### Service Object contract (API equivalent of the Page Object contract)
- One class per resource, in `clients/`, constructor takes `APIRequestContext` only
- Methods return a typed response wrapper (status code + parsed body), never a raw assertion
- No JUnit `assert*`/`Assertions.*` calls inside a Service Object — assertions belong in tests
- No business logic (retry loops, polling) beyond what `APIRequestContext`'s own timeout config
  already provides

### Assertion rules
- Assert on status code AND response body shape/values — never status code alone
- No `Thread.sleep` — ever
- No blanket `try/catch` that swallows a failed assertion
- Custom timeouts only when justified in a code comment

### Seed file
`api-tests-java/src/test/java/com/aitest/api/tests/SeedApiTest.java` is the baseline every API
agent references for base URL and environment reachability. If it is broken, fix it before
anything else.

---

## When adding a new test (either stack)

- Mirror the target app/API's URL or resource structure inside `tests/`
- Reuse existing page objects / service objects — do not create parallel infra
- Load test data from `tests/data/` (UI) or `src/test/resources/` (API), not inline
- Tag tests appropriately (`@smoke`, `@regression`, `@critical`)

## Forbidden (both stacks)

- Do not skip, `fixme`, `@Disabled`, or comment out failing tests to make a run green
- Do not use `page.evaluate` (UI) unless there is no MCP tool alternative
- Do not commit `.env`, credentials, `storage-state.json`, auth tokens, or real API keys
- Do not modify `playwright.config.ts` or `api-tests-java/pom.xml` without asking
- Do not add new npm or Maven dependencies without asking
- Do not use `page.pause()` (UI) or `Thread.sleep` (API) in committed code

## Healer-specific rule (overrides the default subagent behavior, both stacks)

The bundled Healer subagents' default instructions (both `.claude/agents/*-healer.md` and their
Copilot `.agent.md` equivalents) permit marking a stubborn test as skipped/disabled/fixme.
**That default is overridden for this project.** A Healer must never silently skip or soften a
test in either stack. See `.claude/agents/playwright-test-healer.md` (UI) and
`.claude/agents/api-test-healer.md` (API) for the full escalation rule: after 2 failed fix
attempts, stop and report — do not skip, do not weaken the assertion, do not increase
timeouts/retries to paper over a real issue.

## When you (the agent) are unsure

- Ask a clarifying question before generating code
- Prefer a smaller, focused change over a big refactor
- If a required file does not exist, ask before creating it
