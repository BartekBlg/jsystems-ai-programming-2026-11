# Capture responses/timeout-note

- Date: 2026-11-26
- Endpoint: POST /api/v1/responses
- Request: minimal text request, java.net.http.HttpRequest.timeout(1 ms)
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1)
- Java exception: `java.net.http.HttpConnectTimeoutException: HTTP connect timed out`
- Note: no response body exists; the request may still be processed and billed on the provider side.
