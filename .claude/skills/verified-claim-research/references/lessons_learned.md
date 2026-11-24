# Lessons learned (from the session this skill was built from)

Each item: what happened → what to do instead. Read once before Phase 1.

## Process mistakes by the lead
1. **Report saved without asking where.** Output went to a temporary session folder.
   → Ask the user where to save the reports *before starting* (Phase 0), unless the prompt already names a folder.
2. **PDF tooling added only in iteration 2.** Iteration-1 agents read tables from lossy HTML summaries → many PARTIAL statuses and a whole re-verification round.
   → Run `setup_tools.sh` in Phase 0 and put the tool paths in the brief from the start.
3. **Wrong premise in a verifier task.** The lead asked an agent to read "per-language Polish numbers" from Global-MMLU charts; the paper only reports tier averages.
   → Phrase tasks as "check whether X reports per-group numbers; if yes extract…", never assume the shape of the data.
4. **Agents didn't know what was already established.** A2/A3/A4 each wrote "source of the article not identified" and re-fetched OneRuler, although A1 had confirmed it.
   → After each batch, append an "Established facts (do not re-research)" section to BRIEF.md.
5. **Overlapping scopes.** A2 and A6 both extracted MMLU-ProX; A1, A3, A5 all summarised OneRuler.
   → Give each scout an explicit "out of scope: covered by agent Ax" line.
6. **Final report written before the last iteration returned**, then patched repeatedly; agent count in the method section was wrong at first.
   → Keep a running iteration log file (`W/research/LOG.md`) and write the report only after the last verifier returns.
7. **Killing a helper process with `pkill -f` killed the shell running the next commands** (exit 144, a copy step silently skipped).
   → Start servers in background and kill by PID; never chain important commands after a `pkill`.
8. **Artifact preview attempts all failed** (file:// blocked, localhost denied, published URL needs sign-in) — 3 wasted calls.
   → Rely on `node --check` for the JS and a careful build; tell the user plainly that the rendered page was not previewed.

## Agent failure modes seen
9. **Devil's-advocate agent reported numbers that did not exist in the cited table** (CUS-QA). → Always send its numbers to a separate verifier before use.
10. **Column mis-assignment risk in flattened PDF tables** (21 unlabeled columns). → Render the page with ghostscript and read headers.
11. **Text vs table inconsistencies inside papers** (Swahili 90.8 vs 59.6; MAPS RU 39.1 vs 34.0; OneRuler Korean). → Report as inconsistency in the source; don't pick one silently.
12. **"Before/after" mis-attribution** (Bielik 82.98→77.41 were two different models). → Check that both numbers refer to the same subject.
13. **A subagent opened the user's browser pane by mistake.** → Brief says: use WebSearch/WebFetch/curl only, no browser tools.
14. **Rate limits** (Semantic Scholar 429, some WebSearch calls). → OpenAlex script for citations; spread searches; retry once later.

## What worked well (keep doing)
- One shared BRIEF.md with source rules and statuses.
- Narrow scopes with concrete names, IDs and multilingual search phrases.
- Launching verifiers as soon as one report arrives, not after the whole batch.
- Citation audit of newest IDs (0 hallucinated IDs found, but numbers corrected).
- Lead recomputing aggregates from raw CSV (Global PIQA: 177 models) — strongest single piece of evidence.
- Follow-up questions to an agent that already read the paper (SendMessage), instead of a new agent.
- One template + build script for bilingual artifacts (numbers can't drift between languages).
