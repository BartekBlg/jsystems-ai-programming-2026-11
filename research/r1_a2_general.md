# R1 / A2 - Polish vs English on general knowledge and reasoning benchmarks (2025-2026 focus)

Agent: A2, iteration 1. Date of research: 2026-11-24.

## 1. Summary

- **No 2025-2026 parallel benchmark found that puts Polish on the same footing as English for recent frontier models with a clean PL-EN gap.** The two most-used translated MMLU-style sets exclude Polish: OpenAI MMMLU (14 languages, no PL) and MMLU-ProX (29 languages, no PL). Global-MMLU includes Polish, but I could not extract its per-language numbers.
- **The best same-model, parallel PL vs EN evidence is Grzybowski et al., arXiv 2412.00559 (EMNLP 2025 Findings), Table 4.** On matched Polish-exam and professionally translated English-exam questions, 13 of 16 models score higher in English on both subsets (Meditron3-70B, Bielik-11B-v2.2 and GPT-4o are the exceptions). Most gaps are 1-6 points for larger/stronger models and 5-18 points for small or medical-specialised models. GPT-4o is slightly higher in Polish (LEK +1.27, LDEK +1.38). This is a medical domain, not general knowledge.
- **MMLU-ProX (arXiv 2503.10497, Table 3): for Slavic neighbours (CS, RU, UK, SR), most frontier/open models are 0-6 points below English**, with some exceptions (GPT-4o on Czech/Serbian, DeepSeek-R1 on Serbian) and some very large drops for small models (InternLM3-8B, Phi4-14B on Serbian). Polish was not measured. Some extracted values look suspicious (see section 3); raw-check before quoting.
- **Long-context retrieval (ONERULER, arXiv 2503.01996, COLM 2025, Fig. 4b / Sec 3.1):** Polish is ranked 1st of 26 languages (avg NIAH 88% at 64K and 128K) and English 6th (83.9%). This is the opposite sign to the knowledge benchmarks, and it is a synthetic task on older models (Gemini 1.5 Flash, o3-mini-high, Qwen2.5, Llama 3.x). It supports the "Polish article" headline only for this narrow task.
- **Net answer to the key question:** on parallel knowledge/reasoning benchmarks, Polish is generally equal-to-lower than English (typically -1 to -6 points for strong models, larger for weak ones; occasional small positive gaps for GPT-4o-class models). A Polish ranking first would require a non-parallel or task-specific comparison. Limited N and mostly 2024-2025 models; no VERIFIED 2026 frontier per-language table found.

## 2. Findings table

Columns: Benchmark | Model | EN score | PL score | PL-EN gap | Metric | Source+location | Status

