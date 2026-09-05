# AI Test Healing Workflow Guide

This guide walks through the full lifecycle used in this repo — **Plan → Generate → Execute →
Heal → Report** — for both stacks: the UI suite (`ui-tests/`, Playwright TypeScript) and the API
suite (`api-tests-java/`, Playwright Java). Every rule referenced here lives in
[`CLAUDE.md`](../CLAUDE.md) at the repo root; this guide is the walkthrough, `CLAUDE.md` is the
source of truth.

## 0. Which AI drives this?

**Claude Code is primary.** Everything below is written for it, using the subagents in
`.claude/agents/`. If Claude Code isn't available:

- **GitHub Copilot (VS Code)** is the first backup — equivalent `.agent.md` files live in
  `.github/agents/`, and `.vscode/mcp.json` registers the same MCP server for the UI stack. Open
  Copilot Chat, pick the matching mode from the dropdown, and use the same prompts below.
- **OpenAI Codex CLI** is the second backup — it has no separate agent-role files; it reads
  `AGENTS.md` (symlinked to `CLAUDE.md`) automatically and works from your own prompts plus that
  rulebook.

All three read the same rules. Pick whichever tool is available; the workflow doesn't change.

---

## 1. UI stack (`ui-tests/`) — Plan → Generate → Execute → Heal

### 1.1 Plan

Use the `playwright-test-planner` subagent (Claude Code) — it drives a real browser to explore
the app before writing the plan.

```
Use the playwright-test-planner agent to explore https://www.saucedemo.com and cover the login
flow: standard_user success, locked_out_user error, empty username, empty password, invalid
credentials. All passwords are secret_sauce. Save the plan to ui-tests/specs/saucedemo-login.md.
```

Output: a numbered plan like the real example already in this repo,
[`ui-tests/specs/saucedemo-login.md`](../ui-tests/specs/saucedemo-login.md). Scenarios are
numbered `1.1`, `1.2`, … — the Generator references them by number, never by title.

### 1.2 Generate

Use the `playwright-test-generator` subagent, pointing it at a scenario number:

```
Use the playwright-test-generator agent to implement scenario 1.1 from
ui-tests/specs/saucedemo-login.md. Create any missing page objects under ui-tests/src/pages/,
following BasePage's contract, and write the spec to
ui-tests/tests/auth/standard-login.spec.ts.
```

It follows the strict locator priority (`getByRole` → `getByLabel` → `getByTestId` → `getByText`
→ nothing else without asking), the Page Object contract (readonly locators, no `expect()`
inside), and runs the test itself before reporting done. See
[`ui-tests/src/pages/LoginPage.ts`](../ui-tests/src/pages/LoginPage.ts) and
[`ui-tests/tests/auth/standard-login.spec.ts`](../ui-tests/tests/auth/standard-login.spec.ts) for
the real, passing result.

### 1.3 Execute

```bash
cd ui-tests
npx playwright test                    # full suite
npx playwright test --grep @smoke      # tag-filtered
npx playwright show-report             # open the last HTML report
```

Or via the MCP server: `test_list` / `test_run` / `test_debug`.

### 1.4 Heal

When a test fails, invoke `playwright-test-healer`:

```
Use the playwright-test-healer agent to fix the failing test in
ui-tests/tests/auth/standard-login.spec.ts and produce the mandatory Healer Report.
```

It classifies the failure (A–F: locator drift, UI restructure, copy change, real regression,
environment issue, flakiness) **before** touching anything, applies the minimal fix, and — per
`CLAUDE.md`'s override of its own default behavior — **never** marks a stubborn test `fixme()`.
After 2 failed attempts it stops and reports instead of continuing to iterate. See
`.claude/agents/playwright-test-healer.md` for the full escalation rule and the mandatory report
template.

---

## 2. API stack (`api-tests-java/`) — Plan → Generate → Execute → Heal

The API stack mirrors the UI loop exactly, with Service Objects (`clients/`) standing in for Page
Objects and JUnit 5 + Maven standing in for `@playwright/test`.

### 2.1 Plan

```
Use the api-test-planner agent to explore https://jsonplaceholder.typicode.com/posts and cover:
getting a post by id, a not-found id, filtering by userId, and creating a post. Save the plan to
api-tests-java/specs/jsonplaceholder-posts.md.
```

The Planner only issues `GET`/`HEAD` requests during exploration — it never mutates real state.
See the real example: [`api-tests-java/specs/jsonplaceholder-posts.md`](../api-tests-java/specs/jsonplaceholder-posts.md).

### 2.2 Generate

```
Use the api-test-generator agent to implement scenario 1.1 from
api-tests-java/specs/jsonplaceholder-posts.md. Create a PostsApiClient Service Object under
api-tests-java/src/main/java/com/aitest/api/clients/ if one doesn't exist, and write the test to
api-tests-java/src/test/java/com/aitest/api/tests/PostsApiTest.java.
```

