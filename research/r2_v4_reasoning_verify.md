# R2 V4: Verification of reasoning / agent numbers (iteration 2)

Method: PDFs downloaded to scratchpad/dl/<name>/paper.pdf and converted with pdf2txt.py. Numbers were read from the converted text, and row sums and averages were re-checked where possible. Table captions in these PDFs sit below their tables, which was confirmed against table content.

## 1. Summary
- MCLM (2502.17407): Polish and other Slavic languages are included (pl, ru, uk, cs, bg, hr, sk, sl, mk). On MT-AIME2024 the reference models do not show a consistent English advantage. o3-mini scores English 36.67 vs Polish 43.33, and GPT-4o-mini scores English 33.33 vs Polish 13.33. Small N (30 problems per AIME language, so 1 problem = 3.33 pts).
- The printed "Average" on MT-AIME2024 is the mean over 55 languages (recomputed: 45.33 for o3-mini, 20.18 for GPT-4o-mini). English sits below that average for o3-mini (36.67).
- PolyMath (2504.18428): Table 5a values for Russian and English are CONFIRMED for all five models. Polish is not one of the 18 languages.
- Yong et al. (2505.05408): the Swahili 90.8 figure in the text is NOT in Table 3. The Table 3 Swahili-query, French-reasoning cell is 55.6. The value 59.6 is the Swahili-query, English-reasoning cell. 90.8 appears only in the German-query, French-reasoning cell.
- MAPS (2505.15935 v3, 10 Feb 2026): 11 non-English languages (de, es, pt-BR, ja, ru, zh, it, ar, he, ko, hi). Russian is the only Slavic language. The "up to 16%" is explained by Table 17: ODR GAIA original, English 47.0 vs Arabic/Hindi 31.0, which is a 16.0-point absolute gap. The Table 4 "-12%" is the mean gap (47.4 vs 35.8).
- Polish 2026 sources: Bielik reasoning report (2603.10640) exists, and its title is "Making Bielik LLM Reason (Better): A Field Report". It is Polish-only with no English comparison. LLMzSzŁ (2501.02266) has Polish-only questions and no English comparison. The EACL 2026 Findings paper "Breaking the Illusion of Reasoning in Polish LLMs" has Polish-vs-English Belebele numbers (Table 3).
- Multilingual tool calling: MASSIVE-Agents (Findings EMNLP 2025) has Polish and Russian rows with EN numbers (Table 5). Ticket-Bench has no Slavic language. MLCL has no Slavic language and its numbers appear only in figures.

## 2. Findings table