| Benchmark | Model | EN score | PL score | PL-EN gap | Metric | Source+location | Status |
|---|---|---|---|---|---|---|---|
| Polish-English medical exams (LEK, matched) | GPT-4o | 88.69 | 89.96 | +1.27 | % correct | Grzybowski et al. arXiv 2412.00559v2, Table 4 (LEK) | VERIFIED (via WebFetch extraction of arXiv HTML) |
| same | GPT-4o | 72.51 | 73.89 | +1.38 | % correct | same, Table 4 (LDEK) | VERIFIED (same) |
| same | GPT-4o-mini | 75.92 | 75.88 | -0.04 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | GPT-4o-mini | 56.88 | 54.94 | -1.94 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Llama-3.1-70B | 82.60 | 80.94 | -1.66 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Llama-3.1-70B | 63.17 | 61.75 | -1.42 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Mistral-Large | 82.40 | 76.75 | -5.65 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Mistral-Large | 62.14 | 56.29 | -5.85 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Qwen2.5-72B | 81.93 | 76.87 | -5.06 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Qwen2.5-72B | 63.33 | 58.35 | -4.98 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Qwen2.5-7B | 67.73 | 51.92 | -15.81 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Qwen2.5-7B | 48.38 | 43.71 | -4.67 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Llama-3.1-8B | 64.69 | 51.25 | -13.44 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Llama-3.1-8B | 47.71 | 41.06 | -6.65 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Mistral-Small | 63.70 | 51.72 | -11.98 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Mistral-Small | 43.47 | 40.90 | -2.57 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Bielik-11B-v2.2-Instruct (PL-specialised) | 56.98 | 62.36 | +5.38 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Bielik-11B-v2.2-Instruct (PL-specialised) | 42.88 | 43.20 | +0.32 | % correct | same, Table 4 (LDEK) | VERIFIED |
| same | Meditron3-70B | 66.75 | 68.37 | +1.62 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Meditron3-8B | 60.34 | 45.99 | -14.35 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | JSL-MedLlama-3-8B-v2.0 | 54.09 | 35.98 | -18.11 | % correct | same, Table 4 (LEK) | VERIFIED |
| same | Llama-3.2-3B | 52.08 | 39.22 | -12.86 | % correct | same, Table 4 (LEK) | VERIFIED |
| MMLU-ProX (29 langs; PL not included) | Llama3.3-70B | 65.7 | 63.8 (CS) / 61.1 (RU) / 59.9 (UK) / 63.0 (SR) | n/a for PL; CS -1.9, RU -4.6, UK -5.8, SR -2.7 | accuracy % (5-shot CoT) | Xue et al. arXiv 2503.10497, Table 3 | VERIFIED (numbers checked against raw PDF text, dl/mmlux/paper.txt; Polish absent) |
| same | GPT-4.1 | 79.8 | 77.5 (CS) / 71.2 (RU) / 76.4 (UK) / 76.9 (SR) | CS -2.3, RU -8.6, UK -3.4, SR -2.9 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | o4-mini | 73.7 | 73.5 (CS) / 62.0 (RU) / 73.3 (UK) / 72.6 (SR) | CS -0.2, RU -11.7, UK -0.4, SR -1.1 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | DeepSeek-R1 | 79.5 | 76.8 (CS) / 76.4 (RU) / 76.8 (UK) / 80.9 (SR) | CS -2.7, RU -3.1, UK -2.7, SR +1.4 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | DeepSeek-V3 | 79.6 | 74.7 (CS) / 74.9 (RU) / 74.2 (UK) / 72.9 (SR) | CS -4.9, RU -4.7, UK -5.4, SR -6.7 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | Qwen3-235B-Think | 80.7 | 80.5 (CS) / 77.0 (RU) / 78.8 (UK) / 80.2 (SR) | CS -0.2, RU -3.7, UK -1.9, SR -0.5 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | Qwen3-235B | 73.5 | 71.8 (CS) / 72.9 (RU) / 72.5 (UK) / 71.1 (SR) | CS -1.7, RU -0.6, UK -1.0, SR -2.4 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | Qwen3-32B | 71.8 | 67.7 (CS) / 68.0 (RU) / 68.0 (UK) / 67.2 (SR) | CS -4.1, RU -3.8, UK -3.8, SR -4.6 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | Gemma3-27B | 66.5 | 62.6 (CS) / 62.5 (RU) / 61.7 (UK) / 61.7 (SR) | CS -3.9, RU -4.0, UK -4.8, SR -4.8 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | Phi4-14B | 71.5 | 63.2 (CS) / 65.2 (RU) / 61.3 (UK) / 50.7 (SR) | CS -8.3, RU -6.3, UK -10.2, SR -20.8 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | Aya-32B | 40.8 | 34.5 (CS) / 36.7 (RU) / 35.9 (UK) / 27.4 (SR) | CS -6.3, RU -4.1, UK -4.9, SR -13.4 | accuracy % | same, Table 3 | VERIFIED (raw PDF) |
| same | GPT-4o | 59.9 | 70.1 (CS) / 62.0 (RU) / 56.8 (UK) / 70.6 (SR) | CS +10.2, RU +2.1, UK -3.1, SR +10.7 | accuracy % | same, Table 3 | VERIFIED (raw PDF; EN 59.9 is the paper value, see section 3) |
| Global-MMLU (42 langs; PL included) | not extracted | n/a | n/a | n/a | accuracy % | arXiv 2412.03304 (ACL 2025); appendix per-language tables not reached | UNVERIFIED (numbers not obtained) |
| EU21 benchmarks (ARC, GSM8K, HellaSwag, MMLU, TruthfulQA avg) | Llama-3.1-70B-Instruct | Avg .79 (MMLU .82, ARC .80, GSM8K .87, HellaSwag .86, TQA .60) | Avg .70 (MMLU .78, ARC .72, GSM8K .69, HellaSwag .74, TQA .59) | Avg -9; MMLU -4; ARC -8; GSM8K -18; HellaSwag -12; TQA -1 | accuracy (fraction) | Thellmann et al. arXiv 2410.08928, Sec 4.2 Table 1 | PARTIAL (dates 10/2024; method of translation not confirmed in extracted text) |
| ONERULER long-context NIAH (26 langs) | avg of Gemini 1.5 Flash, o3-mini-high, Qwen2.5 72B/7B, Llama3.3 70B, Llama3.1 8B (per paper, not fully specified) | 83.9% (rank 6/26) | 88% (rank 1/26) | +4.1 | NIAH accuracy, 64K+128K avg | Kim et al. arXiv 2503.01996v2 (COLM 2025), Sec 3.1 and Fig 4b | PARTIAL (text values; the full ranking is in a figure) |
| LLMzSzŁ (Polish national exams, ~19k Qs) | Large (~70B) models | not in extracted text | not in extracted text | "perform similarly" (qualitative) | accuracy | arXiv 2501.02266 (Jan 2025), Sec 2 and 4.2 | PARTIAL (qualitative claim only; citing Grzybowski et al. 2024) |
| OpenAI MMMLU (14 langs) | all models | n/a | n/a | n/a | accuracy | openai/simple-evals multilingual_mmlu_benchmark_results.md | VERIFIED negative: Polish not in table |
| Bielik-11B-v2 (family) Open PL vs Open LLM EN | Bielik-11B-v2.3-Instruct | Open LLM avg 69.82 (v2.1) / 64.57 MMLU | Open PL 5-shot avg 65.71 | not a parallel comparison | different task suites | Bielik 11B v2 Tech Report arXiv 2505.02410v2, Tables 19, 23 | PARTIAL (not a valid PL-EN gap; suites differ) |
| Bielik 11B v3 | Bielik-11B-v3.0-Instruct | n/a | n/a | n/a | n/a | arXiv 2601.11579 (Jan 2026) | UNVERIFIED (PDF binary not parseable; numbers only from secondary snippets, e.g. PLCC 71.83%) |
| EuroLLM-9B MMLU (EU avg, 5-shot) | EuroLLM-9B-IT | not per-language | not per-language | n/a | MMLU | arXiv 2506.04079, Tables 4-5 | PARTIAL (only EU-average; per-language appendix tables 6-47 not reached) |
| Gamayun 1.5B (2512.21580) | Gemma3-1B / Qwen3-1.7B etc. | MMLU EN (5-shot): 39.8 / 60.2 (Table 2) | Polish Open Leaderboard: 46.8 / 43.2 (Table 3) | not a parallel comparison | different benchmarks | arXiv 2512.21580, Tables 2-3 | PARTIAL (not usable as gap) |

