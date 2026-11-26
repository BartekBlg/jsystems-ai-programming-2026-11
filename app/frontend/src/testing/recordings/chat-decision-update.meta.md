# chat-decision-update

- Recorded: 2026-11-26T12:37:15.380Z (Europe/Warsaw date 2026-11-26)
- App commit: cb0ae10
- Endpoint: POST /api/chat/messages
- Request: RETURN case purchased 20 days ago (decision-return-more-info); history = [assistant decision message, user "Urządzenie zostało dostarczone 3 dni temu."]
- Instance env: RATE_LIMIT_CASES_PER_HOUR=1000, RATE_LIMIT_CHAT_PER_HOUR=1000, fixed CASE_SIGNING_KEY, DECISIONS_MODE=shadow
- DECISIONS_MODE: shadow
- Data: synthetic form values, real photos from assets/example-images only. Responses saved verbatim from the real backend and real OpenRouter.
