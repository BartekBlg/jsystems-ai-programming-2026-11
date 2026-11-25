# R2 V1: Independent verification of OneRuler (arXiv 2503.01996) data

Sources used (all primary, downloaded with curl into empty dirs; no archive content was executed):
- arXiv LaTeX source: `dl_oneruler/src.tar.gz`, extracted to `dl_oneruler/ex/`. Contains `colm2025_conference.tex` and `sections/*.tex`.
- arXiv PDF v3: `dl_oneruler/pdf/paper.pdf` (10 pages main text plus appendix). Text extracted to `dl_oneruler/pdf/paper.txt` with the brief's `tools/pdf2txt.py` (pypdf, venv).
- Figures rendered with ghostscript to `dl_oneruler/render/` (bar_langs.png, by_model.png, xling.png) and read visually.

Note: the BRIEF.md file changed during this run and gained a "PDF extraction" section. I followed it for the text extraction step only.

## 1. Per-language table (Figure 4b, 64K and 128K averaged, NIAH tasks)

Source: Figure 4(b) in the PDF (bar chart `figures/new/bar_langs.pdf`). Values are read from bar heights against the y-axis (about 7.2 px per percentage point), so they are approximate (about plus or minus 0.5 pp). Text-stated values are exact.

| Rank | Lang | Acc % (approx.) | Group |
|---|---|---|---|
| 1 | pl | 88.0 (text, exact) | High |
| 2 | ru | ~86.0 | High |
| 3 | fr | ~85.7 | High |
| 4 | it | ~84.9 | High |
| 5 | es | ~84.5 | High |
| 6 | en | 83.9 (text, exact) | English |
| 7 | uk | ~83.0 | High |
| 8 | sv | ~82.6 | High |
| 9 | pt | ~82.0 | High |
| 10 | de | ~80.4 | High |
| 11 | no | ~80.0 | High |
| 12 | nl | ~77.6 | High |
| 13 | hu | ~77.3 | High |
| 14 | da | ~77.2 | High |
| 15 | ja | ~75.7 | High |
| 16 | cs | ~73.5 | High |
| 17 | vi | ~73.3 | High |
| 18 | fi | ~73.2 | High |
| 19 | fa | ~71.7 | High |
| 20 | sr | ~71.3 | High |
| 21 | hi | ~65.9 | Low |
| 22 | ko | ~65.2 | High |
| 23 | zh | 62.1 (text, exact) | High |
| 24 | sw | ~60.5 | Low |
| 25 | ta | ~54.3 | Low |
| 26 | st | ~44.0 | Low |

Bottom 5: ko, zh, sw, ta, st. Chinese is 23rd, so it is 4th worst, as the text says. All four low-resource languages (hi, sw, ta, st) sit in ranks 21 to 26, which matches the "bottom 6" statement.

## 2. Verification of the headline numbers

- Polish 88% (rank 1): CONFIRMED. Text (Sec. 3, "Results", p. 7): "average accuracy of 88% across all models", 64K and 128K. The bar chart agrees (about 88.5 by pixel).
- English 83.9% (rank 6): CONFIRMED. Same paragraph and abstract ("ranked 6th out of 26").
- Chinese 62.1%: CONFIRMED. Text: "4th worst" with 62.1%. The setting is the 64K and 128K average over all models and NIAH tasks, not a single model.
- Correction to the previous agent: the body text says "Llama 3.1 7B" in one place. The model comparison and figures say Llama 3.1 8B. Use 8B.

## 3. Per-model results (Polish vs English, 128K)

- NOT FOUND as a Polish-vs-English per-model table at 128K. Figure 4(a) (`by_model_performance.pdf`) gives only 64K and 128K averages per model for three groups (English, high-resource, low-resource). Approximate bar values:

| Model | English | High-res | Low-res |
|---|---|---|---|
| Gemini 1.5 Flash | ~99 | ~95 | ~88 |
| Qwen 2.5 72B | ~95 | ~91 | ~68 |
| o3-mini-high | ~76 | ~78 | ~60 |
| Llama 3.3 70B | ~80 | ~69 | ~45 |
| Llama 3.1 8B | ~78 | ~63 | ~30 |
| Qwen 2.5 7B | ~75 | ~68 | ~36 |

- Polish-specific model numbers come only from the text (Sec. 3, "Individual model performance"): o3-mini-high at 128K gets 67% on English, 92% on Polish, and 89% on Ukrainian. CONFIRMED.
- Gemini 1.5 Flash and Qwen 2.5 72B are the only models that do well at 128K (text, Sec. 3).