| # | Benchmark | Model | EN | PL | other Slavic | metric | source + location | status |
|---|---|---|---|---|---|---|---|---|
| 1a | MCLM MT-AIME2024 | o3-mini | 36.67 | 43.33 | RU 56.67, UK 50.00, BG 46.67, HR 46.67, SK 46.67, SL 46.67, CS 36.67, MK 43.33; 55-lang avg 45.33 | acc % (30 problems) | arXiv 2502.17407, Table 26 (p.32) | VERIFIED |
| 1b | MCLM MT-AIME2024 | GPT-4o-mini | 33.33 | 13.33 | RU 23.33, UK 23.33, CS 33.33, BG 20.00, HR 16.67, SK 20.00, SL 16.67, MK 20.00; avg 20.18 | acc % | arXiv 2502.17407, Table 25 (p.31) | VERIFIED |
| 1c | MCLM MT-MATH100 | GPT-4o-mini | 75.76 | 72.73 | RU 73.74, UK 73.74, CS 71.72, BG 72.73, HR 72.73, SK 72.73, SL 72.73, MK 71.72; avg 70.30 | acc % (100 problems) | Table 25 | VERIFIED |
| 1d | MCLM MT-MATH100 | o3-mini | 83.84 | 81.82 | RU 85.86, UK 84.85, CS 84.85, BG 87.88, HR 84.85, SK 87.88, SL 85.86, MK 83.84; avg 84.89 | acc % | Table 26 | VERIFIED |
| 1e | MCLM M-IMO | o3-mini | 29.63 | 37.04 | RU 20.00, UK 29.63, CS 29.63, BG 40.74, HR 33.33, SK 33.33, SL 29.63, MK 33.33; avg 29.75 | acc % | Table 26 | VERIFIED |
| 1f | MCLM M-MO | o3-mini | 80.00 | 40.00 | RU 50.00, CS 53.33, SK 46.67; avg 51.42 | acc % (small N) | Table 26 | VERIFIED |
| 1g | MCLM M-MO | GPT-4o-mini | 50.00 | 26.67 | RU 50.00, CS 36.67; avg 30.81 | acc % | Table 25 | VERIFIED |
| 1h | MCLM MT-MATH100 / MT-AIME2024 / M-MO | Qwen2.5-Math-1.5B-Instr (greedy) | 67.68 / 20.00 / 56.67 | 54.55 / 26.67 / 26.67 | RU 59.60 / 20.00 / 20.00; CS 44.44 / 13.33 / 6.67 | acc % | Table 11 (appendix C) | VERIFIED |
| 1i | MCLM MT-MATH100 / MT-AIME2024 | Qwen2.5-Math-7B-Instr (greedy) | 83.84 / 20.00 | 71.72 / 16.67 | RU 77.78 / 20.00 | acc % | Table 22 | VERIFIED |
| 1j | MCLM MT-MATH100 / MT-AIME2024 | DeepSeek-R1-1.5B + MT-SFT | 67.68 / 20.00 | 62.63 / 16.67 | RU 68.69 / 33.33; CS 57.58 / 33.33 | acc % | Table 20 | VERIFIED |
| 2 | MCLM (Table 4 summary) | Qwen2.5-Math-1.5B / DeepSeek-R1-1.5B / GPT-4o-mini / o3-mini | - | - | - | MT-AIME2024 cross-language mean | Table 4 | VERIFIED (matches Table 11, 26 averages; the English value is not in the mean) |
| 3a | PolyMath Table 5a (Top) | Qwen3-235B-A22B-Thinking | 40.0 | - | RU 38.4 (no PL) ; avg 40.3 | ACC % pass@1 avg@16 | arXiv 2504.18428 (v4, NeurIPS 2025), Table 5a p.10 | VERIFIED (40.0/38.4 exact) |
| 3b | PolyMath Table 5a | Gemini-2.5-pro | 39.2 | - | RU 38.4; avg 38.0 | ACC % | Table 5a | VERIFIED |
| 3c | PolyMath Table 5a | DeepSeek-R1-671B | 35.2 | - | RU 37.6; avg 33.7 | ACC % | Table 5a | VERIFIED |
| 3d | PolyMath Table 5a | QwQ-32B | 36.8 | - | RU 28.8; avg 30.6 | ACC % | Table 5a | VERIFIED |
| 3e | PolyMath Table 5a | o3-mini-medium | 25.6 | - | RU 17.6; avg 23.0 | ACC % | Table 5a | VERIFIED |
| 3f | PolyMath Table 5a | o1-mini / Claude-3.7-thinking | 20.8 / 25.6 | - | RU 24.0 / 21.6 | ACC % | Table 5a | VERIFIED |
| 4 | Yong et al. MGSM | s1 family, Swahili query | 59.6 (sw query, EN reasoning) | - | sw query, FR reasoning 55.6 | acc % | arXiv 2505.05408, Table 3 (p.9) and Sec "Choice of query language" | CORRECTED: text's 90.8 is not in Table 3 for Swahili (90.8 is the de-query, fr-reasoning cell) |
| 5a | Breaking the Illusion (EACL 2026 Findings, 2026.findings-eacl.92) | Llama-PLLuM-8B original | Belebele EN 0.656 | Belebele PL 0.609 | - | accuracy | Table 3 | VERIFIED |
| 5b | same | PLLuM-8B fine-tuned Detailed | Belebele EN 0.774 | Belebele PL 0.709 | - | accuracy | Table 3 | VERIFIED (Aya PL 0.584 vs EN 0.592; different subsets, not parallel) |
| 6a | MASSIVE-Agents BFCL 10k AST, zero-shot (top model per text: Nova Premier) | Nova Premier | 57.37 | 29.47 | RU 35.98; average 34.05 | AST accuracy % | ACL Anthology 2025.findings-emnlp.1099, Table 5 (p.14) + text lines 405-407, 480 | VERIFIED |
| 6b | MASSIVE-Agents | Claude 3.5 v2 Sonnet (FSA, Table 6) | - | - | avg 86.61 | FSA % | Table 6 | VERIFIED (only average read) |
| 7 | MLCL, Lost in Execution (2601.05366) | 5 model families | - | - | none (zh, hi, ig only) | numbers only in figures 4-7 | figures only | NOT FOUND (no numeric table in text) |
| 8 | Ticket-Bench (2509.14477) | GPT-5 passˆ3 | 0.92 | - | no Slavic (pt 0.87, es 0.93, fr 0.92, it 0.92, de 0.92) | passˆ3 | Table 3 (p.6) | VERIFIED, but no Slavic language |
| 8b | Ticket-Bench | Gemini-Pro 2.5 | 0.85 | - | it 0.48, de 0.59, pt 0.60, es 0.54 | passˆ3 | Table 3 | VERIFIED, no Slavic |
| 9 | MAPS (2505.15935 v3) | GPT-5 mini, ASB (ASR, lower better) | 32.7 | 39.5 (RU) | - | ASR % | Table 16 and Table 19 (p.22-24) | VERIFIED (no PL) |
| 9b | MAPS | OpenDeepResearch GAIA, GPT-o1 | 47.3 | RU 39.1 | - | accuracy | Table 16 (p.22) | VERIFIED; CONFLICT: Table 17 "Original" gives RU 34.0 vs EN 47.0 for the same setting |
| 9c | MAPS | GAIA open-source agents, mean of 3 runs | 0.4746 | RU 0.3961 | - | accuracy | Table 20 (p.23) | VERIFIED |
| 9d | MAPS | GAIA unified GPT-5-mini | 55.3 | RU 44.2 | - | accuracy | Table 19 | VERIFIED |
| 9e | MAPS | SWE-Bench, open-source agents | 0.31 | RU 0.33 | - | accuracy | Table 21 | VERIFIED |
| 9f | MAPS | MATH, open-source / unified | 0.514 / 82 | RU 0.507 / 81 | - | accuracy | Table 22 / Table 19 | VERIFIED |
| 9g | MAPS | ASB benchmark, agent not named in caption (run mean) | 0.534 | RU 0.641 | - | ASR (lower better) | Table 23 (appendix, p.25) | VERIFIED values; agent identity not stated in caption |
| 10 | Bielik reasoning report (2603.10640) | Bielik-R:11b in a 111-puzzle ranking | - | Polish-only | - | score % | Table 2 (p.4) | VERIFIED existence and title; no English comparison |
| 11 | LLMzSzŁ (2501.02266) | Mistral-Large-2407 / Llama-3.1-70B / Bielik-11B-v2.1 | - | Polish-only questions | - | accuracy | Table 1 (text) | VERIFIED; no English-translated comparison |

