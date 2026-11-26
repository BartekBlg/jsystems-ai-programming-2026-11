# Implementation Plan — Hardware Service Decision Copilot PoC (`hsdc-poc`)

> **Status: DRAFT awaiting review.** Planning does not authorize execution, new agents, issue creation, push, publication or merge.
> **Date:** 2026-11-26 · **Plan name:** `hsdc-poc` · **Branch at planning time:** `design-system` · **Delivery:** whole PoC, one pull request (decided 2026-11-26).

**Sources of truth** (linked, not copied): [PRD](../PRD.md) · [ADR-000](../ADR/000-main-architecture.md) · [ADR-001](../ADR/001-backend-api.md) · [ADR-002](../ADR/002-llm-integration.md) · [ADR-003](../ADR/003-frontend.md) · [ADR-004](../ADR/004-decisions-prescreening.md) · **[ADR-005 (Proposed, approve with this plan)](../ADR/005-contract-clarifications.md)** · [Design guidelines](../design-guidelines.md) · [`assets/design-tokens.json`](../../assets/design-tokens.json) · [`assets/homepage.png`](../../assets/homepage.png) · [Policies](../policies/) · [Root AGENTS.md](../../AGENTS.md).

Path shorthands: `B` = `app/backend/src/main/java/pl/elektrodemo/copilot`, `BT` = `app/backend/src/test/java/pl/elektrodemo/copilot`, `BR` = `app/backend/src/main/resources`, `BTR` = `app/backend/src/test/resources`, `F` = `app/frontend/src/app`, `FE2E` = `app/frontend/e2e`.

---

## 1. Baseline (inspected 2026-11-26)

| Item | Observed |
|---|---|
| Application code | **None.** `app/` contains only `README.md`, a generic scaffold checklist that ADR-000..005 supersede. There is no `pom.xml`, `package.json`, entry point or test. ADR-001 and ADR-003 prescribe generating the scaffold (Initializr, Angular CLI), so no existing scaffold is replaced. |
| Working tree | `M .mcp.json`: an **uncommitted change that hard-codes a Context7 API key**. It is never staged by this plan; the user should revert it and rotate the key. `?? docs/plans/`, `?? docs/ADR/005-…` (this planning output). |
| Specs | PRD and ADR-000..004 accepted 2026-11-25. ADR-005 proposed 2026-11-26. Design guidelines and tokens are present. |
| GitHub | `BartekBlg/jsystems-ai-programming-2026-11`, default branch `main`, **Issues disabled**, viewer permission **ADMIN**, so Issues can be enabled after approval. |
| Verified tools | `node v24.22.3` (Angular 22 needs `^24.15.0`), `npm`, `npx`, `gh`, `claude 2.1.332`, `codex`, `playwright-cli` (`~/.local/bin`), `opencode 1.19.0`, `python3 3.14.4`. |
| Missing tools | **`java`, `mvn`, `docker`**: not installed. `sudo` is denied by settings. Decision: install Temurin JDK 25 into `~/.jdks` (T00). Docker stays unavailable, so TAC-07 and TAC-001-07 are an accepted, reported gap unless a Docker host is provided. **`ctx7` CLI** is not installed; Context7 is available as an MCP server for Claude Code only. |
| OpenCode models | Free models work now: `opencode/mimo-v2.6-flash-free`, `opencode/deepseek-v4.1-flash-free` and others (`opencode models`). **No OpenRouter credential** (`opencode providers list` → 0 credentials), so `openrouter/openai/gpt-6-luna` is not usable until T00. |
| Agent harness | Claude Code `Agent` tool accepts `model: "sonnet" \| "haiku" \| "opus"`, which resolve to the Claude 5.5 family. No custom roles exist in `.claude/agents/`. |
| Project settings | `.claude/settings.json` denies `curl`, `wget`, `rm`, `docker run`, `git reset --hard` and reading `.env`. A PreToolUse agent hook reviews every `pom.xml` dependency change. `plansDirectory` = `./docs/plans`. |
| Build/test/start commands | **None exist yet, and none were run.** The commands below are *declared* by the ADRs (as corrected by ADR-005 D4) and are verified by the task named in the last column. |
| Baseline failures | No suite exists, so there are none to report. |

| Purpose | Declared command | Verified in |
|---|---|---|
| Backend tests only | `cd app/backend && ./mvnw test -Dskip.frontend=true` | T01 |
| Full build, all unit/integration/Vitest tests, JAR | `cd app/backend && ./mvnw verify` | T03 |
| Start | `java -jar app/backend/target/service-copilot-*.jar`, then `GET /actuator/health` returns `UP` | T03 |
| Frontend unit tests | `cd app/frontend && npm run test -- --watch=false` | T02 |
| Frontend dev | `cd app/frontend && npm start` (proxy to :8080) | T02 |
| E2E on the real stack | `cd app/frontend && npx playwright test` | T06 |
| Manual QA | `playwright-cli` against `http://localhost:8080` | T42 |

---

## 2. Cross-document check (plan × PRD × ADRs × AGENTS.md)

Every item below could stop an agent or leave it unsure. *Resolution* says where the answer lives; items marked **open** need the human.

