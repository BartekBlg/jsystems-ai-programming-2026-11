# Does AI "speak best in Polish"? Source check and 2025–2026 evidence

*Multi-agent research report · as of 24 November 2026*

## TL;DR

1. **The cryps.pl article is based on the OneRuler benchmark** (Kim, Russell, Karpińska, Iyyer; UMD / Microsoft / UMass Amherst; arXiv [2503.01996](https://arxiv.org/abs/2503.01996), COLM 2025). The article's numbers match the paper: **Polish 88.0% (1st of 26), English 83.9% (6th)**. The result covers only a synthetic *needle-in-a-haystack* (NIAH) task at 64K–128K tokens, run on models from late 2024 and early 2025.
2. **The article over-reads the result.** The paper does not study the "best language for prompting", and co-author M. Karpińska told PAP that this was not studied. Each language also uses a **different book** as the haystack (PL: *Noce i dnie*, EN: *Little Women*). The context budget is counted in tokens, and Polish needs about 1.7–2.3× more tokens per word than English, so a Polish haystack holds less text.
3. **No one has replicated it.** We screened 39 papers citing OneRuler (2025–2026). None reports long-context PL-vs-EN numbers that confirm or refute the result.
4. **Newer data (2025–2026) on other tasks shows English ahead of or level with Polish:**
   - knowledge and commonsense on **identical questions** (Global PIQA, 177 models): PL averages **−7.6 pts** vs EN. Frontier 2026 models (GPT-5.4, Gemini 3.1 Pro, Claude Sonnet 4.6) are **at parity** (±2 pts, within noise);
   - agents and tools: EU-24+ multi-turn dialogue games (2026) PL 59.2 vs EN 61.9; function calling (MASSIVE-Agents) PL **29.5 vs EN 57.4** on exact argument matching;
   - retrieval and RAG: on parallel passages, modern embedders lose about 1 nDCG point in Polish. LLMs prefer documents **in the query language**, not English ones.
5. **The exception: knowledge about Poland.** Two 2026 papers (Masked Advantage, PoVisLE) show that for questions about Polish culture and realities, **prompting in Polish beats English** in frontier models (GPT-5.4, Claude Sonnet 4.6 and 5, Gemini 3 Flash). On culture-agnostic questions English still leads.
6. **Does language affect agent reasoning? Yes, moderately.** Forcing the reasoning trace into a non-English language costs about 7 pts on average. On GAIA (MAPS), agents score 47.4% in English vs 35.8% averaged over other languages. The gap shrinks as model size and quality grow, and for 2026 frontier models it is often within measurement error.

---

## 1. The source and what the paper actually says

| Item | Value (verified in the PDF) |
|---|---|
| Paper | *One ruler to measure them all: Benchmarking multilingual long-context language models*, arXiv 2503.01996 v3 (30 Sep 2025), header "Published as a conference paper at COLM 2025" |
| Languages | 26 |
| Models | Gemini 1.5 Flash, o3-mini-high, Qwen2.5-72B/7B, Llama 3.3-70B, Llama 3.1-8B, DeepSeek-R1 (EN only) |
| Tasks | 5 NIAH variants (incl. a "none" answer option) + 2 aggregation tasks (CWE) |
| Context lengths | 8K, 32K, 64K, 128K; 50 examples per configuration |
| Headline | PL 88.0% (1st), EN 83.9% (6th), ZH 62.1% (23rd); averaged over 64K+128K and all models |
| Most striking case | o3-mini-high at 128K: EN 67%, PL 92%, UK 89% |
| Information-controlled analysis (App. D) | English "typically 8th"; Slavic languages (RU, UK, PL) rank high |
| Authors' explanations | pretraining data availability, script, language family, tokenizer; "remains unclear" why Chinese does poorly, among other things |

**26-language ranking** (PL, EN and ZH exact from the text; the rest read from the Fig. 4b chart, ±0.5 pts):
pl 88.0 · ru ~86.0 · fr ~85.7 · it ~84.9 · es ~84.5 · **en 83.9** · uk ~83.0 · sv ~82.6 · pt ~82.0 · de ~80.4 · no ~80.0 · nl ~77.6 · hu ~77.3 · da ~77.2 · ja ~75.7 · cs ~73.5 · vi ~73.3 · fi ~73.2 · fa ~71.7 · sr ~71.3 · hi ~65.9 · ko ~65.2 · zh 62.1 · sw ~60.5 · ta ~54.3 · st ~44.0

This is **not a pan-Slavic effect**: Czech ranks 16th and Serbian 20th.

### Article vs paper

| Article claim | Paper | Verdict |
|---|---|---|
| Polish 1st of 26, ~88% | 88.0%, 64K/128K | ✅ matches |
| English 6th, "under 84%" | 83.9% | ✅ matches |
| Chinese 62.1% | 62.1%, 23rd | ✅ matches |
| "Polish best for prompting" (media framing) | not studied; co-author denies it | ❌ over-interpretation |
| Polish grammar reduces ambiguity | not in the paper | ❌ not in source |
| "26 other languages" | 26 in total | ⚠️ error |
| Models: Gemini 1.5 Flash, Llama 3, Qwen2.5 | omits o3-mini and DeepSeek-R1 | ⚠️ incomplete |

### Confounders

- **Different haystack texts.** Each language uses a different book (Appendix A). The Polish source book has 452,847 tokens; the English one has 233,668.
- **Budget measured in tokens, not content.** Polish needs about 1.9× the tokens of English with cl100k_base (Petrov et al. 2023, App. C). Current tokenizers use 2.2–2.8 tokens per word for Polish vs about 1.2 for English (Bielik v3, arXiv 2604.10799, Tab. 1). At the same 128K tokens, a Polish haystack holds fewer words.
- **Instruction language** shifts accuracy by up to about 20 pts (Fig. 6a). A Polish context at 128K scores 86% with Polish instructions and 74% with English ones.
- The paper's text and its Fig. 6a heatmap **disagree** on the Korean values.
- Small sample: 50 examples per configuration. No formal significance tests.

---

## 2. Newer data 2025–2026: Polish and other Slavic languages vs English

### 2.1 Knowledge and commonsense, identical questions in both languages

| Benchmark | Model (year) | EN | PL | PL−EN | Status |
|---|---|---|---|---|---|
| Global PIQA, parallel split, 103 items ([2510.24081](https://arxiv.org/abs/2510.24081), results CSV) | Gemini 3.1 Pro (2026) | 100.0 | 100.0 | 0 | ✅ |
| | GPT-5.4 (2026) | 96.1 | 98.1 | +1.9 | ✅ |
| | Claude Sonnet 4.6 (2026) | 95.1 | 97.1 | +1.9 | ✅ |
| | Gemma 4 31B-it (2026) | 90.3 | 89.3 | −1.0 | ✅ |
| | Qwen3-32B (2025) | 84.5 | 78.6 | −5.8 | ✅ |
| | Llama 3.1 70B-Instruct | 70.9 | 70.9 | 0 | ✅ |
| | **177 models (mean)** | | | **−7.6** | ✅ (PL>EN: 29, PL<EN: 138) |
| | 97 instruct models, mean | 56.9 | 51.2 | −5.7 | ✅ |
| EU21 ([2410.08928](https://arxiv.org/abs/2410.08928), Tab. 1) | Llama-3.1-70B-Instruct | 0.79 | 0.70 | −9 (GSM8K −18) | ✅ |
| PL/EN medical exams ([2412.00559](https://arxiv.org/abs/2412.00559), Tab. 4) | GPT-4o | 88.69 | 89.96 | +1.3 | ✅ |
| | Llama-3.1-70B | 82.60 | 80.94 | −1.7 | ✅ |
| | Qwen2.5-7B | 67.73 | 51.92 | −15.8 | ✅ |
| | Bielik-11B-v2.2 | 56.98 | 62.36 | +5.4 | ✅ |
| Gamayun ([2512.21580](https://arxiv.org/abs/2512.21580)) | small 1–2B models | — | — | −2 to −18 | ⚠️ EN/PL protocol not confirmed |

In Global PIQA, Polish is the **best-performing Slavic language** (instruct mean: PL 51.2; CS 48.8; UK 48.6; SK 47.3; BG 47.0; HR 46.0; SR 44.9; SL 43.7). With n=103 the standard error is about 3–5 pts, so ±2-pt differences for a single model are noise.

**Official Anthropic data (2026, Claude Fable 5.2 / Opus 5.5 / Sonnet 5.5 / Haiku 5.5)**: zero-shot Global-MMLU as a % of the English score. All 14 listed languages reach 87–99.8% of English. **Neither Polish nor any other Slavic language is listed.** The page notes that differences below about 1 pt are noise.

**Other Slavic languages, MMLU-ProX** ([2503.10497](https://arxiv.org/abs/2503.10497), identical 11,829 questions; no Polish): Czech averages about −2.5 pts. Reasoning models are close to parity (Qwen3-235B-Think CS 80.5 vs EN 80.7; o4-mini 73.5 vs 73.7).

### 2.2 Long context

- No OneRuler replication that includes Polish (39 citing papers screened via OpenAlex and Semantic Scholar).
- Other long-context benchmarks (MLNeedle, mLongRR, MLRBench, MGAL 2026) **do not include Polish**.
- COMPASS ([2604.20720](https://arxiv.org/abs/2604.20720)) runs OneRuler per language, but its results are only in a heatmap (not extracted).

### 2.3 Retrieval and RAG

| Finding | Numbers | Source | Status |
|---|---|---|---|
| Embedders on **identical** passages (Belebele retrieval) | Qwen3-Embedding-8B: EN 99.37 / PL 98.52 / CS 98.30 / RU 98.62 / UK 98.15 nDCG@10 | MTEB results repo | ✅ |
| BM25 weaker in Polish (translated BEIR) | NQ 32.6→20.1; HotpotQA 60.2→49.2; MSMARCO 47.7→41.9 | BEIR-PL [2305.19840](https://arxiv.org/abs/2305.19840), Tab. 3 | ✅ |
| Polish-specific embedders beat multilingual ones on Polish | stella-pl-retrieval-8k 61.59 vs Qwen3-Embedding-8B 59.21 | PL-MTEB [2405.10138](https://arxiv.org/abs/2405.10138) (ACL 2026 Findings) | ✅ |
| Retriever and generator prefer **query-language** documents | aya-expanse-8b, ko query: ko docs 49.66 vs en docs 38.08 | Park & Lee [2502.11175](https://arxiv.org/abs/2502.11175) (ACL 2025 Findings) | ✅ |
| 68% of top-10 retrieved docs in the query language; generation uses native docs 42% vs foreign 29.6% | | Faux Polyglot [2407.05502](https://arxiv.org/abs/2407.05502) (NAACL 2025) | ✅ |
| Same English evidence, non-English question = lower score | GPT-4o: EN 62.4; DE 55.9; ZH 54.7 | XRAG [2505.10089](https://arxiv.org/abs/2505.10089) | ✅ |
| mRAG citation accuracy lower for Russian | Llama-3.3-70B: EN 85.9 vs RU 74.5 | "Linguistic Nepotism" [2509.13930](https://arxiv.org/abs/2509.13930) | ✅ |

**No matched PL-vs-EN RAG study exists** (same corpus and pipeline run in both languages). This is a research gap.

### 2.4 Reasoning

| Finding | Numbers | Source | Status |
|---|---|---|---|
| MCLM, o3-mini: Polish higher on AIME, lower on M-MO | AIME PL 43.3 vs EN 36.7; M-IMO 37.0 vs 29.6; **M-MO 40 vs 80** | [2502.17407](https://arxiv.org/abs/2502.17407), Tab. 26 | ✅ (n=30, so 1 problem = 3.3 pts) |
| MCLM, GPT-4o-mini | AIME PL 13.3 vs EN 33.3; MATH100 72.7 vs 75.8 | Tab. 25 | ✅ |
| Forcing the reasoning trace into the user's language | about −7 pts on average; unforced, only about 52% of traces stay in the target language | [2505.22888](https://arxiv.org/abs/2505.22888), Tab. 1 | ✅ |
| Budget forcing mostly helps English | AIME EN +20 pts vs other languages +1.94 | [2502.17407](https://arxiv.org/abs/2502.17407) | ✅ |
| PolyMath (no PL), RU vs EN | Qwen3-235B-Think 38.4 vs 40.0; DeepSeek-R1 37.6 vs 35.2 | [2504.18428](https://arxiv.org/abs/2504.18428), Tab. 5a | ✅ |
| Models route through English in hidden layers | 50–70% of words routed via EN (2024 models) | Schut et al. [2502.15603](https://arxiv.org/abs/2502.15603) | ✅ |
| Shared multilingual features (Claude 3.5 Haiku) | about 74% of features active in all 3 languages | Anthropic, *Biology of an LLM* (2025) | ✅ |

### 2.5 Agents and tool calling

| Finding | Numbers | Source | Status |
|---|---|---|---|
| MAPS: GAIA in English vs other-language mean | 47.4% vs 35.8%; RU 39.6 vs EN 47.5 (3 runs) | [2505.15935](https://arxiv.org/abs/2505.15935) v3 | ✅ (RU 39.1 vs 34.0 disagree between Tab. 16 and 17) |
| MAPS: SWE-bench and MATH almost unaffected | SWE: RU 0.33 vs EN 0.31; MATH 0.507 vs 0.514 | Tab. 21–22 | ✅ |
| MAPS: weaker safety outside English | attack success rate: RU 0.641 vs EN 0.534 | Tab. 23 | ✅ |
| MASSIVE-Agents, BFCL AST (full call incl. arguments) | Nova Premier EN 57.4 / **PL 29.5**; Claude 3.5 Sonnet 53.7 / 19.0 | ACL 2025.findings-emnlp.1099, Tab. 5 | ✅ |
| MASSIVE-Agents, function selection only | Nova Premier EN 91.6 / PL 89.5 | Tab. 6 | ✅ |
| Multi-turn dialogue games (2026) | GPT-5.4 EN 91.8 / PL 89.5; Claude Opus 4.8 84.4 / 82.3; 9-model mean 61.9 / 59.2; Gemma-4 50.6 / **59.7** | "Language Equality has a Price" [2608.01395](https://arxiv.org/abs/2608.01395), Tab. 4 | ✅ |
| PL vs EN clinical reasoning (2026) | 4 of 6 models significantly better in English; GPT-5 21.5 vs 22.8 | J. Clin. Med. 15(11):4082 | ⚠️ abstract and table |

### 2.6 Poland-specific knowledge and multimodal models (2026)

| Finding | Numbers | Source | Status |
|---|---|---|---|
| Culture-agnostic questions: English better (GlobalGap < 0). Poland-specific questions: Polish better (LocalGap > 0). Scale: IRT ability in logits | GPT-5.4: −0.34 / **+0.40**; Claude Sonnet 4.6: −0.40 / **+0.80**; Gemini 3 Flash: −0.32 / **+2.26**; Llama-3.1-8B: −0.89 / −0.36; Qwen3-14B: −0.74 / −1.02 | "The Masked Advantage" [2606.07422](https://arxiv.org/abs/2606.07422), Fig. 2 (read from heatmap, ±0.02) | ✅ (checked on the figure by the lead) |
| Authors' conclusion | a weaker local-language score usually reflects weaker proficiency in that language, not missing cultural knowledge | same, abstract | ✅ |
| Polish VLM benchmark: PL prompt vs EN prompt (same image) | Qwen3.5-397B (thinking) PL 75.16 / EN 68.12; Claude Sonnet 5 69.94 / 66.94; **GPT-5.4 68.35 / 70.40**; Qwen3.5-9B 38.75 / 47.04 | PoVisLE [2608.07763](https://arxiv.org/abs/2608.07763), Tab. 14 | ✅ (n=465, single run) |

Takeaway: **strong models answer questions about Polish realities better in Polish**, while weak models gain from translation into English. Outside OneRuler, this is the only area where Polish consistently beats English.

---

## 3. Answers to the research questions

**Is Polish more effective than English?**
In two narrow areas. The first is OneRuler NIAH (long context, older models), which has substantial confounders. The second is questions about Polish culture and realities in strong 2026 models. On every other application we found, Polish is **equal** to English (2026 frontier models) or **lower** (smaller and open models, tool calling, forced-language reasoning).

**Does language affect RAG?**
Yes. What matters most is that the **query language matches the document language**, not that English is better. Retrievers and generators prefer documents in the query language. Modern embedders lose about 1 pt in Polish. Classic BM25 loses clearly in Polish because of inflection.

**Does language affect agent reasoning?**
Yes, measurably, depending on the task:
- the largest loss is in **exact tool-call formatting** (arguments) and complex web tasks (GAIA);
- the smallest is in math and code (SWE-bench, MATH: <1 pt);
- reasoning in English usually beats reasoning forced into another language;
- the gap **narrows as models improve**, and for 2026 frontier models it is often within noise.

### Practical takeaways (for GIS / Python / ETL teams)
1. **Keep the corpus and the queries in the same language.** With Polish documents, ask in Polish. Consider Polish embedders (PL-MTEB) or BM25 with lemmatization as a hybrid.
2. **Write tool schemas, function names and the agent's system prompt in English.** The answer to the user can still be in Polish.
3. **Do not force the language of the reasoning trace.** Let the model "think" in English.
4. **Budget for tokens.** Polish text uses about 1.7–2.3× more tokens, which means higher cost and less content per context window.
5. **Ask about Polish realities in Polish** (regulations, place names, institutions). With strong models this measurably helps.
6. **Run a small PL/EN eval on your own data** instead of relying on media rankings.

---

## 4. Methodology

- **Agents:** 17 Haiku subagent runs (6 in iteration 1 and 11 for verification and gap-filling, plus 1 follow-up QA question), coordinated by a lead agent that handled quality control, validation and feedback.
- **Rules:** only papers, preprints, official leaderboards, model cards and raw result files counted as evidence. Media and forums served only as pointers. Statuses: VERIFIED / PARTIAL / UNVERIFIED.
- **Iterations:**

| # | Goal | Outcome |
|---|---|---|
| 1 | 6 agents: article source, general benchmarks, RAG, reasoning/agents, long context + tokenization, other Slavic languages | OneRuler identified; evidence map |
| 2 | V1–V4: verify OneRuler (LaTeX + figures), RAG numbers, Global-MMLU and model cards, MCLM/MAPS/PolyMath | corrections (e.g. Swahili 90.8→59.6; mmlw-e5-base); new Belebele data |
| 3 | V5–V6: audit of 11 arXiv IDs from 2026; newer LLMs (Global PIQA, EuroEval) | 0 hallucinated IDs; Bielik 82.98/77.41 corrected (two different models, not "before/after") |
| 4 | V7–V8: 39 papers citing OneRuler; Global-MMLU figures; Anthropic page | no replication; Global-MMLU has no per-language results (premise corrected) |
| 5 | V9–V10: MASSIVE-Agents column mapping; devil's-advocate search | PL 29.5 vs EN 57.4 confirmed; no counter-evidence |
| 6 | V11: Masked Advantage (2026), PoVisLE (2026), CUS-QA, OneRuler haystack books | new nuance in §2.6; **correction**: the CUS-QA numbers V10 reported do not match Tab. 11, so they were excluded |

- **Lead spot checks (in raw PDF/CSV text):** OneRuler numbers (88.0 / 83.9 / 62.1 / o3-mini 67 vs 92), MCLM Tab. 25–26, MASSIVE-Agents, the Anthropic page (grep: 0 hits for "Polish"), Global PIQA (recomputed over 177 models), EU-24+ Tab. 4.
- **Tooling:** `pypdf` was installed mid-run to read PDF tables, and ghostscript was used to render vector figures.

## 5. Limitations
- The OneRuler ranking beyond PL/EN/ZH is read from a chart (±0.5 pts).
- There are no official PL-vs-EN tables for Claude, GPT-5.x or Gemini 3 on knowledge benchmarks. Global PIQA is the only parallel source with 2026 frontier models, and it has only 103 items per language.
- Some 2026 papers (COMPASS, Masked Advantage) report Polish results only in figures.
- Agent evidence for Polish comes mostly from MASSIVE-Agents (2024–2025 models) and EU-24+ (dialogue games, not tool-use benchmarks).

## 6. Working files
The full agent reports (`r1_*.md` … `r6_*.md`) and the shared brief are in the session's `research/` folder. Interactive versions: the PL and EN artifacts.
