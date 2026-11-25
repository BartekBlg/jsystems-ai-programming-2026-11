# Czy AI „mówi najlepiej po polsku”? Weryfikacja źródeł i dane 2025–2026

*Raport z researchu wieloagentowego · stan na 24 listopada 2026*

## TL;DR

1. **Źródłem artykułu cryps.pl jest benchmark OneRuler** (Kim, Russell, Karpińska, Iyyer; UMD / Microsoft / UMass Amherst; arXiv [2503.01996](https://arxiv.org/abs/2503.01996), COLM 2025). Liczby w artykule zgadzają się z publikacją: **polski 88,0% (1. miejsce z 26), angielski 83,9% (6.)**. Dotyczy to jednak wyłącznie syntetycznego zadania *needle-in-a-haystack* (NIAH) przy kontekście 64K–128K tokenów i modeli z przełomu 2024/2025.
2. **Artykuł nadinterpretuje wynik.** Publikacja nie bada „najlepszego języka do promptowania”. Współautorka M. Karpińska powiedziała PAP wprost, że tego nie badano. Do tego każdy język ma w teście **inną książkę** jako „stóg siana” (PL: „Noce i dnie”, EN: „Little Women”), a budżet liczony jest w tokenach. Polski potrzebuje ich ok. 1,7–2,3× więcej na słowo, więc polski stóg zawiera mniej tekstu.
3. **Nikt nie powtórzył tego wyniku.** Przejrzeliśmy 39 prac cytujących OneRuler (2025–2026). Żadna nie podaje danych dla polskiego i angielskiego w długim kontekście, które by go potwierdzały lub obalały.
4. **Nowsze dane (2025–2026) dla innych zadań pokazują, że angielski jest lepszy lub równy:**
   - wiedza i commonsense na **tych samych pytaniach** (Global PIQA, 177 modeli): PL średnio **−7,6 pkt** względem EN. Modele frontier 2026 (GPT-5.4, Gemini 3.1 Pro, Claude Sonnet 4.6) są **na równi** (±2 pkt, w granicach szumu);
   - agenci i narzędzia: wieloturowe gry dialogowe EU-24+ (2026) PL 59,2 vs EN 61,9; wywołania funkcji (MASSIVE-Agents) PL **29,5 vs EN 57,4** (dokładny format argumentów);
   - RAG i wyszukiwanie: embeddery na równoległych fragmentach tracą po polsku ok. 1 pkt nDCG; LLM-y wolą dokumenty **w języku zapytania**, a nie po angielsku.
5. **Wyjątek: wiedza o Polsce.** Dwie prace z 2026 r. (Masked Advantage, PoVisLE) pokazują, że przy pytaniach o polską kulturę i realia **prompt po polsku daje lepsze wyniki** niż po angielsku w modelach frontier (GPT-5.4, Claude Sonnet 4.6 i 5, Gemini 3 Flash). Przy pytaniach „uniwersalnych” angielski nadal prowadzi.
6. **Czy język wpływa na rozumowanie agenta? Tak, umiarkowanie.** Rozumowanie wymuszone w innym języku niż angielski kosztuje średnio ok. 7 pkt. Na GAIA (MAPS) agenci osiągają po angielsku 47,4%, a średnio w innych językach 35,8%. Luka maleje wraz z wielkością i jakością modelu. W modelach frontier 2026 jest często w granicach błędu pomiaru.

---

## 1. Źródło artykułu i co naprawdę mówi publikacja

| Element | Wartość (zweryfikowana w PDF) |
|---|---|
| Publikacja | *One ruler to measure them all: Benchmarking multilingual long-context language models*, arXiv 2503.01996 v3 (30.09.2025), nagłówek „Published as a conference paper at COLM 2025” |
| Języki | 26 |
| Modele | Gemini 1.5 Flash, o3-mini-high, Qwen2.5-72B/7B, Llama 3.3-70B, Llama 3.1-8B, DeepSeek-R1 (tylko EN) |
| Zadania | 5 wariantów NIAH (w tym z odpowiedzią „brak”) + 2 zadania agregacji (CWE) |
| Konteksty | 8K, 32K, 64K, 128K; 50 przykładów na konfigurację |
| Wynik nagłówkowy | PL 88,0% (1.), EN 83,9% (6.), ZH 62,1% (23.); średnia 64K+128K, wszystkie modele |
| Najbardziej jaskrawy przypadek | o3-mini-high przy 128K: EN 67%, PL 92%, UK 89% |
| Analiza kontrolująca ilość informacji (App. D) | angielski „typowo 8.”, wysoko języki słowiańskie (RU, UK, PL) |
| Wyjaśnienia autorów | dostępność danych treningowych, pismo, rodzina językowa, tokenizer; „pozostaje niejasne” m.in. dlaczego chiński wypada słabo |

**Ranking 26 języków** (PL, EN i ZH dokładnie z tekstu, pozostałe odczytane z wykresu Fig. 4b z dokładnością ok. ±0,5 pkt):
pl 88,0 · ru ~86,0 · fr ~85,7 · it ~84,9 · es ~84,5 · **en 83,9** · uk ~83,0 · sv ~82,6 · pt ~82,0 · de ~80,4 · no ~80,0 · nl ~77,6 · hu ~77,3 · da ~77,2 · ja ~75,7 · cs ~73,5 · vi ~73,3 · fi ~73,2 · fa ~71,7 · sr ~71,3 · hi ~65,9 · ko ~65,2 · zh 62,1 · sw ~60,5 · ta ~54,3 · st ~44,0

To **nie jest efekt całej rodziny słowiańskiej**: czeski jest 16., a serbski 20.

### Artykuł a publikacja

| Twierdzenie artykułu | Publikacja | Ocena |
|---|---|---|
| Polski 1. z 26, ok. 88% | 88,0%, 64K/128K | ✅ zgodne |
| Angielski 6., „niespełna 84%” | 83,9% | ✅ zgodne |
| Chiński 62,1% | 62,1%, 23. miejsce | ✅ zgodne |
| „Polski najlepszy do promptowania” (ujęcie medialne) | nie badano; zaprzecza współautorka | ❌ nadinterpretacja |
| Polska gramatyka redukuje niejednoznaczność | brak w publikacji | ❌ brak w źródle |
| „26 innych języków” | 26 łącznie | ⚠️ błąd |
| Modele: Gemini 1.5 Flash, Llama 3, Qwen2.5 | pomija o3-mini i DeepSeek-R1 | ⚠️ niepełne |

### Czynniki zakłócające (confoundery)

- **Różne teksty tła.** Każdy język ma inną książkę (Appendix A). Polski stóg ma 452 847 tokenów źródła, angielski 233 668.
- **Budżet w tokenach, nie w treści.** Polski potrzebuje ok. 1,9× więcej tokenów niż angielski w cl100k_base (Petrov i in. 2023, App. C). We współczesnych tokenizerach jest to 2,2–2,8 tokena na słowo wobec 1,2 dla angielskiego (Bielik v3, arXiv 2604.10799, Tab. 1). Przy tych samych 128K tokenów polski stóg ma mniej słów.
- **Język instrukcji** zmienia wynik nawet o ok. 20 pkt (Fig. 6a). Kontekst polski z instrukcją polską przy 128K daje 86%, a z instrukcją angielską 74%.
- W publikacji jest **niespójność** między tekstem a heatmapą Fig. 6a (wartości dla koreańskiego).
- Mała próba: 50 przykładów na konfigurację. Brak formalnych testów istotności.

---

## 2. Nowsze dane 2025–2026: polski i języki słowiańskie a angielski

### 2.1 Wiedza i commonsense, te same pytania w obu językach

| Benchmark | Model (rok) | EN | PL | PL−EN | Status |
|---|---|---|---|---|---|
| Global PIQA, parallel, 103 pytania ([2510.24081](https://arxiv.org/abs/2510.24081), CSV wyników) | Gemini 3.1 Pro (2026) | 100,0 | 100,0 | 0 | ✅ |
| | GPT-5.4 (2026) | 96,1 | 98,1 | +1,9 | ✅ |
| | Claude Sonnet 4.6 (2026) | 95,1 | 97,1 | +1,9 | ✅ |
| | Gemma 4 31B-it (2026) | 90,3 | 89,3 | −1,0 | ✅ |
| | Qwen3-32B (2025) | 84,5 | 78,6 | −5,8 | ✅ |
| | Llama 3.1 70B-Instruct | 70,9 | 70,9 | 0 | ✅ |
| | **177 modeli (średnia)** | | | **−7,6** | ✅ (PL>EN: 29, PL<EN: 138) |
| | 97 modeli instruct, średnia | 56,9 | 51,2 | −5,7 | ✅ |
| EU21 ([2410.08928](https://arxiv.org/abs/2410.08928), Tab. 1) | Llama-3.1-70B-Instruct | 0,79 | 0,70 | −9 (GSM8K −18) | ✅ |
| Egzaminy medyczne PL/EN ([2412.00559](https://arxiv.org/abs/2412.00559), Tab. 4) | GPT-4o | 88,69 | 89,96 | +1,3 | ✅ |
| | Llama-3.1-70B | 82,60 | 80,94 | −1,7 | ✅ |
| | Qwen2.5-7B | 67,73 | 51,92 | −15,8 | ✅ |
| | Bielik-11B-v2.2 | 56,98 | 62,36 | +5,4 | ✅ |
| Gamayun ([2512.21580](https://arxiv.org/abs/2512.21580)) | małe modele 1–2B | — | — | −2 do −18 | ⚠️ protokół EN/PL niepotwierdzony |

W Global PIQA polski jest **najlepszym językiem słowiańskim** (średnia instruct: PL 51,2; CS 48,8; UK 48,6; SK 47,3; BG 47,0; HR 46,0; SR 44,9; SL 43,7). Przy n=103 błąd standardowy wynosi ok. 3–5 pkt, więc różnice ±2 pkt dla pojedynczego modelu są szumem.

**Oficjalne dane Anthropic (2026, Claude Fable 5.2 / Opus 5.5 / Sonnet 5.5 / Haiku 5.5)**: zero-shot Global-MMLU jako % wyniku angielskiego. Wszystkie 14 języków ma 87–99,8% wyniku EN. **Polskiego ani żadnego języka słowiańskiego nie ma w tabeli.** Strona zaznacza, że różnice poniżej ok. 1 pkt to szum.

**Inne języki słowiańskie, MMLU-ProX** ([2503.10497](https://arxiv.org/abs/2503.10497), te same 11 829 pytań; polskiego brak): czeski średnio ok. −2,5 pkt. Modele rozumujące są prawie na równi (Qwen3-235B-Think CS 80,5 vs EN 80,7; o4-mini 73,5 vs 73,7).

### 2.2 Długi kontekst

- Brak replikacji OneRuler z polskim (39 prac cytujących przejrzanych przez OpenAlex i Semantic Scholar).
- Inne benchmarki długiego kontekstu (MLNeedle, mLongRR, MLRBench, MGAL 2026) **nie obejmują polskiego**.
- COMPASS ([2604.20720](https://arxiv.org/abs/2604.20720)) uruchamia OneRuler dla każdego języka, ale wyniki są tylko na heatmapie (niewyodrębnione).

### 2.3 Wyszukiwanie informacji i RAG

| Ustalenie | Liczby | Źródło | Status |
|---|---|---|---|
| Embeddery na **tych samych** fragmentach (Belebele retrieval) | Qwen3-Embedding-8B: EN 99,37 / PL 98,52 / CS 98,30 / RU 98,62 / UK 98,15 nDCG@10 | repo wyników MTEB | ✅ |
| BM25 po polsku słabszy (tłumaczony BEIR) | NQ 32,6→20,1; HotpotQA 60,2→49,2; MSMARCO 47,7→41,9 | BEIR-PL [2305.19840](https://arxiv.org/abs/2305.19840), Tab. 3 | ✅ |
| Polskie embeddery lepsze od wielojęzycznych na polskim | stella-pl-retrieval-8k 61,59 vs Qwen3-Embedding-8B 59,21 | PL-MTEB [2405.10138](https://arxiv.org/abs/2405.10138) (ACL 2026 Findings) | ✅ |
| Retriever i generator wolą dokumenty **w języku zapytania** | aya-expanse-8b, zapytanie ko: dok. ko 49,66 vs dok. en 38,08 | Park & Lee [2502.11175](https://arxiv.org/abs/2502.11175) (ACL 2025 Findings) | ✅ |
| 68% top-10 w języku zapytania; generacja 42% z natywnych vs 29,6% z obcych | | Faux Polyglot [2407.05502](https://arxiv.org/abs/2407.05502) (NAACL 2025) | ✅ |
| Te same dowody EN, pytanie nie-EN = gorzej | GPT-4o: EN 62,4; DE 55,9; ZH 54,7 | XRAG [2505.10089](https://arxiv.org/abs/2505.10089) | ✅ |
| Cytowania w mRAG gorsze dla rosyjskiego | Llama-3.3-70B: EN 85,9 vs RU 74,5 | „Linguistic Nepotism” [2509.13930](https://arxiv.org/abs/2509.13930) | ✅ |

**Brak dopasowanego badania RAG PL vs EN** (ten sam korpus i pipeline w obu językach). To luka badawcza.

### 2.4 Rozumowanie

| Ustalenie | Liczby | Źródło | Status |
|---|---|---|---|
| MCLM, o3-mini: polski lepszy na AIME, gorszy na M-MO | AIME PL 43,3 vs EN 36,7; M-IMO 37,0 vs 29,6; **M-MO 40 vs 80** | [2502.17407](https://arxiv.org/abs/2502.17407), Tab. 26 | ✅ (n=30, czyli 1 zadanie = 3,3 pkt) |
| MCLM, GPT-4o-mini | AIME PL 13,3 vs EN 33,3; MATH100 72,7 vs 75,8 | Tab. 25 | ✅ |
| Wymuszone rozumowanie w języku użytkownika | średnio ok. −7 pkt; bez wymuszenia tylko ok. 52% śladów w języku docelowym | [2505.22888](https://arxiv.org/abs/2505.22888), Tab. 1 | ✅ |
| Budget forcing pomaga głównie po angielsku | AIME EN +20 pkt vs inne języki +1,94 | [2502.17407](https://arxiv.org/abs/2502.17407) | ✅ |
| PolyMath (bez PL), RU vs EN | Qwen3-235B-Think 38,4 vs 40,0; DeepSeek-R1 37,6 vs 35,2 | [2504.18428](https://arxiv.org/abs/2504.18428), Tab. 5a | ✅ |
| Modele „przechodzą przez angielski” w warstwach ukrytych | 50–70% słów trasowanych przez EN (modele z 2024) | Schut i in. [2502.15603](https://arxiv.org/abs/2502.15603) | ✅ |
| Wspólne cechy wielojęzyczne (Claude 3.5 Haiku) | ok. 74% cech aktywnych we wszystkich 3 językach | Anthropic, *Biology of an LLM* (2025) | ✅ |

### 2.5 Agenci i wywołania narzędzi

| Ustalenie | Liczby | Źródło | Status |
|---|---|---|---|
| MAPS: GAIA po angielsku vs średnia innych | 47,4% vs 35,8%; RU 39,6 vs EN 47,5 (3 przebiegi) | [2505.15935](https://arxiv.org/abs/2505.15935) v3 | ✅ (niespójność RU 39,1 vs 34,0 między Tab. 16 i 17) |
| MAPS: SWE-bench i MATH prawie bez straty | SWE: RU 0,33 vs EN 0,31; MATH 0,507 vs 0,514 | Tab. 21–22 | ✅ |
| MAPS: bezpieczeństwo gorsze poza EN | ASR ataków: RU 0,641 vs EN 0,534 | Tab. 23 | ✅ |
| MASSIVE-Agents, BFCL AST (pełne wywołanie z argumentami) | Nova Premier EN 57,4 / **PL 29,5**; Claude 3.5 Sonnet 53,7 / 19,0 | ACL 2025.findings-emnlp.1099, Tab. 5 | ✅ |
| MASSIVE-Agents, sam wybór funkcji | Nova Premier EN 91,6 / PL 89,5 | Tab. 6 | ✅ |
| Wieloturowe gry dialogowe (2026) | GPT-5.4 EN 91,8 / PL 89,5; Claude Opus 4.8 84,4 / 82,3; średnia 9 modeli 61,9 / 59,2; Gemma-4 50,6 / **59,7** | „Language Equality has a Price” [2608.01395](https://arxiv.org/abs/2608.01395), Tab. 4 | ✅ |
| Rozumowanie kliniczne PL vs EN (2026) | 4 z 6 modeli istotnie lepsze po angielsku; GPT-5 21,5 vs 22,8 | J. Clin. Med. 15(11):4082 | ⚠️ abstrakt i tabela |

### 2.6 Wiedza specyficzna dla Polski i modele multimodalne (2026)

| Ustalenie | Liczby | Źródło | Status |
|---|---|---|---|
| Pytania uniwersalne: angielski lepszy (GlobalGap < 0); pytania o Polskę: polski lepszy (LocalGap > 0). Skala: zdolność IRT w logitach | GPT-5.4: −0,34 / **+0,40**; Claude Sonnet 4.6: −0,40 / **+0,80**; Gemini 3 Flash: −0,32 / **+2,26**; Llama-3.1-8B: −0,89 / −0,36; Qwen3-14B: −0,74 / −1,02 | „The Masked Advantage” [2606.07422](https://arxiv.org/abs/2606.07422), Fig. 2 (odczyt z heatmapy ±0,02) | ✅ (sprawdzone osobiście na wykresie) |
| Wniosek autorów | słabszy wynik w języku lokalnym zwykle oznacza słabszą biegłość w tym języku, a nie brak wiedzy kulturowej | tamże, abstrakt | ✅ |
| Polski benchmark VLM: prompt PL vs prompt EN (to samo zdjęcie) | Qwen3.5-397B (thinking) PL 75,16 / EN 68,12; Claude Sonnet 5 69,94 / 66,94; **GPT-5.4 68,35 / 70,40**; Qwen3.5-9B 38,75 / 47,04 | PoVisLE [2608.07763](https://arxiv.org/abs/2608.07763), Tab. 14 | ✅ (n=465, jeden przebieg) |

Wniosek: **mocne modele lepiej odpowiadają po polsku na pytania o polskie realia**, a słabe modele zyskują na tłumaczeniu na angielski. To jedyny obszar poza OneRuler, w którym polski systematycznie wygrywa z angielskim.

---

## 3. Odpowiedzi na pytania badawcze

**Czy język polski jest skuteczniejszy od angielskiego?**
W dwóch wąskich obszarach. Pierwszy to OneRuler NIAH (długi kontekst, starsze modele), z istotnymi czynnikami zakłócającymi. Drugi to pytania o polską kulturę i realia w mocnych modelach z 2026 r. W pozostałych zastosowaniach polski jest **równy** angielskiemu (modele frontier 2026) albo **gorszy** (modele mniejsze i otwarte, wywołania narzędzi, wymuszone rozumowanie).

**Czy język wpływa na RAG?**
Tak, ale kluczowa jest **zgodność języka zapytania i dokumentów**, a nie wyższość angielskiego. Retrievery i generatory wolą dokumenty w języku zapytania. Nowoczesne embeddery mają po polsku stratę ok. 1 pkt. Klasyczne BM25 traci po polsku wyraźnie przez fleksję.

**Czy język wpływa na poziom rozumowania agenta?**
Tak, mierzalnie, ale zależnie od zadania:
- największa strata dotyczy **precyzyjnego formatu wywołań narzędzi** (argumenty) i złożonych zadań webowych (GAIA);
- najmniejsza dotyczy matematyki i kodu (SWE-bench, MATH: <1 pkt);
- rozumowanie w języku angielskim jest zwykle lepsze niż wymuszone w innym języku;
- luka **maleje wraz z jakością modelu**, a dla frontier 2026 często mieści się w szumie.

### Praktyczne wnioski (dla zespołów GIS / Python / ETL)
1. **Trzymaj korpus i zapytania w tym samym języku.** Przy polskich dokumentach pytaj po polsku. Rozważ polskie embeddery (PL-MTEB) lub hybrydę BM25 z lematyzacją.
2. **Schematy narzędzi, nazwy funkcji i system prompt agenta pisz po angielsku.** Odpowiedź dla użytkownika może być po polsku.
3. **Nie wymuszaj języka śladu rozumowania.** Pozwól modelowi „myśleć” po angielsku.
4. **Licz koszt tokenów.** Polski tekst to ok. 1,7–2,3× więcej tokenów, czyli wyższy koszt i mniej treści w oknie kontekstu.
5. **Pytania o polskie realia** (przepisy, nazwy miejscowości, instytucje) zadawaj po polsku. W silnych modelach to realnie pomaga.
6. **Zrób własny mały eval PL/EN** na swoich danych zamiast polegać na rankingach medialnych.

---

## 4. Metodologia

- **Agenci:** 17 uruchomień subagentów Haiku (6 w iteracji 1 i 11 weryfikacyjnych/uzupełniających, plus 1 pytanie kontrolne), koordynowanych przez agenta prowadzącego (kontrola jakości, walidacja, feedback).
- **Zasady:** dowodem były tylko publikacje, preprinty, oficjalne leaderboardy, karty modeli i surowe pliki wyników. Media i fora służyły tylko jako drogowskazy. Statusy: VERIFIED / PARTIAL / UNVERIFIED.
- **Iteracje:**

| # | Cel | Wynik |
|---|---|---|
| 1 | 6 agentów: źródło artykułu, benchmarki ogólne, RAG, rozumowanie/agenci, długi kontekst + tokenizacja, inne języki słowiańskie | identyfikacja OneRuler, mapa dowodów |
| 2 | V1–V4: weryfikacja OneRuler (LaTeX + wykresy), liczb RAG, Global-MMLU i kart modeli, MCLM/MAPS/PolyMath | korekty (np. Swahili 90,8→59,6; mmlw-e5-base), nowe dane Belebele |
| 3 | V5–V6: audyt 11 identyfikatorów arXiv z 2026, nowe LLM-y (Global PIQA, EuroEval) | 0 halucynacji ID; korekta Bielik 82,98/77,41 (różne modele, nie „przed/po”) |
| 4 | V7–V8: 39 prac cytujących OneRuler; wykresy Global-MMLU; strona Anthropic | brak replikacji; Global-MMLU nie ma wyników per język (korekta założenia) |
| 5 | V9–V10: rozstrzygnięcie kolumn MASSIVE-Agents; „adwokat diabła” | potwierdzenie PL 29,5 vs EN 57,4; brak kontrdowodów |
| 6 | V11: Masked Advantage (2026), PoVisLE (2026), CUS-QA, książki w OneRuler | nowy niuans §2.6; **korekta**: liczby CUS-QA podane przez V10 nie zgadzają się z Tab. 11, więc wykluczono je z raportu |

- **Kontrole lidera (spot-checki w surowym tekście PDF/CSV):** liczby OneRuler (88,0 / 83,9 / 62,1 / o3-mini 67 vs 92), MCLM Tab. 25–26, MASSIVE-Agents, strona Anthropic (grep: 0 trafień „Polish”), Global PIQA (przeliczenie 177 modeli), EU-24+ Tab. 4.
- **Narzędzia:** do czytania tabel z PDF doinstalowaliśmy w trakcie `pypdf`, a wykresy wektorowe renderowaliśmy przez ghostscript.

## 5. Ograniczenia
- Ranking OneRuler poza PL/EN/ZH jest odczytany z wykresu (±0,5 pkt).
- Brak oficjalnych tabel PL vs EN dla Claude, GPT-5.x i Gemini 3 na benchmarkach wiedzy. Global PIQA to jedyne źródło równoległe z modelami frontier 2026, a ma tylko 103 pytania na język.
- Część prac z 2026 r. (COMPASS, Masked Advantage) podaje wyniki dla polskiego tylko na wykresach.
- Dane o agentach dla polskiego pochodzą głównie z MASSIVE-Agents (modele z 2024–2025) i EU-24+ (gry dialogowe, nie benchmarki narzędziowe).

## 6. Pliki robocze
Pełne raporty agentów (`r1_*.md` … `r6_*.md`) i brief są w katalogu `research/` sesji. Wersje interaktywne: artefakty PL i EN.