| # | Finding | Resolution |
|---|---|---|
| X1 | The server has no defined source for the current chat decision category (`ChatPrompt.currentCategory` vs `ChatRequest` fields) | ADR-005 D1 |
| X2 | `CaseContextDto` fields differ between ADR-000 §5 and ADR-001 §4; `source` and `assessmentSource` duplicate each other | ADR-005 D2 |
| X3 | `409 DECISION_ALREADY_MADE` and a problem+json `404` are not in the ADR-000 §6 code table | ADR-005 D3 |
| X4 | `./mvnw -f app/backend/pom.xml` from the root (TAC-01, Dockerfile) points to a wrapper that does not exist there | ADR-005 D4 |
| X5 | ADR-002 R6 requires a §5 citation for a rejected return, but the validator does not check it | ADR-005 D5 |
| X6 | The Jackson 2/3 mapper for HMAC canonical JSON is unspecified | ADR-005 D6 |
| X7 | TAC-01 "no skipped tests except e2e" conflicts with the `eval` and capture tags | ADR-005 D7 |
| X8 | PRD asks for the "shop logo"; `assets/logo.svg` is DevPowers with a usage-rights warning | User decision → ADR-005 D8 (ElektroDemo text wordmark) |
| X9 | AC-26 "offers Nowe zgłoszenie" vs a header-only button in ADR-003 | User decision → ADR-005 D9 (inline button) |
| X10 | AC-41, AC-42 and AC-49 error paths cannot be provoked against a healthy OpenRouter without mocks | ADR-005 D10: a real instance with a faulty configuration (invalid key, 1 ms timeout, wrong Decisions path) still calls the real OpenRouter |
| X11 | The default rate limit of 10 assessments/hour per IP would throttle E2E and QA runs from localhost | ADR-005 D10 (E2E/QA env limits) |
| X12 | Without `CASE_SIGNING_KEY` a restart invalidates stored cases, which breaks refresh E2E across restarts | ADR-005 D10 (fixed key in E2E/QA) |
| X13 | The AGENTS.md test table names roles `be-dev`, `fe-dev`, `qa-engineer`, but AGENTS.md also says no custom roles exist | They are labels only. This plan maps them to rows: unit → worker rows, E2E → T06/T41, QA → orchestrator T42 |
| X14 | AGENTS.md Manual QA says logo placement must match the brand reference, while ADR-005 D8 drops the DevPowers asset | The placement, size and style still follow the reference; only the asset differs |
| X15 | CLAUDE.md requires Context7, but `ctx7` is missing, OpenCode has no Context7, and `.mcp.json` holds a hard-coded key | T00 configures Context7 for OpenCode via MCP (key from env) or `npx ctx7`, whichever T00 verifies |
| X16 | `curl` and `wget` are denied, but the JDK, Initializr zip and Playwright browsers need downloads | The user runs downloads as `!` commands (T00, T01); `npx playwright install chromium` goes through npm (allowed) |
| X17 | `rm` is denied, but the plan lifecycle requires deleting the plan and briefs after the DoD | Delete with `git rm` in a final commit (permission prompt expected) |
| X18 | ADR-000 §10 "Timeout" row mixes a WireMock delay with the frontend 60 s state | The backend part is an integration test (WireMock from a real capture with an added delay). The frontend part is Vitest with fake timers. The real-stack part is the D10 instance with a 1 ms step timeout calling the real OpenRouter |
| X19 | ADR-002 §3.4 (`instructions` honored?) and §3.7 (SDK ignores keep-alives?) are open items | Answered by T04 captures and recorded in `*.meta.md`; T20 follows the result |
| X20 | Frontend unit tests that mock `fetch`/HttpClient need recorded backend responses, which exist only after the backend runs | T26 records real backend responses; T33 depends on it |
| X21 | ADR-004 switch from `shadow` to `gated` as the default needs 30 shadow-logged QA cases | Not part of the DoD; the PoC ships `shadow` by default. All three modes are still exercised for real in T41/T42 (§4.1) |
| X27 | E2E and QA would run only in `shadow` mode, so the `gated` and `llm-only` decision paths would never be exercised for real | §4.1 mode matrix; T41 and T42 run every mode against the real APIs |
| X28 | Some chat streams cannot be captured on demand (a forbidden decision update); the ADRs do not say how such a mock may be built | §4.1 rule: derive it from a real capture by changing only the text inside `delta` events, and record that in `*.meta.md` |
| X22 | PRD §12: `assets/example-images/README.md` describes an employee user | Out of scope ("separate change"); untouched |
| X23 | AGENTS.md is silent on where plans and briefs live and when they are deleted | This plan's lifecycle (§3.3) applies; no AGENTS.md change needed for this PoC |
| **X24 open** | PR base: the PRD/ADR commits are on `design-system`, not `main` | Human: merge the docs to `main` first, or let the PoC PR include them |
| **X25 open** | Docker gap (TAC-07, TAC-001-07) | Human: accept as unverified in this PR, or provide a Docker host |
| **X26 open** | ADR-005 status | Human: accept ADR-005 together with this plan |

---

## 3. Orchestration

### 3.1 Roles and model policy

| Role | Who | Use for |
|---|---|---|
| Orchestrator | Claude Code, **Opus 5.5** (this session) | Architecture calls, contradictions, shared contracts, prompts, captures, briefs, choosing worker/model per row, reading diffs, re-running checks, all Git writes, Status column, final acceptance with the human |
| Strong worker | Claude Code subagent, **Sonnet 5.5** (`Agent(model:"sonnet")`) | Concurrency, streaming, SDK integration, framework-heavy web layers |
| Cheap worker | Claude Code subagent, **Haiku 5.5** (`Agent(model:"haiku")`) | Pure, fully specified units with exact test tables |
| Research / bulk worker | OpenCode headless, **GPT-6 Luna** (`openrouter/openai/gpt-6-luna`) | Parallel docs research that reads a lot; well-specified frontend components; independent cross-family review |
| Free worker | OpenCode headless, **free model** (`opencode/deepseek-v4.1-flash-free` or `opencode/mimo-v2.6-flash-free`, re-check `opencode models` at dispatch) | Low-risk docs research and config lookups; fallback when the OpenRouter budget runs out |

Rules:
- **Ad hoc subagents only.** No `.claude/agents/*.md` or OpenCode agent files: every task is one-off and the brief carries the expertise. Add a role file only if a role recurs across later features.
- **Escalation on evidence:** missing red→green proof, a wrong API used twice, or a review rejection twice moves the row up one level (free → Luna → Haiku → Sonnet → orchestrator). Record the change in the row's Owner/model cell.
- **Docs lookup order for every agent:** Context7 (MCP `resolve-library-id` + `query-docs`, or `npx ctx7` if T00 verifies it) and `<tool> --help`, then the vendor's official docs, and web search last. Library versions come from `pom.xml` and `package-lock.json`.
- **Research is delegated** (R-rows). Only conclusions with source links return to the orchestrator, saved as research briefs.
- **Workers never:** run `git add/commit/checkout/switch/stash/reset/rebase`; install packages; edit `pom.xml`, `package.json`, lockfiles, `application.yaml`, shared contracts (T05, T30) or files outside their row; weaken tests; invent API shapes absent from captures. A needed out-of-scope change is reported, not made.
- **Concurrency:** each brief lists the concurrently running rows and the paths they own. One checkout; disjoint paths; **no worktree** is planned because no two concurrent rows share a file. Worktree creation stays ask-first per settings.
- **Harness fallback:** if `Agent` subagents or OpenCode are unavailable, the orchestrator executes the same matrix sequentially in critical-path order.

### 3.2 OpenCode headless — how to run it

Setup (T00, once):

```bash
# Provider: in an interactive `opencode` session run /connect → OpenRouter, or export the key in your shell only
export OPENROUTER_API_KEY=...          # never in files, briefs or logs
opencode models | grep -E 'openrouter/openai/gpt-6-luna|free'
mkdir -p .agent-logs && grep -qx '.agent-logs/' .gitignore || echo '.agent-logs/' >> .gitignore
```

Permissions live in the root `opencode.json`. A headless run auto-rejects every `ask`, and the last matching pattern wins, so allow exactly what a worker needs and deny Git writes and installs. The orchestrator narrows `edit` to the row's paths before each dispatch and restores it after:

```json
{
  "$schema": "https://opencode.ai/config.json",
  "permission": {
    "edit": { "*": "deny", "app/frontend/src/app/form/**": "allow", "docs/.agent-briefs/hsdc-poc-research-*.md": "allow" },
    "bash": {
      "*": "deny",
      "ls *": "allow", "cat *": "allow", "grep *": "allow",
      "npm run test*": "allow", "npx ng build*": "allow",
      "git diff*": "allow", "git status*": "allow",
      "git add*": "deny", "git commit*": "deny", "git push*": "deny", "npm install*": "deny", "npm i *": "deny"
    },
    "webfetch": "allow"
  }
}
```

Run one row: the brief is attached with `-f`, the model is chosen with `-m`, stdin is closed, and output goes to a log that stays out of Git:

```bash
opencode run -m openrouter/openai/gpt-6-luna \
  -f docs/.agent-briefs/hsdc-poc-T32-form.md \
  "Do the task described in the attached brief." </dev/null 2>&1 \
  | tee .agent-logs/hsdc-poc-T32-form.log
```

