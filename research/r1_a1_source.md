# R1 A1: Primary source of the cryps.pl article (Polish AI ranking claim)

Status summary: the hypothesis is CONFIRMED. The article is a media report on the OneRuler paper (arXiv 2503.01996). The core numbers match the paper. Several article claims are not in the paper, and the paper does not claim Polish is best for prompting.

Note on method: paper text was read through WebFetch of arxiv.org/html (model-summarised extraction, with some truncation). The PDF fetch failed (10 MB limit). Numbers below are VERIFIED in the paper text as returned, but the top-10 table (Fig. 4b) and per-language Polish cross-lingual numbers were NOT read. Those items are marked PARTIAL.

### 1. Summary

- Article: cryps.pl, "Sztuczna inteligencja 'mówi' po polsku? Nasz język zdeklasował angielski w najnowszym rankingu", by Maciej Halikowski, published 25 Oct 2025. It names the OneRuler paper and links arxiv.org/pdf/2503.01996. The paper is the primary source.
- Paper: Kim, Russell, Karpinska, Iyyer. "One ruler to measure them all: Benchmarking multilingual long-context language models." arXiv 2503.01996 (v1 3 Mar 2025, v3 30 Sep 2025, cs.CL). The v3 is described as the COLM 2025 version by secondary sources. The arXiv page itself names no venue; this is UNVERIFIED from the primary page.
- Ranking: English is 6th of 26 and Polish is 1st, on long contexts (64K and 128K). Averages are 83.9% (English) and 88% (Polish). Chinese is 23rd of 26 (4th worst) with 62.1%.
- The ranking is an average over NIAH tasks and models, at long context, on one book per language. The authors do not claim that Polish is a better language for AI or for prompting. Co-author Marzena Karpińska (Microsoft) said publicly (via PAP) that prompting was not studied and that Polish being best cannot be concluded from the study.
- Article errors and additions: it says "26 innych języków" (26 other languages) while the paper has 26 languages total. It omits models (o3-mini, DeepSeek-R1) and adds explanations (Polish grammar reduces ambiguity; English as foundation) that are not in the paper.

### 2. Findings table

