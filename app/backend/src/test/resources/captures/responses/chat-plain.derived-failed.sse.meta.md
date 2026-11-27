# Derived capture chat-plain.derived-failed.sse

- DERIVED, not a real response (plan section 4.1 rule). Source: `chat-plain.sse` (+ `chat-plain.headers.json` for status/headers).
- Exact change: in the single `response.completed` event line, `"type":"response.completed"` was replaced by `"type":"response.failed"` and the nested `"status":"completed"` by `"status":"failed"`. All other bytes unchanged (deltas and `data: [DONE]` retained).
- Purpose: terminal failure event (T20 gateway: must report LLM_FAILED after deltas were already delivered).