- Use `-m opencode/deepseek-v4.1-flash-free` for free-model rows. Add `--format json` for machine-readable events and `--variant high` for more reasoning.
- Use `-c` or `-s <session-id>` to send review fixes to the same session.
- **Never pass `--auto`** outside a trusted sandbox: it approves every permission that is not explicitly denied.
- The worker's final message is its report. The orchestrator then runs `git diff -- <row paths>` and re-runs the row's checks itself.

### 3.3 Briefs, logs and plan lifecycle

- After execution is authorized, the orchestrator compiles each brief **immediately before dispatch** as `docs/.agent-briefs/hsdc-poc-<task>-<slug>.md`. Research outputs are `docs/.agent-briefs/hsdc-poc-research-<topic>.md`.
- Each brief contains:
  - the concurrent rows and their paths;
  - the goal and observable outcome;
  - authoritative links (traceability only);
  - **excerpts** of the needed ADR rows, contracts, current type signatures, captured payloads and predecessor outputs;
  - the exhaustive editable-file list and the read-only context files;
  - ordered TDD steps, with the exact command and expected red failure, then green;
  - test inputs and expected results;
  - the forbidden actions (§3.1);
  - the report format: diff summary, commands with pass/fail/skip counts, red→green evidence, contradictions and anything unverified.
- **Commit the plan, ADR-005 and briefs** while the work runs (`Docs: …` commits, link-checked). Worker logs (`.agent-logs/`) are never committed.
- **After the DoD (§7)** is met, the orchestrator removes the plan and `docs/.agent-briefs/hsdc-poc-*` with `git rm` in one `Docs: remove completed hsdc-poc plan and briefs` commit. Git history keeps them.

### 3.4 Status tracking

- The **Status column below is authoritative**. Values: `todo`, `brief`, `dispatched`, `review`, `done`, `blocked:<reason>`, `cut`. Only the orchestrator updates it, in the same commit as the row's work (or a `Docs: plan status` commit).
- **GitHub Issues (after approval):** enable them (`gh repo edit --enable-issues`, ADMIN verified), create one issue per row titled `hsdc-poc <Task>: <title>`, then write `#<n>` into the Task cell and into the orchestrator's task list. Commit messages reference `#<n>`.

---

## 4. Dependency matrix (authoritative)

Every implementation row follows test-first: failing behavioral test → expected red → minimum code → green → refactor. Each row ends in **one orchestrator commit** `Area: summary (#issue)` that stages named paths only. "Counts" means passed/failed/skipped numbers copied from the runner output.

### 4.1 Test realism rules (binding for every row and brief)

**E2E (T06, T40, T41) and Manual QA (T42) use only real components.**
- The packaged JAR, a real browser, the real OpenRouter Responses API (vision, decision and chat models) and the real OpenRouter Decisions API.
- **Forbidden:**
  - Playwright `page.route`, `route.fulfill` and `context.route`;
  - service workers or a mock server;
  - WireMock or any test double on the runtime classpath;
  - `OPENROUTER_BASE_URL` or `DECISIONS_URL` pointing anywhere other than `https://openrouter.ai`;
  - fake clocks.