## 4. Cross-lingual result (instruction language vs context language)

Source: Figure 6(a) (`xling_by_len.pdf`), averaged over NIAH tasks and models, rows = context language, columns = instruction language.

| Context | Instr | 8K | 32K | 64K | 128K |
|---|---|---|---|---|---|
| en | en | 97 | 97 | 91 | 77 |
| en | ko | 83 | 75 | 71 | 61 |
| en | pl | 90 | 84 | 82 | 66 |
| ko | en | 86 | 79 | 75 | 64 |
| ko | ko | 73 | 71 | 67 | 59 |
| ko | pl | 79 | 70 | 67 | 58 |
| pl | en | 91 | 89 | 85 | 74 |
| pl | ko | 74 | 74 | 72 | 63 |
| pl | pl | 90 | 94 | 91 | 86 |

- Polish context: Polish instructions are best (86 at 128K), with English next (74). Korean instructions are worst (63).
- English context: English instructions are best at every length. Switching to Korean drops it from 91 to 71 at 64K. This matches the text.
- Korean context: the text says that at 128K switching instructions from Korean to English raises accuracy 61% to 77%. The heatmap does not show that: the Korean-context row gives 59 (Korean instr) and 64 (English instr). The 61 and 77 values appear in the English-context row (en context, ko instr 61, en instr 77). This is an internal inconsistency in the paper (or a row-label mix-up). Flag it.
- Overall: the instruction language matters most in English contexts and Korean contexts. For Polish contexts, Polish instructions win.

## 5. Degradation curve 8K to 128K

- Text: the gap between the top-5 and bottom-5 languages by Wikipedia size grows from 11% at 8K to 34% at 128K. CONFIRMED (Sec. 3).
- English-vs-Polish curve across 8K to 128K: NOT FOUND as numbers in the text. The Figure 3 heatmap gives per-model values per language and context length, not an English vs Polish series. A rough English-only trend is available only from the xling figure above (en context, en instr: 97, 97, 91, 77). Treat that as partial and not directly comparable.
- Text gives one English-specific aggregate: average English CWE-easy accuracy is 31.5%, over all models.

## 6. Venue

- CONFIRMED (primary PDF header and LaTeX template): every page header says "Published as a conference paper at COLM 2025", and the source uses `colm2025_conference.sty`. Not checked: the OpenReview or COLM accepted-papers listing (I did not search it in this run). Acceptance is therefore inferred from the template, which is strong evidence.

## 7. Authors' explanations (paraphrased; exact sentence locations given)

- Conclusion (Sec. 6): the gaps are hypothesised to stem from pretraining data availability, script, language family, and tokenizer specifications. Short phrase: "stem from factors such as pretraining data availability, script, language family, and tokenizer specifications."
- Sec. 3 "English and Chinese are not the highest-performing languages": they say it "remains unclear" why high-resource Chinese does worse. They note that all four low-resource languages rank in the bottom six, and that the top 10 are Slavic, Romance, and Germanic languages, all with large Wikipedia size and Latin scripts (Wikipedia-size link, Figure 7).
- Footnote 12: Chinese errors are mainly wrong "none" answers, especially from o3-mini-high.
- Appendix D (information-content-controlled analysis, `appendix.tex` ~line 609): with token-count controlled, Slavic languages (Russian, Ukrainian, Polish) "perform highly", above Romance languages such as Italian, French, and Spanish. English is "typically 8th". Korean, Hindi, and Chinese are at the low end. Footnote: attributed partly to newer tokenizer gains (cl100k_base vs o200k_base).
- The paper does not claim Polish is the best language for prompting. It does not study prompting.

## Status summary

- Polish 88% rank 1: CONFIRMED.
- English 83.9% rank 6: CONFIRMED.
- Chinese 62.1% (4th worst, rank 23): CONFIRMED.
- Full 26-language table: PARTIAL (bar heights, approx. plus or minus 0.5 pp; the 3 exact values above are from text).
- Per-model results at 128K Polish vs English: PARTIAL (only text for o3-mini; others read from Fig. 4a at 64K and 128K averages by resource group).
- Cross-lingual: CONFIRMED with one inconsistency (Korean-context 61 to 77 in text vs heatmap).
- Degradation: PARTIAL (gap 11% to 34% confirmed; English vs Polish curve NOT FOUND).
- Venue COLM 2025: CONFIRMED from the paper header (OpenReview not checked).
- Authors' explanations: CONFIRMED (sections and appendix listed above).
- Correction: Llama 3.1 is 8B, not 7B (body typo).
