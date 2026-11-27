# problem-400-validation

- Recorded: 2026-11-26T12:37:05.859Z (Europe/Warsaw date 2026-11-26)
- App commit: cb0ae10
- Endpoint: POST /api/cases/assessments
- Request: multipart form={"caseType":"COMPLAINT","category":"SMARTPHONE","modelName":"","purchaseDate":"2026-11-29","reason":"abc"}, photo=phone-1.jpg
- Instance env: RATE_LIMIT_CASES_PER_HOUR=1000, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed CASE_SIGNING_KEY, DECISIONS_MODE=shadow
- DECISIONS_MODE: shadow
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