## 3. Details per source

**MCLM (Son et al., arXiv 2502.17407, ACL 2025).** The benchmark includes Polish and the other Slavic languages in MT-MATH100 (55 languages), MT-AIME2024 (38), M-IMO (38) and M-MO (11; includes pl, ru, cs, sk). Table 4 is the summary, and its AIME means (16.36, 17.21, 19.94, 20.18, 45.33) match the per-language tables. The abstract statement about "+20 English AIME vs +1.94 other-language average" for budget forcing is confirmed. Counts are small (MT-AIME2024 accuracies move in steps of 3.33 pts, which implies 30 problems; M-MO and M-IMO per-language sets are smaller still), so differences of several points are within noise. Reading the tables, English is not uniformly higher than Polish or Slavic: for o3-mini on AIME, Polish (43.33), Russian (56.67) and Ukrainian (50.00) all exceed English (36.67). The M-MO English figure (80.00 for o3-mini) rests on a tiny set.

**PolyMath (Wang et al., arXiv 2504.18428 v4, NeurIPS 2025).** Table 5a (PolyMath-Top) lists 18 languages (en, zh, ar, bn, de, es, fr, id, it, ja, ko, ms, pt, ru, sw, te, th, vi). Polish is absent. Each "avg." column is the mean over the 18 languages (recomputed for DeepSeek-R1: 605.9/18 = 33.66). The top three models by average are Qwen3-235B (40.3), Gemini-2.5-pro (38.0) and DeepSeek-R1 (33.7). Their EN vs RU results are 40.0/38.4, 39.2/38.4 and 35.2/37.6. Only the 18-language averages are reported, so no separate EN-vs-RU average exists in the paper.

**Yong et al. (arXiv 2505.05408).** Table 3 is the query-language x reasoning-language matrix. The average along the diagonal is 81.2. The Swahili row is 45.6, 58.8, 59.6, 55.2, 55.6, 47.6, 48.4, 44.4, 32.4, 45.2, 52.0 (bn, de, en, es, fr, ja, ru, sw, te, th, zh). The text's claim that translating the question from Swahili to French lifts accuracy from 59.6 to 90.8 is therefore wrong on the table's own numbers: the same query with French reasoning is 55.6. The 90.8 appears only in the German-query, French-reasoning cell. Probable explanation: a transcription or mix-up in the text; the paper's own table does not support it.

**Breaking the Illusion of Reasoning in Polish LLMs (Pihulski et al., EACL 2026 Findings, 2026.findings-eacl.92).** Fine-tunes Llama-PLLuM-8B on Polish translations of Mixture-of-Thoughts (MoT-PL). Table 3: the original model gets Belebele PL 0.609 / EN 0.656, and the best (Detailed) variant gets 0.709 / 0.774. MoT-PL-eval (Polish reasoning) rises from 0.316 to 0.437. English LightR1 falls from 0.172 to 0.146, so Polish fine-tuning costs some English reasoning. The Aya PL/EN columns are different subsets, so they are not parallel. No arXiv ID was found for this paper, and arXiv 2601.11579 returned by search is "Bielik 11B v3", a different paper.

