# R3 V6: Polish vs English on recent (2025-2026) LLMs, reading and understanding tasks

Scope: same-model PL vs EN numbers for 2025-2026 models. Belebele 2023 numbers excluded.
Accuracy/F1 values are in percent. "Parallel" means the same items translated into both languages.

## 1. Summary

- **Belebele pol_Latn vs eng_Latn for 2025-26 models: not found in any primary source I opened.**
  Gemma 3 (arXiv 2503.19786) has no Belebele. Qwen3 (arXiv 2505.09388, Table 37) reports Belebele only as language-family aggregates, so Polish and English are not separable. Bielik 11B v3 (arXiv 2601.11579, Table 13) reports only a 28-language European average. EuroLLM-22B (arXiv 2602.05879) has no Belebele, and its understanding tables omit Polish.
- **Global PIQA (arXiv 2510.24081, per-language CSV in the GitHub repo) is the only source with truly parallel PL vs EN numbers for 2025-26 models.** Parallel split, frontier models: Polish is equal to or higher than English for GPT-5.4 (98 vs 96), Gemini 3.1 Pro (100 vs 100), Gemma 4 31B-it (89 vs 90, about a tie), and Qwen3.5-27B-instruct (93 vs 83). Open base models and small models are usually lower in Polish, by 5 to 15 points. Caveat: about 100 items per language, so the standard error is roughly ±4 to ±5 points.
- **EuroEval (native Polish vs English datasets, not parallel):** on reading comprehension, PoQuAD (PL) is about 30 F1 below SQuAD (EN) for the same models (n=78 models, mean gap -29.8). The gap is most likely dataset difficulty, not language. On knowledge, the gap is about -15 points (MMLU vs LLMzSzŁ).
- **MultiWikiQA (arXiv 2509.04111, 2-shot F1, native Wikipedia per language, not parallel):** Polish is 10 to 18 F1 below English for the same models.
- **LMArena/Arena Text, Polish filter vs overall (snapshot 2026-11-22, rank order only):** the Polish filter ranks some Claude models higher than the overall board does, and vice versa for Muse Spark. This is human preference, not accuracy.
- **Conclusion for the article's claim:** on parallel understanding tasks (Global PIQA), frontier models show no meaningful PL deficit, and some show a small PL advantage. On native-dataset reading tasks, Polish is clearly lower. The article's "Polish beats English" claim is not supported by any accuracy benchmark I checked, and the parallel data is closer to a tie than a win.

## 2. Findings table