## 3. Details per source

**Grzybowski et al., arXiv 2412.00559 (v2, revised Sep 2025; EMNLP 2025 Findings).** Polish medical licensing/specialisation exams (LEK, LDEK, PES) plus English versions (LEK en, LDEK en, professionally translated by the examination center). Table 4 compares the matched PL and EN questions for 16 models, including Bielik-11B-v2.2 and GPT-4o. Metric is % correct. The authors state that "all medical models, except Meditron3-70B, perform better on the English versions," that the gap narrows as model quality rises, and that GPT-4o scores higher on Polish. This is the strongest parallel evidence found. Limitations: medical domain only, pass-rate framing, a mix of 2024 models, and a few models (GPT-4o family) are run via API. The numbers were extracted with WebFetch from the arXiv HTML, not copied from a raw PDF table; spot-check Table 4 before quoting.

**MMLU-ProX, arXiv 2503.10497 (2025).** 29 languages, translated from MMLU-Pro (11,829 identical questions per language). Polish is NOT among the 29. Table 3 (5-shot CoT accuracy, 15 models) was checked against the raw text of the arXiv PDF (downloaded and extracted locally): the EN, CS, RU, UK and SR rows match the values in section 2. Caveats that are in the paper itself: GPT-4o EN is 59.9, below its CS (70.1) and SR (70.6); InternLM3-8B CS is 0.0. Treat these as reported values, not as errors of extraction.

