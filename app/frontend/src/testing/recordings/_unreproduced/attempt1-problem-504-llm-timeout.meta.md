# problem-504-llm-timeout

- Recorded: 2026-11-26T12:38:50.701Z (Europe/Warsaw date 2026-11-26)
- App commit: cb0ae10
- Endpoint: POST /api/cases/assessments
- Request: multipart form={"caseType":"RETURN","category":"LAPTOP","modelName":"Lenovo ThinkPad T14 Gen 4","purchaseDate":"2026-11-21","reason":""}, photo=laptop-2.webp
- Instance env: valid OpenRouter key, --llm.timeouts.vision=1ms (ADR-005 D10), RATE_LIMIT_CASES_PER_HOUR=1000, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed CASE_SIGNING_KEY, DECISIONS_MODE=shadow
- DECISIONS_MODE: shadow
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
