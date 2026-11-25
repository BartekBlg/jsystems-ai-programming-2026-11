# R4 V7 - Full screen of OneRuler (arXiv 2503.01996) citers

Date: 2026-11-24. Sources: OpenAlex `cites:W4415337897` (30 records), Semantic Scholar citations (27 records, no pagination needed), union = 39 unique papers (one paper, 2605.12227, appears under two titles). V5 checked five papers (2504.12845, 2608.20853, 2601.05366, 2609.35378, 2606.06203); these are listed but not re-screened.

Note: 2601.05366 (V5) does NOT appear in either the OpenAlex or Semantic Scholar list today. Treat it as a V5 finding that the APIs did not confirm.

## 1. Counts

- Unique citers: 39 (OpenAlex 30, Semantic Scholar 27, overlap large).
- Relevant (topic: multilingual evaluation, long-context, NIAH/retrieval, RAG, tokenization, Slavic, cross-lingual reasoning): 19 incl. 1 marginal; 5 were V5-skipped; 15 not relevant.
- Relevant papers that report results for Polish: 6 (Lara Mini, Language Equality, BPE-Polish, COMPASS, JCM Polish-EN, BMC Polish-EN). One more (quantization, 2505.20276) uses OneRuler with a per-group English vs non-English split, but Polish is not named.
- Papers that run OneRuler themselves or extend it: 4 (2611.16956, 2604.20720, 2505.20276, 2603.03543). Two more cite OneRuler only as background (2504.08202, 2605.12227).

## 2. Full citer table

Relevance: Y = relevant topic; N = not; S = skipped (V5). "Polish?" = Polish results present in the paper.

