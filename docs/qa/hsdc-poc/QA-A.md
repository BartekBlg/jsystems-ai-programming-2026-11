# QA-A: shadow mode flows, screenshots, brand (port 8091)

Instance: default (DECISIONS_MODE unset = shadow), JAR /tmp/claude-1000/qa/service-copilot-qa.jar. Log: /tmp/claude-1000/qa/QA-A-8091.log. Browser: playwright-cli session `qaa`. Console errors: 0 (checked at 1280 and 360). Java process stopped by PID; browser closed.
Screenshots: `QA-A/` (24 files; no duplicates except as noted).

## Steps (expected vs observed)
1. 1280 empty form: OK (`form-1280-empty.png`). Empty submit: errors for Rodzaj, Kategoria, Nazwa, Data, Zdjecie visible (`form-1280-errors.png`; Powod optional); focus moved to first invalid field (Zwrot radio). OK.
2. Return (Zwrot, Laptop, Lenovo ThinkPad T14 Gen 4, 21.11.2026 = 5 days ago, laptop-2.webp): form filled (`form-1280-filled.png`). Progress both stages seen: stage 1 spinner + "Analizujemy zdjecie" (`progress-1280-stage1.png`, captured on the complaint case), stage 2 check + "Przygotowujemy decyzje" (`progress-1280-stage2.png`). Chat: sections Decyzja, Uzasadnienie, Co widac na zdjeciu, Kolejne kroki in order, § citations, canonical disclaimer, and the statutory sentence ("Nie ogranicza to Twojego ustawowego prawa do odstapienia od umowy"). Badge "Przekazane do specjalisty" (allowed), summary panel with thumbnail (`chat-1280-first-message.png`). Thumbnail dialog opens/closes with Escape (`thumbnail-dialog-1280.png`). OK.
3. Chat: Shift+Enter inserts newline (textarea value "Linia pierwsza\nLinia druga"); Enter sends. Typing indicator (three dots) captured while waiting (`chat-1280-streaming-typing.png`); streamed reply with §6 refs (`chat-1280-after-reply.png`). `chat-1280-streaming.png` shows reply already complete (too fast), kept for reference. OK.
4. Reload: same conversation, summary and badge present (AC-38 OK). `chat-1280-after-reload.png` is pixel-identical to first-message because the chat scrolls to the top after reload (see observation O1). "Nowe zgloszenie" dialog "Rozpocznac nowe zgloszenie? Biezaca rozmowa zostanie usunieta." with Anuluj / Rozpocznij nowe (`new-case-dialog-1280.png`); confirm gives an empty form (all inputs empty, `form-1280-after-new-case.png`). OK.
5. Complaint (Reklamacja, Smartfon, Samsung Galaxy S24, 27.09.2026 = 60 days, "Ekran pekl po upadku z biurka.", phone-1.jpg): chat works, category "Odrzucone" (not Przyjete), reasoning cites mechanical damage §4 ust. 1 pkt 1 (AC-28 OK). `form-1280-complaint-filled.png`, `chat-1280-complaint.png`.
6. 360 px: form, errors (full-page and viewport), filled, date picker, progress, chat collapsed summary, expanded summary, chat return: `form-360-*.png`, `datepicker-360.png`, `progress-360.png`, `chat-360-collapsed-summary.png`, `chat-360-expanded-summary.png`, `chat-360-return.png`. `document.documentElement.scrollWidth` = 360 on every screen. OK.
7. Brand (computed styles + screenshots):
   - Cream background rgb(255,249,235) = #fff9eb: OK
   - Body font Atkinson Hyperlegible, ink #101820: OK
   - Headings (h2 etc.) Bangers: OK (h1 element not used; the heading is rendered by a different element, Bangers visible)
   - Buttons: Bangers 20px, 3px ink border, radius 7px, shadow 4px 4px 0 0 (no blur), red #d30f10 with white text: OK
   - Panels/inputs: thick ink borders, hard offset shadows: OK (screenshots)
   - Yellow demo note rgb(255,212,71) = #ffd447, 3px bottom border: OK
   - Wordmark top-left: text wordmark "ElektroDemo" in red Bangers, 163x32 px (guideline ~158 px, text-based not SVG): OK/near
   - Header: sticky, 67 px tall with 3px bottom border (guideline 76 px): minor deviation (D3)
8. App log (ids only): per case `prescreen.shadow` present (RETURN gate=UNCERTAIN x2, COMPLAINT gate=CONFIDENT); `llm.usage` for vision (3), decision (3), chat (2). Vision model latency 5-8 s.

## Observations / defects
- O1 (low): after reload the chat view is at the top (first message) instead of the latest message; user must scroll to see the latest exchange.
- D1 (low, visual): at 360 px the error text "Podaj nazwe lub model urzadzenia (2-100 znakow)." wraps/gets clipped in the one-line subscript area ("...(2-100" visible in `form-360-errors.png`); error texts of neighbouring fields are tightly packed and the floating label of the next field touches the error line (`form-360-filled.png`). Expected: full error text visible. Severity: low.
- D2 (low, possibly tool artifact): at 360 px, playwright's `click mat-select` fails repeatedly because the sticky header intercepts pointer events when the select is scrolled under the header (no scroll-padding-top / scroll-margin for the sticky header). A human can scroll manually, but programmatic scrollIntoView (e.g. focus-to-invalid-field) may hide the field under the header. Severity: low.
- D3 (info): header height 67 px vs design guideline 76 px.
- Full-page screenshots draw the sticky header mid-page (capture artifact, not an app defect).

## Orchestrator review note (2026-11-26)
- Duplicate check (md5): only `chat-1280-after-reload.png` = `chat-1280-first-message.png`, explained by O1 (after reload the list is at the top) — kept as evidence of O1.
- Triage: O1 (PRD §9.3 scroll to latest on load) and D1 (AC-04/AC-45 error text clipped at 360 px) are defects; D2 (scroll padding under the sticky header) and D3 (header 67 px vs 76 px guideline) are cheap alignments. All four go to T42-fix-1.
