### Key usage and cost

- The key is used BYOK: `GET /api/v1/key` `usage` stays at the same value (OpenRouter-billed cost 0; unchanged also across the T04b Jev run). Real spend is in each response's `usage.cost_details.upstream_inference_cost` (sum over all non-stream captures about 0.0041 USD, each stream about 0.0004 USD), total well below 0.01 USD.

### Did OpenRouter honor `instructions`? YES

- `responses/probe-instructions`: instruction "Zacznij odpowiedź od słowa KONTROLA" produced a reply starting with "KONTROLA".
- Decision captures follow the prompt heading format (`## Decyzja`, `## Uzasadnienie`, `## Co widać na zdjęciu`, `## Kolejne kroki`) and chat follows chat.md (`Zaktualizowana decyzja: …` in `chat-decision-update`). No need for a leading system message.
- `response.created` in the stream echoes the full `instructions` text (policy included) in the event body.

### Streaming (`chat-plain.sse`, `chat-decision-update.sse`)

- There are no `event:` lines; every event is a `data: {json}` line whose `type` field names the event, followed by a blank line, and a final `data: [DONE]`.
- Event types seen: `response.created`, `response.in_progress`, `response.output_item.added`, `response.content_part.added`, `response.output_text.delta`, `response.output_text.done`, `response.content_part.done`, `response.output_item.done`, `response.completed` (terminal; carries `usage`). With reasoning in the stream (`chat-decision-update`) a reasoning `output_item.added`/`done` pair precedes the message item.
- Keep-alive: `chat-plain.sse` has no comment lines. `chat-decision-update.sse` has 3 SSE comment lines, each exactly `: ` (colon and space, not the text `: OPENROUTER PROCESSING`), at the start while the model was reasoning. A consumer must ignore comment lines.
- Pre-stream errors (invalid key) are a plain JSON body with HTTP 401 (`chat-invalid-key.json`, `{"error":{"message":"User not found.","code":401}}`).
- Reasoning `summary` is null (not requested); the Responses API returns `reasoning` output items in non-streaming bodies (`output` = `[reasoning, message]`).

### Errors

- `responses/error-400`: invalid `reasoning.effort` gives HTTP 400 `{"error":{"code":"invalid_prompt","message":"Invalid Responses API request"},"metadata":{"raw":"<zod issues JSON string>"}}`. The raw issue lists valid efforts: max, xhigh, high, medium, low, minimal, none.
- `responses/error-401` and `chat-invalid-key`: HTTP 401 `{"error":{"message":"User not found.","code":401}}`.
- `responses/error-429`: MISSING (not provokable safely).

### Decisions API (TypeSafe Jev 1.14, T04b, 2026-11-26)

15 real calls (10 request A, 2 request B, 3 error probes); 12 returned 200, 3 were the expected errors. Unchanged from the first attempt: validation runs before auth (400 without a valid key needed). The old Luna route's 401 `account_deactivated` is gone.

- **Model and provider.** Sent `model: "typesafe/jev-1.14"`; every 200 echoes `model: "typesafe/jev-1.14-20261014"` (dated snapshot) and `provider: "TypeSafe"` (first-party). Errors carry neither.
- **Answer shapes (96 answers, 0 refusals).**
  - `noul`: `{type, noul}` only. It has **no `confidence` field** in any of the captured noul answers (ADR-004 §2/§3.4 claim one; the documented fallback `max(p, 1-p)` is therefore what applies). Values have 2 decimals (e.g. 0.77).
  - `choice`: `{type, choice, probabilities (all options, sum 1), confidence}`; often hard 1/0.
  - `score`: `{type, score (continuous, e.g. 3.95), legend {"0": level text, ...} (present, echoes the criteria), probabilities (string keys "0".."N-1"), confidence}`.
  - Top level: `{model, answers, usage, id, provider}`; `usage = {input_tokens, output_tokens, cost, is_byok, cost_details.upstream_inference_cost...}`. `usage.cost` is 0 (BYOK), the real cost is `cost_details.upstream_inference_cost` (about 0.0003 USD per image call, 0.0015 USD for all 12 successful calls). Input tokens about 6.9k per image call.
- **Latency (12 successful calls).** min 229 ms, median 308 ms, max 597 ms (largest image laptop-1, 1482x1536). Far below the 6 s budget. Errors: 32 ms (400), 90 ms (400 image format), 171 ms (401).
- **Refusals.** None among the 96 answers.
- **Confidence behaviour.** Jev is confident (>= 0.80) on `choice` questions, but `noul` questions often sit at 0.55-0.80 (`max(p, 1-p)`) and `score` conditions are frequently low, so see the generated gate table: only phone-1 complaint is CONFIDENT, the rest UNCERTAIN. Typical blockers: `photo_usable` 0.70-0.78 and the `use_*`/`missing_*` return questions. The 0.80 threshold is likely too strict for noul and should be tuned in shadow mode (ADR-004 6.4).
- **Errors.**
  - `error-400` (invalid question type): `{"error":{"message":"<zod issues JSON string>","code":400},"user_id":"<redacted>"}`; discriminator options `noul|choice|score|box`.
  - `error-401` (invalid key): `{"error":{"message":"User not found.","code":401}}`.
  - `error-image-format` (original laptop-2.webp as `data:image/webp`): HTTP 400 `{"error":{"message":"TypeSafe Decisions accepts only base64 data URL images in PNG or JPEG format","code":400}}`.
- **Image bounds.** All five downscaled JPEGs satisfied the Jev bounds (dimensions logged in each meta): laptop-1 1482x1536, laptop-2 863x843, phone-1 900x1200, phone-2 387x516, phone-3 449x445. The bounds-violation, 413 and refusal paths were not captured (no image generated to provoke them; no refusal occurred).
- **Request B.** Injection guard sentence appended to every B question. The model_name/reason text does not influence A. B answers are plain noul (e.g. b-return: `return_reason_reports_defect` 0.01 for "Zmieniłem zdanie...", `reason_contains_instructions` 0.22; b-complaint: `description_matches_photo` 0.04, `model_matches_photo` 0.01 for a Samsung S24 label on phone-2).

### Other notes

- `responses/timeout-note.meta.md`: a 1 ms `HttpRequest.timeout` surfaced as `java.net.http.HttpConnectTimeoutException: HTTP connect timed out` (the connection phase timed out first, not a read timeout).
- Assessment results: return/laptop-2 gives photoSufficient true, WEAR, resellableAsNew false (R3/R4 path). complaint phone-1 (chosen as `assessment-complaint-ok`) shows MECHANICAL_DAMAGE/MEDIUM (rule C4: ESCALATED only); phone-2 and phone-3 gave defectVisible false/UNKNOWN. `assessment-insufficient` (phone-1 as Laptop) gave photoSufficient false, as expected, so no retry was needed.
- Responses `usage` carries `input_tokens_details.cached_tokens`/`cache_write_tokens`, `output_tokens_details.reasoning_tokens`, `cost` (0, BYOK), `is_byok`, `cost_details.upstream_inference_cost`.
