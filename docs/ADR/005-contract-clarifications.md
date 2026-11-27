# ADR-005: Contract Clarifications Found During Implementation Planning

**Date:** 2026-11-26
**Status:** Accepted (2026-11-27, by the user; amends the referenced sections of ADR-000…ADR-003)
**Relates to:** [`000-main-architecture.md`](000-main-architecture.md), [`001-backend-api.md`](001-backend-api.md), [`002-llm-integration.md`](002-llm-integration.md), [`003-frontend.md`](003-frontend.md), [`004-decisions-prescreening.md`](004-decisions-prescreening.md)

---

## 1. Scope

The cross-check of the PRD, ADR-000..004 and `AGENTS.md` before implementation found gaps and contradictions that would leave an implementing agent unsure what to do. This ADR resolves them. When accepted, it amends the referenced ADR sections; where this ADR and an earlier ADR differ, this ADR wins.

---

## 2. Decisions

### D1. Current decision category in chat requests
**Context:** [ADR-002 §4](002-llm-integration.md#4-data-structures) gives `ChatPrompt.currentCategory`, but [ADR-001 §4](001-backend-api.md#4-data-structures) `ChatRequest.messages[]` carries only `{id, role, text}`, and decision updates in chat are not signed ([ADR-000 Decision 8.4](000-main-architecture.md#84-hmac-signed-casecontext)). The server has no defined source for the current category.
**Decision:** `ChatService` derives `currentCategory` from the request: it takes the last assistant message whose first line is a valid `Zaktualizowana decyzja: <label>` that is **not** in `chatForbiddenCategories`. If there is none, it uses the signed `caseContext.decision.category`. The client cannot set the category directly. Forged history can at most claim an allowed category, and the hard rules stay enforced by `chatForbiddenCategories` from the signed `ruleFacts`.
**Consequence:** `ChatRequest` is unchanged. `ChatService` has unit tests for each derivation branch.

### D2. Single `CaseContextDto` field set
**Context:** [ADR-000 §5](000-main-architecture.md#5-data-models) lists `assessmentSource`, `prescreen` and `consistencySignals`. [ADR-001 §4](001-backend-api.md#4-data-structures) omits them. ADR-000 §5 and [ADR-004 §3.6](004-decisions-prescreening.md#36-mapping-a-confident-result-to-imageassessment) also add `source` inside `ImageAssessment`.
**Decision:** `CaseContextDto` = `contextVersion`, `caseType`, `form`, `submittedAt`, `assessment`, `assessmentSource` (`LLM` | `DECISIONS`), `prescreen` (nullable), `consistencySignals` (nullable), `ruleFacts`, `decision` (nullable, `{category}`). The authoritative source field is the context-level `assessmentSource`. `ImageAssessment` does not serialize its own `source`. Nullable fields are serialized as JSON `null`, never omitted, so canonical JSON is stable.

### D3. Missing error codes
**Context:** [ADR-001 §5.2](001-backend-api.md#52-post-apicasesdecisions) uses `409 DECISION_ALREADY_MADE`, and TAC-001-06 expects a problem+json `404` for an unknown `/api/**` path. Neither code is in the [ADR-000 §6](000-main-architecture.md#6-api--interface-contracts) table.
**Decision:** Add `409 DECISION_ALREADY_MADE` ("Decyzja dla tego zgłoszenia została już przygotowana.") and `404 NOT_FOUND` ("Nie znaleziono zasobu.") to the code table. The frontend treats both like `INTERNAL` (AC-41 message).

### D4. Maven wrapper invocation
**Context:** TAC-01 and the [ADR-001 §3.8](001-backend-api.md#38-dockerfile-appdockerfile-build-context--repository-root) Dockerfile use `./mvnw -f app/backend/pom.xml …` from the repository root, but the wrapper is generated in `app/backend/` ([ADR-001 §3.1](001-backend-api.md#31-project-initialization-maven--spring-initializr)). No `./mvnw` exists at the root.
**Decision:** The canonical commands run inside the module: `cd app/backend && ./mvnw <goal>`. From the root, the equivalent is `app/backend/mvnw -f app/backend/pom.xml <goal>`, and it is used only after T01 verifies it works. The Dockerfile sets `WORKDIR /src/app/backend` before `./mvnw`. TAC-01, TAC-02 and TAC-07 are read with these commands.

### D5. Rule R6 rejection needs a §5 citation
**Context:** [ADR-002 §3.3](002-llm-integration.md#33-deterministic-policy-rules-policyruleengine) row R6 allows `REJECTED` for a return "only with a citation of return policy §5 (exclusions)". [ADR-002 §3.6](002-llm-integration.md#36-first-decision-validation-decisionmessagevalidator) does not check this.
**Decision:** `RuleResult` gets `rejectionRequiresCitation` (`"§5"` for R6, otherwise empty). `DecisionMessageValidator` treats `category = REJECTED` without that citation as invalid, which triggers the existing single corrective retry and then `502 LLM_FAILED`.

### D6. Canonical JSON for HMAC
**Context:** [ADR-001 §3.1](001-backend-api.md#31-project-initialization-maven--spring-initializr) notes that Jackson 3 (Spring) and Jackson 2 (OpenAI SDK) coexist. The `CanonicalJson` mapper is unspecified.
**Decision:** `CanonicalJson` uses a dedicated Jackson 3 (`tools.jackson`) `JsonMapper` with sorted properties and sorted map keys, no indentation, ISO-8601 dates and default shortest-form doubles. It is never the SDK's Jackson 2 mapper. A test verifies that the signature still validates after a JavaScript `JSON.parse`/`JSON.stringify` round trip with reordered keys (the browser storage path).

### D7. Tests excluded from `verify`
**Context:** TAC-01 allows no skipped tests "except those tagged `e2e`". [ADR-004 §3.8](004-decisions-prescreening.md#38-logging-and-evaluation) adds `@Tag("eval")`, and the capture tooling needs real API calls.
**Decision:** Tests tagged `eval` and `capture` are **excluded** through Surefire/Failsafe `excludedGroups`, so they are not executed or reported as skipped. TAC-01 counts only executed tests. Playwright E2E runs outside Maven.

### D8. Header brand mark
**Context:** PRD §9 asks for the shop logo of the fictional "ElektroDemo". `assets/logo.svg` is the DevPowers wordmark, and [design guidelines](../design-guidelines.md#assets) require checking usage rights.
**Decision (user, 2026-11-26):** The header shows a text wordmark "ElektroDemo" in Bangers plus the product name "Copilot serwisowy", styled with the design tokens. `assets/logo.svg` is not shipped. The favicon is a simple token-colored SVG created in the frontend. Placement and size follow [design guidelines: Logo usage](../design-guidelines.md#logo-usage) (top-left, about 158 px wide). This amends [ADR-003 §3.1 step 7 and §3.4 Shell](003-frontend.md#31-project-initialization).

### D9. Inline "Nowe zgłoszenie" action for an insufficient photo
**Context:** PRD Flow 4.3 and AC-26 require the first message to "offer" **Nowe zgłoszenie**. [ADR-003 §3.4](003-frontend.md#34-screens) has only the header button.
**Decision (user, 2026-11-26):** When `caseContext.assessment.photoSufficient = false`, the chat shows a **Nowe zgłoszenie** button directly under the first agent message. It opens the same AC-39 confirmation dialog.

### D10. Real-stack verification of error paths
**Context:** AC-41 and AC-42 failures cannot be provoked against a healthy OpenRouter, and E2E and manual tests must not use mocks ([AGENTS.md test strategy](../../AGENTS.md#verification-required-before-every-commit)).
**Decision:** Error paths are provoked against the **real OpenRouter API** by starting the real JAR with a deliberately faulty configuration. There are no stubs, no request interception and no fake hosts. Every request still leaves the application and reaches OpenRouter.

| Path | Real instance configuration | Real call and outcome |
|---|---|---|
| `LLM_FAILED` (AC-41, AC-43) | `OPENROUTER_API_KEY=sk-or-v1-invalid-e2e` | OpenRouter answers a real `401`; the app maps it to `502 LLM_FAILED` |
| `LLM_TIMEOUT` (AC-41) | The real key, with the vision step timeout overridden to `1ms` by a Spring command-line property | The request is sent to OpenRouter and aborted by the SDK timeout; the app returns `504 LLM_TIMEOUT` |
| Chat failure + retry (AC-42) | The case is created on a valid instance. The test stops it and starts an invalid-key instance on the same port with the same `CASE_SIGNING_KEY`, sends a chat message, then restarts the valid instance and clicks **Ponów** | Real `401` → SSE `error` event, then a real successful stream with no duplicated message |
| Rate limit (`429`) | `RATE_LIMIT_CASES_PER_HOUR=1` | The second real assessment is rejected by the real filter |
| Pre-screening fail-open (AC-49) | `DECISIONS_MODE=gated`, `DECISIONS_URL=https://openrouter.ai/api/alpha/decisions-nonexistent-e2e` | The real OpenRouter host answers `404`; the case continues through the real vision LLM |

`LlmProperties` per-step timeouts are ordinary Spring properties (names fixed in T05), so the override needs no new environment variable. The 60 s browser timer itself (AC-30) is unit-tested with fake timers, because a backend step always ends earlier (26 s + 34 s).

Happy-path E2E and manual QA set `RATE_LIMIT_CASES_PER_HOUR=1000`, `RATE_LIMIT_CHAT_PER_HOUR=1000` and a fixed `CASE_SIGNING_KEY`, so suites are not throttled and stored cases survive restarts.

---

## 3. Consequences

- (+) Every contract an implementing agent needs is defined. No agent has to choose between conflicting ADR text.
- (-) ADR-000 §6, ADR-001 §4 and §3.8, ADR-002 §3.6 and ADR-003 §3.1 and §3.4 must be read together with this ADR until they are edited in place.

**Review trigger:** Any later contract change. Fold these amendments into the area ADRs when the PoC is accepted.
