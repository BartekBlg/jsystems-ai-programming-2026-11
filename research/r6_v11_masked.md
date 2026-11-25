# R6 V11: Masked Advantage, PoVisLE, CUS-QA Table 11, OneRuler appendix

Method: arXiv abs/HTML/PDF downloaded with curl into scratchpad/dl/v11 and converted to text; figures rendered with ghostscript and pypdf-cropped for reading. Values from figure images are approximate (+/-0.02 logits). Status scale as in BRIEF.md.

## 1. arXiv 2606.07422 "The Masked Advantage: Uncovering Local-Language Access to Cultural Knowledge in LLMs"
- Authors: Yang Zhang, Xiao Fei, Amr Mohamed, Sarah Almeida Carneiro, Mersin Konomi, Mingmeng Geng, Ahmed Asaad, Guokan Shang, Michalis Vazirgiannis. Submitted 5 Jun 2026. Status: VERIFIED (abs page).
- Design: 13 locales incl. Polish. CA = culture-agnostic items (Global-MMLU, locale language). CS = culture-specific items in the local language (Polish items from INCLUDE). CSEN = the same CS items translated to English (Claude Sonnet 4, human-checked). "English" means the CSEN / English-query arm.
- Metric: NOT accuracy. The paper uses 1PL-IRT ability theta (logits). GlobalGap = theta(CA, local) - theta(CA, en). LocalGap = theta(CS, local) - theta(CS, en). KnowledgeGap = LocalGap - GlobalGap. Positive = local language wins.
- Raw accuracy: not reported per locale or model in text or appendix. Section 6.3 says theta vs accuracy correlates at mean r=0.996 but gives no raw numbers. So the "before IRT" column cannot be filled from the paper. Marked as NOT AVAILABLE.
- Main conclusion (sentence, 23 words): "When a model performs worse in a local language, the cause is often not missing cultural knowledge but weaker proficiency in that language."

### Polish values (IRT logits; read from Figure 2 heatmap, PDF p.5, approximate)
| Model | GlobalGap (CA, PL-EN) | LocalGap (CS, PL-EN) | KnowledgeGap |
|---|---|---|---|
| GPT-5.4-mini | -0.35 | +0.40 | +0.75 |
| GPT-5.4 | -0.34 | +0.40 | +0.74 |
| Gemini-3-Flash | -0.32 | +2.26 | +2.58 |
| Claude-Sonnet-4.6 | -0.40 | +0.80 | +1.20 |
| Llama-3.1-8B | -0.89 | -0.36 | +0.53 |
| Llama-3.1-70B | -0.66 | +0.30 | +0.96 |
| Qwen3.5-9B | -0.83 | +0.74 | +1.57 |
| Gemma-4-31B | -0.49 | +1.00 | +1.49 (arithmetic; figure cell sign hard to read) |
| Jais-2-70B-Chat | -1.31 | -0.36 | +0.95 |
| Aya-Expanse-8B | -0.45 | -0.51 | -0.06 |
| Mean of the 24 models shown in Fig. 2 (my average of the read values) | -0.65 | +0.11 | ~+0.76 |

Caveat: Qwen3.5-9B knowledge value is +1.57 by arithmetic; the read cell was +1.57 in the figure.

### Polish-specific text values (exact, from text or Table 1 / Table 3)
- Table 1, resource level 3 (HI, FA, KO, PL, NL), mean: GlobalGap -0.76, LocalGap -0.07, KnowledgeGap +0.68.
- Table 3 (language-adapted model, Bielik-Minitron-7B, PL), text-extracted; arithmetic check passes (Know = Local - Global: 0.75 - (-0.41) = 1.16):
  - GlobalGap adapted -0.41 (delta +0.25 vs PL baseline of general models)
  - LocalGap adapted +0.75 (delta +0.64)
  - KnowledgeGap adapted +1.16 (delta +0.40)
- Section 6.2: "SFT adds target-language cultural knowledge for Greek and Polish."

### Accuracy-based reading the paper does support
- Culture-agnostic: English is higher for all models on all 13 locales (cell mean -0.79 logits). For PL, GlobalGap is negative for every model in the figure.
- Culture-specific: LocalGap is positive for PL for about half the models shown, and the largest values are the closed frontier models and Gemma-4-31B.
- After proficiency adjustment (KnowledgeGap), PL is positive for every model shown.

Status: PARTIAL. Per-model Polish numbers are read from the figure (approximate). No accuracy numbers exist in the paper.

