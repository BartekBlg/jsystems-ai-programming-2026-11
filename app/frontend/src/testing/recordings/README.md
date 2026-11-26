# Backend recordings (hsdc-poc T26)

Verbatim responses of the REAL running backend (real JAR, real OpenRouter, synthetic form data, photos from
`assets/example-images/*`). Produced by `app/frontend/scripts/record-backend.mjs`. Each recording has a
`<name>.meta.md` (date, commit, endpoint, request, instance env). JSON recordings: `<name>.json` =
`{status, headers, body}` plus `<name>.body.raw` (exact body text). SSE: `<name>.sse` (exact text) + `<name>.headers.json`.
Never edit these files; re-record instead. Model outputs vary between runs.

| Recording | Status | Notes |
|---|---|---|
| assessment-return-ok / decision-return-ok | 200 / 200 | RETURN, LAPTOP, laptop-2.webp, 5 days; decision category ESCALATED (model judged signs of wear) |
| assessment-complaint-ok / decision-complaint-ok | 200 / 200 | COMPLAINT, SMARTPHONE, phone-1.jpg, 60 days; REJECTED |
| assessment-insufficient / decision-insufficient | 200 / 200 | phone photo for a laptop; photoSufficient=false, MORE_INFO_NEEDED |
| assessment-return-more-info / decision-return-more-info | 200 / 200 | RETURN 20 days; MORE_INFO_NEEDED (asks for delivery date) |
| problem-400-validation | 400 | VALIDATION_FAILED with fieldErrors |
| problem-400-unsupported-image | 400 | UNSUPPORTED_IMAGE (text bytes named photo.jpg) |
| problem-401-signature | 401 | INVALID_CASE_SIGNATURE |
| problem-409-decided | 409 | DECISION_ALREADY_MADE |
| problem-413-too-large | 413 | PHOTO_TOO_LARGE |
| problem-429 | 429 | RATE_LIMITED (instance with RATE_LIMIT_CASES_PER_HOUR=1) |
| problem-502-llm-failed | 502 | LLM_FAILED (invalid-key instance) |
| problem-504-llm-timeout | 504 | LLM_TIMEOUT, recorded at commit b3e58d2 (valid key, `--llm.timeouts.vision=1ms`, DECISIONS_MODE=llm-only); earlier failed attempts are in `_unreproduced/README.md` |
| chat-plain.sse | 200 | `delta` events, then `done` |
| chat-decision-update.sse | 200 | `decision` (ESCALATED) first, then `delta`s, `done` |
| chat-error.sse | 200 | `event: error` `LLM_FAILED` (invalid-key instance, same signing key) |
| problem-400-chat-history | 400 | last message not from the user |
| problem-401-chat-signature | 401 | INVALID_CASE_SIGNATURE |
