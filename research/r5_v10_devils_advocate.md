# R5 V10 (devil's advocate): evidence that could contradict or nuance the project conclusion

Method note: WebFetch returns a model-summarised page, so numbers below are marked as "fetch-extracted" unless read from the PDF text. Status scale as in BRIEF.md.

## Summary
- Nothing found that replicates or refutes OneRuler's Polish result in 2026. No 2026 long-context multilingual benchmark with Polish was found (no "OneRuler v2", no "PolLongBench").
- The strongest contradicting evidence is about **Slavic languages below English**, not above it: CUS-QA (arXiv 2507.22752) shows English answers beating Czech/Slovak/Ukrainian local-language answers for several models on regional questions.
- The strongest nuance for "native language beats English" is culture-specific knowledge access (arXiv 2606.07422, June 2026): English wins on culture-agnostic items, local language wins on local-knowledge access after proficiency adjustment. Polish is one of 13 locales there.
- Reasoning evidence (2025) still points to English-reasoning advantage; only Russian of the Slavic languages is covered.
- Polish-specific prompt-language experiments: none found. The OneRuler authors (per press) say the result is not about prompting.

## Findings

| # | Candidate | What was tested | Numbers | Models | Primary? | Status |
|---|---|---|---|---|---|---|
| 1 | OneRuler (arXiv 2503.01996, COLM 2025) | NIAH/aggregation long context, 26 langs | Pl 88.0 vs En 83.9 (64K/128K avg, text p.7). Instruction-language effect: En/Pl/Ko contexts, switching instruction to lower-resource language drops accuracy (Fig. 6a, "significantly impacts"; up to 20% per search summaries) | o3-mini-high, Gemini 1.5 Flash, Qwen2.5, Llama 3.x | Yes (paper, code mungg/OneRuler) | VERIFIED earlier (r2_v1). Note: paper text has no formal significance test; "not statistically significant" claim comes only from press quoting Karpińska. Different book per language (Pl: Nights and Days vol. 3; En: Little Women) is a confound the paper's appendix table confirms. |
| 2 | CUS-QA (Libovický et al., arXiv 2507.22752 v2, Aug 2025) | Regional QA, local language (cs/sk/uk) vs English translation | Table 11, textual QA, en minus local: CZ EuroLLM +(−3.8), Llama-3.1-8B +2.8, Llama-3.3-70B −1.3; SK Llama-3.1-8B +6.9, Mistral-7B +9.1; UA Llama-3.1-8B +11.4, Mistral-7B +10.7, Llama-3.3-70B −0.2. Visual QA gaps −2.5 to +4.9 | EuroLLM-9B, Llama 3.1-8B/3.3-70B/4-Scout, Mistral-7B; VLMs gemma3, idefics, maya | Yes (arXiv, not peer-reviewed in that version; a later TACL 2026 version was reported in search, not opened) | PARTIAL: table read via fetch summary; re-check PDF. Key contradiction: English beats the local Slavic language in many cells. Not Polish. |
| 3 | Masked Advantage (Zhang et al., arXiv 2606.07422, June 2026) | 13 locales (Polish is the only Slavic one), ~80 models, IRT separation of proficiency vs local knowledge | English advantage on culture-agnostic questions (consistent, all models/locales; no exact figure in text). Level-3 group (HI, FA, KO, PL, NL): GlobalGap −0.76, LocalGap −0.07, KnowledgeGap +0.68 (IRT logits). Bielik-Minitron-7B PL: Local Adapt +0.64 (Δ +1.16) — column order ambiguous | ~80 models incl. Bielik | Yes (arXiv v2) | PARTIAL: Polish only in aggregated group and one adapted model; per-locale values are in figures. Nuance: after proficiency adjustment, local-language knowledge access is slightly better. |
| 4 | Towards Multilingual LLM Evaluation for European Languages (arXiv 2410.08928, Oct 2024) | 5 translated benchmarks (ARC, HellaSwag, MMLU, TruthfulQA, GSM8K), 20 EU langs | Llama-3.1-70B-Instruct: English avg 0.79, Polish 0.70, Czech 0.71; family avg Slavic 0.680 vs Germanic 0.745 (Gemma-2-27B: 0.694 vs 0.727) | Llama-3.1-70B, Gemma-2-27B | Yes | PARTIAL (fetch-extracted). Older and translation-based, but supports "Slavic below English". |
| 5 | Reasoning Lingua Franca (arXiv 2510.20647, Oct 2025) | English vs native-language reasoning, MGSM + GPQA | Per-language accuracy only in figures; text gives LiT fractions only. Slavic coverage: Russian only | Qwen3-32B, QwQ-32B | Yes | PARTIAL; supports English-reasoning advantage, not Polish. |
| 6 | Long CoT Across Languages (arXiv 2508.14828, Aug 2025) | En-CoT vs target-CoT, 9 languages | Not opened in detail; abstract-level: target-language CoT trails English; no Slavic in search snippet | Various | Yes | UNVERIFIED numbers |
| 7 | Is Translation All You Need? (arXiv 2403.10258, 2024) | Native vs translate-test prompting | Native prompting better on culture-related tasks (abstract-level only) | GPT-3.5/LLaMA-class | Yes | UNVERIFIED numbers; old. |
| 8 | Disentangling Language and Culture (Ying et al., arXiv 2505.24635, ACL 2025), "Cultural-Linguistic Synergy" | Culture-matched language vs English | Indonesian culture in Indonesian +15.7 pts over English (text, Sec. 3.3, avg 8 models); Chinese +8.8; Persian −0.95. **No Slavic language in the study** (en, zh, es, id, ko, fa, su) | 8 models incl. Llama-3-8B/70B, Qwen2.5 | Yes | PARTIAL (fetch-extracted text) |
| 9 | Bielik 11B v3 (arXiv 2601.11579) and PLLuM / gender-inclusive Polish prompting (arXiv 2508.18466) | Polish prompt in PL vs EN for gender-inclusive generation | Prompt-language outcome not stated in abstract | Bielik, PLLuM | Yes | UNVERIFIED for numbers; no PL-vs-EN accuracy |
| 10 | PoVisLE (arXiv 2608.07763, Aug 2026) | Polish VLM benchmark, 1,117 images, 2,366 VQA pairs | Abstract only; search summary says weaker models gain from English prompts, stronger ones from Polish, but numbers not confirmed in the fetched page | VLMs | Yes | UNVERIFIED for claims; worth reading HTML. |
| 11 | Long-context Polish encoders (arXiv 2603.12191) and Polish ModernBERT (arXiv 2609.01379) | Encoder models, Polish long documents | No PL-vs-EN prompt comparison | Encoders | Yes | Not relevant to prompt language; note only. |
| 12 | Polish media claim "Polish best for prompting" (Oct-Nov 2025: android.com.pl, benchmark.pl, spidersweb.pl, scienceinpoland.pl) | Press pointers | Press only | - | No | Pointer only. Science in Poland and spidersweb report the OneRuler team disputing the prompting claim (per secondary coverage). |

