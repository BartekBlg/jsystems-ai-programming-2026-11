# Capture responses/assessment-complaint-phone-1

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses
- Model: openai/gpt-6-luna
- Status: 200
- Request: COMPLAINT, category Smartfon, image phone-1.jpg (downscaled JPEG q0.85, fit 1536), reasoning low, max_output_tokens 4000, store false, strict json_schema, no model name/reason
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: Result: photoSufficient=true, confidence=MEDIUM, defectVisible=true, probableCause=MECHANICAL_DAMAGE
