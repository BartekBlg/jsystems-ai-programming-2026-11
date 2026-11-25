# R4 / V8 - Global-MMLU per-language Polish vs English, Anthropic multilingual table, Gamayun protocol check

Date: 2026-11-24. Agent V8, iteration 4.

## Summary

- **Task 1 (Global-MMLU, arXiv 2412.03304): NOT VERIFIED, no per-language PL or EN number exists in the paper.** The premise is wrong. Figs 9-11 (`main.tex` lines 497-509, files `figures/evaluation/mmlu_evaluation_{hig,mid,low}_res.pdf`) plot one point per model, averaged over a resource tier (high / mid / low), with the standard deviation across languages as the label. They are not per-language bars. I rendered the high-resource figure to PNG and read it: 9 models, CA and CS panels, mean accuracy about 64-90%. Polish is a high-resource language (appendix list, `main.tex` line 449), so it sits inside the high-tier average only.
- **Polish in Global-MMLU-Lite: absent.** The HF card for CohereLabs/Global-MMLU-Lite (23 languages, v3.0 May 2026) has no `pl`, `ru`, `uk` or `sr`. Czech (`cs`) was added only in v3.0. The Lite v1.0 set of 15 languages (the one in the arXiv paper) has no Polish either. Gemma 3 (arXiv 2503.19786) reports only the aggregate "Global MMLU-Lite" score.
- **Task 2 (Anthropic multilingual page): fetched with curl, verified.** Polish, Czech, Russian, Ukrainian and Serbian are absent. The table lists 14 languages plus English, all relative to each model's own English score.
- **Task 3 (Gamayun, arXiv 2512.21580): protocols NOT confirmed as identical.** The English row is labelled "MMLU (5-shot)". The Polish table (Table 19) has no shot count, and Appendix H says the multilingual tables were "primarily" evaluated zero-shot.

## 1. Global-MMLU (arXiv 2412.03304)

Source: arXiv e-print (LaTeX source, 10.9 MB) downloaded to `scratchpad/dl/gmmlu_src_v8/`, extracted to `x/`.

| Question | Finding | Location |
|---|---|---|
| Is Polish in the set? | Yes, `pl` Polish, Latin, High resource | `appendix.tex` line 39; `main.tex` lines 232-233 and 449 |
| Per-language accuracy in the paper? | None found in text, tables or figures | `main.tex` 455-515; `appendix.tex` |
| What do Figs 9-11 show? | Per-model mean over a resource tier (high / mid / low) on CA and CS subsets; labels are standard deviations across languages | `main.tex` 497-509; rendered `research/v8_img/hig_res.png` |
| Tables with numbers? | Only rank-change tables (ranking_changes figures and tables), not accuracy | `main.tex` ~1000-1100 |
| Lite-set numbers? | Fig. (gmmlu_lite) is also aggregate per model | `main.tex` 476 |

Status for PL vs EN per-language accuracy: **UNVERIFIED / not available in the source.**

Possible next step (not done here): run the HF dataset CohereLabs/Global-MMLU per-language split for `pl` and `en` with a model of your choice. That gives a fresh measurement, not the paper's number.

## 2. Global-MMLU-Lite / other reports

| Source | Polish? | Per-language table? | Status |
|---|---|---|---|
| HF CohereLabs/Global-MMLU card (`README.md`, raw) | Yes, `pl` High (line 2353); card lists 42 languages incl. `cs`, `en`, `ru`, `sr`, `uk` | No results table in card | VERIFIED (language list only) |
| HF CohereLabs/Global-MMLU-Lite card | No PL, RU, UK, SR. 23 languages: ar, bn, cs, cy, de, en, fr, hi, hu, id, it, ja, ko, my, or, pt, es, sk, sq, sw, tg, yo, zh. Paper results = the original 15 | No | VERIFIED (language list); Polish absent |
| Aya Expanse, arXiv 2412.04261 (`/html/` version) | Polish is in the 23-language pre-training list; Global-MMLU evaluated 5-shot across 23 languages as one aggregate | Only aggregate in text; `Polish` appears once | PARTIAL (no PL score) |
| Gemma 3, arXiv 2503.19786 (`/html/`) | No Polish mention; uses "Global-MMLU-Lite" as one aggregate score for pre-trained (Table 13) and instruction-tuned models (Table 18). Per-language values not shown in text | No | PARTIAL (aggregate only) |

## 3. Anthropic multilingual support page (task 2)

Fetched with: `curl -sL https://platform.claude.com/docs/en/build-with-claude/multilingual-support -o dl/anthropic_ml_v8/page.html` (566 KB, server-rendered; the table text is present in the HTML).

grep for Polish / Czech / Russian / Ukrainian / Serbian / Slavic / Bulgarian: **0 hits.**

Verbatim table ("Performance data"), zero-shot Global-MMLU, % of the same model's English score:

| Language | Claude Fable 5.2 | Claude Opus 5.5 | Claude Sonnet 5.5 | Claude Haiku 5.5 |
|---|---|---|---|---|
| English | 100% | 100% | 100% | 100% |
| Spanish | 99.8% | 99.3% | 99.0% | 98.1% |
| Portuguese (Brazil) | 99.4% | 99.2% | 98.7% | 98.1% |
| Italian | 99.5% | 99.2% | 98.4% | 97.7% |
| French | 99.6% | 99.3% | 98.7% | 97.7% |
| Indonesian | 99.4% | 99.1% | 98.6% | 97.1% |
| German | 99.5% | 99.1% | 98.5% | 97.4% |
| Arabic | 99.6% | 99.3% | 98.3% | 96.6% |
| Chinese (Simplified) | 99.6% | 99.3% | 98.5% | 97.7% |
| Korean | 99.6% | 99.1% | 98.3% | 97.0% |
| Japanese | 99.5% | 99.2% | 98.4% | 97.4% |
| Hindi | 99.4% | 99.2% | 98.5% | 96.6% |
| Bengali | 99.2% | 98.6% | 97.2% | 95.4% |
| Swahili | 99.2% | 98.3% | 96.9% | 94.0% |
| Yoruba | 98.5% | 96.3% | 93.6% | 87.0% |

Methodology sentences (verbatim): "The following table shows zero-shot scores for Claude models on Global-MMLU, which extends the MMLU ... test set to 42 languages. Each score is a percentage of the same model's English score." / "All scores use adaptive thinking at the max effort level. Scores exclude questions that safety classifiers blocked or that went unanswered. For each score in the table, at most 1.7 percent of the questions are excluded." / "Use the table to compare languages within a model, not to rank models. Differences of less than about 1 percentage point are within measurement noise."

Note: the page reports relative scores only. No absolute accuracies are given, so it cannot be used to derive a Polish value.

## 4. Gamayun (arXiv 2512.21580), Table 2 (EN) vs Table 19 (PL)

Source: `research/2512.21580.html` (existing), converted to text (`research/v8_img/gamayun.txt`).

Setting lines (verbatim):
- Section 5.1: "we follow the standardized protocol from the LM Evaluation Harness initiative (Gao et al., 2024) for most datasets. We use zero-shot evaluation in all cases unless explicitly stated otherwise."
- Table 2 (EN) row: "MMLU (5-shot)"; text: "MMLU (5-shot)" and "MMLU-pro (5-shot)" listed under English benchmarks.
- Section 4.1.1 / Appendix E: "Table 8 ... shows the MMLU MCF (5-shot) results", where MCF = options A-D. Table 8 instruct rows match Table 2 (Gemma3-1b-it 39.8; Llama3.2-1B-it 45.6; Qwen2.5-1.5b-it 59.7).
- Appendix H (before Table 10): "We primarily evaluated model in a zero-shot scenario using lm-harness-eval and lighteval frameworks for the evaluation." Also: "Qwen3 was evaluated in a no-thinking mode."
- Table 19 caption: "Polish language results"; row "MMLU-pl (Global)" with no shot count. Table 20 (Polish leaderboard) is explicitly "0-shot with chat template".
- Table 3 (RU): "MMLU-ru (5-shot)" row, same 5-shot label as the EN row.

Values (MMLU, %):

| Model | EN, Table 2 "MMLU (5-shot)" | PL, Table 19 "MMLU-pl (Global)" | Gap |
|---|---|---|---|
| Gemma3-1B | 39.8 | 34.75 | -5.05 |
| Llama3.2-1.24B | 45.6 | 34.8 | -10.8 |
| Gamayun-1.47B | 46.4 | 40.47 | -5.93 |
| Qwen2.5-1.54B | 59.7 | 42.00 | -17.7 |
| EuroLLM-1.66B | 28.3 | 26.25 | -2.05 |
| Qwen3-1.7B | 60.2 | 43.47 | -16.7 |

Verdict: **PARTIAL, not the same protocol as far as the paper states.** The EN row is explicitly 5-shot MCF; the PL row has no stated shot count, and the appendix says the multilingual tables were primarily zero-shot. The gaps therefore mix a language effect with a possible shot-count effect. Prompt format for Table 19 is not stated. Table 19 also uses the Global-MMLU Polish translation, not the EN MMLU set.

## 5. Search log / sources

- arXiv e-print 2412.03304 (gzip tar), extracted; figures rendered with `gs -sDEVICE=png16m -r110` (hig_res only).
- `figures/evaluation/*.pdf` text extracted with `gs -sDEVICE=txtwrite` (labels = std devs only).
- HF: `https://huggingface.co/datasets/CohereLabs/Global-MMLU/raw/main/README.md`, same for Global-MMLU-Lite.
- arXiv HTML: 2412.04261 and 2503.19786 (`/html/`).
- Anthropic: `platform.claude.com/docs/en/build-with-claude/multilingual-support` via curl.

## 6. Open questions

1. Per-language Polish and English accuracy for Global-MMLU (paper or HF per-language split) is not in any official source found. Measuring it would require running a model on the HF split `pl` and `en`.
2. Gamayun: ask the authors whether Table 19 used 0-shot or 5-shot MCF.
3. Anthropic page gives no absolute numbers and no Slavic languages. A Claude-specific Polish value is not available from an official source.
