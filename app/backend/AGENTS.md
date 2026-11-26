# Backend (Spring Boot 4.2, Java 25, Maven wrapper)

Scope: `app/backend`. Architecture: `docs/ADR/001-backend-api.md`, `002-llm-integration.md`.

## Commands (run from `app/backend`; verified)
- `./mvnw test -Dskip.frontend=true` - backend unit tests only, skips Node/npm/Angular (fast loop).
- `./mvnw -B verify` - full build: installs Node/npm into `target/`, `npm ci`, Angular build, Vitest, backend unit + IT, JAR. Needs network on first run, ~2-3 min.
- `./mvnw verify -Dskip.frontend=true` - backend unit + IT without the frontend.
- JAR: `target/service-copilot-0.0.1-SNAPSHOT.jar` contains the SPA at `BOOT-INF/classes/static/index.html` (check: `jar tf <jar> | grep static/index.html`).
- Start: `OPENROUTER_API_KEY=… java -jar target/service-copilot-0.0.1-SNAPSHOT.jar` (export the key in your shell from an untracked source; never print or commit it). Ready in ~9 s.
- Health: `http://localhost:8080/actuator/health` -> `{"status":"UP",...}`. App: `http://localhost:8080/`.
- Run one Maven build at a time (memory).

## Package layout (`pl.elektrodemo.copilot.*`, ADR-001 section 3.2)
- `web` - controllers, `ApiExceptionHandler` (problem+json), SPA fallback, rate-limit filter.
- `cases` - assessment/decision services, policy rule engine.
- `chat` - chat service, decision gate, history validation.
- `prescreen` - Luna Decisions pre-screening (ADR-004).
- `llm` - all OpenRouter/SDK usage behind `LlmGateway`.
- `prompts` - `PromptRepository`, loads `classpath:prompts/*.md` at startup.
- `policy` - `PolicyRepository`, loads `classpath:policies/*.md`; fails fast if missing.
- `image` - magic-byte detection, downscaling.
- `security` - case signing, canonical JSON, rate limiter.
- `config` - typed properties, `ClockConfig`.
(Only some packages exist yet; add the rest per ADR.)

## Rules
- Services take an injected `Clock` (Europe/Warsaw); never call `LocalDate.now()` / `Instant.now()` directly.
- Tests tagged `eval` and `capture` are excluded from `verify` via `excluded.test.groups` (ADR-005 D7); they are not run or reported as skipped.
- Mocks of OpenRouter responses come only from real captures in `src/test/resources/captures` (task T04); never hand-invent a response shape.
- Controllers hold no business logic.
- SpaFallbackIT must pass both with and without `-Dskip.frontend=true` (test stub `src/test/resources/static/index.html` vs the real build).
