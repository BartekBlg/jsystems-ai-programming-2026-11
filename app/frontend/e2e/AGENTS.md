# E2E (real stack) guidelines

## Run
- Build first: `cd app/backend && ./mvnw -B verify`, `cd app/frontend && npm ci`.
- Load the key and run in ONE shell command, never print it: `. ~/.hsdc.env && npx playwright test <file>.spec.ts`.
- Run spec files one at a time (memory). Both projects run: `mobile-360`, `desktop-1280`.
- Ports: 8080 = Playwright webServer (JAR, log in `test-results/real-stack/`); extra instances 8081-8090 via `support/real-stack.ts`
  (chat 8085, errors 8086, modes 8087). Stop JARs you start by PID; never `pkill -f`/`pgrep -f` patterns matching your own command line.
- The global teardown prints the "real-stack proof" (usage steps, prescreen events, no mock libs in the JAR, 0 interception hits).

## Realism rules (plan §4.1)
- No `page.route`, `route.fulfill`, `context.route`, service workers, HAR replay, fake clocks. Observe with `waitForResponse` only.
- Faulty configurations are real JAR instances (`invalidKey`, `visionTimeout1ms`, `decisionsNotFound`, mode helpers).
- Real example photos from `assets/example-images/` only. Test data is synthetic.

## Assertions
- Robust to LLM variance: category within the allowed set, heading order inside `article.message--agent`, `§\d`, disclaimer text,
  Polish UI texts from `fixtures.ts` `TEXT` (mirrors `labels.ts`). Never exact model prose. Never weaken an assertion to pass.
- Gate/source: assert on the gate observed in the per-case log slice (CONFIDENT => DECISIONS + no vision `llm.usage`; UNCERTAIN => LLM + vision).
- Expected HTTP errors log a browser console line; allow only that line via `test.use({ expectedConsoleErrors: [...] })`.

## Locator lessons
- Prefer `getByRole` with names; `{ exact: true }` for texts also present in intro/help text (e.g. "Dodaj zdjęcie urządzenia.").
- No `data-testid` in the app: use `article.message--agent`, `.message-content` (text without the "Agent" speaker label),
  `.decision-badge`, `.inline-new-case`, `.typing-indicator`.
- At 360 px the case summary is a collapsed accordion and its title renders twice: use `.first()` and `expandSummary(page)`.
- Material select: click the combobox on its arrow side (`position: { x: width - 20 }`), the label/required marker overlays the middle.
  The radio group is one Tab stop. Dates can be typed as DD.MM.YYYY.
