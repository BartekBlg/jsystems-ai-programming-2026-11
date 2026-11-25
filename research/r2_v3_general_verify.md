# R2 / V3 - Polish vs English on general-knowledge benchmarks (verification, iteration 2)

Date: 2026-11-24. Scope: Global-MMLU, official model docs, Qwen3 / Gamayun / Bielik v3 / EU21 / INCLUDE.
PDFs were downloaded to `scratchpad/dl/{gmmlu,qwen3,eu21,include,bielik3,gamayun}/paper.pdf` and extracted with `tools/pdf2txt.py`.

## 1. Summary

- **Global-MMLU (arXiv 2412.03304) does include Polish, but the per-language accuracies are not in the extracted text.** Polish is in the high-resource group (Sec 4.1 / Table 5). Per-language accuracies appear only in bar charts (Figs 9-11, averaged by resource tier) and as rank-change tables (Tables 2-4, 6-7). No PL or EN accuracy number could be read. Status: UNVERIFIED.
- **Anthropic's official multilingual page (Global-MMLU, zero-shot, % of each model's own English score) does NOT list Polish or any Slavic language.** It shows 15 of the 42 languages for Claude Fable 5.2, Opus 5.5, Sonnet 5.5 and Haiku 5.5. The lowest rows are Yoruba (87.0% for Haiku 5.5) and Swahili, and the docs say differences under about 1 point are noise.
- **The only parallel PL-vs-EN pairs found in this pass come from Gamayun (arXiv 2512.21580, Tables 2 vs 19), for small models (1-2B).** The Polish gap ranges from -2 (EuroLLM) to -18 points (Qwen2.5-1.5B). The English and Polish runs are in the same paper but are not strictly the same evaluation setup.
- **INCLUDE (Polish present) is not parallel to English.** Its only per-language numbers are for GPT-4o (Polish 78.80 avg, 5-shot). Claude and Gemini are not evaluated in the paper.
- **Bielik 11B v3 (arXiv 2601.11579, v1 dated 30 Dec 2025, title confirmed) reports Polish and English suites on different task sets.** It gives no valid PL-EN gap.
- **Net for the general-knowledge question:** no 2025-2026 frontier model with a parallel Polish-vs-English MMLU-style number was found in an official source. Anthropic's docs show Slavic-free, near-parity (98-100%) numbers for the languages they list. Polish is not among them.

## 2. Findings table

