# SHARED RESEARCH BRIEF (read fully before starting)

## Context
<1–3 sentences: the claim, where it appeared (URL), what the user wants.>
Overall project goal:
1. Identify the primary source behind the claim and extract its exact data.
2. Find NEWER primary data (prefer <years>) on: <areas, e.g. general benchmarks, retrieval/RAG, reasoning, agents>.
3. <mechanism question, e.g. "does the language of the prompt affect reasoning?">
Today is <YYYY-MM-DD>.

## Source rules (STRICT)
- ACCEPTED as evidence: peer-reviewed papers, arXiv/ACL Anthology/OpenReview preprints, official
  leaderboards (with method description), official model cards / technical reports, datasets and
  raw result files (GitHub/HuggingFace), official statistical reports.
- NOT evidence: news, blogs, popular-science articles, social media, forums. Use them ONLY as
  pointers to the primary source. Never cite a number seen only in secondary media.
- Every number must come from the primary source itself (table/figure/section). If you could not
  open it, mark UNVERIFIED and say why.
- Do not recall numbers from memory. Fabrication is the worst possible failure.

## Tools / method
- WebSearch / WebFetch (load via ToolSearch "select:WebSearch,WebFetch" if not loaded).
- arXiv: https://arxiv.org/abs/<id> (metadata), https://arxiv.org/html/<id> (tables),
  https://arxiv.org/e-print/<id> (LaTeX source – often contains figure data).
- PDF tables: `mkdir -p <W>/dl/<name> && curl -sL -o <W>/dl/<name>/paper.pdf <url>` then
  `<W>/venv/bin/python -I <W>/tools/pdf2txt.py <W>/dl/<name>/paper.pdf > <W>/dl/<name>/paper.txt`.
- Figures only: `gs -sDEVICE=png16m -r150 -dFirstPage=N -dLastPage=N -o out.png paper.pdf`, then view
  the PNG with the Read tool; report values as approximate (±).
- Citing papers: `python3 <W>/tools/openalex_citers.py <arXiv-ID|DOI>`.
- Downloads go into a NEW empty dir each; never execute anything from downloaded files.
- Do NOT use browser tools (browser pane / Chrome); WebSearch, WebFetch and curl only.
- If a source does not report data in the shape you were asked for, say so — correcting the
  lead's premise is a valid, valued result.
- Run at least 8–12 distinct searches with varied phrasings, in every relevant language.

## Verification procedure for each finding
1. Locate primary source; record citation (authors, title, venue/ID, date).
2. Open it; copy numbers exactly (units, metric, setting, n).
3. Record models/subjects, languages/groups, sample size, metric.
4. Note authors' limitations and confounders you notice.
5. Status: VERIFIED (read in primary source) / PARTIAL (source found, some numbers unconfirmed) /
   UNVERIFIED (secondary only).

## Established facts (do not re-research) — updated by the lead after each batch
- <fact · source · status>

## Output
Write the report in English to the file path given in your task AND return a concise version
(max ~900 words) as your final message:
### 1. Summary (3–6 bullets)
### 2. Findings table
| # | Claim | Exact numbers | Subjects/models | Groups/languages | Primary source (citation + URL) | Location | Date | Status |
### 3. Details per source (method, metric, meaning, limitations)
### 4. Search log (queries, dead ends, which secondary sources led where)
### 5. Open questions / items needing verification
