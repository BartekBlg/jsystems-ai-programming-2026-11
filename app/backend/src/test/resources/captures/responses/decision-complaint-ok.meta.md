# Capture responses/decision-complaint-ok

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses
- Model: openai/gpt-6-luna
- Status: 200
- Request: COMPLAINT decision from responses/assessment-complaint-ok; model name "Samsung Galaxy S24", purchase 2026-05-26 (rule C4, days 184, allowed [ESCALATED]), reasoning medium, max_output_tokens 8000, instructions = prompt + '# Regulamin' + policy, store false, strict json_schema decision_draft
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: Result: category=ESCALATED; message starts with greeting then '## Decyzja': true; has Uzasadnienie/Co widać/Kolejne kroki headings: true. Instructions honored evidence: heading format of prompt followed = see message flag above.