**Global-MMLU, arXiv 2412.03304 (ACL 2025).** 42 languages, Polish is in the high-resource tier (Table 5, Appendix A). The main text has only rank-change tables (Tables 2 and 4) and aggregate figures. The per-language accuracy appendix was not reachable in the extracted HTML (I read ~163k characters; the appendix F tables were cut off). Numbers not obtained; re-run with the PDF or the HF dataset (CohereForAI/Global-MMLU).

**OpenAI MMMLU (simple-evals).** The per-language table has 14 languages (Arabic, Bengali, Chinese-Simplified, French, German, Hindi, Indonesian, Italian, Japanese, Korean, Portuguese-BR, Spanish, Swahili, Yoruba) and no Polish and no English. Models: o3-high, o1, o4-mini-high, o3-mini-high, GPT-4.5-preview, GPT-4.1, GPT-4o, GPT-4.1-mini, GPT-4o-mini, GPT-4.1-nano. Not usable for PL.

**EU21 benchmarks, Thellmann et al. arXiv 2410.08928.** The only Polish-vs-English per-model pair I found for a general-knowledge set beyond the medical one. Only Llama-3.1-70B-Instruct was visible in the extracted text; appendix tables for other models were not reached. The gap is larger on GSM8K (-18) and HellaSwag (-12) than on MMLU (-4). Whether these are machine-translated is not confirmed in the extracted text.

**ONERULER, Kim et al. arXiv 2503.01996 (COLM 2025).** Synthetic NIAH and aggregation tasks in 26 languages, 8K-128K contexts. Polish is 1st (88%) and English 6th (83.9%) on the 64K/128K NIAH average. The paper also says that instruction language can shift performance by up to 20 points in cross-lingual setups. Sign of the gap is task-dependent: this is not a knowledge benchmark.

**LLMzSzŁ, arXiv 2501.02266 (Jan 2025).** ~19k Polish national exam questions. Its own Table (Appendix) lists Polish-focused and multilingual models. The extracted text contains the qualitative claim that ~70B models perform similarly in Polish and English and that smaller models do best in their primary training language, citing Grzybowski et al. 2024 (the earlier version of 2412.00559). No PL-vs-EN table for the same model was found in the text I could read.

**PLCC, arXiv 2503.00995 (Mar 2025).** 600 Polish culture/language questions. Polish-only, so it cannot give a PL-EN gap. Leaderboard at huggingface.co/spaces/sdadas/plcc. Secondary-only numbers (not used in the table): Bielik-11B-v3.0-Instruct 71.83% (from Bielik 11B v3 report snippet).

**Bielik v2/v3 reports (arXiv 2505.02410, 2505.02550, 2601.11579, 2604.10799).** These report Polish suites (Open PL LLM Leaderboard, Polish EQ-Bench, CPTUB, PLCC, LLMzSzŁ) and English suites (Open LLM Leaderboard v1/v2) for the same model, but on different task sets, so they cannot be read as a PL-EN gap. Table 19 of v2 has English Open LLM Leaderboard scores (e.g. Bielik-11B-v2.3-Instruct MMLU 64.57, ARC 59.30); Table 23 has Open PL 5-shot average (65.71). The Bielik 11B v3 PDF is binary-only in the tools I had; its numbers remain UNVERIFIED.