| Benchmark | Model (date) | EN | PL | gap (PL-EN) | parallel? | source + location | status |
|---|---|---|---|---|---|---|---|
| Global PIQA (parallel split) | GPT-5.4 (2026) | 96 | 98 | +2 | yes | arXiv 2510.24081; `eval_results/results_per_language.csv` (GitHub mrlbenchmarks/global-piqa), rows gpt-5-4 | VERIFIED (CSV); cross-checked for Gemini 3.1 Pro against paper Table 4 |
| Global PIQA (parallel) | GPT-5.4 mini / nano | 92 / 83 | 94 / 87 | +2 / +5 | yes | same CSV | VERIFIED (CSV) |
| Global PIQA (parallel) | Gemini 3.1 Pro (2026) | 100 | 100 | 0 | yes | same CSV; paper Table 4 (EN 100%) | VERIFIED |
| Global PIQA (parallel) | Gemma 4 31B-it (2026) | 90 | 89 | -1 | yes | same CSV | VERIFIED (CSV) |
| Global PIQA (parallel) | Gemma 3 27B-it (2025) | 78 | 82 | +4 | yes | same CSV | VERIFIED (CSV) |
| Global PIQA (parallel) | Qwen3.5-27B-instruct (2026) | 83 | 93 | +11 | yes | same CSV | VERIFIED (CSV); single model, n about 100 |
| Global PIQA (parallel) | Qwen3-32B-instruct (2025) | 84 | 79 | -6 | yes | same CSV | VERIFIED (CSV) |
| Global PIQA (parallel) | Llama-3.1-70B-Instruct (2024) | 71 | 71 | 0 | yes | same CSV | VERIFIED (CSV) |
| Global PIQA (parallel) | Apertus-70B-Instruct-2509 (2025) | 69 | 65 | -4 | yes | same CSV | VERIFIED (CSV) |
| Global PIQA (parallel) | EuroLLM-9B-Instruct (2025) | 44 | 50 | +6 | yes | same CSV | VERIFIED (CSV); low absolute level |
| Global PIQA (non-parallel) | GPT-5.4 / Gemini 3.1 Pro | 95 / 96 | 97 / 99 | +2 / +3 | no | same CSV | VERIFIED (CSV) |
| Global PIQA (non-parallel) | Qwen3.5-27B / Gemma 4 31B-it | 96 / 96 | 93 / 97 | -3 / +1 | no | same CSV | VERIFIED (CSV) |
| EuroEval reading comp. (SQuAD EN vs PoQuAD PL, F1) | Qwen3-32B no-thinking (2025), n=78 models mean | 72.2 (mean) | 42.4 (mean) | -29.8 | no (native datasets) | EuroEval/leaderboards `results.tar.gz` (results.processed.jsonl), EuroEval v16.x; datasets `squad`, `poquad` | VERIFIED (raw records); same model: 89.2 vs 60.1 (-29.0) |
| EuroEval reading comp. (F1) | GPT-5 (2025-08) | 83.7 | 45.3 | -38.5 | no | same JSONL, ver 15.15 | VERIFIED (raw records) |
| EuroEval reading comp. (F1) | Claude Sonnet 4.5 (2025-09), no-thinking | 84.8 | 47.8 | -37.0 | no | same JSONL, ver 16.3.0 | VERIFIED (raw records) |
| EuroEval knowledge (acc) | GPT-4.1 (2025-04), MMLU EN vs LLMzSzŁ PL | 86.0 | 76.5 | -9.5 | no | same JSONL (`mmlu`, `llmzszl`) | VERIFIED (raw records); mean over 21 models -15.4 |
| EuroEval knowledge (acc) | Gemma 3 27B-pt (2025) | 77.8 | 68.5 | -9.2 | no | same JSONL | VERIFIED (raw records) |
| MultiWikiQA (F1, 2-shot) | Mistral Small 3.1 24B base (2025) | 79.5 | 69.6 | -9.9 | no | arXiv 2509.04111, Table 4 (en row) and Table 6 (pl row); "Mistral Base" column, model per Table 1 | VERIFIED (numbers read in text extraction; column-to-model mapping inferred) |
| MultiWikiQA (F1, 2-shot) | Mistral Small 3.1 24B instruct (2025) | 78.5 | 64.7 | -13.8 | no | same | VERIFIED (as above) |
| MultiWikiQA (F1, 2-shot) | Llama 3.1 8B instruct (2024) | 71.4 | 55.8 | -15.6 | no | same | VERIFIED (as above) |
| Belebele, Qwen3 family aggregate | Qwen3-32B thinking vs Gemma-3-27B-IT (2025) | n/a | n/a | n/a | n/a | arXiv 2505.09388, Table 37 ("Indo-European" column, pol and eng pooled) | VERIFIED, aggregate only; PL-EN not separable |
| Belebele, Bielik 11B v3 | 28-language European average (2026) | n/a | n/a | n/a | n/a | arXiv 2601.11579, Table 13 (avg 82.98) | PARTIAL: no per-language rows |
| Arena Text, Polish filter vs overall (rank order) | Claude Opus 5.5 High (2026-11-22 snapshot) | #5 (1507) | #8 (1506) | rank -3 | n/a (preference) | arena.ai/leaderboard/text and /text/polish, read by WebFetch | UNVERIFIED-ish: values come from a summarizing fetch, not raw data; re-open to confirm |

Not found in primary sources: Belebele pol_Latn/eng_Latn for Gemma 3, Llama 4, Qwen3, EuroLLM-22B, Apertus (no Belebele hit in text), Global-MMLU or INCLUDE per-language PL vs EN for frontier models.

## 3. Details per source

