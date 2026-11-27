# Derived capture chat-plain.derived-truncated.sse

- DERIVED, not a real response (plan section 4.1 rule). Source: `chat-plain.sse` (+ `chat-plain.headers.json` for status/headers).
- Exact change: all lines from the `data: {"type":"response.completed",...}` event onward were removed (that event, its blank line and the final `data: [DONE]`). Nothing else changed.
- Purpose: stream that ends after the last `response.output_item.done` without a terminal event (T20 gateway: must report LLM_FAILED).