| Benchmark | Model | EN | PL | other Slavic | gap | metric | source + location | status |
|---|---|---|---|---|---|---|---|---|
| Global-MMLU (42 langs; PL in high-resource tier) | all models in paper (GPT-4o, Claude Sonnet, Gemini, Llama-3.1, Qwen2.5, Aya Expanse, etc.) | not extracted | not extracted | not extracted | n/a | accuracy % (CA/CS subsets) | Cohere, arXiv 2412.03304v2, Figs 9-11 (bar charts only), Table 5 (language list), Sec 4.1-4.2 | UNVERIFIED (numbers not in text layer) |
| Global-MMLU rank changes | Aya Exp., CommandR+, Gemma2, Llama-3.1, Mistral Nemo, Qwen2.5, SEA-LION, GPT-4o, Claude Sonnet | n/a | rank-change only (Polish row in Table 6) | n/a | n/a | rank change CA vs CS | same, Tables 2-4, 6-7 | VERIFIED as rank data only; not accuracy |
| Anthropic docs, Global-MMLU zero-shot | Claude Fable 5.2 | 100% | not listed | not listed | n/a | % of own EN score | platform.claude.com/docs/en/build-with-claude/multilingual-support (table "Performance data"; redirect from docs.anthropic.com and docs.claude.com) | PARTIAL (WebFetch rendition of the page; Polish absent, verified absence in 15 listed rows) |
| same | Claude Opus 5.5 | 100% | not listed | not listed | n/a | same | same | PARTIAL (same) |
| same | Claude Sonnet 5.5 | 100% | not listed | not listed | n/a | same | same | PARTIAL (same) |
| same | Claude Haiku 5.5 | 100% | not listed | not listed | n/a | same | same; lowest listed: Yoruba 87.0%, Swahili 94.0%, Bengali 95.4% | PARTIAL (same) |
| Artificial Analysis Global-MMLU-Lite (leaderboard) | Gemini 3.1 Pro Preview (top, 93.2% avg) | not shown | not shown | not shown | n/a | accuracy | artificialanalysis.ai/evaluations/global-mmlu-lite | UNVERIFIED for per-language; chart legend lists 16 languages with NO Polish, Czech, Russian, Ukrainian or Serbian |
| Gemini 3 model card | Gemini 3 family | n/a | not found | n/a | n/a | n/a | search only; deepmind.google model-card pages not opened | NOT FOUND |
| Qwen3 tech report (2025-05-15) | Qwen3 base and instruct (0.6B-235B) | n/a | n/a | n/a | n/a | MMMLU (14 langs, no PL), INCLUDE (44 langs incl. pl) | arXiv 2505.09388, Sec 3 (eval list), Table 10 (language list), Tables 4, 12 (aggregates only) | NO PL-EN PAIR: Polish is in INCLUDE's language list but only aggregate scores are reported |
| Gamayun tech report (arXiv 2512.21580, Dec 2025), MMLU English vs Polish (same paper) | Gemma3-1B | 39.8 | 34.75 | RU 34.5 (gap -5.3) | PL -5.05 | MMLU 5-shot acc % | Table 2 (MMLU 5-shot row) vs Table 19 (MMLU-pl (Global)) and Table 3 (MMLU-ru) | PARTIAL (same paper; EN and PL evals may differ in setup; sizes 1-2B) |
| same | Llama3.2-1.24B | 45.6 | 34.8 | RU 35.9 (-9.7) | PL -10.8 | same | same | PARTIAL |
| same | Qwen2.5-1.54B | 59.7 | 42.00 | RU 47.5 (-12.2) | PL -17.7 | same | same | PARTIAL |
| same | Qwen3-1.7B | 60.2 | 43.47 | RU 50.3 (-9.9) | PL -16.7 | same | same | PARTIAL |
| same | EuroLLM-1.7B | 28.3 | 26.25 | RU 27.1 (-1.2) | PL -2.05 | same | same | PARTIAL |
| same | Gamayun-1.47B (authors' model) | 46.4 | 40.47 | RU 41.2 (-5.2) | PL -5.9 | same | same | PARTIAL |
| EU21 / "Towards Multilingual LLM Evaluation for European Languages" (arXiv 2410.08928v2, 17 Oct 2024; Thellmann et al.) | Llama-3.1-70B-Instruct | avg .79 (ARC .80, GSM8K .87, HellaSwag .86, MMLU .82, TQA .60) | avg .70 (ARC .72, GSM8K .69, HeSw .74, MMLU .78, TQA .59) | CS .71 (MMLU .78); SK .69; BG .66; SL .67 | PL: avg -9; MMLU -4; ARC -8; GSM8K -18; HeSw -12; TQA -1 | accuracy (fraction) | Table 1 (p.6), raw PDF text | VERIFIED for this one model. Only one per-model per-language table exists; Tables 12-13 are 21-language averages with no EN |
| INCLUDE (arXiv 2411.19799), regional exam QA | GPT-4o, 5-shot (only model with per-language rows) | not a parallel comparison (no EN exam set) | 78.80 (avg over Polish samples) | UK 85.60, BG 90.70, SR 91.60, MK 92.40, HR 88.40, RU 75.00, BE 50.90 | n/a | accuracy % | Table 6 (p.21-22), Table 9-11 (per-subject) | VERIFIED (numbers from text layer). Claude / Gemini not evaluated in INCLUDE |
| Bielik 11B v3 (arXiv 2601.11579v1, 30 Dec 2025) | Bielik-11B-v3.0-Instruct | Open LLM (EN) avg 72.45, MMLU 71.11, ARC 64.59 (Table 11) | Open PL avg 65.93 (Sec 5.10 text, Table 4/5 suite) | INCLUDE-base-44, 20 EU langs avg 64.8 (Table 12) | not valid: different task suites | accuracy | Tables 11, 12, Sec 5.6-5.10 | VERIFIED numbers; NOT a parallel gap |
| Bielik 11B v3 (same) | Bielik-11B-v3-Instruct on INCLUDE-base-44 | n/a | 69.0 | avg 64.8 (20 langs) | n/a | accuracy | Table 12, Sec 5.7 | VERIFIED (text layer) |
| OpenAI MMMLU (simple-evals) | GPT-4.1, o3, o4-mini etc. | n/a | not in list (14 langs: AR, BN, DE, ES, FR, HI, ID, IT, JA, KO, PT-BR, SW, YO, ZH) | none | n/a | accuracy | openai/simple-evals (carried from r1_a2; not re-fetched in this pass) | VERIFIED negative (no PL) in r1 pass |
| Llama 4 / Mistral 3 official model cards | Llama 4 Scout/Maverick, Mistral Small/Large | n/a | not found | not found | n/a | n/a | Llama 4 card: English MMLU only (Scout 79.6, Maverick 85.5); Mistral Small 24B base card: FR/DE/ES/RU/ZH/KO/JA MMLU, no PL | NOT FOUND (no per-language PL row) |

## 3. Details per source

**Global-MMLU (arXiv 2412.03304v2, ACL 2025).** Polish is in the 42-language set and the high-resource tier (Sec 4.1). The paper reports CA and CS subsets separately (Figs 9-11, averaged over high, mid and low resource tiers). The text layer has no per-language accuracy for Polish or English; the bar-chart values were not readable, and I did not render the PDF pages to images (no renderer installed). Table 6 (Polish row) gives rank changes only. Next step if needed: read Figs 9-11 visually, or load CohereLabs/Global-MMLU from HuggingFace and recompute.

**Anthropic multilingual support (platform.claude.com, the current home of the docs).** Table "Performance data": zero-shot Global-MMLU scores as % of the same model's English score, with adaptive thinking at max effort; scores exclude questions blocked by safety classifiers (at most 1.7% excluded). The 15 languages listed are Spanish, Portuguese (BR), Italian, French, Indonesian, German, Arabic, Chinese (Simplified), Korean, Japanese, Hindi, Bengali, Swahili, Yoruba (and English at 100%). Polish, Czech, Russian, Ukrainian, Serbian and Bulgarian are not listed. Caveat: the content was read through WebFetch, which returns a model-written rendition of the page. The table values look verbatim, but the raw page was not diffed.

**Qwen3 (arXiv 2505.09388, 15 May 2025).** Multilingual benchmarks: MMMLU (14 languages, no pl), INCLUDE (44 languages, pl included), MGSM, MT-AIME2024, PolyMath, MLogiQA (Table 10). Results are aggregates only (for example INCLUDE 75.6 and MMMLU 79.8 for Qwen3-235B-A22B non-thinking, Table 12). No Polish-specific score is reported.

**Gamayun (arXiv 2512.21580).** Table 2 (English benchmarks) and Table 19 (Polish, "MMLU-pl (Global)" from the Global-MMLU Polish translation) cover the same six models at 1-2B size. The EN MMLU row is "MMLU (5-shot)"; the paper's MMLU MCF protocol is described around Table 8. Table 3 gives MMLU-ru (5-shot) for the same models. Because EN and PL come from different sections, the setup may differ slightly, so treat these as PARTIAL. Gap sizes are large for Qwen2.5 and Qwen3 (-17 and -17 points) and small for EuroLLM (-2). This is the only within-paper PL-EN pair for general MMLU-style knowledge that I found for 2025 models.

**EU21 (arXiv 2410.08928v2).** Table 1 lists accuracies per language for Llama-3.1-70B-Instruct, with the English row as the reference. Polish rows are translations (EU20 benchmark set), so the English and Polish items are not identical. Sec 4.3 reports family averages: Germanic 0.745, Romance 0.726, Slavic 0.680 for Llama-3.1-70B; Gemma-2-27B 0.727 / 0.716 / 0.694. The paper title on arXiv is "Towards Multilingual LLM Evaluation for European Languages", not "OpenGPT-X" as stated in r1. Only one model has per-language rows in the main text; Appendix Tables 12-19 average over 21 languages.

**INCLUDE (arXiv 2411.19799).** Polish is included (Table 8 metadata: latin, Indo-European, Slavic, West, high-resource). The evaluated models are GPT-4o, Llama-3.1, Aya-expanse, Qwen2.5, Mistral and Gemma. Claude and Gemini are not evaluated. Per-language rows (Table 6, GPT-4o 5-shot) are the only per-language numbers; Polish 78.80, Ukrainian 85.60, Serbian 91.60, Bulgarian 90.70, Russian 75.00. Because INCLUDE is native regional exam content, there is no English counterpart, so it cannot give a parallel gap.

**Bielik 11B v3 (arXiv 2601.11579v1).** Title "Bielik 11B v3: Multilingual Large Language Model for European Languages", authors Ociepa, Flis, Kinas, Wróbel, Gwoździej; submitted 30 Dec 2025 (arXiv citation_date 2025/12/30). Open PL and Open LLM (EN) are different suites, so the report does not support a PL-vs-EN gap. The English Open LLM v1 table is Table 11; INCLUDE-base-44 is Table 12.

**Anthropic, OpenAI, Google, Meta, Mistral.** Anthropic: see above. OpenAI MMMLU has no Polish (r1 check, not re-fetched). Google: the Gemini 3.1 Flash-Lite model card links to a methodology page; no per-language Polish numbers were found in search snippets. Artificial Analysis Global-MMLU-Lite: the chart legend has no Polish. Meta: Llama 4 card gives English MMLU only. Mistral: the Small 24B base card has no Polish row.

## 4. Search log (this pass)

- Downloads: arXiv PDFs 2412.03304, 2505.09388, 2410.08928, 2411.19799, 2601.11579, 2512.21580 (extracted with pdf2txt.py via venv python -I).
- WebFetch: platform.claude.com multilingual-support page (after redirects from docs.claude.com and docs.anthropic.com); artificialanalysis.ai global-mmlu-lite.
- WebSearch: "Gemini 3 model card multilingual MMLU per-language Polish"; "Global-MMLU per-language accuracy table ... 2025 arXiv"; "Qwen3 OR Llama 4 OR Mistral technical report per-language MMLU Polish".
- Dead ends: Global-MMLU appendix accuracies (figures only); no PDF renderer installed (no pdftoppm, mutool or PyMuPDF), so chart values cannot be read; Gemini model card per-language tables not found; Llama 4 and Mistral per-language tables not found.

## 5. Open questions

1. Global-MMLU per-language values for pl and en: render Figs 9-11 or pull CohereLabs/Global-MMLU results (install pymupdf in the venv to render pages).
2. Anthropic table: confirm that Polish is absent from the full 42-language table (only 15 shown on the page).
3. Gamayun EN vs PL setups: confirm the MMLU protocol in Table 2 matches Table 19 (see Table 8 MCF description).
4. Recent (2025-2026) frontier-model PL-EN pairs on a MMLU-style set are still missing from official sources; INCLUDE and Global-MMLU per-language for Claude or Gemini would be the closest evidence.
