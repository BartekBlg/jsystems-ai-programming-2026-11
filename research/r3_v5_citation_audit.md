# R3 V5 - Citation audit and OneRuler replication search

Date of audit: 2026-11-24. Sources: arXiv export API (metadata), arXiv HTML (tables), PDF via pdf2txt where HTML was unavailable.

## PART A - Citation audit

| ID | Exists | Real title / first author / date | Claim verified? | Corrections |
|---|---|---|---|---|
| 2601.11579 | Yes | "Bielik 11B v3: Multilingual Large Language Model for European Languages", Ociepa, Flis, Kinas; 2025-12-30 | Title and report match. Belebele 82.98 (Table 13, 28-language subset) confirmed in PDF. Its own tokenizer Table 2 has different rows (EuroLLM 1.88/1.27, Qwen3 2.69/1.17, Mistral 7B v0.2 3.22/1.28, APT4 1.62/1.98) but NO Llama 3.2 or Gemma3 rows. | Tokenizer table is in this report too, with Mistral 7B v0.2 instead of "Bielik original" label. Llama2 row exists (2.94/1.34). |
| 2604.10799 | Yes | "Advancing Polish Language Modeling through Tokenizer Optimization in the Bielik v3 7B and 11B Series", Ociepa, Flis, Kinas; 2026-04-12 | Table 1 numbers all confirmed: APT4 1.62/1.98; Bielik 11B v3 3.22/1.28; Llama 3.2 2.81/1.17; Qwen3 2.69/1.17; Gemma3 2.20/1.20; EuroLLM 1.88/1.27. | Belebele claim is WRONG as stated. Table 8 shows Bielik-11B-v3.0-Instruct AVG 82.98 and a DIFFERENT model, Bielik-PL-11B-v3.0-Instruct, AVG 77.41 (Polish-specialised variant). It is not a before/after. Previous version Bielik-11B-v2.6-Instruct = 68.67. |
| 2505.02550 | Yes | "Bielik v3 Small: Technical Report", Ociepa, Flis, Kinas; 2025-05-05 | PARTIAL. Table 2 contains APT3, APT4, Mistral v0.1 (3.22/1.28), APT4 1.62/1.98 and a row for "Qwen2.5" with 2.69/1.17. | Does NOT contain the Llama 3.2, Gemma3 or EuroLLM rows. Its Qwen row is labelled Qwen2.5 (vocab 151665), while 2604.10799 labels the identical numbers Qwen3 (vocab 151669). One label is wrong; treat the Qwen row as unresolved. |
| 2605.24718 | Yes | "The Tokenizer Tax Across 25 European Languages: Domain Invariance, Cross-Lingual Few-Shot Effects, and the Ukrainian Penalty", Volodymyr Ovcharov; 2026-05-23 (single author, not a team) | Polish 2.25 confirmed in Table 4 (mean; min 1.88, max 2.60). English 1.23 and Ukrainian 2.66 confirmed in the text (Experiment 2 and contributions list), not as table rows. | Use the text for EN/UK. Table 4 is a mean over 6 tokenizers on SIB-200, so these are not per-model numbers. |
| 2608.20853 | Yes | "MGAL: A Multilingual Granularity-Aware Long-Context Benchmark", Chunhan Li, Chenglin Xu, Zongyang Zhang; 2026-08-21 | Confirmed: six UN languages (Arabic, Chinese, English, French, Russian, Spanish), 8K-128K contexts. No Polish. | None. Note: Polish is absent. |
| 2603.10640 | Yes | "Making Bielik LLM Reason (Better): A Field Report", Adam Trybus, Bartosz Bartnicki, Remigiusz Kinas; 2026-03-11 | Exists and is a Bielik-R reasoning report. Its benchmarking section was not number-checked. | Numbers UNVERIFIED. |
| 2601.05366 | Yes | "Lost in Execution: On the Multilingual Robustness of Tool Calling in Large Language Models", Zheng Luo et al.; 2026-01-08 | Multilingual tool-calling study confirmed. The acronym "MLCL" does not appear in the text checked. Polish is not among the languages in the text checked (Chinese and Hindi are named). | Acronym unverified; no Polish. |
| 2512.21580 | Yes | "Gamayun's Path to Multilingual Mastery: Cost-Efficient Training of a 1.5B-Parameter LLM", Alexander Podolskiy et al.; 2025-12-25 | Exists. Trained on 12 languages including Polish; has a Russian-focused evaluation. | No numbers checked. |
| 2504.12845 | Yes | "Can LLMs reason over extended multilingual contexts? Towards long-context evaluation beyond retrieval and haystacks", Amey Hengle et al.; 2025-04-17 (later EACL 2026 per OpenAlex) | Table 1 gives only averages across languages, not per-language results. "English highest at 4k and at >=32k" NOT verified: I did not extract the per-language rows. | Status UNVERIFIED for that claim. Polish not in the text checked. |
| 2305.15425 | Yes | "Language Model Tokenizers Introduce Unfairness Between Languages", Petrov, La Malfa, Torr, Bibi; 2023-05-17 | Appendix tokenizer premium table (PDF) confirmed: Polish 1.91 (cl100k_base) and 1.70 (LLaMA); Czech 2.11; Russian 2.49; Ukrainian 3.00. | None. |
| 2412.00559 | Yes | "Polish-English medical knowledge transfer: A new benchmark and results", Grzybowski, Pokrywka, Ciesiółka, Kaczmarek; 2024-11-30 | Table 4 (column order LEK, LEK(en), LDEK, LDEK(en)). Values: GPT-4o PL 89.96 / EN 88.69; Llama-3.1-70B PL 80.94 / EN 82.60; Bielik-11B-v2.2 PL 62.36 / EN 56.98; Qwen2.5-7B PL 51.92 / EN 67.73. All four match the claim. | None. Note that Qwen2.5-7B and Llama-3.1-70B are EN > PL. |