**MAPS (Hofman et al., arXiv 2505.15935 v3, 10 Feb 2026; EACL 2026 Findings).** The 11 non-English languages are de, es, pt-BR, ja, ru, zh, it, ar, he, ko, hi (Sec 3 "Translated Languages"). Russian is the only Slavic language, and Polish is not included. The "up to 16%" figure appears in Sec 4.2 and matches Table 17 (ODR, GAIA original): EN 47.0 vs AR and HI 31.0, a 16.0-point absolute gap. The Table 4 value of -12% is the mean across languages (47.4 vs 35.8, a gap of 11.6). The paper uses absolute percentage points, not relative ones. Internal inconsistency: Table 16 gives ODR GPT-o1 RU 39.1 while Table 17 gives RU 34.0 for the same agent and setting.

**Bielik reasoning report (arXiv 2603.10640, Trybus, Bartnicki, Kinas, 12 Mar 2026).** Title: "Making Bielik LLM Reason (Better): A Field Report". Preprint, 5 pages. Table 2 ranks models on 111 Polish puzzles (o3 87%, bielik-r:11b 56%, Bielik-v2.3 29%). All questions are Polish. The report has no English-vs-Polish comparison and no per-language numbers. Its own training data is in English (1.3M distilled traces) and in Polish (143k RL tasks).

**LLMzSzŁ (Jassem et al., arXiv 2501.02266).** About 19k questions from Polish national exams, all in Polish. The "English" in the paper refers to the model language family, not to English versions of the questions. Table 1 gives best scores: Mistral-Large-2407 67.17, Llama-3.1-70B 66.59, Bielik-11B-v2.1 57.52. Its English comparison comes only through a cited work (arXiv 2412.00559, medical exams) that is not reasoning-focused.

**MASSIVE-Agents (Kulkarni et al., Findings EMNLP 2025, ACL Anthology 2025.findings-emnlp.1099).** BFCL-style function calling in 52 languages, built from MASSIVE. Table 5 (10k split, zero-shot, AST) gives EN 57.37, PL 29.47 and RU 35.98 for the top model (named Nova Premier in the text, which also gives average 34.05). The Table 5 column ordering in the PDF text is ambiguous, but the text confirms the mapping. Germanic and Romance languages do best, and Polish and Russian sit in the lower-middle range.

**Ticket-Bench (Almeida et al., arXiv 2509.14477).** Six languages: pt, en, es, fr, it, de. No Slavic language. Table 3 passˆ3 values exist for every model (GPT-5 en 0.92, Gemini-Pro 2.5 en 0.85 vs it 0.48).

**MLCL, Lost in Execution (Luo et al., arXiv 2601.05366).** Languages: Chinese, Hindi, Igbo, against English. No Slavic language. Results are in figures 4-7 only (error distributions), and no numeric table exists in the text. Status NOT FOUND for numbers.

## 4. Search log
- "MASSIVE-Agents multilingual tool calling 52 languages" returned the ACL Anthology page (downloaded, Table 5 read).
- "Polish reasoning benchmark LLM 2026 arXiv English vs Polish" returned the Bielik report and the EACL Polish reasoning paper. It also returned arXiv 2601.11579, which is Bielik 11B v3 (not the reasoning paper).
- "polski benchmark rozumowania LLM Bielik-R PLLuM olimpiada matematyczna": nothing on PLLuM reasoning or a Polish math olympiad benchmark.
- "Polish math olympiad LLM benchmark PLLuM reasoning arXiv 2026": nothing matching. PLLuM instruction corpus (2511.17161) is an instruction-tuning paper, not reasoning.
- "Breaking the Illusion of Reasoning in Polish LLMs" search: returned the ACL Anthology ID 2026.findings-eacl.92, which was downloaded.

## 5. Open questions
- MCLM per-language values for Qwen2.5-Math-1.5B with budget forcing (Table 21) have an unclear column layout in the extracted text, so they were not used.
- MAPS Table 23 (ASB, unnamed agent) and the MAPS per-language SWE-bench/MATH breakdown are only partly mapped to agents.
- MASSIVE-Agents Table 5: confirm the column-to-model mapping in the PDF image if the number is used in publication.
- The EACL paper has no arXiv ID found yet; search again later.
- Yong et al.: confirm with the authors which cell the 90.8 refers to (probably a German-query cell mis-cited).