| # | Claim | Exact numbers | Models | Languages | Primary source | Location | Date | Status |
|---|---|---|---|---|---|---|---|---|
| 1 | OneRuler covers 26 languages | 26 | n/a | 26 (list below) | arXiv 2503.01996 | Sec. 2 "Languages"; Abstract | 3 Mar 2025 (v1), 30 Sep 2025 (v3) | VERIFIED |
| 2 | Polish ranks 1st on long-context NIAH | avg NIAH accuracy 88% "across all models" at 64K and 128K | all models pooled | pl | arXiv 2503.01996 | Sec. 3.1; Conclusion ("Polish taking the top spot") | as above | VERIFIED |
| 3 | English ranks 6th of 26 | avg NIAH accuracy 83.9% (article: "niespełna 84%", matches) at 64K and 128K | all models pooled | en | arXiv 2503.01996 | Intro; Abstract ("ranked 6th out of 26"); Sec. 3.1 | as above | VERIFIED |
| 4 | Chinese is low | 62.1% average, rank 23 of 26 ("4th worst") | all models pooled | zh | arXiv 2503.01996 | Sec. 3.1 | as above | VERIFIED (number and "4th worst" from text; article gives 62.1% and no rank) |
| 5 | Ranking refers to 64k and 128k | n/a | n/a | n/a | arXiv 2503.01996 | Sec. 3.1 ("at context lengths of 64K and 128K..."); Fig. 4 caption | n/a | VERIFIED |
| 6 | Top 10 are Slavic, Romance, Germanic | n/a | n/a | n/a | arXiv 2503.01996 | Sec. 3.1 text | n/a | PARTIAL (family claim in text; exact ranks 2-5 and 7-10 are in Fig. 4b, not read) |
| 7 | Tasks: NIAH variants incl. multi-value/multi-key/none, and aggregation | 5 NIAH variants + 2 CWE variants = 7 tasks | n/a | n/a | arXiv 2503.01996 | Sec. 2 | n/a | VERIFIED |
| 8 | Context lengths tested | 8K, 32K, 64K, 128K; 50 examples per configuration | n/a | n/a | arXiv 2503.01996 | Sec. 2 | n/a | VERIFIED |
| 9 | Models evaluated | Open: Qwen2.5-72B-Instruct, Qwen2.5-7B-Instruct, Llama-3.3-70B-Instruct, Llama-3.1-8B-Instruct (text says "7B"; Table 5 says 8B), DeepSeek-R1 (English only). Closed: gemini-1.5-flash, o3-mini-2025-01-31 (high effort) | 7 | n/a | arXiv 2503.01996 | Sec. 3 "Model selection"; Table 5, App. B | n/a | VERIFIED (text-extraction; check Table 5) |
| 10 | "None" option reduces accuracy | S-NIAH accuracy drop of 32% at 128K in English when none sentence is added | several | en | arXiv 2503.01996 | Sec. 4; Fig. 1b; App. C.1 | n/a | VERIFIED |
| 11 | Cross-lingual: instruction language changes results | up to 20%; English context + Korean instructions at 64K: 91% to 71%; Korean context + English instructions at 128K: 61% to 77% | models per Fig. 6a | en, ko, pl (pl numbers not read) | arXiv 2503.01996 | Abstract; Sec. 4; Fig. 6a | n/a | VERIFIED for en/ko; Polish cross-lingual numbers PARTIAL |
| 12 | Authors' explanation: data availability and script/family/tokenizer | n/a | n/a | n/a | arXiv 2503.01996 | Sec. 6 Conclusion; App. D (tokenizer gains for Korean, Hindi, Chinese) | n/a | VERIFIED |
| 13 | Polish has the best structure / reduces ambiguity | n/a | n/a | n/a | not found in paper | n/a | n/a | NOT IN PAPER (article's own speculation, attributed vaguely to "researchers") |
| 14 | Polish is best for prompting (media claim) | n/a | n/a | n/a | Karpińska via PAP, naukawpolsce.pl | interview | Oct/Nov 2025 | REFUTED by co-author ("Nie. Wcale tego nie badaliśmy."); paper has no such claim |
| 15 | Microsoft denial of the Polish-best claim | n/a | n/a | n/a | Spider's Web article (Marcin Kusz, 12 Nov 2025) | secondary | 12 Nov 2025 | UNVERIFIED: no Microsoft statement found; the only attributable denial is Karpińska's (who is Microsoft-affiliated per the PAP piece and the paper's email domain) |

Full list of 26 languages (paper, Sec. 2): Chinese, Czech, Danish, Dutch, English, Finnish, French, German, Hindi, Hungarian, Italian, Japanese, Korean, Norwegian, Persian, Polish, Portuguese, Russian, Serbian, Sesotho, Spanish, Swahili, Swedish, Tamil, Ukrainian, Vietnamese.

### 3. Details per source

**Primary: arXiv 2503.01996 (OneRuler).** Adapts English RULER (Hsieh et al., 2024) to 26 languages. The authors wrote English instructions and native speakers translated them. Each language has one open-domain, non-copyrighted book (Table 4 lists original publication language). The NIAH filler context is built from that book. Metric is accuracy. Affiliations on the first page: University of Maryland, College Park; Microsoft; UMass Amherst. The emails suggest Kim, Russell and Iyyer are at UMD and Karpińska at Microsoft. The paper says NSF funded it (IIS-2046248, IIS-2312949, IIS-2202506). Code: github.com/mungg/OneRuler (MIT; the README matches the paper; it has no result tables, only a heatmap image; 27 commits; 52 stars). Limitations the authors state: the none option makes models over-cautious, and many models may have been post-trained on NIAH without it. Source-text differences per language are a confounder: the text says "one per language" and "most of the books are older due to copyright restrictions." It does not say the books are non-translations. Model choice and book choice are therefore confounds for the Polish result.

**Authors' explanation for the ranking.** The conclusion says the differences "stem from factors such as pretraining data availability, script, language family, and tokenizer specifications." The text notes that all 4 low-resource languages rank in the bottom 6. It says "it remains unclear why some high-resource languages like Chinese fare worse than anticipated." A footnote attributes Chinese errors mainly to incorrect "none" answers. Appendix D (information-content-controlled analysis) says English is typically 8th there, and that Slavic languages (Russian, Ukrainian, Polish) "perform highly." Korean, Hindi and Chinese sit at the low end, which the authors attribute partly to tokenizer gains in newer tokenizers. No claim that Polish is best for prompting. The paper does not study prompting at all.

**Co-author statements (secondary, reported by PAP; used as pointers only).** Marzena Karpińska (Microsoft) told PAP: "Nie. Wcale tego nie badaliśmy" (No. We didn't study that at all), and "na pewno nie możemy na jego podstawie stwierdzić, że polski jest najlepszym językiem" (we certainly cannot conclude from it that Polish is the best language). She also said: "Stworzyliśmy narzędzie do diagnozowania modeli językowych" (we created a tool for diagnosing language models). The book-level explanation (Noce i dni, which the journalist suggests may be easier to search) is the journalist's hypothesis in indirect speech, not a direct Karpińska quote. Reported via naukawpolsce.pl news 110407; the Polish-language primary PAP text is the one to cite if needed.

**Media article (cryps.pl).** Author Maciej Halikowski, 25 Oct 2025. Names OneRuler, the paper, the arXiv link and the four authors. Institutions: "m.in." Microsoft and University of Maryland. Claims Polish 1st of "26 analyzed languages" with ~88%; English 6th with "niespełna 84%"; Chinese 62.1% (no rank). Models: Gemini 1.5 Flash, "advanced versions" of Llama 3, Qwen2.5. Contexts: 64k and 128k. Errors: "26 innych języków" conflicts with 26 total. Also omits o3-mini and DeepSeek-R1. Adds claims not in the paper (Polish complexity aids precision; Polish structure reduces ambiguity; English is the foundation of training). The article does not link any source beyond the arXiv PDF.

**Secondary coverage (pointers only).** Several Polish outlets (Spider's Web, Benchmark.pl, iMagazine, Android.com.pl, Nauka w Polsce, Science in Poland, Polskie Radio, National Geographic PL, PPC Land, Notebookcheck) reported the same benchmark. Spider's Web's headline "Microsoft dementuje" credits Microsoft, but the body's only attributable denial is Karpińska's. Benchmark.pl and iMagazine repeat the "Polish best for AI" framing. None cites a different primary source than arXiv 2503.01996.

### 4. Search log

Queries and fetches:
1. WebFetch cryps.pl article: identified OneRuler, the arXiv link, figures and claims.
2. WebFetch arxiv.org/abs/2503.01996: title, authors, dates (v1 3 Mar 2025, v3 30 Sep 2025), no venue on the page.
3. WebFetch arxiv.org/html/2503.01996 (two passes, incl. offset 100000): languages, models, tasks, contexts, rank, cross-lingual, none option. Truncated at Fig. 4b.
4. WebFetch arxiv.org/pdf/2503.01996v3: failed (10 MB limit).
5. WebFetch arxiv.org/html/2503.01996v3: rank quotes, 64K/128K wording, no prompting claim, book-per-language, affiliations, NSF grant. Second pass: conclusion, Appendix D, Figure 6.
6. WebSearch "OneRuler multilingual long-context benchmark Polish first English sixth COLM 2025": found arxiv v1/v2/v3, github mungg/OneRuler, and the COLM claim in secondary text.
7. WebSearch "One ruler to measure them all Kim Russell Karpinska Iyyer github": only arXiv; no repo surfaced here (repo found in query 6).
8. WebFetch github.com/mungg/OneRuler: MIT license, README matches the paper, no result tables.
9. WebSearch "OneRuler polski język najlepszy AI długi kontekst ranking": Polish media, Microsoft denial headline, PAP/naukawpolsce links.
10. WebFetch spidersweb.pl Microsoft denial article: no Microsoft statement found; only Karpińska's quote.
11. WebFetch scienceinpoland.pl: Karpińska PAP quotes.
12. WebFetch naukawpolsce.pl: primary Polish quotes.

Dead ends: the PDF fetch (too large); the top-10 ranking list (in Figure 4b, an image; not extracted); a Microsoft official statement (not found); the COLM venue listing (not on arXiv page; not checked directly on OpenReview or the COLM site).

Secondary sources that led to primary: cryps.pl gave the arXiv 2503.01996 link. PAP/naukawpolsce gave the Karpińska statements.

### 5. Open questions / items that need further verification

1. Top 10 languages and exact ranks: read Fig. 4(b) image or the data in the GitHub repo results (not in README). Need PDF or image access. Ranks 2-5 and 7-10 are unverified.
2. Polish and English cross-lingual numbers in Fig. 6 (Polish contexts with other instruction languages): not read.
3. Models table: confirm Table 5 (Llama-3.1-8B vs "7B" in text) and the exact o3-mini and DeepSeek versions.
4. Confirm COLM 2025 acceptance on the COLM or OpenReview page (not checked directly; secondary sources say it).
5. Confirm the Microsoft official statement, if any (no primary found; the Spider's Web headline is unsupported by its body).
6. Polish-language PAP original (naukawpolsce article) full text read only in part; the date is only partly confirmed (article updated 10 Nov 2025).
7. Per-model results (the paper has them in appendix tables and figures); not extracted.
8. Whether any NIAH "none" effect applies to the Polish ranking: the ranking is pooled across models including o3-mini, which over-answers "none"; this is a confounder worth a sensitivity check using the GitHub code.

Tips for next time (for the team): a 10 MB PDF limit means fetching the arXiv HTML version or the PDF via `curl` plus `pdftotext` in a scratch folder works better. Also, in Claude Code, `gh api repos/mungg/OneRuler/contents` can pull the evaluation outputs so the ranking can be recomputed rather than read from the paper's figures.