## PART B - OneRuler (arXiv 2503.01996) follow-ups

OpenAlex work ID for OneRuler: W4415337897 (32 citations at time of query). Citing works list retrieved via `filter=cites:W4415337897`. Relevant multilingual or long-context items:

- 2504.12845 (Hengle et al., "Can LLMs reason over extended multilingual contexts?", EACL 2026). Languages include Arabic and Hindi, not Polish. Table 1 gives only averages. Per-language numbers NOT extracted.
- 2608.20853 (MGAL). Six UN languages, no Polish, no per-language numbers extracted.
- 2601.05366 (MLCL tool-calling). No Polish, not a long-context study.
- 2609.35378 "Multilinguality in Hybrid Attention LLMs". Covers Russian, Hindi, Greek, Irish, Chinese, English; no Polish; KL and LM loss by language, not a long-context accuracy result.
- 2606.06203 "Dense Contexts Are Hard Contexts". No Polish or Slavic language mentioned.
- 2609.17553 "The Limits of BPE Tokenization in Polish" (OpenAlex-listed, not opened).

Conclusion: no citing paper found that reports long-context per-language accuracy for Polish against English. The "Polish >= English in long context" claim is NOT confirmed or contradicted by any OneRuler follow-up checked. The only Polish-English comparison in this set is the medical exam table (2412.00559), which is not a long-context study. Coverage was limited: OpenAlex returned 32 citers, of which I screened titles and opened 4-5.

## Notes
- Recurring risk: several Bielik-team reports share tables with mislabelled rows (Qwen2.5 vs Qwen3). Cite the specific paper and table, not "the Bielik table".
- The Belebele "82.98 -> 77.41" claim should be replaced by "Bielik-11B-v3.0-Instruct 82.98 vs Bielik-PL-11B-v3.0-Instruct 77.41 (2604.10799 Table 8)".