## Search log (≥12 queries, all run)
1. OneRuler replikacja polski najlepszy język long context
2. czy polski jest najlepszym językiem dla AI badanie promptowanie
3. prompting in Polish vs English experiment LLM 2025 arXiv
4. OneRuler multilingual long-context benchmark replication 2026
5. non-English prompts outperform English prompts LLM culturally specific questions native language prompting
6. Not all languages are created equal in LLMs arXiv
7. Karpińska OneRuler Polish prompting statement statistically significant
8. PolLongBench OR long-context Polish benchmark 2026 arXiv
9. Polish instructions vs English instructions LLM accuracy Bielik PLLuM prompt language comparison
10. Slavic languages LLM benchmark Polish Czech Russian English prompt language performance 2025 2026 arXiv
11. self-translate native prompting vs English prompting multilingual LLM reasoning 2025 arXiv
12. język instrukcji a jakość odpowiedzi LLM polski vs angielski eksperyment
13. MMLU-ProX OR Global-MMLU Polish English gap LLM 2025 per-language results (no Polish numbers found)
14. Polish Bielik PLLuM RULER long-context evaluation English prompts 2026 (none found)
15. multilingual reasoning language of thought English vs non-English CoT accuracy 2025 arXiv

Dead ends: no OneRuler replication or v2; no PolLongBench; no Polish-vs-English prompt study for Bielik/PLLuM with accuracy numbers; Karpińska quotes only via press (scienceinpoland.pl, notesfrompoland.com; the latter did not contain her quotes on significance).

## Open questions
- Open the CUS-QA PDF and the 2606.07422 HTML figures to get per-locale Polish numbers (Fig. 2, Appendix E).
- Check the TACL 2026 version of CUS-QA (2026.tacl-1.67, not opened).
- Check the MMLU-ProX / Global-MMLU per-language tables for Polish (not opened).
- Read PoVisLE HTML for the English-vs-Polish prompt ablation.
- Ask OneRuler authors or check the GitHub repo for a significance test; none found in paper text.
