# Derived capture chat-decision-update.derived-accepted.sse

- DERIVED, not a real response (plan section 4.1 rule). Source: `chat-decision-update.sse` (+ `chat-decision-update.headers.json` for status/headers).
- Exact change: only the `delta` text of the label deltas (` Pr`, `zek`, `azane`, ` do`, ` specjal`, `isty`) was replaced by ` Przyj`, `ęte`, ``, ``, ``, `` so that the first line reads `Zaktualizowana decyzja: Przyjęte` instead of `Zaktualizowana decyzja: Przekazane do specjalisty`. All other bytes unchanged (including `response.completed` and `data: [DONE]`).
- Purpose: reply whose first line announces ACCEPTED, which is forbidden in chat for a C1 complaint (liability period expired; complaint-policy.md section 2.3).
