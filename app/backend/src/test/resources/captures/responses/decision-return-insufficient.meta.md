# Capture responses/decision-return-insufficient

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses
- Model: openai/gpt-6-luna
- Status: 200
- Request: RETURN decision from responses/assessment-insufficient; model name "Lenovo ThinkPad T14 Gen 4", purchase 2026-11-21 (rule R2, days 5, allowed [MORE_INFO_NEEDED]), reasoning medium, max_output_tokens 8000, instructions = prompt + '# Regulamin' + policy, store false, strict json_schema decision_draft
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: Result: category=MORE_INFO_NEEDED; message starts with greeting then '## Decyzja': true; has Uzasadnienie/Co widać/Kolejne kroki headings: true. Instructions honored evidence: heading format of prompt followed = see message flag above.
