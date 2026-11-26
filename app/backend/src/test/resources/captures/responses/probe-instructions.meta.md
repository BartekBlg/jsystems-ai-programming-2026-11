# Capture responses/probe-instructions

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses
- Model: openai/gpt-6-luna
- Status: 200
- Request: instructions="Zacznij odpowiedź od słowa KONTROLA...", user asks how are you, reasoning low, store false
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: INSTRUCTIONS_HONORED=true. Output text starts with: "KONTROLA, mam się dobrze, dziękuję!"
