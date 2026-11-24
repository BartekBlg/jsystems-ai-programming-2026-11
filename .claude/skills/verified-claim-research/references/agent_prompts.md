# Agent prompt templates

All agents: `model: "haiku"`, `subagent_type: "general-purpose"`, `run_in_background: true`.
Always give absolute paths. Always name the output file. Keep final messages short (word cap).

## Scout agent (iteration 1)
```
You are research agent <ID> (iteration 1). First read the shared brief: <W>/research/BRIEF.md and follow it strictly.

TASK: <one-sentence scope>. Key question: <what number/comparison decides it>.

Out of scope (covered by other agents): <topics + agent IDs>. Already established (see BRIEF "Established facts"): do not re-research.

Steps / where to look (examples, not exhaustive):
- <named benchmark / dataset / paper + arXiv ID if suspected>
- <official leaderboard / model card / statistical office page>
- Hypothesis to VERIFY (do not assume it is true): <your guess>
Search phrases: "<phrase EN>", "<phrase in local language>", "<phrase with year>", ...

For each source extract: <exact fields, e.g. score for group X and Y, same model, metric, n>, compute <gap>.
Note when numbers are not directly comparable (different items, translations, protocols).

Save your full report to: <W>/research/r1_<id>_<topic>.md
Include a table: <columns you need for synthesis>.
```

## Verifier (iteration 2+)
```
You are verification agent V<n> (iteration <k>). Read the shared brief (incl. the tool sections): <W>/research/BRIEF.md
Then read the previous report(s): <files>.

TASK A — VERIFY (re-open each primary source, read the actual table/figure, write CONFIRMED / CORRECTED old→new / NOT FOUND):
1. <claim + numbers + claimed location>
...
TASK B — FILL GAPS (prefer newest data): <specific missing comparison + where to look>

Save to <W>/research/r<k>_v<n>_<topic>.md. Final message max 600–700 words: verdict per item + table.
```

## Citation audit
```
... PART A — For EACH ID: open https://arxiv.org/abs/<id> (or export.arxiv.org/api/query?id_list=<id>);
record: exists? exact title, first author, date, matches claimed content? Then verify the specific numbers in the paper.
<list of IDs with the claim attached to each>
PART B — <citer screen or replication search>
Final: audit table (ID | exists | real title | claim verified? | corrections).
```

## Citer screen
```
... List ALL papers citing <source> via `python3 <W>/tools/openalex_citers.py <id>` (also try Semantic Scholar once).
Table of all citers: title | year | ID | relevant? (Y/N + 5-word reason).
For each relevant one: does it replicate/extend/critique the source for <target group>? Exact numbers + location.
Verdict: does any newer work confirm or contradict "<claim>"?
```

## Devil's advocate
```
... Current conclusion of the project (to be challenged): "<conclusion>".
Actively search for PRIMARY evidence that CONTRADICTS or NUANCES it, especially: replications after the media coverage,
<specific counter-hypotheses>, newer benchmarks. Report every candidate (primary or not, status).
Be explicit if nothing contradicts the conclusion after ≥12 varied searches (list them).
```
Remember to verify the devil's advocate's numbers with a separate agent before using them.

## Follow-up to an existing agent
Use `SendMessage` to the agent id with a precise question (definitions, sample sizes, protocol details,
what a split means). Ask it to append the answer to its own report under "QA follow-up".
