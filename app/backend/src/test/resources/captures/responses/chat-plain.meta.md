# Capture responses/chat-plain

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses (stream=true)
- Model: openai/gpt-6-luna
- Status: 200
- Request: chat on responses/decision-return-ok (decision ESCALATED), user message "Jak mam zapakować urządzenie?", reasoning low, max_output_tokens 4000, instructions = chat.md + '# Regulamin' + return policy
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: Keep-alive comments (': OPENROUTER PROCESSING') present: false. events=[]. First line of reply: "Zapakuj laptop bezpiecznie, najlepiej w oryginalne opakowanie, jeśli je masz. Dołącz akcesoria i dowód zakupu (§6 ust. 1). Oryginalne opakowanie nie jest wymagane w regulaminie; zadbaj, by urządzenie nie przemieszczało się i było zabezpieczone przed uszkodzeniem w transporcie."