- **Global PIQA** (Oct 2025; arXiv 2510.24081; text mentions GPT-5.4, Gemini 3.1 Pro, Gemma 4): 141 language varieties in total (136 non-parallel, 131 in the parallel split, paper lines 15-20 and 164-165), with exactly 100 examples per language on the non-parallel split (paper line 265). The paper's Table 4 gives per-model best and worst languages for closed systems, and its Gemini 3.1 Pro row (English 100%) matches the CSV, which I used as a consistency check. Limitations: about 100 items per language (roughly ±4 to ±5 points at 95% CI); instruct models were scored by exact match and base models by loglikelihood accuracy, so metrics are mixed across rows. The paper says the full per-language results are in the GitHub repo, not the PDF.
- **EuroEval** (repo `EuroEval/leaderboards`, `results.tar.gz`, version 16.x records): Polish and English datasets are native, not parallel. The reading-comprehension pair is SQuAD (EN) vs PoQuAD (PL); the knowledge pair is MMLU vs LLMzSzŁ; the commonsense pair is HellaSwag vs Winogrande-PL, which gives a near-zero mean gap (-0.9, n=74). Read the -30 F1 reading gap as dataset-level, not language-level.
- **MultiWikiQA** (Sep 2025, v3 Mar 2026; arXiv 2509.04111): LLM-generated questions over Wikipedia articles in 306 languages, 2-shot decoders and encoder baselines. Polish and English use different Wikipedia articles, so the gap includes article and question-generation effects. The paper's Table 1 lists six models; the column names in Tables 4 and 6 ("Mistral Base", "Llama Instruct") are abbreviations. I mapped them to Mistral-Small-3.1-24B and Llama-3.1-8B from Table 1; this mapping is inferred.
- **Qwen3** (May 2025; arXiv 2505.09388, Table 36-37): Belebele on 80 languages, grouped by family. Polish is in Indo-European (40 languages) with English, so no PL-only or EN-only number. Gemma-3-27B-IT and Qwen3-32B are at 89 to 91 on the family average.
- **Gemma 3** (Mar 2025; arXiv 2503.19786): no Belebele string in the text. Its multilingual table (pretrained models) covers MGSM, Global-MMLU-Lite, WMT24++ and FLoRes as averages without per-language Polish rows.
- **EuroLLM-22B** (Feb 2026; arXiv 2602.05879): no Belebele. Its per-language tables (Hellaswag, MMMLU, ARC-C, MMLU-ProX) list the EU languages da, de, es, ... but not pl. Polish appears only in translation tables (FLORES, WMT24++). So EuroLLM-22B offers no PL vs EN understanding number.
- **Bielik 11B v3** (Jan 2026; arXiv 2601.11579): Belebele average over 28 European languages (Table 13). INCLUDE (Table 12) gives Polish-only scores (Bielik-11B-v3-Instruct 69.0 Polish, average 64.8 over 20 languages), with no English column.
- **Arena (LMArena renamed to Arena, Jan 2026)**: the Polish filter is labelled "Best LLM for Polish (Polski): Text Arena". The top 15 include Muse Wonder, Gemini 4 Argon, and Claude Fable 5.1. Overall top 15 include Gemini 4 Carbon as #1 and Claude Opus 5.5 as #5. Preference votes, not accuracy, and the Polish sample is much smaller. Numbers come from WebFetch summaries and need to be checked directly.

## 4. Search log

Queries (WebSearch): "Gemma 3 technical report Belebele per language pol_Latn eng_Latn"; "EuroEval Polish leaderboard results 2026"; "Global PIQA multilingual physical commonsense benchmark 2025 Polish results"; "LMArena Polish language leaderboard"; "EuroLLM-22B technical report Belebele English Polish scores"; "Qwen3 technical report Belebele multilingual results"; "Belebele pol_Latn eng_Latn accuracy 2025 instruction-tuned models table per language"; "PLLuM technical report Belebele Polish English reading comprehension"; "Salamandra OR Teuken technical report 2025 Belebele per-language results Polish"; "Global-MMLU OR INCLUDE benchmark Polish per-language results 2025 frontier models English comparison"; "EuroEval Polish monolingual leaderboard github results pl".

Useful leads: euroeval.com pages are JavaScript-rendered (WebFetch returned only the title). The data came from `github.com/EuroEval/leaderboards` (results.tar.gz) instead. Global PIQA per-language numbers came from `raw.githubusercontent.com/mrlbenchmarks/global-piqa/main/eval_results/results_per_language.csv`. Dead ends: the Belebele searches returned encoder results (XLM-R, InfoXLM) and 2024 papers only.

Files downloaded to scratch (not for reuse): `scratchpad/dl/gemma3/paper.txt`, `qwen3/paper.txt`, `eurollm22/paper.txt`, `gpiqa/results_per_language.csv`, `euroeval/x/results.processed.jsonl`, `bielik3b/paper.txt`, `apertus/paper.txt`, `mwqa/paper.txt`.

## 5. Open questions / items that need further verification

