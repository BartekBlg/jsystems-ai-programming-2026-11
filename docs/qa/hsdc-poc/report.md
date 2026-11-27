# Manual QA report — Hardware Service Decision Copilot PoC (hsdc-poc, T42)

**Date:** 2026-11-26 · **Branch:** `feat/hsdc-poc` · **Tested build:** JAR built from `8e4892f` (QA-A/B/C) and fixes verified on `ab30fb9` (T42-fix-1) · **Tool:** `playwright-cli` driving the real app (no interception, no mocks, no fake clocks; plan §4.1)

Real dependencies: OpenRouter Responses API (`openai/gpt-6-luna` for vision, decision and chat) and OpenRouter Decisions API (`typesafe/jev-1.14`). Synthetic form data only; photos only from `assets/example-images/`.

## Summary

| Part | Scope | Result | Details |
|---|---|---|---|
| QA-A | `shadow` mode: form + validation, return and complaint flows, chat streaming, reload, new case, 360/1280 px, brand comparison | Pass after T42-fix-1 (4 low/medium defects fixed) | [QA-A.md](QA-A.md), screenshots in [QA-A/](QA-A/) |
| QA-B | `gated` (CONFIDENT and UNCERTAIN) and `llm-only` modes, AC-35 | Pass (reported AC-35 issue triaged as a wrong test expectation) | [QA-B.md](QA-B.md), [QA-B/](QA-B/) |
| QA-C | ADR-005 D10 error paths on real faulty-configuration instances | Pass, 6/6 | [QA-C.md](QA-C.md), [QA-C/](QA-C/) |
| QA-D | Real pre-screen evaluation (`PrescreenEvaluationIT`, `@Tag("eval")`) | Run complete; ground truth pending | [prescreen-eval.md](prescreen-eval.md) |
| T42-fix-1 | Fixes for QA-A defects, re-verified on the real stack | Done | [T42-fix-1/](T42-fix-1/) |

Open defects: **none**.

## Decision-mode matrix (plan §4.1)

| Mode | Case | Observed | Evidence |
|---|---|---|---|
| `shadow` | Return (laptop-2.webp), complaint (phone-1.jpg) | Vision LLM always runs; one `prescreen.shadow` event per case; complaint category Odrzucone (AC-28) | QA-A §2, §5, §8 |
| `gated` CONFIDENT | Complaint, phone-1.jpg | `prescreen.gated gate=CONFIDENT visionLlmSkipped=true`; only a `decision` `llm.usage` line (no vision call) | QA-B §1 |
| `gated` UNCERTAIN | Return, laptop-2.webp | `gate=UNCERTAIN` (LOW_CONFIDENCE on photo/use questions), `visionLlmSkipped=false`, vision + decision `llm.usage` lines | QA-B §1 |
| `gated` fail-open | Decisions URL returns a real 404 | `prescreen.gated gate=UNCERTAIN reasons=[HTTP_404]`, vision + decision ran, customer saw no difference (AC-49) | QA-C §5 |
| `llm-only` | Return and complaint | No `prescreen.*` events, no Decisions calls | QA-B §2 |

## Error paths (ADR-005 D10)

| Path | Instance | Observed | AC |
|---|---|---|---|
| LLM failure | invalid key | "Nie udało się przygotować decyzji. Spróbuj ponownie za chwilę." with Spróbuj ponownie / Wróć do formularza; form values and photo kept; no technical text | AC-41, AC-43 |
| LLM timeout | real key, `llm.timeouts.vision=1ms` | same AC-41 state (504 behind it) | AC-41 |
| Chat failure + retry | valid → invalid-key → valid instance swap, same signing key | "Nie wysłano" + Ponów; after the valid restart Ponów delivered a reply; user message not duplicated | AC-42 |
| Rate limit | `RATE_LIMIT_CASES_PER_HOUR=1` | "Wysłano zbyt wiele zgłoszeń. Spróbuj ponownie później." | 429 |
| Refresh during analysis | valid | form with values, no photo, "Przetwarzanie zostało przerwane. Wyślij zgłoszenie ponownie." | AC-40 |