- **Error paths** use only the real faulty-configuration instances from [ADR-005 D10](../ADR/005-contract-clarifications.md#d10-real-stack-verification-of-error-paths). Each of those requests still reaches OpenRouter.

**Every decision-making method is exercised for real.** T41 runs this mode matrix, and T42 repeats one return and one complaint case per mode:

| `DECISIONS_MODE` | What runs for real | Checked evidence |
|---|---|---|
| `shadow` (default; full suite) | Decisions A+B in parallel with the vision LLM, `PolicyRuleEngine`, decision LLM with consistency signals, chat LLM | `prescreen.shadow` log event per case; `assessmentSource=LLM` |
| `gated` (return + complaint + insufficient photo) | Decisions A+B, `ConfidenceGate`. On `CONFIDENT` the mapper replaces the vision LLM; on `UNCERTAIN` the vision LLM runs with the request-A signals | `prescreen.gated` log event with `visionLlmSkipped` true/false; `assessmentSource` matches. At least one case per outcome, using example images that T04 showed to be confident or uncertain |
| `llm-only` (return + complaint) | Vision LLM, rule engine, decision LLM; no Decisions call | No Decisions request in the log |

**Proof that calls were real** (asserted by the T41 global teardown and repeated in T42):
- the application log of each run contains one token-usage line per model step and the expected `prescreen.*` events, with numbers only and no text content (ADR-000 Decision 8.7);
- the OpenRouter activity count for the key increased during the run (checked by the human in T42);
- `jar tf` on the built JAR lists no `wiremock` or `mockito` classes;
- `grep -rn "page.route\|route.fulfill\|context.route" app/frontend/e2e` returns nothing.

**Every endpoint is exercised for real.**
- *Through the UI:* `POST /api/cases/assessments`, `POST /api/cases/decisions`, `POST /api/chat/messages`, `GET /` and SPA fallback.
- *Through the Playwright `request` fixture against the same real JAR:*
  - `GET /actuator/health`;
  - an 11 MB real JPEG → `413`;
  - a GIF renamed `.jpg` → `400 UNSUPPORTED_IMAGE`;
  - an invalid form → `400 VALIDATION_FAILED` with Polish `fieldErrors`;
  - a `caseContext` with one changed field → `401` on `/decisions` and on `/chat/messages`;
  - a repeated decision on a decided context → `409`;
  - `GET /api/unknown` → `404` problem+json;
  - an assessment that reaches the rate limit on the D10 instance → `429`.

  Every response body is checked by the TAC-05 no-leak rule.

**Mocks in unit and integration tests come only from real recordings.**
- External HTTP (OpenRouter Responses and Decisions) is stubbed with WireMock serving **T04 captures verbatim**.
- Backend responses consumed by the frontend are stubbed only from **T26 recordings** of the real running backend.
- In-process ports (`LlmGateway`, `PrescreenService`) may be mocked in unit tests, but their return values are **deserialized from the same captures** (`CaptureFixtures` helper in `BT/support/`), never hand-written.
- A shape that cannot be recorded on demand (a forbidden chat decision update, a stream that fails mid-way, a delayed response) is **derived from a real capture**:
  - change only the text inside `delta` events, or truncate the stream at an event boundary, or add a WireMock delay;
  - save it as `<capture>.derived-<reason>.*` with a `*.meta.md` that names the source capture and the exact change.
- Documentation (R4, Context7, vendor pages) can guide what to record but is **never** the source of a mock body.
- **The orchestrator rejects** any row whose diff adds a stub body without a capture or recording reference.

### Phase 0 — Environment, research, scaffolds, captures, contracts

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|
| **T00** Environment | `java -version` shows 25 (`~/.jdks/…`, `JAVA_HOME` in shell profile); `opencode models` lists Luna; Context7 is reachable from OpenCode; `.agent-logs/` is ignored; branch `feat/hsdc-poc` exists; Issues are enabled and one issue exists per row | [ADR-000 stack](../ADR/000-main-architecture.md#technology-stack), ADR-005 D4 | **Plan + ADR-005 approved**; the user runs the JDK download and key setup as `!` commands and approves enabling Issues | Orchestrator + human — needs credentials and approvals | `opencode.json`, `.gitignore`, this plan (Issue IDs) | Command outputs pasted into the T00 issue. Commits `Docs: approved hsdc-poc plan and ADR-005` and `Chore: OpenCode worker config`. `.mcp.json` never staged. | todo |
| **R1** Research: OpenAI Java SDK 4.113 | Excerpts with links: Responses `create` with `input_image`, class-based structured output, `createStreaming` event types, request-level timeout, exception types, how to set `reasoning.effort` and extra headers | [ADR-002 §2](../ADR/002-llm-integration.md#2-technology-documentation-references), [§6.2](../ADR/002-llm-integration.md#62-use-openai-java-directly-with-a-custom-spring-bean) | T00 | OpenCode **Luna**: reads a large SDK repo and docs cheaply, parallel with R2–R5 | `docs/.agent-briefs/hsdc-poc-research-openai-java.md` | Orchestrator spot-checks 3 snippets against Context7 `/openai/openai-java`; R1 output is the T20 brief input | todo |
| **R2** Research: Spring Boot 4.2 | `SseEmitter` with virtual threads, ProblemDetail customization, multipart limits → 413 mapping, Jackson 3 sorted-key `JsonMapper`, `RestClient` timeouts, `excludedGroups`, frontend-maven-plugin 2.0.2 executions | [ADR-001 §2](../ADR/001-backend-api.md#2-technology-documentation-references), ADR-005 D6–D7 | T00 | OpenCode **Luna** (as R1) | `docs/.agent-briefs/hsdc-poc-research-spring-boot.md` | Spot check; input for T03, T11, T22, T23 | todo |
| **R3** Research: Angular 22.3 stack | Material 22 M3 theme + `mat.*-overrides`, datepicker `pl-PL`, `provideMarkdown` + `marked` peer version, Vitest in Angular 22, `BreakpointObserver` | [ADR-003 §2](../ADR/003-frontend.md#2-technology-documentation-references) | T00 | OpenCode **Luna** (as R1) | `docs/.agent-briefs/hsdc-poc-research-angular.md` | Spot check; input for T02, T30–T35 | todo |
| **R4** Research: OpenRouter APIs | Responses SSE event names, keep-alive comment format, error body shapes, Decisions alpha request/response schema (`state`, question types `noul`/`choice`/`score`) | [ADR-002 §3.7](../ADR/002-llm-integration.md#37-chat-streaming-and-decisiongate), [ADR-004 §2](../ADR/004-decisions-prescreening.md#2-technology-documentation-references) | T00 | OpenCode **Luna** (as R1) | `docs/.agent-briefs/hsdc-poc-research-openrouter.md` | Guides the T04 capture tool only. **Mocks are never derived from it** — T04 captures are | todo |
| **R5** Research: Playwright 1.65 | `playwright.config.ts` with `webServer` for the JAR, two projects (360/1280 px), console-error listener, real-stack timeouts | [ADR-000 §10](../ADR/000-main-architecture.md#10-testing-strategy), TAC-08 | T00 | OpenCode **free model** (`deepseek-v4.1-flash-free`): small, low-risk docs lookup | `docs/.agent-briefs/hsdc-poc-research-playwright.md` | Spot check; input for T06 | todo |
| **T01** Backend scaffold | Initializr project in `app/backend` (ADR-001 §3.1 parameters) with wrapper; **all** ADR dependencies added once (SDK, Thumbnailator, TwelveMonkeys, WireMock standalone, JaCoCo); generated context test green | [ADR-001 §3.1](../ADR/001-backend-api.md#31-project-initialization-maven--spring-initializr) | T00, R2 | Orchestrator — `pom.xml` is a hotspot reviewed by the dependency hook | `app/backend/**` (generated), `app/backend/pom.xml` | `./mvnw -v` (Java 25, record wrapper version); `./mvnw test -Dskip.frontend=true` (counts); `dependency:tree` shows Jackson 2 not downgraded. Commit `Backend: Spring Boot scaffold` | todo |
| **T02** Frontend scaffold | Angular 22.3 workspace per ADR-003 §3.1 steps 1–6 (Material, ngx-markdown, `@playwright/test@1.65.1`, Chromium, proxy); no DevPowers logo (D8) | [ADR-003 §3.1](../ADR/003-frontend.md#31-project-initialization), ADR-005 D8 | T00, R3 (parallel with T01: disjoint folders) | Orchestrator — owns `package.json` and the lockfile | `app/frontend/**` (generated) | `npm run build` with zero warnings (TAC-003-02); `npm run test -- --watch=false` (counts). Commit `Frontend: Angular scaffold` | todo |
| **T03** One-JAR build + skeleton runtime | Maven builds Angular into `static/`; policies copied to `classpath:policies/`; `application.yaml` per ADR-001 §3.7; `SpaFallbackController`; health; nested `app/backend/AGENTS.md` + `app/frontend/AGENTS.md` with **verified** commands | [ADR-000 8.1](../ADR/000-main-architecture.md#81-single-spring-boot-jar-serving-the-angular-build), [ADR-001 §3.6–3.7](../ADR/001-backend-api.md#36-frontend-build-inside-maven), TAC-01, TAC-02, TAC-001-06, ADR-005 D3, D4, D7 | T01, T02 | Orchestrator — integration of two build descriptors | `app/backend/pom.xml`, `BR/application.yaml`, `B/web/SpaFallbackController.java`, `BT/web/SpaFallbackIT.java`, `BT/policy/PoliciesOnClasspathTest.java`, nested `AGENTS.md` ×2 | **Red first:** SPA fallback and policies-on-classpath tests fail. Then `./mvnw verify` (counts) and, recorded separately, start the JAR: health `UP` in < 15 s and `/` serves `index.html`, checked with `playwright-cli`. Commit `Build: Angular inside the Spring Boot JAR` | todo |
| **T04** Real OpenRouter captures | Verbatim status, headers and body (raw SSE for streams) plus `*.meta.md` (date, endpoint, model, request summary, SDK version, no credentials) for:
- return, complaint and insufficient-photo assessments;
- decisions for each case type, including one `MORE_INFO_NEEDED`;
- chat streams: plain reply, a reply that starts with `Zaktualizowana decyzja:` (provoked by giving a delivery date), and a stream after a real invalid-key `401`;
- Decisions A for return and complaint, both confident and low-confidence, plus Decisions B;
- real `400`, `401` and `429` error bodies;
- a timeout behavior note.

Each example image is labeled with its gate outcome for the T41 `gated` cases. Derived variants follow the §4.1 rule. Answers X19 | [ADR-000 Captures](../ADR/000-main-architecture.md#captures-required-before-writing-openrouter-mocks), [ADR-004 §8](../ADR/004-decisions-prescreening.md#8-testing-strategy), TAC-002-06, [AGENTS.md TDD rules](../../AGENTS.md#tdd-rules) | T01, R1, R4; OpenRouter spending limit set by the human | Orchestrator — handles the key and spends credit | `BTR/captures/**`, `BT/capture/CaptureTool.java` (`@Tag("capture")`, excluded per D7) | `grep -rn "sk-or" app/` is empty; one file per required capture; an unobtainable capture → `blocked:` with the missing prerequisite, never an invented shape. Commit `Backend: real OpenRouter captures` | todo |
| **T05** Backend shared contracts | Enums/records/DTOs/ports compile with no behavior: `CaseType`, `EquipmentCategory`, `DecisionCategory` (Polish labels), `Return/ComplaintImageAssessment`, `RuleResult` (+ D5 field), `CaseContextDto` (D2), request/response DTOs, `ErrorCode` (D3), `LlmGateway`, `ChatSink`, `DecisionPrompt`, `ChatPrompt`, `AppProperties`, `ClockConfig`, exception types | [ADR-000 §5](../ADR/000-main-architecture.md#5-data-models), [ADR-001 §3.2, §4](../ADR/001-backend-api.md#32-packages), [ADR-002 §4–5](../ADR/002-llm-integration.md#4-data-structures), ADR-005 D2, D3, D5 | T03, T04 (field names match captures) | Orchestrator — serialized shared contract | Contract files in the ADR-001 §3.2 packages (exact list recorded in the commit) | JSON round-trip test of `CaseContextDto` (nulls kept, D2). Commit `Backend: shared contracts` | todo |
| **T06** Failing acceptance E2E | Playwright specs for Flow 4.1 (return happy path) and AC-13 (form validation) on the real JAR, failing because the UI does not exist yet | [PRD 4.1](../PRD.md#41-return--happy-path), [PRD 4.5](../PRD.md#45-form-validation-errors), AC-01..AC-13, AC-21, AC-46 | T03, R5 | Claude **Haiku**: short TS specs from an exact step list | `app/frontend/playwright.config.ts`, `FE2E/return-flow.spec.ts`, `FE2E/form-validation.spec.ts` | Run fails on missing locators, not on configuration. Commit `Test: failing acceptance E2E` | todo |

### Phase 1 — Backend pure units (T10–T15 parallel; T16 on the orchestrator)

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|
| **T10** Image | Magic-byte detection; 40 MP guard; EXIF-aware downscale to ≤ 1536 px JPEG q0.85; no upscaling; alpha flattened; metadata stripped | AC-09, AC-10, AC-15; [ADR-001 §3.4](../ADR/001-backend-api.md#34-image-processing), TAC-001-02 | T05 | **Haiku**: pure, table-specified | `B/image/**`, `BT/image/**`, `BTR/images/**` (generated synthetic fixtures + `assets/example-images` read-only) | ADR-001 §8 rows "Magic bytes" and "Downscale"; backend test command counts | todo |
| **T11** Security | `CanonicalJson` (D6), `CaseSigner` (HMAC-SHA256, constant-time compare), `RateLimiter` (fixed window, `Clock`) | [ADR-000 8.4](../ADR/000-main-architecture.md#84-hmac-signed-casecontext), [8.6](../ADR/000-main-architecture.md#86-in-memory-per-ip-rate-limiting), TAC-001-03, TAC-001-05, ADR-005 D6 | T05, R2 | **Haiku**: pure, exact cases | `B/security/**`, `BT/security/**` | Tampered field, reordered keys, JS round trip, wrong key, window rollover | todo |
| **T12** Rule engine | `PolicyRuleEngine` rows R1–R6, C1–C6, `chatForbiddenCategories`, `rejectionRequiresCitation` (D5), comment block naming the mirrored policy § | AC-24..AC-28; [ADR-002 §3.3](../ADR/002-llm-integration.md#33-deterministic-policy-rules-policyruleengine), TAC-002-01, ADR-005 D5 | T05 | **Haiku**: table-to-test mapping | `B/cases/PolicyRuleEngine.java`, `BT/cases/PolicyRuleEngineTest.java` | Parameterized test per row: 14/15/30/31 days, 2 years ± 1 day, leap day, Europe/Warsaw midnight; JaCoCo branch coverage 100 % | todo |
| **T13** Validation | `@ValidCaseForm`, trimming, not-future date with the injected clock, exact PRD Polish messages, all errors collected | AC-04..AC-07; [ADR-001 §3.3](../ADR/001-backend-api.md#33-validation) | T05 | **Haiku** | `B/web/validation/**`, `BT/web/validation/**` | Boundaries 1/2/100/101, 9/10, 2000/2001; whitespace-only input | todo |
| **T14** Policy + prompt loaders | `PolicyRepository`, `PromptRepository`: fail fast on a missing file; policy text byte-equal to `docs/policies/*.md` | AC-29; [ADR-001 §3.2](../ADR/001-backend-api.md#32-packages), [ADR-002 §3.4](../ADR/002-llm-integration.md#34-prompt-structure-and-files) | T05 | **Haiku** | `B/policy/**`, `B/prompts/**`, `BT/policy/**` (except T03's test), `BT/prompts/**` | Missing-file startup failure; byte equality | todo |
| **T15** Validator + gate | `DecisionMessageValidator` (headings order, label, `§` citation, C1 text, D5, canonical disclaimer appended) and `DecisionGate` (first line or 120 chars; unknown/forbidden/valid label) | AC-21..AC-23, AC-36, [PRD disclaimer](../PRD.md#mandatory-disclaimer); [ADR-002 §3.6](../ADR/002-llm-integration.md#36-first-decision-validation-decisionmessagevalidator), [§3.7](../ADR/002-llm-integration.md#37-chat-streaming-and-decisiongate), TAC-002-04, TAC-002-05 | T05 | **Haiku**: pure string logic; escalate to Sonnet if gate chunking fails review | `B/cases/DecisionMessageValidator.java`, `B/chat/DecisionGate.java`, `BT/cases/DecisionMessageValidatorTest.java`, `BT/chat/DecisionGateTest.java` | Chunk boundaries inside the first line and inside multibyte characters. Validator inputs: the T04 decision captures plus documented mutations (missing heading, missing `§`, wrong label). Gate inputs: the `delta` sequences of the T04 chat captures | todo |
| **T16** Prompt texts | Five Polish instruction files implementing PRD §11 allowed/forbidden behavior, section order, citation rule, injection rule, AC-37, using the T04 finding on `instructions` | AC-16, AC-20, AC-21, AC-37; [PRD §11](../PRD.md#11-agent--system-behavior-specification); [ADR-002 §3.4](../ADR/002-llm-integration.md#34-prompt-structure-and-files) | T04 | Orchestrator — product-critical wording | `BR/prompts/*.md` | PRD §11 checklist review; exercised for real by T40/T41 | todo |

Concurrency: T10–T15 own disjoint paths and run together (at most 4 Claude workers at once). T16 runs alongside on the orchestrator. Phase 3 rows T30–T32 can run at the same time.

### Phase 2 — LLM gateway, use cases, web layer, pre-screening

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|
| **T20** LLM gateway | `OpenRouterClientConfig`, `LlmProperties`, `OpenAiSdkLlmGateway` (structured outputs, streaming → `ChatSink`, per-step timeouts, one retry before output, error mapping, token-usage log), startup capability check | [ADR-002 §3.1–3.2, 3.5, 3.8](../ADR/002-llm-integration.md#31-components), [§6.1](../ADR/002-llm-integration.md#61-responses-api-instead-of-chat-completions), TAC-03, TAC-002-02, TAC-002-07 | T04, T05, T16, R1 | **Sonnet**: SDK API nuances, streaming lifecycle | `B/llm/**`, `BT/llm/**` | WireMock stubs **only from T04 captures**. Request journal: no `store`/`previous_response_id`; step 1 without `modelName`/`reason`; model and effort per step; keep-alives ignored | todo |
| **T21** Use cases | `AssessmentService` (`llm-only` path), `DecisionService` (allowed set, one corrective retry, D5, signing, 409) | Flows 4.1–4.3; AC-15..AC-30; [ADR-001 §5.1–5.2](../ADR/001-backend-api.md#51-post-apicasesassessments), [ADR-002 §6.4](../ADR/002-llm-integration.md#64-deterministic-allowed-categories--server-validation), TAC-002-03 | T10–T15, T20 | **Haiku**: orchestration with a mocked `LlmGateway` port (internal dependency, not an external endpoint); escalate to Sonnet on a second failed review | `B/cases/AssessmentService.java`, `B/cases/DecisionService.java`, `BT/cases/AssessmentServiceTest.java`, `BT/cases/DecisionServiceTest.java`, `BT/support/CaptureFixtures.java` | Unit tests with exact retry counts. `LlmGateway` mock returns come only from `CaptureFixtures` (T04 captures, §4.1) | todo |
| **T22** Web layer (cases) | `CaseController`, `ApiExceptionHandler` (all codes incl. D3), `RateLimitFilter`, shared problem+json assertion helper | AC-41, AC-43; [ADR-001 §5](../ADR/001-backend-api.md#5-interface-contracts), [§6.3](../ADR/001-backend-api.md#63-rfc-9457-problemdetail-with-stable-error-codes), TAC-05, TAC-06, TAC-001-01, TAC-001-03, TAC-001-05 | T21 | **Sonnet**: MockMvc multipart, filter order, log-capture assertions | `B/web/CaseController.java`, `B/web/ApiExceptionHandler.java`, `B/web/RateLimitFilter.java`, `BT/web/CaseControllerIT.java`, `BT/web/RateLimitIT.java`, `BT/support/ProblemAssertions.java` (`CaptureFixtures` read-only) | Integration with WireMock from captures. **Integration check:** orchestrator runs full `./mvnw verify` (counts) | todo |
| **T23** Chat | `ChatHistoryValidator`, `ChatService` (D1 derivation, forbidden-update regeneration), `ChatController` SSE (heartbeat, disconnect closes upstream, exactly one terminal event) | Flow 4.4; AC-33..AC-37, AC-42; [ADR-001 §3.5, §5.3](../ADR/001-backend-api.md#35-streaming-sse-relay), [ADR-002 §6.5](../ADR/002-llm-integration.md#65-first-line-decision-gate-in-chat), TAC-001-04, ADR-005 D1 | T22 (`ApiExceptionHandler` read-only) | **Sonnet**: concurrency + SSE | `B/chat/ChatService.java`, `B/chat/ChatHistoryValidator.java`, `B/web/ChatController.java`, `BT/chat/ChatServiceTest.java`, `BT/chat/ChatHistoryValidatorTest.java`, `BT/web/ChatControllerIT.java` | Captured stream: success, `response.failed`, forbidden update → regeneration → `error`; full `verify` | todo |
| **T24** Pre-screen package | `DecisionsClient`, `PrescreenQuestionCatalog`, `PrescreenService`, `ConfidenceGate` (100 % branch coverage), `PrescreenAssessmentMapper`, `ShadowComparator`, `PrescreenProperties`, `PrescreenEvaluationIT` (`@Tag("eval")`) | AC-48, AC-49; [ADR-004 §3.1–3.8](../ADR/004-decisions-prescreening.md#31-components-package-copilotprescreen), TAC-004-01, TAC-004-05, TAC-004-07 | T04, T05 (parallel with T20–T23: own package) | OpenCode **Luna**: large but fully tabled package; parallel with Claude workers; escalate to Sonnet on a second failed review | `B/prescreen/**`, `BR/prescreen/templates_pl.properties`, `BT/prescreen/**`, `BTR/prescreen/expected.yaml` (structure; human fills ground truth) | Unit tests plus parsing of T04 Decisions captures; eval suite excluded (D7) | todo |
| **T25** Mode orchestration | `AssessmentService` runs `llm-only`/`shadow`/`gated` with fail-open; request-A signals to the vision LLM only after an `UNCERTAIN` gate in `gated` mode; `consistencySignals`/`prescreen` into the decision prompt | [ADR-004 §3.5](../ADR/004-decisions-prescreening.md#35-modes-decisions_mode), [§6.5](../ADR/004-decisions-prescreening.md#65-fail-open-to-the-llm-path), TAC-004-02/03/04/06 | T21, T22, T24 | Orchestrator — edits shared services after two predecessors | `B/cases/AssessmentService.java`, `B/cases/DecisionService.java`, `BT/cases/PrescreenModesIT.java` | Call counts per mode on the WireMock journal; full `verify` | todo |
| **T26** Backend API recordings for frontend mocks | Real responses of the running JAR with real OpenRouter and synthetic data:<br>• assessment 200 and decision 200 for each case type, including insufficient photo;<br>• problem+json 400/401/409/413/429;<br>• 502 and 504 from the D10 invalid-key and 1 ms-timeout instances;<br>• raw chat SSE: success, decision update, and `error` from the D10 invalid-key instance | [AGENTS.md TDD mock rule](../../AGENTS.md#tdd-rules), ADR-005 D10 | T23, T25 | Orchestrator — real calls with the key | `app/frontend/src/testing/recordings/**` (+ `*.meta.md`) | No secrets; each file has request, date and app commit. Commit `Test: recorded backend responses` | todo |

### Phase 3 — Frontend (T30–T32 parallel with Phases 1–2; all paths in `app/frontend`)

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|
| **T30** Frontend contracts + shell | `models.ts` (mirrors T05/D2), `labels.ts` (every Polish UI text from the PRD), `app.config.ts` providers, `app.ts` shell: ElektroDemo wordmark (D8), "Copilot serwisowy", demo note, screen switch placeholders | AC-44, AC-46; [ADR-003 §3.1–3.2](../ADR/003-frontend.md#32-application-structure), [ADR-000 §5](../ADR/000-main-architecture.md#5-data-models), ADR-005 D8 | T02, T05 | Orchestrator — shared contract | `F/core/models.ts`, `F/core/labels.ts`, `F/app.ts`, `F/app.html`, `F/app.scss`, `F/app.config.ts`, `app/frontend/public/favicon.svg` | Vitest: demo note and wordmark rendered; build green | todo |
| **T31** Pure utilities | `image-utils` (magic bytes, 320 px thumbnail), `sse-parser` (generic SSE grammar), `storage.service` (versioned keys, quota/parse guards) | AC-09, AC-10, AC-38, AC-40; [ADR-003 §3.5–3.6, §4](../ADR/003-frontend.md#36-photo-handling) | T30 | **Haiku**: pure units with exact tables | `F/core/image-utils.ts`, `F/core/sse-parser.ts`, `F/core/storage.service.ts` + `*.spec.ts` | Parser: CRLF, comments, multi-line data, split mid-line and mid-UTF-8 (spec grammar, not an endpoint mock) | todo |
| **T32** Form screen | `case-form` + `photo-picker` per AC-01..AC-14 and PRD §9.1 | [PRD 9.1](../PRD.md#91-form-screen), AC-01..AC-14, AC-47; [ADR-003 §3.4](../ADR/003-frontend.md#34-screens) | T30, T31 (`image-utils` read-only) | OpenCode **Luna**: well-specified UI, runs parallel with Claude backend workers | `F/form/**` | Vitest rows "Field order", "Validation messages", "Reason label/hint", "Size limit" (ADR-003 §8) | todo |
| **T33** State + API clients | `CaseStore`, `api.service`, `chat-stream.service`; byte-at-a-time stream test (TAC-003-03) | AC-14, AC-30, AC-34, AC-38..AC-42; [ADR-003 §3.3, §3.5, §5](../ADR/003-frontend.md#33-state-management-casestore) | T31, **T26** | **Sonnet**: signals, streaming, abort/retry semantics | `F/core/case-store.ts`, `F/core/api.service.ts`, `F/core/chat-stream.service.ts` + specs | `fetch`/HttpClient mocks **only from T26 recordings**; rows "Chat stream service", "Store persistence", "Retry without duplication"; 60 s timer with fake timers | todo |
| **T34** Progress + chat screens | `progress-screen`, `chat-screen`, `case-summary`, `message-list`, `composer`, both dialogs, inline **Nowe zgłoszenie** (D9) | Flows 4.3, 4.4, 4.6, 4.7; AC-26, AC-31..AC-42, AC-45, AC-47; [PRD 9.2–9.3](../PRD.md#92-progress-screen); [ADR-003 §3.4, §3.8](../ADR/003-frontend.md#38-accessibility-and-language), ADR-005 D9 | T33 | OpenCode **Luna**: component work from exact specs; escalate to Sonnet on a second failed review | `F/progress/**`, `F/chat/**` | Vitest: badge update, Enter/Shift+Enter, D9 button only when `photoSufficient=false`. Message, context and stream fixtures are loaded from T26 recordings, never hand-written | todo |
| **T35** Wiring + theme | Screens wired into the shell; Material M3 theme + brand overrides; light theme | [ADR-003 §3.7](../ADR/003-frontend.md#37-theming-and-brand-alignment), [design: colors](../design-guidelines.md#colors), [typography](../design-guidelines.md#typography), [components](../design-guidelines.md#components), [logo usage](../design-guidelines.md#logo-usage), TAC-003-07 | T32, T34, R3 | Orchestrator — shell and global styles hotspot | `F/app.ts`, `F/app.html`, `F/app.scss`, `app/frontend/src/styles.scss`, `app/frontend/src/theme/**` | `npm run build` zero warnings; full Vitest counts; full `./mvnw verify` | todo |

### Phase 4 — Real-stack integration, E2E, manual QA, packaging

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|
| **T40** Stack integration | T06 specs green on the real JAR + real OpenRouter + real Decisions (`shadow`), with D10 env (`RATE_LIMIT_*=1000`, fixed `CASE_SIGNING_KEY`); the "proof that calls were real" checks of §4.1 pass | TAC-01, TAC-02, ADR-005 D10, §4.1 | T25, T35 | Orchestrator — integration is serialized | Glue fixes only, in files of finished rows (row reopened, Status updated); `FE2E/support/real-stack.ts` (starts and stops real JAR instances with a given env, collects the app log) | Four separate records: build, test counts/skips, startup (health < 15 s), E2E result. Plus log evidence of real model calls and `jar tf` without test doubles | todo |
| **T41** Full E2E suite (real stack only) | UI flows: complaint; insufficient photo + D9; chat with the AC-35 date check; decision update; refresh in chat and in progress; new case; keyboard; 360/1280 px; no console errors.<br>**Mode matrix** (`shadow`, `gated` CONFIDENT and UNCERTAIN, `llm-only`).<br>**Endpoint checks** through the `request` fixture.<br>**Error paths** through the D10 real instances: invalid key → AC-41; 1 ms timeout → AC-41; chat `401` → AC-42 "Nie wysłano" + Ponów without duplication; `429`; Decisions `404` → AC-49 fail-open. All as specified in §4.1 | Flows 4.2–4.7; AC-09, AC-10, AC-41..AC-43, AC-48, AC-49; TAC-05, TAC-08, TAC-001-01/03/06, TAC-003-04/05/06, TAC-004-02/03/04/06, ADR-005 D10 | T40 | **Sonnet**: real-LLM non-determinism needs robust assertions (category sets, section headings, not exact text) plus process control of several real instances | `FE2E/**` (T06 files may be extended; `FE2E/support/real-stack.ts` extended) | `npx playwright test` counts per project and per mode. The global teardown asserts the §4.1 proof of real calls. The interception grep is empty | todo |
| **T42** Manual QA (real app, real APIs) | Human-like runs with `playwright-cli` on the real JAR:<br>• one return and one complaint per decision mode (§4.1);<br>• each error path on the D10 real instances;<br>• a run of `PrescreenEvaluationIT` (`@Tag("eval")`, real Decisions + vision LLM) producing `target/prescreen-eval.md`;<br>• screenshots of form, form errors, progress, error state, chat (first message, update, failed message) and the new-case dialog at 360 and 1280 px;<br>• brand comparison against `assets/homepage.png` and the tokens; console clean;<br>• OpenRouter activity count checked before and after the session | [AGENTS.md Manual QA](../../AGENTS.md#manual-qa-required-after-every-task-that-affects-the-running-app), TAC-003-07, [design summary](../design-guidelines.md#visual-style-summary), AC-44..AC-47 | T40 (parallel with T41) | Orchestrator via `playwright-cli` + human for the OpenRouter activity check — acceptance stays with a capable model | `docs/qa/hsdc-poc/*.png`, `docs/qa/hsdc-poc/report.md`, `docs/qa/hsdc-poc/prescreen-eval.md` (copied from `target/`) | Every defect becomes a fix row (`T4x-fix-n`) owned by the original row's worker | todo |
| **T43** Docker + env | `app/Dockerfile` (D4 `WORKDIR`), `.dockerignore`, `.env.example` with all ADR-000 §7 variables | [ADR-001 §3.8](../ADR/001-backend-api.md#38-dockerfile-appdockerfile-build-context--repository-root), [ADR-000 §7](../ADR/000-main-architecture.md#7-environment-variables), ADR-005 D4 | T40 | **Haiku**: small, template-like files | `app/Dockerfile`, `.dockerignore`, `.env.example` | **Docker unavailable here:** static review only; TAC-07 reported unverified (X25) | todo |

### Phase 5 — Independent review and pull request

| Task | Observable outcome | PRD/ADR links | Prerequisites | Owner/model | Editable files | Verification and handoff | Status |
|---|---|---|---|---|---|---|---|
| **T50** Independent review | (a) OpenCode **Luna**, read-only permissions, given the branch diff and the PRD/ADR paths but none of the orchestrator's conclusions; (b) Claude `/code-review high`; (c) `/security-review` (secrets, signature, prompt injection) | [AGENTS.md Code Review Rules](../../AGENTS.md#code-review-rules) | T41, T42, T43 | Luna (cross-family view) + Opus (acceptance) | none (reports in `.agent-logs/`) | The orchestrator reproduces each finding before accepting it; fixes become new rows; re-review of the fixed head | todo |
| **T51** Pull request | One PR `feat/hsdc-poc` → base per X24, linking every issue, with build/test/startup/E2E/QA evidence recorded separately, skips, and known gaps (TAC-07) | All | T50 resolved; **human authorizes the push** | Orchestrator | — | PR body checklist = §7 DoD; no merge by an agent | todo |
| **T52** Plan cleanup | Plan and `docs/.agent-briefs/hsdc-poc-*` removed with `git rm`; ADR-005 folded or kept as accepted | §3.3 | DoD met (PR merged by the human) | Orchestrator | plan, briefs | `Docs: remove completed hsdc-poc plan and briefs` | todo |

---

## 5. Critical path, hotspots, failure handling

**Critical path:** `T00 → T01 → T03 → T04 → T05 → T16 → T20 → T21 → T22 → T23 → T25 → T26 → T33 → T34 → T35 → T40 → T41/T42 → T50 → T51`.
The highest-risk links are T04 (real captures, credit, alpha API) and T20 (SDK version newer than the Context7 index). R1–R5, T10–T15, T24 and T30–T32 run off the critical path.

**Shared-file hotspots (single owner each):**

| File(s) | Owner | Rule |
|---|---|---|
| `app/backend/pom.xml` | Orchestrator (T01, T03) | All dependencies are added in T01; the hook reviews each change |
| `app/frontend/package.json`, `package-lock.json` | Orchestrator (T02) | Workers never install; a missing package is reported |
| `BR/application.yaml` | Orchestrator (T03) | Property needs are reported; the orchestrator adds them |
| T05 contracts, `F/core/models.ts`, `F/core/labels.ts` | Orchestrator | A contract change is committed before any dependent dispatch |
| `AssessmentService`, `DecisionService` | T21 → T25 | Strictly serial |
| `ApiExceptionHandler` | T22 | T23 reads it and never edits it |
| `F/app.ts`, `styles.scss`, theme | Orchestrator (T30, T35) | Screen components never edit the shell |
| `BTR/captures/**`, `src/testing/recordings/**` | Orchestrator (T04, T26) | Read-only for every worker |
| `opencode.json` | Orchestrator | Narrowed per dispatch, restored after |

**Failure and rollback:**
- One commit per row. A bad row is undone with `git revert <sha>`; there is no history rewriting.
- A diff that touches unlisted files, or missing red→green evidence, is rejected and re-briefed once, then escalated (§3.1).
- A capture or recording cannot be obtained: the row and its dependents go to `blocked:`, the missing prerequisite is reported, and no shape is invented.
- OpenRouter is unavailable or over budget: Luna rows move to a free OpenCode model or to Haiku. Real-stack E2E/QA waits; it is never replaced by mocks.
- A spec contradiction found during work: the orchestrator stops the affected rows and amends ADR-005 (or a new proposed ADR) for human approval.
- Set an OpenRouter key spending limit before T04 ([ADR-000 8.6](../ADR/000-main-architecture.md#86-in-memory-per-ip-rate-limiting)).

## 6. Smaller fallback (cut order)

1. Cut **T24/T25**: ship `DECISIONS_MODE=llm-only` and mark ADR-004 deferred. Customer behavior is unchanged; AC-48/49 are reported unmet.
2. Cut **T43** (unverifiable here anyway).
3. Reduce **T41** to return, complaint, refresh, one error path, at 1280 px. T42 keeps both widths.
4. No subagents/OpenCode: the orchestrator runs the same matrix sequentially.
5. Minimum demonstrable slice: T00–T05, T10–T16, T20–T22, T26 (without chat), T30–T33, T35, T40, T42: form → progress → first decision on the real stack, gaps listed in the PR.

## 7. Definition of Done (whole plan)

- [ ] ADR-005 accepted; every row `done` or `cut` with its reason recorded.
- [ ] `cd app/backend && ./mvnw verify`: zero failures, zero skipped (D7 exclusions are not counted); counts recorded (TAC-01).
- [ ] The JAR starts with only `OPENROUTER_API_KEY` set; health `UP` in < 15 s; `/` serves the SPA (TAC-02).
- [ ] TAC-03..06, TAC-001-01..06, TAC-002-01..07, TAC-003-01..07, TAC-004-01..07 evidenced (test names or QA screenshots).
- [ ] Every AC-01..AC-49 maps to at least one passing unit, integration or E2E test, or to a QA screenshot (traceability table in the PR).
- [ ] Playwright E2E green on the real stack at 360 and 1280 px, with no interception, no test doubles and no console errors (TAC-08), and with every item of the §4.1 mode matrix, endpoint checks and D10 error paths passing.
- [ ] Proof of real calls recorded for E2E and QA: token-usage and `prescreen.*` log lines per step, OpenRouter activity count increased, `jar tf` without test doubles, interception grep empty.
- [ ] Manual QA report covering every decision mode and error path, with screenshots, brand comparison and `prescreen-eval.md` from a real eval run; no open defects.
- [ ] Mocks exist only in unit/integration tests. Every stub body or fixture references a T04 capture or T26 recording, or a documented `derived-*` variant of one.
- [ ] Nested `app/backend/AGENTS.md` and `app/frontend/AGENTS.md` list the verified commands; `.env.example` is complete.
- [ ] Independent reviews resolved; PR opened with evidence; **the human merges**.
- [ ] Known gaps stated: TAC-07/TAC-001-07 (Docker) unless verified.
- [ ] T52 cleanup commit removes the plan and briefs.

## 8. Unresolved decisions (human)

1. **X24 PR base:** merge the PRD/ADR docs from `design-system` to `main` first, or include them in the PoC PR.
2. **X25 Docker:** accept TAC-07 unverified, or provide a Docker host.
3. **X26 ADR-005:** accept, amend or reject (D1–D7 are orchestrator proposals; D8–D9 are your decisions recorded).
4. **Ground truth** for `prescreen/expected.yaml` (eval suite) must be filled by a human.
5. **`.mcp.json`:** revert the hard-coded Context7 key and rotate it.

## 9. First bounded task and review gate

- **Review gate (now):** review this plan and ADR-005. Nothing else happens until you approve.
- **First task after approval: T00.**
  1. You run the JDK 25 download and the OpenRouter `/connect` (or key export) as `!` commands and approve enabling Issues.
  2. The orchestrator commits the plan and ADR-005, writes `opencode.json` and `.gitignore`, verifies `java -version`, `opencode models` (Luna) and Context7 access for OpenCode, creates `feat/hsdc-poc`, and creates one issue per row with its IDs written into §4.
  3. Then the research rows R1–R5 are dispatched in parallel.
- **Later gates:** after T05 (contracts), after Phase 2 + Phase 3 (before integration), and before T51 (push and PR need your explicit authorization).
