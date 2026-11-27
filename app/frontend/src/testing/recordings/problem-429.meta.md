# problem-429

- Recorded: 2026-11-26T12:37:41.837Z (Europe/Warsaw date 2026-11-26)
- App commit: cb0ae10
- Endpoint: POST /api/cases/assessments
- Request: second request of the hour; multipart form={"caseType":"RETURN","category":"LAPTOP","modelName":"Lenovo ThinkPad T14 Gen 4","purchaseDate":"2026-11-21","reason":""}, photo=photo.jpg = bytes of docs/README.md (text), so no LLM call
- Instance env: RATE_LIMIT_CASES_PER_HOUR=1, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed CASE_SIGNING_KEY, DECISIONS_MODE=shadow
- DECISIONS_MODE: shadow
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