## UI and brand (QA-A, computed styles + screenshots)

- Screens at 360 and 1280 px: form, form errors, progress (both stages), chat (first message, streaming indicator, collapsed/expanded summary), new-case dialog, thumbnail dialog. `document.documentElement.scrollWidth` equal to the viewport on every screen (AC-45). Console: 0 errors, 0 warnings in all non-error flows.
- Brand vs `assets/homepage.png` and `docs/design-guidelines.md`: cream background `#fff9eb`; Atkinson Hyperlegible body, Bangers headings and buttons; 3 px ink borders; hard offset shadows without blur (buttons `4px 4px 0`, panels `6px 6px 0`); red primary `#d30f10` with white text; yellow demo note `#ffd447`; ElektroDemo text wordmark top-left (~158 px, ADR-005 D8). Header height 76 px after T42-fix-1.
- First agent message structure on every observed decision: greeting, "Decyzja", "Uzasadnienie", "Co widać na zdjęciu", "Kolejne kroki", a `§` citation in the justification, canonical disclaimer, and for Zwrot the statutory-right sentence (AC-21, AC-23).

## Defects found and their resolution

| ID | Found in | Description | Resolution |
|---|---|---|---|
| QA-A O1 | QA-A | After reload the chat showed the first message instead of the latest (PRD §9.3) | Fixed in T42-fix-1 (scroll after restored messages render), re-verified at 360 px |
| QA-A D1 | QA-A | Model-name error clipped to one line at 360 px (AC-04/AC-45) | Fixed in T42-fix-1 (dynamic subscript sizing) |
| QA-A D2 | QA-A | Sticky header could cover a field scrolled into view | Fixed in T42-fix-1 (`scroll-padding-top`) |
| QA-A D3 | QA-A | Header 67 px vs 76 px guideline | Fixed in T42-fix-1 |
| QA-B D1 | QA-B | "Send-back deadline ignores delivery date" | **Not a defect**: return policy §6.2 counts sending back from the withdrawal statement; the delivery date was used for the §2.1 withdrawal window (AC-35 satisfied). The QA brief's expectation was wrong |
| QA-C obs. 1–3 | QA-C | Retry offered on 429; stale "przerwane" notice per origin; capability check "OK" with invalid key | Triaged as by design (see QA-C.md review note) |

Earlier smoke-check defects (T35b: bundle budget, typed pl-PL date, thumbnail, message spacing) were fixed in T35-fix-1 before this QA round.

## Pre-screen evaluation (QA-D)

10 real image × case-type runs (5 photos × RETURN/COMPLAINT), 30 real calls: 1/10 CONFIDENT at threshold 0.80 (phone-1 as COMPLAINT, agreeing with the vision LLM on photo, damage and cause), 9/10 UNCERTAIN on low confidence of `noul` photo/use questions (Jev 1.14 returns no `noul` confidence, so `max(p, 1 − p)` applies — ADR-004 Decision 6.6). Decisions latency 276–3409 ms (budget 6 s). **Ground truth in `app/backend/src/test/resources/prescreen/expected.yaml` is still TODO (human)**, so agreement against ground truth is not computed; the switch from `shadow` to `gated` (ADR-004 §6.4) stays Proposed.

## Proof that calls were real

- App logs of every QA instance contain `llm.usage` lines (numbers and OpenRouter `gen-…` request ids only, no text content) per model step, and the expected `prescreen.shadow` / `prescreen.gated` events.
- The OpenRouter activity-count check by a human (plan §4.1) was **declined by the user**; `/api/v1/key` usage is not usable for this BYOK key. Evidence therefore relies on the log lines above, `jar tf` without `wiremock`/`mockito` classes (T40) and the empty interception grep over `app/frontend/e2e` (T40, T41).

## Evidence hygiene

Screenshots were checked for byte-identical duplicates (`md5sum`). Two mislabeled QA-C "progress" screenshots (identical to the error state, because the invalid-key failure is immediate) were removed; the progress screen is evidenced by QA-A. One QA-A duplicate pair documents defect O1.
