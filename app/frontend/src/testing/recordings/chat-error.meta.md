# chat-error

- Recorded: 2026-11-26T12:38:11.641Z (Europe/Warsaw date 2026-11-26)
- App commit: cb0ae10
- Endpoint: POST /api/chat/messages
- Request: same request as chat-plain, sent to an instance with an invalid OpenRouter key (same CASE_SIGNING_KEY) so the stream ends with event: error
- Instance env: OPENROUTER_API_KEY = deliberately invalid placeholder from ADR-005 D10, RATE_LIMIT_CASES_PER_HOUR=1000, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed CASE_SIGNING_KEY, DECISIONS_MODE=shadow
- DECISIONS_MODE: shadow
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
