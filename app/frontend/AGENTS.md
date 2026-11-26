# Frontend (Angular 22.3, Vitest)

Scope: `app/frontend`. Architecture: `docs/ADR/003-frontend.md`; design: `docs/design-guidelines.md`.

## Commands (run from `app/frontend`; verified through the Maven build)
- `npm ci` - install exact dependencies (do not `npm install` new packages without agreement).
- `npm run test -- --watch=false` - Vitest unit tests once (7 files / 100 tests at last count).
- `npm run build` - production build into `dist/frontend/browser` (Maven copies it into the JAR).
- `npm start` - dev server (`ng serve`) with `/api` proxied to `http://localhost:8080` (`proxy.conf.json`); start the backend JAR first.
- Full integration: `cd ../backend && ./mvnw -B verify` builds and tests the frontend too.

## Rules
- All user-visible UI text (Polish) comes from `src/app/core/labels.ts`; no string literals in templates.
- No Angular Router: screens are state-driven (ADR-003). Do not add `@angular/router` navigation.
- Unit-test mocks of backend responses come only from `src/testing/recordings` (task T26), derived from real captures; do not hand-write response shapes.
- E2E (Playwright) runs against the real stack only: no `page.route`, no request mocking.
- Check every touched screen manually with playwright-cli against the running JAR (screenshot, no console errors, Polish text).
- Brand tokens and spacing follow `docs/design-guidelines.md`; brand wordmark is text "ElektroDemo" (ADR-005 D8).