**Gamayun (arXiv 2512.21580, Dec 2025).** Polish Open Leaderboard (Table 3) and English MMLU 5-shot (Table 2) are different benchmarks, so no gap is computed. Translated MMLU-pl (Global) values from search snippets (Gemma3 34.75, Qwen3 43.47) are not verified in the paper body I could read.

**EuroLLM-9B (arXiv 2506.04079, Jun 2025).** Tables 4-5 give only EU-average MMLU (e.g. EuroLLM-9B 55.68 pre-trained, Gemma-2-9B 66.19). Per-language appendix tables (A.1.5 English, A.1.15 Polish) exist but were beyond the extracted text.

## 4. Search log

Queries run (WebSearch, standard mode unless noted):
1. "MMLU-ProX multilingual benchmark 29 languages Polish results" - rate-limited (too_many_requests).
2. "Global-MMLU Polish per-language results arXiv 2412.03304" - rate-limited.
3. "OpenAI MMMLU per-language accuracy table Polish" - returned simple-evals link (useful).
4. "Polish LLM benchmark MMLU Polish English gap 2025 arXiv" - PLCC, LLMzSzŁ, Bielik leads.
5. "Bielik v3 technical report benchmark Polish English comparison" - Bielik v3 Small, 2601.11579, 2604.10799.
6. "Claude multilingual MMLU performance by language Polish model card" - rate-limited. NOT re-run. Anthropic model cards not checked (open item).
7. "Open PL LLM leaderboard ... same model" - no same-model PL-EN pair found.
8. "LLMzSzŁ benchmark results ..." - LLMzSzŁ appendix leads.
9. "Polish vs English benchmark same LLM performance gap ... 2025 study" - found 2412.00559 (key) and ONERULER pointer.
10. "Global-MMLU Polish accuracy per-language appendix Gemma Llama results" - Gamayun / EuroLLM leads.
11. "ONERULER long context multilingual 26 languages Polish arXiv" - got 2503.01996.
12. "Slavic languages LLM benchmark Czech Russian Ukrainian ..." - CUS-QA, BenCzechMark (native, not parallel).
13. "PLCC ... results ..." - no per-model English table.
14. "Polish MMLU translated benchmark Qwen3 Gemma 3 ..." - Gamayun, PUMA/PoVisLE (multimodal, out of scope).
15. "Grzybowski ... 2412.00559 GPT-4o Polish higher than English" - confirmed paper and venue.

Fetches (WebFetch of primary arXiv HTML): OpenAI simple-evals (GitHub), 2503.10497 (twice), 2601.11579 (pdf and html - 404 on html v1), 2412.03304 v2 (two offsets), 2410.08928, 2412.00559v2, 2506.04079, 2512.21580, 2503.01996v2, 2505.02410v2 (two offsets), 2501.02266v1.

Dead ends: Bielik 11B v3 PDF (binary, not parseable with available tools; no pdftotext/pdftoppm installed); Global-MMLU appendix tables not reached; Anthropic/Claude per-language tables not searched successfully (rate limit).

## 5. Open questions / items that need further verification

1. Re-check MMLU-ProX Table 3 raw values (GPT-4o EN 59.9, InternLM3 CS 0.0, duplicate 62.0 values) against the PDF.
2. Extract Global-MMLU per-language appendix for pl and en (HF dataset CohereForAI/Global-MMLU or PDF appendix). This is the best direct PL-vs-EN source on a general-knowledge set with Polish.
3. Anthropic / Google / OpenAI 2025-2026 model cards with per-language tables: not checked successfully. Claude's multilingual MMLU table (historically averaged across languages) needs direct verification.
4. Bielik 11B v3 (2601.11579) and Bielik v3 Small (2505.02550) tables: need text extraction from PDF (install poppler or pypdf) to get Open PL vs Open LLM pairs.
5. EU21 (2410.08928) appendix per-model PL vs EN tables: need the full PDF.
6. Whether "Polish ranked first" in the source article comes from ONERULER-type long-context data or from a leaderboard. The primary source behind the article is not yet identified in this iteration.
