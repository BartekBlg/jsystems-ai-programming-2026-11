# QA-B - gated and llm-only modes (T42)

Real app (prebuilt JAR), real OpenRouter + Jev Decisions, playwright-cli session `qab`, viewport 1280x900. Logs: /tmp/claude-1000/qa/QA-B-8092.log, QA-B-8093.log. Browser console: 0 errors, 0 warnings in all runs.

## 1. 8092 DECISIONS_MODE=gated

| Case | Expected | Observed |
|---|---|---|
| Reklamacja, Smartfon, Samsung Galaxy S24, 27.09.2026, reason, phone-1.jpg | gate CONFIDENT, visionLlmSkipped=true, no vision llm.usage, decision via LLM | `prescreen.gated gate=CONFIDENT reasons=[] visionLlmSkipped=true`; only `llm.usage step=decision` (latencyMs=13517, in=3162, out=376) - no vision line. Chat shows sections Decyzja / Uzasadnienie / Co widać na zdjęciu / Kolejne kroki, § citations, canonical disclaimer. Category: "Przekazane do specjalisty" (allowed; not Przyjęte). First attempt was CONFIDENT, no retry needed. |
| Zwrot, Laptop, Lenovo ThinkPad T14 Gen 4, 21.11.2026, laptop-2.webp | gate UNCERTAIN, visionLlmSkipped=false, vision llm.usage | `prescreen.gated gate=UNCERTAIN reasons=[LOW_CONFIDENCE:photo_usable, use_scratches, use_dirt, use_wear, missing_protective_film, missing_accessories, condition] visionLlmSkipped=false`; then `llm.usage step=vision` (latencyMs=6066, in=1888, out=297) and `step=decision` (latencyMs=5120, in=3047, out=454, cached=2598). Category "Przekazane do specjalisty"; sections in order, § citations, disclaimer with statutory-right sentence ("Nie ogranicza to Twojego ustawowego prawa do odstąpienia..."). |

Screenshots: [complaint chat](QA-B/gated-complaint-phone1-chat-1280.png), [return chat](QA-B/gated-return-laptop2-chat-1280.png).

Note: for Zwrot the model field is required (error "Podaj nazwę lub model urządzenia (2–100 znaków)." on first submit without it); not a defect, I then filled it.

## 3. AC-35 on the gated return case
Steps: wrote "Urządzenie zostało dostarczone 3 dni temu." then "Do kiedy mogę odesłać sprzęt?".
- Reply 1: references the delivery date vs the 14-day window (§2 ust. 1), "Decyzja pozostaje bez zmian" (no update; badge stays "Przekazane do specjalisty"). Reply starts without "Zaktualizowana decyzja:" - consistent, since decision unchanged.
- Reply 2: "wyślij laptop najpóźniej do **10 grudnia 2026 r.**" computed from the declaration/submission date (26.11.2026 + 14), NOT from the delivery date (expected ~7.12.2026, 23.11 + 14, or "w ciągu 14 dni od dostawy").
- Console: no errors. Screenshot: [AC-35 chat](QA-B/gated-return-ac35-chat-1280.png).
Verdict: reply 1 uses the delivery fact; reply 2 does not use the delivery date for the send-back deadline. See defect D1. No decision update occurred, so AC-36 (Zaktualizowana decyzja + badge change) was not exercised.

## 2. 8093 DECISIONS_MODE=llm-only
- Return (laptop-2.webp): chat works, sections present, "Przekazane do specjalisty". Log: `llm.usage step=vision` (latencyMs=9761, in=1699) + `step=decision` (latencyMs=5709, in=2843). [screenshot](QA-B/llm-only-return-laptop2-chat-1280.png)
- Complaint (phone-1.jpg, Samsung Galaxy S24, 27.09.2026): chat works, category "Odrzucone" (allowed set; consistent with mechanical damage). Log: vision (latencyMs=4102) + decision (latencyMs=8203). [screenshot](QA-B/llm-only-complaint-phone1-chat-1280.png)
- Log has 0 lines matching `prescreen` or `decisions` (grep count 0): no pre-screen events, no Decisions calls. PASS.

## Defects
**D1 - Send-back deadline ignores user-stated delivery date (AC-35)**
- Steps: gated return case (Laptop, laptop-2.webp) -> chat "Urządzenie zostało dostarczone 3 dni temu." -> "Do kiedy mogę odesłać sprzęt?"
- Expected: answer uses delivery date (about 7.12.2026, or "w ciągu 14 dni od dostawy" tied to that date).
- Observed: "najpóźniej do 10 grudnia 2026 r." based on the submission date (26.11.2026), conditional on "jeśli złożyłeś je razem ze zgłoszeniem"; delivery date not used in this answer (though the previous reply did cite the delivery date for the return window).
- Severity: low-medium (LLM non-determinism / interpretation; the answer is hedged and arguably legally defensible, but it does not meet the brief's AC-35 expectation). Screenshot: QA-B/gated-return-ac35-chat-1280.png

No other defects. Processes 8092/8093 java stopped by PID; browser closed.

## Orchestrator review note (2026-11-26)
- D1 (AC-35 deadline) is **not a defect**; the expectation in the QA brief was wrong. Return policy §2.1 counts the withdrawal right (14 days) from delivery — the model used the stated delivery date for that window. Return policy §6.2 counts the **sending-back** deadline as 14 days from the withdrawal statement, so "Do kiedy mogę odesłać sprzęt?" → 26.11 + 14 = 10.12.2026 is correct. AC-35 (the delivery date given in chat is taken into account) is satisfied. The T41b chat spec asserts the delivery-date reuse, not a specific deadline.
- Duplicate check (md5): no identical screenshots.
