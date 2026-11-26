# problem-400-chat-history

- Recorded: 2026-11-26T12:37:06.310Z (Europe/Warsaw date 2026-11-26)
- App commit: cb0ae10
- Endpoint: POST /api/chat/messages
- Request: history = [assistant decision message] only (last message not from user)
- Instance env: RATE_LIMIT_CASES_PER_HOUR=1000, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed CASE_SIGNING_KEY, DECISIONS_MODE=shadow
- DECISIONS_MODE: shadow
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