1. Belebele PL vs EN for any 2025-26 model with per-language rows: still not found. Candidates not yet opened: Salamandra (arXiv 2502.08489, appendix), PLLuM (arXiv 2511.03823, "Evaluation Challenges and Benchmarks"), Aya Vision/Expanse, Llama 4 model card. Status: UNVERIFIED, not checked.
2. Arena Polish vs overall numbers should be re-read directly (open arena.ai/leaderboard/text/polish) before quoting. Snapshot was 2026-11-22.
3. Global PIQA: the per-model PL vs EN gap for Claude models is not in the CSV (only in paper Table 4, which lists best and worst languages, not PL). Check the parallel split for Claude Sonnet 4.6 separately.
4. EuroEval: I used the raw leaderboard records; a formal check of the Polish leaderboard page's current ranking is still pending because euroeval.com is JavaScript-rendered.
5. The Global PIQA parallel split uses different metric types for base vs instruct models; a like-for-like comparison needs the base-only or instruct-only subset.

## QA follow-up (Global PIQA parallel vs non-parallel, arXiv 2510.24081)

Source checked: `scratchpad/dl/gpiqa/paper.txt` (text of arXiv 2510.24081 PDF). Line numbers refer to that extraction.

**(1) Same items or native per-language items?** Parallel split = the SAME items translated. Quotes:
- Abstract (lines 15-20): "In the parallel split, we translate more 'culturally agnostic' commonsense reasoning questions into 131 language varieties, for direct cross-lingual comparisons."
- Section 3 (line 165): non-parallel split is "non-parallel (i.e. not translated across languages) to allow authors to write culturally-specific examples for their languages."
- Section 4 (lines 329-330): the non-parallel split "makes direct comparisons between languages difficult. Thus, we also construct a parallel split."
So pol_latn and eng_latn in the parallel split are the same questions. Non-parallel pol_latn and eng_latn are different items.

**(2) Source language and translator.** Source = English. Quote (Section 4, lines 336-344): "two native English speakers wrote 109 examples in English ... each example was machine translated into all 131 language varieties." MT was "either Gemini 2.5 Pro or Gemini 3.0 Flash." Human correction: "each translation was corrected or verified by at least one native speaker of the target language" (page 7). So translation is MT plus native-speaker human correction. Mean character edit distance 24.9 chars (12.9%) per example, across languages. Polish-specific translation details are not in the main text.

**(3) English parallel set.** Yes. The official parallel split has 103 examples (line 372: "103 human-verified parallel examples in 131 language varieties (except for Ekpeye, which has 101 examples)"). English is the source, so eng_latn has the same 103 items as pol_latn. In the CSV both appear with 177 model rows each. Six examples were dropped from all languages after correction (line 359-360), so the per-language count is 103 minus those drops; I did not confirm whether 103 is the final count after drops, the paper's wording suggests it is.

**(4) Statements about languages at or above English, or Polish.** The paper does not state a general "language X beats English" claim. Relevant points:
- Table 4 (Section H.3, page 42): the best language for several closed models is non-English. Examples: GPT-5.4 best spa_latn_peru 99%; Claude Sonnet 4.6 best fin_latn 99%; Gemini 3 Flash best srp_cyrl 99%. For Gemini 3.1 Pro, the best is eng_latn at 100%.
- Lines 484-485 say some systems "achieve over 90% accuracy on both the parallel and non-parallel splits". No Polish-specific statement appears in the main text.
- Line 364 and Section 4: the parallel split is described as mainly enabling comparisons across languages, not claiming any language is better.
Polish appears only in Table 1 (language list) and appendix dataset descriptions (group 0025, non-parallel).

**(5) Noise and confidence intervals.** The paper does not report per-language confidence intervals or significance tests. Statements: figures show "error bars indicate one standard error of the mean" (Figures 3, 11, lines 463, 2513, 2599). Limitation (line 530): "the sample size per language is only 100 examples." The 103 parallel items are the only data size given for parallel. Per-language values in the GitHub CSV are aggregate accuracies, not per-item outputs.
My own calculation, not from the paper: at n=103 the binomial standard error is about 3 points at p=0.9 (95% CI about ±6) and about 5 points at p=0.5 (±10). A PL-EN difference of 2 to 5 points on a single model is within noise. The parallel design allows a paired test (McNemar-type on the same items), but the CSV has no per-item outputs, so I did not run one.
