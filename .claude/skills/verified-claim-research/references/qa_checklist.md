# Lead QA checklist (run on every agent report)

**Sourcing**
- [ ] Every number has a primary source + location (table/figure/section). Media-only numbers → UNVERIFIED, excluded.
- [ ] Numbers came from the table/PDF/CSV, not a WebFetch summary of HTML. If summary-only → send to verifier.
- [ ] Values read from figures are marked approximate.
- [ ] IDs from the latest 12–18 months → queue for citation audit.

**Comparability**
- [ ] Same model, same items (parallel/translated) for the two groups? If native datasets per group → label "not parallel".
- [ ] Same protocol (shots, prompt, thinking mode, judge)? If unknown → flag.
- [ ] Sample size known? Small n (e.g. 30 problems, 100 items) → compute rough SE (≈ sqrt(p(1-p)/n)) and call ±few-point gaps noise.
- [ ] Column/row mapping in extracted tables unambiguous? If not → render the page and read headers.

**Consistency**
- [ ] Do two agents disagree on the same number? → verifier.
- [ ] Does the paper disagree with itself (text vs table, two tables)? → report as an inconsistency in the source, don't pick silently.
- [ ] Is a "before/after" or "A vs B" actually the same subject? (classic mis-attribution)
- [ ] Is the result the *opposite* of the general pattern? → lead spot-check before believing.

**Premises**
- [ ] Did the agent correct an assumption in *your* brief? Accept good corrections and log them.

**Lead spot-check (do yourself)**
- grep headline numbers in `W/dl/**/paper.txt`; view rendered PNGs for figure values; recompute aggregates from raw CSV.
- Log each spot-check in the method section ("lead verified in raw PDF/CSV: …").
