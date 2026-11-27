# QA-C — error paths on faulty-configuration instances (T42)

JAR: `/tmp/claude-1000/qa/service-copilot-qa.jar` (commit 8e4892f), ports 8094-8096, one instance at a time. Real OpenRouter. Logs: `/tmp/claude-1000/qa/QA-C-*.log`. Date 2026-11-26. Purchase date used: 21.11.2026 (5 days ago).

Result: all 6 scenarios PASS. No blocking defects; 3 minor observations below.

| # | Scenario | Expected | Observed | Result |
|---|---|---|---|---|
| 1 | 8094, invalid key, Zwrot/Laptop/laptop-2.webp | Error state with exact text, buttons "Spróbuj ponownie" + "Wróć do formularza", no technical text; back keeps values + photo | Error "Nie udało się przygotować decyzji. Spróbuj ponownie za chwilę." + both buttons, no technical text. Wróć do formularza: radio, category, model, date and photo `laptop-2.webp` kept. At 360 px layout fine, `scrollWidth`=360. Spróbuj ponownie re-submitted and gave the same error (3 failed requests in log). HTTP 502 behind it. Log: `llm failure step=vision ... UnauthorizedException status=401`, `prescreen.shadow.skipped reason=LLM_ASSESSMENT_FAILED` | PASS |
| 2 | 8095, real key, `--llm.timeouts.vision=1ms` | Same AC-41 error state (504) | Same message and buttons; browser console shows only `504 /api/cases/assessments`. Log: `llm failure step=vision ... cause=OpenAIIoException` | PASS |
| 3 | 8096 chat failure + Ponów (AC-42) | After invalid-key restart: message "Nie wysłano" + Ponów; after valid restart, Ponów gives a reply, no duplicate user message | Case created on valid instance, chat reached (decision badge "Przekazane do specjalisty"). On invalid-key instance 8096 the message showed "Nie wysłano" + "Ponów". After restarting valid on 8096 and clicking Ponów: agent reply arrived (`llm.usage step=chat`), "Nie wysłano" gone, user message appears exactly once (DOM count 1). Case survived restart (same signing key). | PASS |
| 4 | 8094, `RATE_LIMIT_CASES_PER_HOUR=1`, real key | 1st OK, 2nd error "Wysłano zbyt wiele zgłoszeń. Spróbuj ponownie później." | 1st reached chat with decision. 2nd: alert with exactly that text (HTTP 429 in console, expected resource-load log). | PASS |
| 5 | 8095, gated + nonexistent decisions URL (AC-49) | Fail-open: submission succeeds; log shows pre-screen unavailable reason and vision llm.usage | Chat reached. Log: `prescreen.gated gate=UNCERTAIN reasons=[HTTP_404] visionLlmSkipped=false`, then `llm.usage step=vision` and `llm.usage step=decision`. No console errors. | PASS |
| 6 | Reload during "Analizujemy zdjęcie" (AC-40) | Form with values, no photo, "Przetwarzanie zostało przerwane. Wyślij zgłoszenie ponownie." | Exactly that message (status region) above form; Zwrot, Laptop, model, date kept; no photo in the dropzone. | PASS |

Console errors: none except the expected failing-API resource logs (502, 504, 429) in the error scenarios. Chat/progress/success flows: 0 errors, 0 warnings.

## Screenshots (`docs/qa/hsdc-poc/QA-C/`)
- s1-form-filled-1280.png, s1-progress-1280.png, s1-error-1280.png, s1-back-to-form-1280.png, s1-progress-360.png, s1-error-360.png
- s2-timeout-error-1280.png
- s3-chat-valid-1280.png, s3-chat-failed-1280.png, s3-chat-retry-ok-1280.png
- s4-ratelimit-1280.png
- s5-gated-failopen-chat-1280.png
- s6-before-reload-1280.png, s6-after-reload-1280.png

## Observations / defects (all minor, none blocking)
1. **Retry offered on rate-limit error (Low, UX).** Steps: scenario 4, second submission. Expected: retry pointless within the hour; observed: error panel still shows "Spróbuj ponownie" (plus Wróć do formularza). Screenshot: s4-ratelimit-1280.png. Possibly by design.
2. **Stale "Przetwarzanie zostało przerwane" notice after a failed submission (Low).** Steps: after scenario 6 (and after the 429 in scenario 4), opening the app on another port in the same browser showed the "interrupted" status plus restored values immediately. The draft/progress marker is evidently kept in cookies or storage shared across localhost ports (cookies ignore ports), so this is most likely a test-environment artifact of using several ports in one browser profile; also arguably expected after an error/abort. Not reproduced as a defect in a single-origin flow. Worth a quick check that the marker is cleared when the error state is dismissed with "Wróć do formularza".
3. **Capability check passes with an invalid key (Info).** On 8094 the startup log says `LLM capability check OK for model=openai/gpt-6-luna` even though the key is invalid (every real call returns 401). The check probably does not authenticate; consider whether readiness should reflect an invalid key.

## Housekeeping
All java processes started by this run were stopped by PID; playwright-cli browser closed. Only `docs/qa/hsdc-poc/QA-C/**` and this file were written in the repo.

## Orchestrator review note (2026-11-26)
- `s1-progress-1280.png` and `s1-progress-360.png` were byte-identical to the `s1-error-*` screenshots: with the invalid key the real 401/502 arrives within about a second, so the "progress" capture already showed the error state. They were removed as mislabeled evidence; the progress screen is evidenced by QA-A. `s2-timeout-error-1280.png` is legitimately identical to `s1-error-1280.png` (same UI state, same text).
- Observations triage: (1) "Spróbuj ponownie" on the 429 panel — kept (AC-41 asks for the retry action; the server detail tells the customer to try later); (2) stale "przerwane" notice — the draft marker is per origin (port), an error state that is followed by a reload correctly reports the interruption (AC-40); no defect; (3) capability check "OK" with an invalid key — the models/endpoints call is public and non-fatal by design (TAC-002-07); no defect.