| # | Title | Year | arXiv / DOI | Relevant? | Polish? | Reason (5 words) |
|---|---|---|---|---|---|---|
| 1 | Lara Mini Base: Language Diversity as a Resource, Not a Trade-off | 2026 | 2611.16956 | Y | Yes (no OneRuler per-language) | Runs OneRuler, 26 languages |
| 2 | Multilinguality in Hybrid Attention LLMs | 2026 | 2609.35378 | S | No | V5 checked |
| 3 | MGAL: Multilingual Granularity-Aware Long-Context Benchmark | 2026 | 2608.20853 | S | No | V5 checked |
| 4 | OctoLong: Mid-Training on Cross-Repository Code Contexts | 2026 | 2608.05141 | N | No | Code long-context only |
| 5 | Language Equality has a Price: Multi-turn LLM Performance for EU-24+ | 2026 | 2608.01395 | Y | Yes (pl, en) | 30-language multilingual evaluation |
| 6 | The Limits of BPE Tokenization in Polish | 2026 | 2609.17553 | Y | Yes (Polish only) | Polish tokenization analysis, no benchmark |
| 7 | Comparative Analysis of TTS and Voice Cloning for Polish | 2026 | 10.1109/HSI70218.2026.11773158 | N | Polish speech only | Speech synthesis, not LLM evaluation |
| 8 | Human vs AI emotional evaluations, GPT-4o, Polish vs English prompts | 2026 | 10.1186/s40359-026-05067-3 | Y (marginal) | Yes (PL vs EN) | Prompt-language effect, not accuracy |
| 9 | NarrativeWorldBench (audio drama) | 2026 | 2606.17391 | N | No | Narrative audio, not multilingual |
| 10 | Dense Contexts Are Hard Contexts | 2026 | 2606.06203 | S | No | V5 checked |
| 11 | XLGoBench: cross-lingual skill gaps with algorithmic tasks | 2026 | 2605.30788 | Y | No | Cross-lingual reasoning, no Polish |
| 12 | Language-Specific Differences in LLM Diagnostic Reasoning (vignettes) | 2026 | 10.3390/jcm15114082 | Y | Yes (PL vs EN) | Polish-English reasoning comparison |
| 13 | A Recipe for Long-Context Reasoning via On-Policy Optimization and Distillation (also listed as "Combining On-Policy...") | 2026 | 2605.12227 | Y | No | Long-context; OneRuler only for decontam |
| 14 | Unveiling Language Routing Isolation in Multilingual MoE Models | 2026 | 2604.03592 | Y | No (no Slavic) | Multilingual MoE, no Slavic |
| 15 | Multilingual LLMs do not comprehend all natural languages to equal degrees | 2026 | 2602.20065 | Y | No data (12 langs, no PL) | Cites OneRuler's Polish claim |
| 16 | KV Admission: long-context inference | 2025 | 2512.17452 | N | No | KV-cache efficiency |
| 17 | English-Czech Output Bias in LLMs | 2025 | 10.34190/icair.5.1.4326 | Y | No (Czech vs EN) | Slavic vs English comparison |
| 18 | Quantification and object perception in MLLMs | 2025 | 2511.08126 | N | No | Linguistics/vision, not multilingual eval |
| 19 | Code Like Humans: medical coding | 2025 | 2509.05378 | N | No | Medical coding, English |
| 20 | OpinioRAG: opinion highlights from reviews | 2025 | 2509.00285 | N | No | RAG on English reviews |
| 21 | LoopServe: multi-turn inference | 2025 | 2507.13681 | N | No | Inference acceleration |
| 22 | Does quantization affect models' performance on long-context tasks? | 2025 | 2505.20276 | Y | Not named (OneRuler groups) | Runs OneRuler, EN vs non-EN |
| 23 | Can LLMs reason over extended multilingual contexts? | 2025 | 2504.12845 | S | No | V5 checked |
| 24 | Harnessing the Unseen: intrinsic knowledge in long-context LMs | 2025 | 2504.08202 | Y | No | Long-context; OneRuler background only |
| 25 | What are Foundation Models Cooking in the Post-Soviet World? | 2025 | 2502.18583 | Y | No (Russian/Ukrainian) | Slavic (RU/UK) multilingual eval |
| 26 | A Survey on LLM Acceleration via KV Cache Management | 2024 | 2412.19442 | N | No | KV-cache survey |
| 27 | (duplicate of #13) | 2026 | 2605.12227 | - | - | Same arXiv ID |
| 28 | LLMs for Real-World Nutrition Assessment | 2025 | 10.3390/nu18010023 | N | No | Nutrition |
| 29 | Tucano 2 Cool: Better Open Source LLMs for Portuguese | 2026 | 2603.03543 | Y | No | Adapts OneRuler texts to Portuguese |
| 30 | Code Understanding is a Bottleneck for Coding Agents | 2026 | 2610.10610 | N | No | Coding agents |
| 31 | Quantifying the Gaps: Taxonomy of Bias in 96 Multilingual Benchmarks | 2026 | no ID (OpenAlex W7125407787) | Y | No numbers | Multilingual eval meta-study, abstract only |
| 32 | COMPASS: Continual Multilingual PEFT with Adaptive Semantic Sampling | 2025/26 | 2604.20720 | Y | Yes (OneRuler per-language) | Runs OneRuler incl. Polish, English |
| 33 | Языковая иерархия в эпоху ИИ (Language hierarchy in the AI era) | 2026 | 10.60797/irj.2026.167.100 | Y (low) | No data | Conceptual Russian essay |
| 34 | Generative/deterministic SE pedagogy with local multi-agent systems | 2026 | 10.56579/rei.v8i3.3644 | N | No | Software-engineering pedagogy |
| 35 | Advanced RAG Systems for Historical Documents | 2026 | no ID (OpenAlex W7166408916) | Y (low) | No (Czech) | Czech RAG, abstract only |
| 36 | LLM screening for Parkinson's device therapy | 2026 | 10.1007/s00702-026-03243-8 | N | No | Clinical screening |
| 37 | SALT microstructure vs LLM chatbots (child language) | 2026 | 10.3389/flang.2026.1929126 | N | No | Child language, JA-EN bilingual |
| 38 | CineSubBench: long-form multilingual movie subtitles | 2026 | 2609.36218 | Y | No (6 subtitle langs, no PL) | Long-form multilingual, no Polish |
| 39 | Vibe Coding versus Clinical Reality | 2026 | 10.1056/aip2600342 | N | No | Clinical AI commentary |
| 40 | How good is AI? Quality evaluation of FAST output in seven languages | 2026 | no ID (OpenAlex W7236884808) | N (unverified) | Unverified (PL only in refs) | Speech interpreting, abstract unavailable |

## 3. Relevant papers: what they actually report

### 3.1 Lara Mini Base (2611.16956) - runs OneRuler
- Uses OneRuler as "four retrieval tasks in 26 languages" (Section 4.1 / D.3). Polish is one of the 26 OneRuler languages (COMPASS lists `pol Polish`, High-resource).
- Table 8 and Table 16 report OneRuler only as averages (32k: Lara 90.1, the best of the five open models). No per-language OneRuler table; no English-vs-Polish split.
- Table 23 (per-language multilingual benchmarks, not OneRuler) has a Polish row (avg about 56.7). Column alignment from the PDF text is uncertain, so treat this as an approximate reading.
- No statement about why Polish is high. Status: PARTIAL.

### 3.2 Language Equality has a Price (2608.01395) - Polish and English, no long-context
- OneRuler is cited only in related work as a translated benchmark (Sec. 1-2). No replication.
- Table 4 (clemscore %, 30 languages x 9 models, mean across games). Per-model, Polish vs English:
  - GPT-5.4: PL 89.5 / EN 91.8
  - Claude Opus 4.8: PL 82.3 / EN 84.4
  - GLM-5.2: PL 64.3 / EN 65.6
  - DeepSeek-V4-Pro: PL 62.5 / EN 64.8
  - Nemotron-3-Ultra: PL 57.1 / EN 68.2
  - Gemma-4: PL 59.7 / EN 50.6
  - Qwen3.6: PL 45.9 / EN 55.6
  - Mistral-L3: PL 49.6 / EN 54.6
  - Apertus: PL 22.0 / EN 21.4
  - Mean across models: PL 59.2 / EN 61.9.
  - Column order was checked against text quotes (GLM-5.2 el 69.9, zh 71.3; Qwen3.6 zh 61.7).
- Status: VERIFIED for Table 4 (not long-context).

### 3.3 BPE Tokenization in Polish (2609.17553)
- Qualitative analysis (Dawidek, Univ. of Lower Silesia). BPE segmentation of Polish diagnostic words, a children's text, and the Constitution preamble. Concludes BPE "remain[s] fundamentally dependent on the frequency of written forms."
- No English comparison, no benchmark, no OneRuler. Relevant only as a tokenization hypothesis; it does not re-analyse OneRuler.

### 3.4 COMPASS (2604.20720) - runs OneRuler per language, Polish and English included
- OneRuler is one of the evaluation sets (26 languages, 7 NIAH variants, 8K-128K).
- Text: "A small subset of HRLs (Hindi, Polish, Portuguese, Dutch, Serbian) show more substantial improvements at 8K and 32K context lengths before tapering off." English is listed among languages that peak early and degrade, because they are "already well-represented in... pre-training corpora... including vast amounts of long-context data."
- Numbers only in a heatmap figure ("Relative change in performance ... on OneRuler"). Not extracted. Status: PARTIAL.

### 3.5 Quantization and long-context (2505.20276) - runs OneRuler, EN vs non-EN groups
- OneRuler with 26 languages, NIAH-single and NIAH-none, 64K and 128K, 50 examples per cell.
- Intro: "accuracy in these languages can drop up to five times more than in English." Polish is not named in the text; the paper groups languages as English / high-resource / low-resource.
- Sec. 5(a), 4-bit drops vs BF16: English -2.1% to -4.6%; high-resource -3.8% (GPTQ-int4) and -14% (BNB-nf4); low-resource -4.7% (AWQ-int4) and -8.4% (BNB-nf4). Post-hoc tests in Table 14 (App. F), not extracted.
- Explicitly "not a consistent pattern" for non-Latin scripts. Polish-specific result: NOT reported. Status: PARTIAL (no Polish).

### 3.6 Multilingual LLMs do not comprehend all languages equally (2602.20065, Moskvina et al., 2026)
- 12 languages (Arabic, Catalan, Chinese, Dutch, English, German, Greek, Italian, Japanese, Russian, Spanish, Turkish). No Polish in own data. English was not the top language for any of 3 models.
- Cites OneRuler (Kim et al., ref. 41) as: "Romance languages, along Polish and Russian, overall outperform English in long-context retrieval tasks, and when controlling for amount of input rather than number of tokens, Slavic languages surpass Romance ones. These results point to potential tokenization effects."
- Says its own results "partially echo" this. It does not re-analyse OneRuler. This is the most explicit 2026 restatement of the OneRuler Polish claim, and it is second-hand.

### 3.7 Polish-English clinical reasoning (JCM 2026, 10.3390/jcm15114082)
- Full text via PMC (PMC13258428). 30 vignettes, 6 LLMs (GPT-5, Bielik, Llama3.3, Meditron3, OpenBioLLM, Qwen2.5), EN vs PL. Cites OneRuler (Kim 2025, ref. 11) only generically ("LLMs... do not perform equally well in all [languages]").
- Table 1 total rubric score (0-24), PL vs EN: GPT-5 21.5 vs 22.8; Bielik 13.0 vs 11.4; Llama3.3 12.0 vs 17.0. Strict diagnosis accuracy: GPT-5 89.7% vs 96.7%; Qwen2.5 75.0% vs 80.0%; Llama3.3 70.4% vs 79.3%.
- Abstract: Qwen2.5, Llama3.3, Meditron3, OpenBioLLM "performed significantly better in English"; GPT-5 and Bielik showed "no statistically detectable English-Polish difference". Status: VERIFIED (abstract + Table 1, PMC).

### 3.8 Human vs AI emotional ratings (BMC Psychology 2026, 10.1186/s40359-026-05067-3)
- GPT-4o rating NAPS images; Polish-language prompt vs English standard prompt; persona prompt.
- Abstract only (PDF download failed). Result: neither persona nor Polish-language prompting "consistently improved agreement"; Polish condition "diverged more strongly in approach-avoidance (d = 0.47)". No accuracy vs English comparison. Status: PARTIAL (abstract only).

### 3.9 Czech-English verbosity (ICAIR 2025, 10.34190/icair.5.1.4326) - Slavic vs English
- 48 prompts x 6 LLMs x 2 languages (576 responses). English responses significantly longer than Czech across all models (medium-large Cohen's d). Not a Polish study. Abstract only.

### 3.10 Others (relevant but no Polish)
- 2605.30788 XLGoBench (PDF): no Polish, no Slavic.
- 2604.03592 MoE language routing (PDF): no Polish, no Slavic.
- 2603.03543 Tucano 2 (HTML): RULER-PT built from OneRuler texts (Portuguese only); no English comparison in the RULER-PT results.
- 2504.08202 and 2605.12227: OneRuler mentioned as background or decontamination source. No Polish.
- 2502.18583 Post-Soviet food (HTML): Russian and Ukrainian only; "Polish" hit is an author name.
- 2609.36218 CineSubBench (HTML): six subtitle languages, no Polish.
- 2611.16956 and 2604.20720 excluded from "no Polish": see 3.1 and 3.4.

## 4. Polish-specific findings

- Explanations for Polish ranking found: only the OneRuler authors' tokenization/input-amount argument, restated in 2602.20065 (Sec. 4 and Discussion). COMPASS attributes its HRL behaviour to pre-training exposure. Quantization paper says no script-level pattern.
- Re-analysis: none found. No citer re-examines OneRuler haystack content or the translation quality of OneRuler instructions. Dawidek (2609.17553) is a qualitative BPE study of Polish, not a re-analysis.
- Replication: none found. 2611.16956, 2604.20720 and 2505.20276 use OneRuler but do not report a Polish-vs-English long-context gap.

## 5. Verdict

No 2025-2026 citer confirms or contradicts "Polish >= English in long-context" with per-language numbers.

- Confirmation of the claim comes only second-hand: 2602.20065 restates OneRuler's statement that Polish and Russian outperform English in long-context retrieval, without new data.
- COMPASS (2604.20720) reports Polish gains at 8K and 32K, but the numbers are in a figure that was not extracted.
- Quantization (2505.20276) reports English as less degraded than non-English groups, which points against English being a special advantage under quantization, but Polish is not separated out.
- Other 2026 Polish-vs-English comparisons, which are not long-context, lean toward English being equal or higher: Language Equality (mean clemscore PL 59.2 vs EN 61.9) and the JCM clinical study (English significantly better for 4 of 6 models, with no significant difference for GPT-5 and Bielik).

Status: UNCONFIRMED for long-context. Best available evidence is OneRuler's own result (cited secondhand) plus one unextracted figure.

## 6. Limitations
- Semantic Scholar and OpenAlex lists may be incomplete (OpenAlex counted 30, SS 27; overlap high).
- HTML was not available for several arXiv papers (2602.20065, 2605.30788, 2604.03592, 2609.17553); PDFs were used.
- Abstract-only: BMC (PDF failed), JCM via PMC full text (ok), ICAIR (abstract), Quantifying the Gaps, Russian essay, Czech RAG, FAST (no abstract).
- COMPASS per-language OneRuler numbers are only in heatmap figures.
- Lara Table 23 column alignment was reconstructed from PDF text and is approximate.
