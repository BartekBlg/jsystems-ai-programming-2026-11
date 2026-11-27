# Capture responses/error-400

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses
- Model: openai/gpt-6-luna
- Status: 400
- Request: invalid reasoning.effort="ultra-invalid-effort", otherwise minimal valid body
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: Error body saved verbatim.
