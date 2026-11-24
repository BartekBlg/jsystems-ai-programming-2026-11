---
name: verified-claim-research
description: Multi-agent, source-verified research that fact-checks a claim (often from a news article, blog post or social media) back to its primary source, then hunts for newer primary data that confirms, nuances or contradicts it, and delivers a verified report (Markdown, optionally bilingual) plus interactive artifacts. Use this skill whenever the user asks to "zrób research", fact-check an article, find "the source of the report this article is based on", look for newer studies/benchmarks/statistics, verify numbers in a ranking, or wants research delegated to several (e.g. Haiku) subagents with a lead that only manages, validates and gives feedback — even if they don't say "fact-check". Especially relevant for AI/LLM benchmark claims, scientific or statistical claims in media, and any request that insists on primary sources only, multiple iterations, or a final report in Polish and English.
---

# Verified claim research

You are the **lead**. Your job is to manage the process, check the quality of what subagents
produce, validate it, give feedback, and delegate re-verification whenever something is doubtful.
You write the final report yourself. You do not do the bulk searching yourself.

Why this shape works: cheap subagents (Haiku) are fast and parallel but sometimes misread tables,
mis-assign columns, or hallucinate identifiers. A lead that spot-checks raw sources and sends
doubtful items to a *fresh* agent catches those errors. In the session this skill was distilled
from, verification rounds caught 5 real errors (a mis-attributed before/after comparison, a
text-vs-table mismatch inside a paper, a wrong premise in the lead's own brief, and fabricated-looking
numbers from a "devil's advocate" agent).

Before starting, skim `references/lessons_learned.md` — it lists the concrete mistakes made the
first time this workflow ran and what to do instead.

## Workflow at a glance

0. Ask where to save → set up tools → decompose into research questions
1. Write BRIEF.md (rules, tools, statuses, output format)
2. Iteration 1: 5–6 scout agents in parallel, non-overlapping scopes
3. Iterations 2…N: QA each report → verifiers, citation audit, citer screen, devil's advocate,
   lead spot-checks; update "Established facts" in the brief after every batch
4. After the last verifier returns: write the report(s) yourself
5. Build interactive artifacts from one template; publish; report to the user

## Phase 0 — Ask, then set up (5 minutes, saves hours)

1. **Ask the user where to save the final reports** if the prompt does not already say so — use
   `AskUserQuestion` (offer: the current project folder, a new folder named after the topic, or
   another path). Reports left in a temporary session folder get lost when the session is deleted.
   Also confirm the report languages and whether they want artifacts, if not stated.
2. Pick a scratch working dir `W` for intermediate files (the session scratchpad if one exists) and
   create `W/research/` with an empty `W/research/LOG.md` (running iteration log: iteration → agents →
   outcome → corrections; you'll need it for the method section).
3. Run `bash <skill>/scripts/setup_tools.sh W` — it creates a venv with `pypdf`, copies
   `pdf2txt.py` and `openalex_citers.py` into `W/tools/`, and reports whether `gs` (ghostscript) and
   `node` exist. Agents without a PDF extractor fall back to lossy HTML summaries — that was the
   #1 source of PARTIAL statuses before this was added.
4. Decompose the user's request into **research questions** (typically: the original source; newer
   data per application area; mechanism/explanation; what the user should do in practice).

## Phase 1 — Write the shared brief

Copy `references/brief_template.md` to `W/research/BRIEF.md` and fill in the context, the
questions, today's date and the tool paths. Every agent prompt starts with "read BRIEF.md and
follow it strictly". One brief instead of repeating rules in every prompt keeps agents consistent
and saves tokens. If you improve the rules mid-run (e.g. add a tool), append a dated section —
agents already running will notice and follow it.

## Phase 2 — Iteration 1: fan out (5–6 agents, parallel, background)

Launch all agents **in one message**, `model: "haiku"`, `subagent_type: "general-purpose"`,
`run_in_background: true`. Use `references/agent_prompts.md` → "Scout agent" template.
Give each agent a narrow scope and, crucially, **concrete places and phrases**: benchmark names,
arXiv IDs you suspect, leaderboards, model cards, and search phrases in every relevant language.
If you have a hypothesis about the source, state it as "Hypothesis to VERIFY (do not assume true)".
Add an explicit "Out of scope (covered by agent Ax)" line to each prompt — without it, three agents
re-summarised the same source paper the first time. When asking an agent to extract something,
phrase it as "check whether the source reports X; if yes, extract…" rather than assuming the data
exists in that shape.

