# R5 V9: MASSIVE-Agents Table 5 column mapping and MAPS Russian checks

Sources (local, already extracted and checked):
- MASSIVE-Agents: Kulkarni et al., "MASSIVE-Agents: A Benchmark for Multilingual Function-Calling in 52 Languages", Findings of ACL: EMNLP 2025, pp. 20193-20215, ACL Anthology 2025.findings-emnlp.1099. Text: `dl/massive/paper.txt`. Rendered pages: `dl/massive/png/p14.png` (Table 5), `p15.png` (Table 6), `p16.png` (Table 7).
- MAPS: arXiv 2505.15935 v3. Text: `dl/maps/paper.txt`.

## 1. MASSIVE-Agents: column mapping (Tables 5-7)

The column headers were read from the rendered PNG of Table 5 (page 14). They are rotated, and the order in the PDF text extraction matches the image. The columns are sorted by the Average row, ascending, so Nova Premier is the last column. Checks: the Average row equals the mean of the 52 language rows for all 21 models in all three tables (0 mismatches), and the Table 2/3 max/min values in the main text match. The Table 6 and Table 7 headers were checked by the same ordering and average consistency, and Table 7 was also read from its PNG.

**Table 5 (line 1252): BFCL zero-shot prompt, 10k split, AST (en-US, pl-PL, ru-RU, avg across 52 languages; pl rank = position of pl-PL among 52 languages, 1 = best)**

| Model | Avg | en-US | pl-PL | ru-RU | pl rank |
|---|---|---|---|---|---|
| Nova Premier | 34.05 | 57.37 | 29.47 | 35.98 | 37/52 |
| Claude 3.5 v2 Sonnet | 27.06 | 53.68 | 18.95 | 28.57 | 47/52 |
| Llama 3.1 405B | 25.61 | 46.32 | 15.26 | 25.93 | 48/52 |
| Claude 3.5 Haiku | 24.94 | 43.16 | 17.37 | 24.34 | 46/52 |
| Nova Pro | 23.65 | 47.89 | 10.53 | 24.34 | 52/52 (last) |
| Llama 3.1 70B (mid) | 18.77 | 34.21 | 13.68 | 23.28 | 36/52 |

For the top model (Nova Premier), 36 languages score above pl-PL and 15 score below. The en-US/pl-PL gap is 27.9 points (pl-PL is about 51% of en-US).

**Table 6 (line 1331): same zero-shot prompt, FSA (function selection only)**
Nova Premier: avg 78.77, en-US 91.58, pl-PL 89.47 (rank 5/52), ru-RU 87.83. Claude 3.5 v2 Sonnet has the highest avg (86.61).

**Table 7 (line 1410): custom one-shot prompt, AST**
Nova Premier: avg 40.40, en-US 63.68, pl-PL 40.00 (rank 30/52), ru-RU 43.91. Nova Pro (avg 42.65) has pl-PL 28.95 (rank 50/52).

## 2. What the three tables measure

- Table 5 (line 1252): zero-shot (BFCL template), 10k split, AST. AST requires the whole function call to match, including arguments.
- Table 6 (line 1331): zero-shot, 10k split, FSA. FSA counts only the correct function selection (paper, Sec. 3.1, lines ~362-377).
- Table 7 (line 1410): custom one-shot prompt, 10k split, AST. Its FSA counterpart is Table 8.
- There is no "relevance" metric in this paper. Tables 9-10 are the full split (zero-shot AST/FSA).
- Polish (pl-PL) appears in every table, as one of the 52 language rows. The paper never discusses it in the text.

## 3. The paper's own statement on language performance

Sec. 3.2, lines ~483-489: "Low-resource languages like Amharic (am-ET) and Javanese (jv-ID) consistently appear among the lowest-performing languages across all models and setups." (21 words.)

Lines ~472-476 name Germanic and Romance languages as consistently high. Slavic languages, including Polish, are not mentioned. Lines ~655-668 on prompt translation say that smaller models degrade when moving away from English instructions, and that larger-model gains "suggest a preference for English instruction" (line ~676, about translated prompts, not user-query language). The abstract (line ~26) reports the top model at 34.05% average, 57.37% English, and 6.81% Amharic.

Status: MASSIVE table numbers VERIFIED (read from the rendered PNG and text, checked by average consistency). The paper does not state a Polish-vs-English conclusion, so the article's claim that Polish beats English is not supported by this paper. In fact, en-US is the top language and pl-PL is at rank 37/52 for the top model.

## 4. MAPS (2505.15935 v3): Russian vs English

The main summary table (Table 4, main text, line ~676) reports only aggregate relative degradation vs English. It has no per-language Russian values:
- GAIA: -12% original, -10% unified backbone.
- ASB: +14.6% ASR, both settings.
- MATH: -0.1% original, -0.9% unified.
- SWE-Bench: -1.3% original, -0.3% unified.

Per-language values come from the appendix (Tables 20-23, English baseline vs Russian, mean of 3 runs; these table captions sit below their tables in the extracted text, and the mapping was checked by value range):
- GAIA (Table 20, line ~1988): EN 0.4746 vs RU 0.3961 (-7.9 pp).
- SWE-Bench (Table 21, line ~2001): EN 0.31 vs RU 0.33 (+0.02, no meaningful gap).
- MATH (Table 22, line ~2014): EN 0.514 vs RU 0.507.
- ASB (Table 23, line ~2030): EN ASR 0.534 vs RU ASR 0.641 (higher ASR = less secure, +10.7 pp).

Status: PARTIAL. Numbers are verified in text; the table-to-benchmark mapping was inferred from captions placed below tables and value ranges. A PNG check of page 24-25 is recommended.

## 5. The 39.1 vs 34.0 inconsistency (Tables 16 vs 17)

- Table 16 (line ~1842, "Cross-Model Agentic Stability", GAIA, GPT-O1 row): EN 47.3%, RU 39.1%, HI 34.0%.
- Table 17 (line ~1917, OpenDeepResearch GPT-O1, GAIA, "Original" row): EN 47.0%, RU 34.0%, HI 31.0%.
- Table 20 (GAIA mean of 3 runs for Russian): 0.3961 (runs 0.4268, 0.4085, 0.3530).

The same setup, GAIA with ODR GPT-O1, gives Russian 39.1% in Table 16, 34.0% in Table 17, and 39.6% (run mean) in Table 20. English is 47.3%, 47.0%, and 47.5% respectively. The 39.1 vs 34.0 values are not the same column in both tables: Table 16 Russian = 39.1 and Table 17 Russian = 34.0, while Table 17 Hindi = 31.0 and Table 16 Hindi = 34.0. So the two tables likely differ by run or setting, and the paper does not explain it. Treat the Russian GAIA gap as roughly 7-13 pp depending on the table; this is an unresolved inconsistency in the paper.

## 6. Open items
- Check pages 24-25 of the MAPS PDF visually to confirm the Table 20-23 mapping.
- The MAPS Table 16 caption says "GPT-4o vs GPT-O1", but the table rows are GPT-O1 and GPT-4o, so the row labels are consistent.
- MASSIVE: no per-language Polish explanation in the paper; the article's "Polish beat English" claim does not come from this paper.
