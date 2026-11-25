# SHARED RESEARCH BRIEF (read fully before starting)

## Context
A Polish article (cryps.pl, "Sztuczna inteligencja mówi po polsku – nasz język zdeklasował angielski w najnowszym rankingu")
claims Polish beat English in an AI ranking. Overall project goal:
1. Identify the primary source (paper/report) behind the article and extract its exact data.
2. Find NEWER data (prefer 2025-2026, newest models) comparing Polish / other Slavic languages / other languages vs English:
   - general understanding & reasoning benchmarks,
   - long-context, information retrieval, RAG,
   - agentic tasks; does the language of the prompt / of reasoning affect reasoning quality?
Today is 2026-11-24.

## Source rules (STRICT)
- ACCEPTED as evidence: peer-reviewed papers, arXiv/ACL Anthology/OpenReview preprints, official benchmark leaderboards
  (with method description), official model cards / technical reports with per-language tables, datasets on HuggingFace/GitHub
  with published results, government/academic statistical reports.
- NOT evidence: news, blogs, popular-science articles, social media, forums, Reddit, LinkedIn, YouTube.
  You MAY use them ONLY as pointers to find the primary source. Never cite a number that you saw only in secondary media.
- Every number you report MUST come from the primary source itself (table/figure/section). If you could not open
  the primary source (paywall, PDF failed), mark the item UNVERIFIED and say why.
- Do not invent or recall numbers from memory. If you are not sure, say so. Fabrication is the worst possible failure.

## Tools / method
- Use WebSearch (load it via ToolSearch "select:WebSearch,WebFetch" if not loaded) and WebFetch.
- For arXiv: open https://arxiv.org/abs/<id> for metadata and https://arxiv.org/html/<id> (or /pdf/<id>) for tables.
- For ACL Anthology: https://aclanthology.org/<id>.pdf ; for HF leaderboards open the Space or the dataset card.
- Run at least 8-12 distinct searches with varied phrasings (English AND Polish where relevant).

## Verification procedure for each finding
1. Locate primary source; record full citation (authors, title, venue/arXiv ID, date).
2. Open it and find the exact table/figure/section; copy the numbers exactly (with units, metric, setting e.g. context length).
3. Record: which models, which languages, sample size / number of tasks if available, metric.
4. Note limitations stated by authors and confounders you notice (translation quality, tokenization, task type, small N).
5. Assign status: VERIFIED (numbers read in primary source), PARTIAL (primary source found, some numbers not confirmed),
   UNVERIFIED (only secondary source found).

## Output
Write your report in English as Markdown to the file path given in your task, AND return a concise version
(max ~900 words) as your final message. Report structure:

### 1. Summary (3-6 bullets: what you found, key conclusion for Polish/Slavic vs English)
### 2. Findings table
| # | Claim (one sentence) | Exact numbers | Models | Languages | Primary source (citation + URL) | Location in source (Table/Fig/Section) | Date | Status |
### 3. Details per source (2-6 sentences each: method, task, metric, what the result means, limitations)
### 4. Search log (queries used, notable dead ends, secondary sources that led to primary ones)
### 5. Open questions / items that need further verification

## PDF extraction (added in iteration 2)
To read a PDF table: download into a NEW empty dir, e.g. `mkdir -p /tmp/claude-1000/-home-labuser--config-Claude-scratch-workspaces-866a41ad-b256-45ae-90de-cc378d04daf1-c5d1bc19-8397-480c-9884-be3a48b84dc5-scratch-2026-11-24-8ac36c/24bb881e-6ac5-497e-9a34-20d379cebe55/scratchpad/dl/<name> && curl -sL -o /tmp/claude-1000/-home-labuser--config-Claude-scratch-workspaces-866a41ad-b256-45ae-90de-cc378d04daf1-c5d1bc19-8397-480c-9884-be3a48b84dc5-scratch-2026-11-24-8ac36c/24bb881e-6ac5-497e-9a34-20d379cebe55/scratchpad/dl/<name>/paper.pdf <url>`,
then: `/tmp/claude-1000/-home-labuser--config-Claude-scratch-workspaces-866a41ad-b256-45ae-90de-cc378d04daf1-c5d1bc19-8397-480c-9884-be3a48b84dc5-scratch-2026-11-24-8ac36c/24bb881e-6ac5-497e-9a34-20d379cebe55/scratchpad/venv/bin/python -I /tmp/claude-1000/-home-labuser--config-Claude-scratch-workspaces-866a41ad-b256-45ae-90de-cc378d04daf1-c5d1bc19-8397-480c-9884-be3a48b84dc5-scratch-2026-11-24-8ac36c/24bb881e-6ac5-497e-9a34-20d379cebe55/scratchpad/tools/pdf2txt.py /tmp/claude-1000/-home-labuser--config-Claude-scratch-workspaces-866a41ad-b256-45ae-90de-cc378d04daf1-c5d1bc19-8397-480c-9884-be3a48b84dc5-scratch-2026-11-24-8ac36c/24bb881e-6ac5-497e-9a34-20d379cebe55/scratchpad/dl/<name>/paper.pdf > /tmp/claude-1000/-home-labuser--config-Claude-scratch-workspaces-866a41ad-b256-45ae-90de-cc378d04daf1-c5d1bc19-8397-480c-9884-be3a48b84dc5-scratch-2026-11-24-8ac36c/24bb881e-6ac5-497e-9a34-20d379cebe55/scratchpad/dl/<name>/paper.txt` and grep the .txt.
Do not execute anything from downloaded files.