Typical split for a claim about AI/benchmarks (adapt to the domain):
A1 original source + article-vs-paper table · A2 general benchmarks with the target language/group
· A3 retrieval/RAG · A4 reasoning/agents/mechanism · A5 replications + confounders (e.g. tokenization)
· A6 neighbouring groups (e.g. other Slavic languages) as fallback evidence.

Each agent saves a full report to `W/research/r<iter>_<id>_<topic>.md` and returns ≤900 words.

## Phase 3 — QA loop (iterations 2…N, at least 5 iterations total if the user asks for it)

As each report arrives (don't wait for all), run the checklist in `references/qa_checklist.md`.
After each batch, append confirmed facts to an **"Established facts (do not re-research)"** section
of BRIEF.md, so later agents build on them instead of rediscovering them, and add a line to LOG.md.
Then either accept, or spawn a **verification agent** ("Verifier" template) for: numbers marked
PARTIAL, values extracted from figures, column-mapping ambiguity, venue/date claims, conflicting
numbers between agents, and anything that would carry a headline in the final report.

Standard verification moves that paid off — schedule them, don't wait for inspiration:
- **Citation audit**: open every arXiv/DOI ID from the newest year(s); confirm title, authors,
  date, and the specific numbers. Newer IDs are where hallucinations hide.
- **Citer screen**: list *all* papers citing the original source
  (`python W/tools/openalex_citers.py <arXiv-ID or DOI>`; Semantic Scholar often returns 429) and
  check each relevant one for replications or critiques.
- **Figure reading**: when numbers exist only in charts, have the agent fetch the arXiv LaTeX source
  (`https://arxiv.org/e-print/<id>`) first, then render pages with ghostscript and read the PNG.
  Mark such values approximate (±).
- **Devil's advocate**: one agent whose only job is to find primary evidence *against* your
  emerging conclusion (including replications after the media hype). Verify its output too — in
  practice this role is the most prone to over-claiming.
- **Follow-up questions** to an agent that already read a paper go via `SendMessage` to that agent
  (cheaper than a new agent).

**Lead spot-checks** — do these yourself with `grep`/small Python on the agents' downloaded text,
CSV or rendered PNG (they leave files under `W/dl/`): the headline numbers, every number that
reverses the expected direction, and any aggregate you will chart. Recompute aggregates from raw
files when possible (e.g. mean gap over all models in a results CSV) — it often yields a stronger,
better-supported statement than any single agent's table.

Stop iterating when: required iteration count is reached, open items are only "nice to have",
and every headline claim is VERIFIED by an agent *and* spot-checked by you. Only after the
required number of iterations may you conclude "no such data exists" — and then say exactly where
you looked.

Keep a short iteration log (goal → outcome → corrections). It goes into the report's method section.

## Phase 4 — Synthesis (you write this yourself)

Start only after the last verifier has returned — writing earlier means patching the report
several times and getting counts in the method section wrong. Use LOG.md for the method section.
Follow `references/report_template.md`. Key rules:
- Every number in the report has a source + location (table/figure/section) + status.
- Separate "what the source says" from "how media framed it" (article-vs-source table).
- Name confounders explicitly; give practical takeaways for the user's actual role/stack.
- Write the report in each language the user asked for as separate `.md` files in **the folder the
  user chose in Phase 0** (e.g. `raport_PL.md`, `report_EN.md`), and copy `W/research/*.md` into a
  `research/` subfolder next to them so the evidence trail survives the session.

## Phase 5 — Interactive artifacts

Load `artifact-design` (and `dataviz` for charts) before writing HTML. For bilingual output, build
**one template + one build script** that emits both language versions from the same data, so the
numbers can't drift between languages. `examples/artifact_template.html` and
`examples/build_bilingual.py` are a working example (ranking bars with filter chips, a
"how much fits" calculator, a gap explorer with expandable source rows, a scatter of per-model
results with hover). Reuse the structure, replace the content. Syntax-check the inline JS with
`node --check` before publishing; publish both, then add cross-links and republish.
Previewing the rendered page is often impossible (file:// and localhost blocked, the published URL
needs sign-in) — don't burn calls on it; tell the user it wasn't previewed and invite feedback.
If you start any helper process, kill it by PID, not `pkill -f` (that once killed the shell and
silently skipped the next commands).

## Communicating with the user during the run

Agents run in the background; after each batch, tell the user in 3–6 lines what came back, what
you accepted, what you sent for verification and why. At the end, report: links, files, the
answer to each research question, corrections made, limitations, and anything you could not check
(say so plainly, e.g. if you could not preview a rendered artifact).