## 2. arXiv 2608.07763 "Jako Tako or Fluent? Presenting PoVisLE: A Polish Vision-Language Evaluation"
- Authors: Anna Kołos, Grzegorz Statkiewicz, Karolina Seweryn, Katarzyna Kowol, Karolina Piosek, Wojciech Kusa. Submitted 7 Aug 2026. Status: VERIFIED (abs page).
- 1,117 images, 2,366 VQA pairs. Prompt-language test: 465 items sampled (15 per subcategory), machine-translated (DeepSeek-V4-Pro) to EN and DE, then manually reviewed. Image and reference answers stay in Polish. Open-ended prompts ask for a Polish answer.
- Table 14 (overall macro accuracy %, PL prompt vs EN prompt; delta = EN - PL):
| Model | PL | EN | Delta |
|---|---|---|---|
| Qwen3.5-397B-A17B (Thinking) | 75.16 | 68.12 | -7.04 |
| Claude Sonnet 5 | 69.94 | 66.94 | -3.00 |
| GPT-5.4 | 68.35 | 70.40 | +2.06 |
| Gemma-4-31B-it | 63.96 | 60.15 | -3.82 |
| Qwen3.5-9B | 38.75 | 47.04 | +8.29 |
| LLaVA-Bielik-11B-v2.6 | 40.44 | 39.02 | -1.42 |
- Authors' reading: models with higher PL accuracy tend to lose more under translation; weaker models gain from English prompts. Stated without a significance test. Table 14 gives no CIs.
- Status: VERIFIED for the table values. Single run, n=465, no CIs in Table 14. Prompt language is confounded with translation of the options.

## 3. CUS-QA (arXiv 2507.22752) Table 11 check
- Submitted 30 Jul 2025. Status: VERIFIED (PDF, page 7, read as image).
- Metric: accuracy (%) on test data, judged by LLaMA 3.3 70B (LLM-as-judge). Upper block = textual QA, lower block = visual QA.
- "local" = the question written in the local language (cs / sk / uk). "en" = the same question in English (annotator-written English translation). The system prompt is English and the answer is in the language of the question (Section 4.1).
- Table 11, textual, en minus local:
| Model | CZ (cs->en) | SK (sk->en) | UA (uk->en) |
|---|---|---|---|
| LLaMA 3.3 70B Ins. | +3.1 (60.0 -> 63.1) | +0.6 (47.7 -> 48.3) | +2.7 (52.2 -> 54.9) |
| LLaMA 3.1 8B Ins. | +6.9 (29.6 -> 36.5) | +6.0 (19.6 -> 25.6) | +8.4 (25.7 -> 34.1) |
| Mistral 7B Ins. v0.3 | +7.5 (22.9 -> 30.4) | +7.9 (14.4 -> 22.3) | +3.2 (24.6 -> 27.8) |
| EuroLLM 9B Ins. | +3.8 | -0.6 | +1.6 |
- The three values in the request do NOT match Table 11:
  - Llama-3.1-8B Ukraine: Table 11 gives +8.4, not +11.4.
  - Mistral-7B Slovakia: Table 11 gives +7.9, not +9.1.
  - Llama-3.3-70B Czechia: Table 11 gives +3.1, not -1.3.
- The strings 11.4 and 9.1 appear in the appendix dev-set metric tables (Tables 21/22), under other metrics, so they are not Table 11. The -1.3 value was not found in Table 11. Status for the requested values: CONTRADICTED by Table 11 (test, LLM judge).

## 4. OneRuler (arXiv 2503.01996) appendix
- Table 4 (appendix, p.17): haystack books per language. Polish: "Nights and Days, Vol. 3" (Noce i dni), Maria Dąbrowska, 1934, 452,847 tokens. English: "Little Women", Louisa May Alcott, 1868, 233,668 tokens. Other examples: cs "The Good Soldier Švejk, Vol. 3", uk "After Finishing School" (Marko Vovchok).
- Translation: Appendix B says "All instructions and needles are verified translations by native speakers of each respective language." Section 3 says 17 Upwork annotators plus 6 volunteers translated and localized the instructions and 100 nouns into 25 languages. Footnote 6: one annotator translated both Polish and Japanese. Appendix B: each data point was authored by a single individual.
- Status: VERIFIED (local text: scratchpad/dl_oneruler/pdf/paper.txt, lines ~205-230, 978-1032, 1065-1075). Note the English and Polish haystacks are different books with different lengths, so the English-vs-Polish comparison also differs by book.

## Search log / notes
- Downloads: scratchpad/dl/v11 (abs/html/pdf for 2606.07422, 2608.07763, 2507.22752). Rendered figure crops: fig2_crop.png, t11_hi.png.
- The browser pane was opened once by mistake (navigate to arxiv abs); no data was taken from it.

## Open questions
- Raw per-locale accuracy for Masked Advantage is not published. The authors' repository might have it; not checked.
- Table 11 vs the -1.3 / +11.4 / +9.1 values: an earlier arXiv version or another table may carry them; only v-current (Table 11, test) was checked.
