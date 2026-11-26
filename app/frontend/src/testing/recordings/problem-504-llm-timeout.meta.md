# problem-504-llm-timeout

- Recorded: 2026-11-26T12:58:12.336Z (Europe/Warsaw date 2026-11-26)
- App commit: b3e58d2
- Endpoint: POST /api/cases/assessments
- Request: multipart form={"caseType":"RETURN","category":"LAPTOP","modelName":"Lenovo ThinkPad T14 Gen 4","purchaseDate":"2026-11-21","reason":""}, photo=laptop-2.webp
- Instance env: RATE_LIMIT_CASES_PER_HOUR=1000, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed test CASE_SIGNING_KEY, DECISIONS_MODE=llm-only, CLI --llm.timeouts.vision=1ms, valid OpenRouter key (not recorded)
- DECISIONS_MODE: llm-only
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
