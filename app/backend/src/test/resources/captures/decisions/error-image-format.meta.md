# Capture decisions/error-image-format

- Date: 2026-11-26
- Endpoint: POST /api/alpha/decisions
- Model: typesafe/jev-1.14
- Status: 400
- Request: valid body, original laptop-2.webp bytes as data:image/webp (Jev accepts PNG/JPEG only)
- HTTP client: java.net.http.HttpClient (JDK 25.0.5.1), Jackson-built JSON, no SDK
- Credentials: none stored (Authorization and Set-Cookie headers removed)
- Notes: Response saved verbatim. Echoed model: -; provider: -; latency: 90 ms; image: non-JPEG data URL (data:image/webp;base), intentionally sent as is.
