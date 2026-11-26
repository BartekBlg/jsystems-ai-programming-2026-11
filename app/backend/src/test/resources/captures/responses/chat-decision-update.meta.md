# Capture responses/chat-decision-update

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses (stream=true)
- Model: openai/gpt-6-luna
- Status: 200
- Request: chat on responses/decision-return-more-info (decision MORE_INFO_NEEDED), user message "Urządzenie zostało dostarczone 3 dni temu.", reasoning low, max_output_tokens 4000, instructions = chat.md + '# Regulamin' + return policy
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: Keep-alive comments (': OPENROUTER PROCESSING') present: false. events=[]. First line of reply: "Zaktualizowana decyzja: Przekazane do specjalisty"
