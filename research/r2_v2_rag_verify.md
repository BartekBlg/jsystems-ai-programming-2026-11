# R2 V2: Verification of R1 A3 (RAG / language effects) and new Slavic/Polish data

Method: arXiv HTML (https://arxiv.org/html/<id>) tables extracted from the HTML source and re-read; arXiv abs pages for venue comments; MTEB results repo (github.com/embeddings-benchmark/results) raw JSON for per-language retrieval; ACL Anthology via WebSearch for venue where arXiv gave none. Local copies in .../scratchpad/research/a6/.

## A. Verification verdicts (R1 A3 claims)

| # | Item | Verdict | Detail |
|---|---|---|---|
| 1 | Park & Lee, arXiv 2502.11175, Table 1 (MLR diagonal) | CONFIRMED | Diagonal bge-m3: en 56.03, zh 44.98, fr 43.18, es 40.76 (also ko 41.15, ja 45.03, it 41.06, pt 39.19). Retrieval metric is MultiLingualRank (MLR). |
| 1b | Table 2 (generation, char 3-gram recall, MKQA) | CONFIRMED | aya-expanse-8b, query ko: ko docs 49.66 vs en docs 38.08. Phi-4 49.25 vs 20.24, Qwen2.5-7B 49.44 vs 22.87, Llama-3.1-8B 49.87 vs 26.48. Query zh (aya): zh docs 38.31 vs en 25.62. |
| 1c | Venue | CONFIRMED | arXiv comment field: "ACL 2025 Findings". |
| 2 | XRAG, arXiv 2505.10089, Table 4 | CONFIRMED | Doc lang En: GPT-4o En 62.40, De 55.90, Es 56.80, Zh 54.70, Ar 54.70 (avg 55.50). Metric: accuracy (%) by 3-LLM judge panel (GPT-4o, Claude 3.5, Mistral-large), judge-human kappa 0.71. |
| 2b | Venue | CONFIRMED (via ACL Anthology listing, search result) | Findings of EMNLP 2025, 2025.findings-emnlp.849. arXiv metadata has no venue; the ACL page was not opened directly. |
| 3 | Faux Polyglot, arXiv 2407.05502 | CONFIRMED | Sec. 4.1.1: native-language docs 68% of top-10 retrieved. Sec. 4.2.1: answers from native 42% (SE 0.54), foreign 29.58% (SE 0.5), both 8.55%, neither 19.8%. Venue: arXiv comment "NAACL 2025" (the "long" label was not checked). |
| 4 | PL-MTEB, arXiv 2405.10138 | CONFIRMED with CORRECTION | Venue: arXiv comment "Accepted for ACL 2026 Findings"; ACL Anthology ID 2026.findings-acl.1773 exists (search result). Retrieval column (11 tasks, nDCG@10): stella-pl-retrieval-8k 61.59 and Qwen3-Embedding-8B 59.21 both confirmed. mmlw-roberta-base retrieval 53.60 confirmed. CORRECTION: the "49.80" for mmlw-e5-base is the 30-task overall average (column "Avg (30)"), not a retrieval value; mmlw-e5-base retrieval = 53.70. The base < small ordering (overall 49.80 vs 58.97; classification 47.37 vs 60.12) is printed in the paper's own Table 2, so it is not a parsing error. Treat it as an anomaly to check against the HF results before citing. |
| 5 | BEIR-PL, arXiv 2305.19840, Table 3 (BM25, NDCG@10, PL vs EN) | CONFIRMED (all 11 datasets) | MSMARCO 41.9 vs 47.7; TREC-COVID 61.0 vs 68.9; NFCorpus 31.9 vs 34.3; NQ 20.1 vs 32.6; HotpotQA 49.2 vs 60.2; FiQA 19.0 vs 25.4; ArguAna 41.36 vs 47.2; CQADupstack 28.37 vs 32.5; DBPedia 22.9 vs 32.1; SciDocs 14.1 vs 16.5; SciFact 62.5 vs 69.1. Paper attributes the gap to Polish inflection (Sec. 4), but the EN numbers are from original BEIR and the PL corpora are machine-translated, so translation is a confounder. |
| 1-5 general | OneRuler (R1 row 6, arXiv 2503.01996) | CONFIRMED in text | Intro and Sec. 3: Polish top at 64K/128K on NIAH, avg 88% across models; English 6th of 26 at 83.9%; Chinese 4th worst at 62.1%. Venue (COLM 2025) not re-checked. Synthetic NIAH, not RAG QA. |

## B. New data (2025-2026 preferred)

### B1. Belebele retrieval, same passages in each language (MTEB results repo, main_score = nDCG@10)
Source: https://github.com/embeddings-benchmark/results, results/<model>/<rev>/BelebeleRetrieval.json. Diagonal subsets (query and docs in the same language), 376 subsets. The main_score equals ndcg_at_10 (checked on one entry).

| Model | EN | PL | CS | RU | UK | PL-EN gap | Notes |
|---|---|---|---|---|---|---|---|
| Qwen3-Embedding-8B | 99.37 | 98.52 | 98.30 | 98.62 | 98.15 | -0.85 | Cross-lingual: PL query -> EN docs 98.94 |
| bge-m3 | 95.63 | 94.01 | 94.03 | 94.66 | 93.73 | -1.62 | PL query -> EN docs 91.85 |
| multilingual-e5-large | 96.50 | 95.28 | 95.15 | 95.28 | 95.16 | -1.22 | EN query -> PL docs 92.29 |

Status: VERIFIED (raw JSON). Caveat: the ceiling is near 99, so the gap is small. This shows the parallel-content effect for retrieval is small for modern embedders; the larger gaps in R1 (BEIR-PL, BM25) are mostly BM25 and translation effects.

### B2. Belebele reading comprehension, same questions (older models, 2023)
Source: arXiv 2308.16884v2 (Belebele), Table 7 (LLMs), accuracy %.

| Model | EN | PL | CS | RU | UK | PL-EN |
|---|---|---|---|---|---|---|
| GPT-3.5-turbo (zero-shot) | 87.7 | 74.7 | 76.9 | 78.4 | 72.8 | -13.0 |
| Llama 2 70B (5-shot) | 90.9 | 79.2 | 81.1 | 82.7 | 80.8 | -11.7 |
| Llama-2-chat 70B (zero-shot) | 78.8 | 61.7 | 65.0 | 67.0 | 65.7 | -17.1 |

Status: VERIFIED (table text). Caveat: models are 2023 generation; use only as a controlled parallel baseline, not as current model rankings.

### B3. Multilingual RAG language preference (Slavic: Russian, no Polish)
Source: "Linguistic Nepotism: Trading-off Quality for Language Preference in Multilingual RAG", arXiv 2509.13930 (v3, Jun 2026), Table 1, citation accuracy (%) on ELI5 long-form mRAG. English vs Russian (delta):
- LLaMA-3.3 70B: EN 85.9, RU 74.5 (-11.4)
- Gemma-3 27B: EN 86.2, RU 77.1 (-9.12)
- Qwen-3 14B: EN 83.0, RU 74.8 (-8.17)
- LLaMA-3.1 8B: EN 67.4, RU 62.1 (-5.30)
- Qwen-3 8B: EN 62.6, RU 50.4 (-12.2)
- Aya-23 8B: EN 60.0, RU 48.1 (-11.9)
Status: VERIFIED (table). Venue not confirmed (the paper's bibliography entries are for other papers). No Polish in this study. It also reports the same pattern as Park & Lee: English evidence is preferred even for Russian queries.

### B4. CUS-QA (Czech, Slovak, Ukrainian vs English, parallel QA)
Source: arXiv 2507.22752v3 (CUS-QA), "Textual QA" manual evaluation tables (metric: Correct+True+Relevant+Coherent, %). Each item has local-language and English versions of the same question and answer. Venue: TACL 2026 (search result; not opened).

| Model | CZ local | CZ English | UA local | UA English |
|---|---|---|---|---|
| Llama-3.3-70B | 58.7 | 52.5 | 51.9 | 47.8 |
| Llama-4-Scout-17B | 39.2 | 37.0 | 49.6 | 40.8 |
| EuroLLM-9B | 35.5 | 31.5 | 26.8 | 26.8 |
| Llama-3.1-8B | 28.7 | 29.4 | 22.6 | 30.4 |
| Mistral-7B-v0.3 | 23.0 | 24.3 | 21.3 | 29.4 |
Status: VERIFIED (table). No Polish. Mixed direction: the local-language advantage holds for the 70B and Llama-4 models, but reverses for the smaller Llama-3.1 and Mistral models on Czech and Ukrainian. Caveat: this is an open-ended, LLM/manual-judged QA, and the "English" column is the English translation of the same question, so translation quality matters.

### B5. Polish RAG / Slavic searches (negative results)
- MIRAGE-Bench (NAACL 2025): 18 languages (ar bn de en es fa fi fr hi id ja ko ru sw te th yo zh). No Polish. Per-language numbers were not extracted.
- MEMERAG (arXiv 2502.17163): built on MIRACL's 18 languages; no Polish. Not opened.
- Bartanowicz & Jassem (LREC 2026): Polish-only RAG comparison (Bielik vs PLLuM), no English baseline. Numbers not extracted.
- "Retrieval or Representation?" (arXiv 2603.04238): Czech and Croatian included, no Polish; OCR/visual RAG, not comparable.
- No 2025-2026 Polish-vs-English RAG benchmark found. Still the main gap.

## C. Summary for the project
- R1's central claims hold. Park & Lee, XRAG, Faux Polyglot, BEIR-PL, and PL-MTEB retrieval numbers were all re-read from the table text; one correction (PL-MTEB 49.80 is the overall average; retrieval 53.70).
- Parallel evaluations (Belebele retrieval, Belebele LLM, CUS-QA) show a consistent but model-dependent PL/Slavic-vs-EN gap: near zero for modern embedders on identical passages (about -1 nDCG), roughly -11 to -17 accuracy for 2023 LLMs, and mixed direction for 2025 LLMs on CS/UA questions.
- Polish was not present in: Park & Lee, XRAG, Faux Polyglot, Linguistic Nepotism, CUS-QA, MIRAGE-Bench. Polish appears in Belebele, MTEB/PL-MTEB, OneRuler, BEIR-PL, PIRB.

## D. Open items
- MMTEB per-language table (arXiv 2502.13595) not extracted. The results repo was the route used instead (B1).
- Venue of OneRuler (COLM 2025) and BEIR-PL not re-checked.
- The cryps.pl article is still not located; OneRuler remains the most likely origin of the "Polish beats English" headline (unverified link).