Framework rules mirror the UI side: a Service Object's constructor takes only the shared
`APIRequestContext`, returns typed responses, and never asserts anything — assertions live only in
the JUnit test. See [`PostsApiClient.java`](../api-tests-java/src/main/java/com/aitest/api/clients/PostsApiClient.java)
and [`PostsApiTest.java`](../api-tests-java/src/test/java/com/aitest/api/tests/PostsApiTest.java)
for the real, passing result — status code AND at least one body field are always asserted
together, never status code alone.

### 2.3 Execute

```bash
cd api-tests-java
mvn test                                  # full suite
mvn -Dtest=PostsApiTest test               # single class
mvn -Dgroups=smoke test                    # tag-filtered (requires JUnit tag filtering config)
```

Request/response bodies are logged at DEBUG level to `api-tests-java/target/req-resp.log`
(configured in `src/test/resources/logback.xml`) — the API equivalent of a UI trace, useful for
both the Healer's diagnosis and for audit.

### 2.4 Heal

```
Use the api-test-healer agent to fix the failing test in
api-tests-java/src/test/java/com/aitest/api/tests/PostsApiTest.java and produce the mandatory
Healer Report.
```

It classifies the failure (A–F: field drift, response-shape restructure, data change, real
regression, environment issue, flakiness) by **reproducing the request independently with `curl`
first** — comparing the raw live response to what the test expects — before touching any code.
Same escalation rule as the UI Healer: 2 failed attempts, then stop and report.

---

## 3. Reporting — pass/fail per step, with screenshots (UI) and request logs (API)

### UI: Trace Viewer (recommended)

Playwright's trace records a screenshot before/after every action, for passing **and** failing
runs, as long as `test.step()` labels the actions (already required by `CLAUDE.md` for flows over
3 actions). Current `ui-tests/playwright.config.ts` traces `on-first-retry`; to capture a trace on
every run (both pass and fail), set `trace: 'on'` — **this is a config change, ask before
applying it** per the "Forbidden" section of `CLAUDE.md`.

```bash
npx playwright show-report                # click a test -> "Trace" tab
npx playwright show-trace <trace.zip>      # or open a trace file directly
```

### UI: inline screenshots in the HTML report (belt-and-suspenders)

For a screenshot embedded directly in the report body per named step regardless of pass/fail,
wrap the action in `test.step()` and attach a screenshot in a `finally` block via
`testInfo.attach(...)`. This is a new `src/utils/` helper — confirm with the user first, since
`CLAUDE.md` requires asking before adding shared infrastructure.

### API: Allure + JUnit XML + request/response log

- **JUnit XML** — `api-tests-java/target/surefire-reports/*.xml`, generated automatically by
  Surefire on every `mvn test` run; this is what CI consumes.
- **Allure** — `allure-junit5` is already a test-scope dependency in `pom.xml`. Generate the HTML
  dashboard with `mvn allure:report` (writes to `target/site/allure-maven-plugin-report/`), or
  serve it live with `mvn allure:serve`.
- **Request/response log** — every Service Object call logs the full request URL, status, and
  body at DEBUG level (see `logback.xml`), captured to `target/req-resp.log` on every run — the
  API equivalent of a UI trace, and what the Healer reads first when reproducing a failure.

---

## 4. Reviewing AI-generated tests — the actual skill

Code generation is easy; review is where engineering judgment matters. Checklist for every
AI-generated change, either stack:

**Framework compliance**
- UI: imports from `src/fixtures/base`, page object extends `BasePage`, correct folder/file name
- API: test extends `BaseApiTest`, talks to the API only through a Service Object, correct
  package/class name

**Locator / field quality**
- UI: `getByRole` wherever possible, no CSS/XPath without a documented exception
- API: assertions target named fields on a parsed model, not raw string-contains on the JSON blob

**Assertion quality**
- Tests the actual feature, not just "page loaded" / "status was 200"
- UI: web-first assertions, no `waitForTimeout`
- API: status code AND at least one body field, never status code alone; no `Thread.sleep`

**Healer-fix scrutiny (the most important check)**
- Did the Healer fix the test, or fix the pass/fail status?
- Was any assertion softened (`toHaveCount` → `toBeVisible`, `assertEquals` → `assertNotNull`)?
- Was a timeout/retry silently increased to paper over real slowness or a real bug?
- Was anything skipped/disabled without your explicit approval?

If the answer to any of the last four isn't immediately "no," reject the fix and fix it by hand.

## 5. Anti-patterns to avoid (both stacks)

| Don't | Do |
|---|---|
| "Fix this test" | "Test X fails with error Y. Diagnose and fix. Do not change assertion intent." |
| Reference a scenario by name | Reference it by number (`scenario 1.1`) — names are ambiguous |
| Point an agent at production | Point it at staging/local/the designated demo target |
| Put credentials in a prompt | Reference `tests/data/*.json` (UI) or `application.properties` /
  env vars (API) |
| Generate and assume it works | Generate, run, and require the pass output in the report |
| Trust a Healer fix blindly | Read every diff — the Healer's failure mode is silent weakening |
