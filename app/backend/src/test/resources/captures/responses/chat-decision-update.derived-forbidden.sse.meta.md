# Derived capture chat-decision-update.derived-forbidden.sse

- DERIVED, not a real response (plan section 4.1 rule). Source: `chat-decision-update.sse` (+ `chat-decision-update.headers.json` for status/headers).
- Exact change: only the `delta` text of the label deltas (` Pr`, `zek`, `azane`, ` do`, ` specjal`, `isty`) was replaced by ` Odrz`, `uc`, `one`, ``, ``, `` so that the first line reads `Zaktualizowana decyzja: Odrzucone` instead of `Zaktualizowana decyzja: Przekazane do specjalisty`. All other bytes unchanged (including `response.completed` and `data: [DONE]`).
- Purpose: reply whose first line announces a category (REJECTED) that is forbidden in chat for a damaged return (chat gate: Forbidden, regeneration with a corrective note; TAC-002-05).
