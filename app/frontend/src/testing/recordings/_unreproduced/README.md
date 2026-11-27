# Not reproduced: 504 LLM_TIMEOUT

Two attempts to provoke `504 LLM_TIMEOUT` per ADR-005 D10 (valid key + vision timeout overridden to ~1 ms, via
`--llm.timeouts.vision=1ms`, and via `--llm.timeouts.vision=PT0.001S` + `LLM_TIMEOUTS_VISION=1ms`) both returned
`200` from the real backend: the vision call ran for about 10.7 s and succeeded (see `llm.usage latencyMs` in the
instance log). The files here are the verbatim recordings of those two attempts. They are NOT 504 mocks.
Do not use them as `problem-504-llm-timeout`. Follow-up needed in the backend (the SDK request timeout is not enforced).

Update (2026-11-26, commit b3e58d2): reproduced after the backend fix for the sub-millisecond timeout. A third run with
`--llm.timeouts.vision=1ms` returned `504 LLM_TIMEOUT` (vision step, `LlmTimeoutException` after about 1.5 s). The verbatim
recording is `../problem-504-llm-timeout.json` (+ `.body.raw`, `.meta.md`). The two attempts above are kept as history.
