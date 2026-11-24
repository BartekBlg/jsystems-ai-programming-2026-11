import json, sys, pathlib, html
B = pathlib.Path(__file__).parent
OUT = pathlib.Path(sys.argv[1])
tpl = (B / "template.html").read_text()
piqa = json.loads((B / "piqa.json").read_text())

RANK = [["pl",88.0,"slavic",1],["ru",86.0,"slavic",0],["fr",85.7,"romance",0],["it",84.9,"romance",0],["es",84.5,"romance",0],
 ["en",83.9,"germanic",1],["uk",83.0,"slavic",0],["sv",82.6,"germanic",0],["pt",82.0,"romance",0],["de",80.4,"germanic",0],
 ["no",80.0,"germanic",0],["nl",77.6,"germanic",0],["hu",77.3,"uralic",0],["da",77.2,"germanic",0],["ja",75.7,"other",0],
 ["cs",73.5,"slavic",0],["vi",73.3,"other",0],["fi",73.2,"uralic",0],["fa",71.7,"other",0],["sr",71.3,"slavic",0],
 ["hi",65.9,"other",0],["ko",65.2,"other",0],["zh",62.1,"other",1],["sw",60.5,"other",0],["ta",54.3,"other",0],["st",44.0,"other",0]]
TOK = [["Llama 3.2",2.81,1.17],["Qwen3",2.69,1.17],["Gemma 3",2.20,1.20],["EuroLLM",1.88,1.27],["Mistral v0.1",3.22,1.28],["Bielik v3 (APT4)",1.62,1.98]]

A="https://arxiv.org/abs/"
# cat, lang, label{pl,en}, cmp, year, gap, nums, note{pl,en}, src, url, status
EV = [
 ("longctx","pl",{"pl":"OneRuler NIAH, średnia 64K+128K","en":"OneRuler NIAH, 64K+128K mean"},"PL vs EN","2025",4.1,"PL 88,0 · EN 83,9",
  {"pl":"Syntetyczne wyszukiwanie „igły w stogu siana”; 7 modeli z przełomu 2024/25; różne książki per język.","en":"Synthetic needle-in-a-haystack retrieval; 7 models from late 2024/early 2025; a different book per language."},"arXiv 2503.01996, §3",A+"2503.01996","V"),
 ("longctx","pl",{"pl":"OneRuler, o3-mini-high przy 128K","en":"OneRuler, o3-mini-high at 128K"},"PL vs EN","2025",25.0,"PL 92 · EN 67 · UK 89",
  {"pl":"Najbardziej jaskrawy przypadek w publikacji; autorzy wskazują na słabą średnią o3-mini.","en":"The most striking case in the paper; the authors point to o3-mini's low average."},"arXiv 2503.01996, §3.1",A+"2503.01996","V"),
 ("knowledge","pl",{"pl":"Global PIQA: GPT-5.4","en":"Global PIQA: GPT-5.4"},"PL vs EN","2026",2.0,"PL 98,1 · EN 96,1 (n=103)",
  {"pl":"Te same pytania przetłumaczone z angielskiego; ±2 pkt to 2 pytania, w granicach szumu.","en":"Same questions translated from English; ±2 pts equals 2 questions, within noise."},"arXiv 2510.24081 + CSV",A+"2510.24081","V"),
 ("knowledge","pl",{"pl":"Global PIQA: Gemini 3.1 Pro","en":"Global PIQA: Gemini 3.1 Pro"},"PL vs EN","2026",0.0,"PL 100 · EN 100",{"pl":"Sufit benchmarku.","en":"Benchmark ceiling."},"arXiv 2510.24081 + CSV",A+"2510.24081","V"),
 ("knowledge","pl",{"pl":"Global PIQA: Claude Sonnet 4.6","en":"Global PIQA: Claude Sonnet 4.6"},"PL vs EN","2026",2.0,"PL 97,1 · EN 95,1",{"pl":"W granicach szumu.","en":"Within noise."},"arXiv 2510.24081 + CSV",A+"2510.24081","V"),
 ("knowledge","pl",{"pl":"Global PIQA: Qwen3-32B","en":"Global PIQA: Qwen3-32B"},"PL vs EN","2025",-5.9,"PL 78,6 · EN 84,5",{"pl":"Model otwarty średniej wielkości.","en":"Mid-size open model."},"arXiv 2510.24081 + CSV",A+"2510.24081","V"),
 ("knowledge","pl",{"pl":"Global PIQA: średnia 177 modeli","en":"Global PIQA: mean of 177 models"},"PL vs EN","2025–26",-7.6,"PL>EN: 29 · PL<EN: 138 · remis/tie: 10",
  {"pl":"Przeliczone przez lidera z surowego CSV (split parallel).","en":"Recomputed by the lead from the raw CSV (parallel split)."},"global-piqa results_per_language.csv",A+"2510.24081","V"),
 ("knowledge","pl",{"pl":"EU21 (tłumaczone MMLU/ARC/GSM8K…): Llama-3.1-70B","en":"EU21 (translated MMLU/ARC/GSM8K…): Llama-3.1-70B"},"PL vs EN","2024",-9.0,"PL 0,70 · EN 0,79 · GSM8K −18",
  {"pl":"Jeden model; pozycje tłumaczone maszynowo.","en":"Single model; machine-translated items."},"arXiv 2410.08928, Tab. 1",A+"2410.08928","V"),
 ("knowledge","pl",{"pl":"Egzamin lekarski LEK: GPT-4o","en":"Polish medical exam LEK: GPT-4o"},"PL vs EN","2024",1.3,"PL 89,96 · EN 88,69",{"pl":"Dopasowane pytania PL/EN; domena medyczna.","en":"Matched PL/EN questions; medical domain."},"arXiv 2412.00559, Tab. 4",A+"2412.00559","V"),
 ("knowledge","pl",{"pl":"Egzamin lekarski LEK: Qwen2.5-7B","en":"Polish medical exam LEK: Qwen2.5-7B"},"PL vs EN","2024",-15.8,"PL 51,92 · EN 67,73",{"pl":"Mały model: duża luka.","en":"Small model: large gap."},"arXiv 2412.00559, Tab. 4",A+"2412.00559","V"),
 ("knowledge","pl",{"pl":"Egzamin lekarski LEK: Bielik-11B-v2.2","en":"Polish medical exam LEK: Bielik-11B-v2.2"},"PL vs EN","2024",5.4,"PL 62,36 · EN 56,98",{"pl":"Polski model lepszy po polsku.","en":"The Polish model scores higher in Polish."},"arXiv 2412.00559, Tab. 4",A+"2412.00559","V"),
 ("knowledge","cs",{"pl":"MMLU-ProX: o4-mini (czeski)","en":"MMLU-ProX: o4-mini (Czech)"},"CS vs EN","2025",-0.2,"CS 73,5 · EN 73,7",{"pl":"Te same 11 829 pytań; polskiego brak w benchmarku.","en":"Same 11,829 questions; Polish not in the benchmark."},"arXiv 2503.10497, Tab. 3",A+"2503.10497","V"),
 ("knowledge","cs",{"pl":"MMLU-ProX: DeepSeek-V3 (czeski)","en":"MMLU-ProX: DeepSeek-V3 (Czech)"},"CS vs EN","2025",-4.9,"CS 74,7 · EN 79,6",{"pl":"","en":""},"arXiv 2503.10497, Tab. 3",A+"2503.10497","V"),
 ("retrieval","pl",{"pl":"Belebele retrieval: Qwen3-Embedding-8B","en":"Belebele retrieval: Qwen3-Embedding-8B"},"PL vs EN","2025",-0.85,"nDCG@10 PL 98,52 · EN 99,37",{"pl":"Te same fragmenty w obu językach; nowoczesny embedder prawie bez straty.","en":"Identical passages in both languages; a modern embedder loses almost nothing."},"MTEB results repo",A+"2502.13595","V"),
 ("retrieval","pl",{"pl":"BEIR-PL: BM25 na NQ","en":"BEIR-PL: BM25 on NQ"},"PL vs EN","2023",-12.5,"nDCG@10 PL 20,1 · EN 32,6",{"pl":"Fleksja szkodzi wyszukiwaniu leksykalnemu; korpus tłumaczony.","en":"Inflection hurts lexical search; translated corpus."},"arXiv 2305.19840, Tab. 3",A+"2305.19840","V"),
 ("retrieval","ru",{"pl":"mRAG cytowania: Llama-3.3-70B (rosyjski)","en":"mRAG citation accuracy: Llama-3.3-70B (Russian)"},"RU vs EN","2025",-11.4,"RU 74,5 · EN 85,9",{"pl":"„Linguistic Nepotism”: model częściej cytuje źródła angielskie.","en":"\"Linguistic Nepotism\": the model cites English sources more readily."},"arXiv 2509.13930, Tab. 1",A+"2509.13930","V"),
 ("reasoning","pl",{"pl":"MCLM AIME: o3-mini","en":"MCLM AIME: o3-mini"},"PL vs EN","2025",6.7,"PL 43,3 · EN 36,7 (n=30)",{"pl":"Różnica 2 zadań; mała próba.","en":"A 2-problem difference; small sample."},"arXiv 2502.17407, Tab. 26",A+"2502.17407","V"),
 ("reasoning","pl",{"pl":"MCLM M-MO: o3-mini","en":"MCLM M-MO: o3-mini"},"PL vs EN","2025",-40.0,"PL 40 · EN 80",{"pl":"Olimpiady krajowe; bardzo mała próba.","en":"National olympiads; very small sample."},"arXiv 2502.17407, Tab. 26",A+"2502.17407","V"),
 ("reasoning","pl",{"pl":"MCLM AIME: GPT-4o-mini","en":"MCLM AIME: GPT-4o-mini"},"PL vs EN","2025",-20.0,"PL 13,3 · EN 33,3",{"pl":"","en":""},"arXiv 2502.17407, Tab. 25",A+"2502.17407","V"),
 ("reasoning","multi",{"pl":"Wymuszone rozumowanie w języku użytkownika","en":"Reasoning trace forced into the user's language"},"X vs std","2025",-7.1,"śr./avg 6 modeli, 11 języków",{"pl":"Bez polskiego; modele R1-distill i Skywork.","en":"No Polish; R1-distill and Skywork models."},"arXiv 2505.22888, Tab. 1",A+"2505.22888","V"),
 ("reasoning","ru",{"pl":"PolyMath: DeepSeek-R1 (rosyjski)","en":"PolyMath: DeepSeek-R1 (Russian)"},"RU vs EN","2025",2.4,"RU 37,6 · EN 35,2",{"pl":"","en":""},"arXiv 2504.18428, Tab. 5a",A+"2504.18428","V"),
 ("agents","pl",{"pl":"MASSIVE-Agents, pełne wywołanie funkcji: Nova Premier","en":"MASSIVE-Agents, full function call: Nova Premier"},"PL vs EN","2025",-27.9,"AST PL 29,5 · EN 57,4",{"pl":"Dokładne dopasowanie argumentów; polski 37. z 52 języków.","en":"Exact argument match; Polish ranks 37th of 52 languages."},"ACL 2025.findings-emnlp.1099, Tab. 5","https://aclanthology.org/2025.findings-emnlp.1099/","V"),
 ("agents","pl",{"pl":"MASSIVE-Agents, wybór funkcji: Nova Premier","en":"MASSIVE-Agents, function selection: Nova Premier"},"PL vs EN","2025",-2.1,"PL 89,5 · EN 91,6",{"pl":"Model wie, którą funkcję wywołać; myli się w argumentach.","en":"The model picks the right function; it errs on the arguments."},"ACL 2025.findings-emnlp.1099, Tab. 6","https://aclanthology.org/2025.findings-emnlp.1099/","V"),
 ("agents","ru",{"pl":"MAPS GAIA: agenci (rosyjski)","en":"MAPS GAIA: agents (Russian)"},"RU vs EN","2026",-7.9,"RU 39,6 · EN 47,5",{"pl":"Średnia 3 przebiegów; polskiego brak.","en":"Mean of 3 runs; no Polish."},"arXiv 2505.15935 v3, Tab. 20",A+"2505.15935","V"),
 ("agents","pl",{"pl":"Gry dialogowe EU-24+: GPT-5.4","en":"EU-24+ dialogue games: GPT-5.4"},"PL vs EN","2026",-2.3,"PL 89,5 · EN 91,8",{"pl":"14 wieloturowych gier; clemscore.","en":"14 multi-turn games; clemscore."},"arXiv 2608.01395, Tab. 4",A+"2608.01395","V"),
 ("agents","pl",{"pl":"Gry dialogowe EU-24+: średnia 9 modeli","en":"EU-24+ dialogue games: 9-model mean"},"PL vs EN","2026",-2.7,"PL 59,2 · EN 61,9",{"pl":"Gemma-4 jako wyjątek: PL 59,7 vs EN 50,6.","en":"Gemma-4 is the exception: PL 59.7 vs EN 50.6."},"arXiv 2608.01395, Tab. 4",A+"2608.01395","V"),
 ("local","pl",{"pl":"PoVisLE (pytania o Polskę, VLM): Claude Sonnet 5","en":"PoVisLE (Poland-specific, VLM): Claude Sonnet 5"},"PL vs EN","2026",3.0,"PL 69,94 · EN 66,94",{"pl":"Prompt PL vs przetłumaczony EN, to samo zdjęcie.","en":"PL prompt vs translated EN prompt, same image."},"arXiv 2608.07763, Tab. 14",A+"2608.07763","V"),
 ("local","pl",{"pl":"PoVisLE: Qwen3.5-397B (thinking)","en":"PoVisLE: Qwen3.5-397B (thinking)"},"PL vs EN","2026",7.0,"PL 75,16 · EN 68,12",{"pl":"","en":""},"arXiv 2608.07763, Tab. 14",A+"2608.07763","V"),
 ("local","pl",{"pl":"PoVisLE: GPT-5.4","en":"PoVisLE: GPT-5.4"},"PL vs EN","2026",-2.1,"PL 68,35 · EN 70,40",{"pl":"Jedyny mocny model lepszy po angielsku.","en":"The only strong model that does better in English."},"arXiv 2608.07763, Tab. 14",A+"2608.07763","V"),
 ("local","pl",{"pl":"PoVisLE: Qwen3.5-9B","en":"PoVisLE: Qwen3.5-9B"},"PL vs EN","2026",-8.3,"PL 38,75 · EN 47,04",{"pl":"Słaby model zyskuje na angielskim.","en":"A weak model gains from English."},"arXiv 2608.07763, Tab. 14",A+"2608.07763","V"),
]

C = {
"pl": dict(
 title="Polski kontra angielski w LLM", lang_name="Polski", eyebrow="Raport z researchu · listopad 2026", otherlang="English version →",
 h1="Czy AI naprawdę „mówi najlepiej po polsku”?",
 lede="Głośny ranking, w którym polski pokonał angielski, pochodzi z jednego benchmarku długiego kontekstu (OneRuler, COLM 2025). Sprawdziliśmy źródło i zebraliśmy nowsze dane z lat 2025–2026: wiedza, wyszukiwanie i RAG, rozumowanie, agenci.",
 ruler_aria="Skala dokładności OneRuler: polski 88,0%, angielski 83,9%, chiński 62,1%",
 verdicts=[("Źródło: OneRuler","Liczby w artykule są poprawne: PL 88,0%, EN 83,9%. Dotyczą jednak tylko syntetycznego wyszukiwania w długim kontekście.","ok","Potwierdzone"),
  ("Nadinterpretacja","Publikacja nie bada „najlepszego języka do promptowania”. Współautorka mówi wprost, że tego nie badano.","warn","Korekta"),
  ("Nowsze dane","W modelach frontier 2026 polski jest na równi z angielskim; w mniejszych modelach i przy narzędziach jest gorszy.","en","Angielski ≥ polski"),
  ("Wyjątek","Pytania o polskie realia: mocne modele odpowiadają lepiej po polsku.","pl","Polski > angielski")],
 s1_eye="1 · Źródło", s1_h="Ranking 26 języków w OneRuler",
 s1_p="Średnia dokładność needle-in-a-haystack przy 64K i 128K tokenów, uśredniona po modelach. Polski jest pierwszy, ale czeski i serbski są w drugiej połowie, więc to nie jest efekt „języków słowiańskich”. Kliknij rodzinę językową, żeby ją wyróżnić.",
 s1_chips_aria="Filtr rodzin językowych",
 s1_note="PL, EN i ZH dokładnie z tekstu publikacji; pozostałe wartości (~) odczytane z wykresu Fig. 4b, ±0,5 pkt. Źródło: arXiv 2503.01996 v3.",
 s2_eye="2 · Czynnik zakłócający", s2_h="Ile tekstu mieści to samo okno kontekstu?",
 s2_p="OneRuler mierzy kontekst w tokenach. Polski potrzebuje więcej tokenów na słowo, więc przy tym samym budżecie polski „stóg siana” jest krótszy w słowach. Dodatkowo każdy język ma inną książkę jako tło (PL: „Noce i dnie”, EN: „Little Women”).",
 s2_ctx="Okno kontekstu:", s2_tok="Tokenizer (tokeny na słowo):", s2_src="Tokeny na słowo: Bielik v3, arXiv 2604.10799, Tab. 1 (jedna próbka tekstu).",
 s2_ratio="tyle polskich słów, ile angielskich, mieści się w oknie", s2_explain="Tokenizer zoptymalizowany pod polski (APT4) odwraca proporcję.",
 s3_eye="3 · Dowody 2024–2026", s3_h="Eksplorator luk: język a angielski",
 s3_p="Każdy wiersz to różnica w punktach procentowych względem angielskiego dla tego samego modelu i tych samych lub dopasowanych pytań. Kliknij wiersz, żeby zobaczyć liczby i źródło.",
 s3_chips_aria="Filtr kategorii", s3_leg_pl="polski lepszy", s3_leg_en="angielski lepszy", s3_leg_other="inny język (CS, RU, wiele)", s3_col1="Benchmark i model",
 s3_note="Wartości poniżej −40 są obcięte (◂). Status: zweryfikowane w tabeli lub pliku źródłowym.",
 s4_eye="4 · Najnowsze modele", s4_h="Global PIQA: te same 103 pytania po polsku i po angielsku",
 s4_p="Każda kropka to model. Nad przekątną: wyższy wynik po polsku. Pod nią: po angielsku. Wypełnione kółka to modele instruct, puste to bazowe. Najedź na kropkę, żeby zobaczyć nazwę.",
 s4_chips_aria="Filtr modeli", s4_aria="Wykres punktowy: wynik po angielsku względem wyniku po polsku dla 177 modeli",
 s4_note="Pytania napisane po angielsku, przetłumaczone maszynowo i zweryfikowane przez native speakerów. Przy n=103 błąd standardowy to ok. 3–5 pkt. Źródło: arXiv 2510.24081, results_per_language.csv, przeliczenie własne.",
 s5_eye="5 · Niuans", s5_h="Pytania o Polskę: tu polski wygrywa",
 s5_p="Dwie prace z 2026 r. pokazują, że przy wiedzy specyficznej dla Polski prompt po polsku daje lepsze wyniki w mocnych modelach. Przy pytaniach uniwersalnych prowadzi angielski. Autorzy Masked Advantage tłumaczą to tak: słabszy wynik w języku lokalnym to zwykle słabsza biegłość w języku, a nie brak wiedzy.",
 s5_table="<tr><th>Model</th><th>Pytania uniwersalne (GlobalGap)</th><th>Pytania o Polskę (LocalGap)</th><th>PoVisLE: PL / EN</th></tr><tr><td>GPT-5.4</td><td class='num en'>−0,34</td><td class='num pl'>+0,40</td><td class='num'>68,35 / <b>70,40</b></td></tr><tr><td>Claude Sonnet 4.6 / Sonnet 5</td><td class='num en'>−0,40</td><td class='num pl'>+0,80</td><td class='num'><b>69,94</b> / 66,94</td></tr><tr><td>Gemini 3 Flash</td><td class='num en'>−0,32</td><td class='num pl'>+2,26</td><td class='num'>—</td></tr><tr><td>Qwen3-14B / Qwen3.5-9B</td><td class='num en'>−0,74</td><td class='num en'>−1,02</td><td class='num'>38,75 / <b>47,04</b></td></tr><tr><td colspan='4' style='color:var(--muted);font-size:.8rem'>GlobalGap i LocalGap: różnica zdolności IRT (logity), język lokalny minus angielski, odczyt z Fig. 2 arXiv 2606.07422 (±0,02). PoVisLE: dokładność %, arXiv 2608.07763, Tab. 14. Sonnet 4.6 w Masked Advantage, Sonnet 5 w PoVisLE.</td></tr>",
 s6_eye="6 · Odpowiedzi", s6_h="Co z tego wynika",
 s6_cards=[("Czy polski jest skuteczniejszy?","Tylko w dwóch wąskich obszarach: NIAH w długim kontekście (starsze modele, czynniki zakłócające) oraz pytania o polskie realia. Poza tym polski jest równy (frontier 2026) albo gorszy (mniejsze modele)."),
  ("Czy język wpływa na RAG?","Tak. Liczy się zgodność języka zapytania i dokumentów. Modele wolą źródła w języku pytania. Embeddery tracą po polsku ok. 1 pkt, a BM25 wyraźnie więcej przez fleksję."),
  ("Czy język wpływa na rozumowanie agenta?","Tak, umiarkowanie. Największa strata dotyczy formatu argumentów narzędzi (−28 pkt), mała przy matematyce i kodzie. Rozumowanie wymuszone poza angielskim kosztuje ok. 7 pkt. Luka maleje z jakością modelu.")],
 s6_tips_h="Praktycznie, dla zespołów GIS, Python i ETL",
 s6_tips=["Trzymaj korpus i zapytania w tym samym języku. Przy polskich dokumentach rozważ polskie embeddery lub BM25 z lematyzacją.","Schematy narzędzi, nazwy funkcji i system prompt agenta pisz po angielsku; odpowiedź dla użytkownika może być po polsku.","Nie wymuszaj języka „myślenia” modelu.","Polski tekst to ok. 1,7–2,3× więcej tokenów: wyższy koszt i mniej treści w oknie.","Pytania o polskie przepisy, nazwy i instytucje zadawaj po polsku.","Zrób mały eval PL/EN na własnych danych zamiast polegać na rankingach medialnych."],
 s7_eye="7 · Metoda", s7_h="Jak powstał ten raport",
 s7_p="17 uruchomień subagentów Haiku w 6 iteracjach. Agent prowadzący zarządzał procesem, weryfikował wyniki i zlecał potwierdzenia przy wątpliwościach. Dowodem były tylko publikacje, preprinty, oficjalne leaderboardy i surowe pliki wyników. Media służyły jedynie jako drogowskazy.",
 s7_steps=[("Mapa dowodów.","6 agentów równolegle: źródło artykułu, benchmarki ogólne, RAG, rozumowanie i agenci, długi kontekst i tokenizacja, inne języki słowiańskie."),
  ("Weryfikacja liczb.","Źródła LaTeX i wykresy OneRuler, tabele RAG, Global-MMLU i karty modeli, MCLM, MAPS i PolyMath. Doinstalowano pypdf do czytania tabel z PDF."),
  ("Audyt cytowań.","11 identyfikatorów arXiv z lat 2025–2026: wszystkie istnieją. Nowe dane: Global PIQA, EuroEval."),
  ("Replikacje.","39 prac cytujących OneRuler: brak replikacji dla polskiego."),
  ("Rozstrzygnięcia.","Kolumny tabeli MASSIVE-Agents odczytane z wyrenderowanej strony; poszukiwanie kontrdowodów („adwokat diabła”)."),
  ("Niuans lokalny.","Masked Advantage i PoVisLE (2026); weryfikacja książek w OneRuler.")],
 s7_corr_h="Korekty wprowadzone w trakcie",
 s7_corr=["Bielik 82,98 → 77,41 to dwa różne modele, a nie wynik „przed i po”.","Swahili 90,8 w tekście Yong i in. nie występuje w tabeli (właściwie 59,6).","Global-MMLU nie raportuje wyników dla pojedynczych języków; błędne założenie w zleceniu.","Liczby CUS-QA podane przez agenta „adwokata diabła” nie zgadzały się z Tab. 11, więc je wykluczono.","Lider osobiście sprawdził w PDF i CSV: OneRuler, MCLM, MASSIVE-Agents, stronę Anthropic, Global PIQA, EU-24+, Masked Advantage."],
 s8_eye="8 · Źródła", s8_h="Najważniejsze publikacje",
 footer="Pełne raporty: raport_PL.md i report_EN.md. Stan danych: 24 listopada 2026.",
 L=dict(dec=",",locale="pl-PL",pl="Polski",en="Angielski",zh="Chiński",tokens="tokenów",words="słów",tpw="tok./słowo",verified="zweryfikowane",partial="częściowo",
   s4_above="polski lepszy",s4_below="angielski lepszy",s4_x="Dokładność po angielsku (%)",s4_y="Dokładność po polsku (%)",s4_models="modeli",s4_mean="średnia PL − EN (pkt)",s4_plwin="lepiej po polsku",s4_enwin="lepiej po angielsku",
   piqaf={"all":"Wszystkie","instruct":"Instruct","base":"Bazowe","frontier":"Wybrane 2025–26"},
   cats={"all":"Wszystko","longctx":"Długi kontekst","knowledge":"Wiedza","retrieval":"Wyszukiwanie i RAG","reasoning":"Rozumowanie","agents":"Agenci i narzędzia","local":"Pytania o Polskę"},
   fams={"all":"Wszystkie","slavic":"Słowiańskie","romance":"Romańskie","germanic":"Germańskie","uralic":"Ugrofińskie","other":"Inne"},
   famname={"slavic":"słowiański","romance":"romański","germanic":"germański","uralic":"ugrofiński","other":"inny"},
   langs={"pl":"polski","ru":"rosyjski","fr":"francuski","it":"włoski","es":"hiszpański","en":"angielski","uk":"ukraiński","sv":"szwedzki","pt":"portugalski","de":"niemiecki","no":"norweski","nl":"niderlandzki","hu":"węgierski","da":"duński","ja":"japoński","cs":"czeski","vi":"wietnamski","fi":"fiński","fa":"perski","sr":"serbski","hi":"hindi","ko":"koreański","zh":"chiński","sw":"suahili","ta":"tamilski","st":"sesotho"}),
 pts="pkt"),
"en": dict(
 title="Polish vs English in LLMs", lang_name="English", eyebrow="Research report · November 2026", otherlang="Wersja polska →",
 h1="Does AI really “speak best in Polish”?",
 lede="The widely shared ranking in which Polish beat English comes from a single long-context benchmark (OneRuler, COLM 2025). We checked the source and gathered newer 2025–2026 evidence on knowledge, retrieval and RAG, reasoning, and agents.",
 ruler_aria="OneRuler accuracy scale: Polish 88.0%, English 83.9%, Chinese 62.1%",
 verdicts=[("Source: OneRuler","The article's numbers are correct: PL 88.0%, EN 83.9%. They cover only synthetic long-context retrieval.","ok","Confirmed"),
  ("Over-interpretation","The paper does not study the “best language for prompting”. A co-author says it was not studied.","warn","Corrected"),
  ("Newer data","In 2026 frontier models Polish is level with English; in smaller models and with tools it is lower.","en","English ≥ Polish"),
  ("The exception","Questions about Polish realities: strong models answer better in Polish.","pl","Polish > English")],
 s1_eye="1 · The source", s1_h="OneRuler ranking of 26 languages",
 s1_p="Mean needle-in-a-haystack accuracy at 64K and 128K tokens, averaged across models. Polish is first, but Czech and Serbian sit in the bottom half, so this is not a “Slavic languages” effect. Select a language family to highlight it.",
 s1_chips_aria="Language family filter",
 s1_note="PL, EN and ZH exact from the paper text; other values (~) read from Fig. 4b, ±0.5 pts. Source: arXiv 2503.01996 v3.",
 s2_eye="2 · A confounder", s2_h="How much text fits in the same context window?",
 s2_p="OneRuler measures context in tokens. Polish needs more tokens per word, so at the same budget a Polish haystack holds fewer words. Each language also uses a different background book (PL: Noce i dnie, EN: Little Women).",
 s2_ctx="Context window:", s2_tok="Tokenizer (tokens per word):", s2_src="Tokens per word: Bielik v3, arXiv 2604.10799, Tab. 1 (single text sample).",
 s2_ratio="Polish words per English word that fit in the window", s2_explain="A Polish-optimized tokenizer (APT4) reverses the ratio.",
 s3_eye="3 · Evidence 2024–2026", s3_h="Gap explorer: language vs English",
 s3_p="Each row is the difference in percentage points vs English for the same model on the same or matched questions. Select a row to see the numbers and the source.",
 s3_chips_aria="Category filter", s3_leg_pl="Polish higher", s3_leg_en="English higher", s3_leg_other="other language (CS, RU, several)", s3_col1="Benchmark and model",
 s3_note="Values below −40 are clipped (◂). Status: verified in the source table or file.",
 s4_eye="4 · Newest models", s4_h="Global PIQA: the same 103 questions in Polish and English",
 s4_p="Each dot is a model. Above the diagonal: higher in Polish. Below it: higher in English. Filled circles are instruct models, hollow ones are base models. Hover a dot to see its name.",
 s4_chips_aria="Model filter", s4_aria="Scatter plot: English score vs Polish score for 177 models",
 s4_note="Questions written in English, machine-translated and verified by native speakers. At n=103 the standard error is about 3–5 pts. Source: arXiv 2510.24081, results_per_language.csv, recomputed by us.",
 s5_eye="5 · The nuance", s5_h="Questions about Poland: here Polish wins",
 s5_p="Two 2026 papers show that for Poland-specific knowledge, prompting in Polish gives better results in strong models. On culture-agnostic questions English leads. The Masked Advantage authors explain it this way: a weaker local-language score usually reflects weaker proficiency in the language, not missing knowledge.",
 s5_table="<tr><th>Model</th><th>Culture-agnostic (GlobalGap)</th><th>About Poland (LocalGap)</th><th>PoVisLE: PL / EN</th></tr><tr><td>GPT-5.4</td><td class='num en'>−0.34</td><td class='num pl'>+0.40</td><td class='num'>68.35 / <b>70.40</b></td></tr><tr><td>Claude Sonnet 4.6 / Sonnet 5</td><td class='num en'>−0.40</td><td class='num pl'>+0.80</td><td class='num'><b>69.94</b> / 66.94</td></tr><tr><td>Gemini 3 Flash</td><td class='num en'>−0.32</td><td class='num pl'>+2.26</td><td class='num'>—</td></tr><tr><td>Qwen3-14B / Qwen3.5-9B</td><td class='num en'>−0.74</td><td class='num en'>−1.02</td><td class='num'>38.75 / <b>47.04</b></td></tr><tr><td colspan='4' style='color:var(--muted);font-size:.8rem'>GlobalGap and LocalGap: IRT ability difference (logits), local language minus English, read from Fig. 2 of arXiv 2606.07422 (±0.02). PoVisLE: accuracy %, arXiv 2608.07763, Tab. 14. Sonnet 4.6 in Masked Advantage, Sonnet 5 in PoVisLE.</td></tr>",
 s6_eye="6 · Answers", s6_h="What it means",
 s6_cards=[("Is Polish more effective?","Only in two narrow areas: long-context NIAH (older models, confounders) and questions about Polish realities. Elsewhere Polish is level (2026 frontier) or lower (smaller models)."),
  ("Does language affect RAG?","Yes. What matters is that the query and document languages match. Models prefer sources in the question's language. Embedders lose about 1 pt in Polish; BM25 loses much more because of inflection."),
  ("Does language affect agent reasoning?","Yes, moderately. The biggest loss is in tool-argument formatting (−28 pts); math and code lose little. Reasoning forced outside English costs about 7 pts. The gap shrinks as models improve.")],
 s6_tips_h="In practice, for GIS, Python and ETL teams",
 s6_tips=["Keep the corpus and the queries in the same language. For Polish documents consider Polish embedders or BM25 with lemmatization.","Write tool schemas, function names and the agent's system prompt in English; the user-facing answer can be in Polish.","Do not force the language the model “thinks” in.","Polish text uses about 1.7–2.3× more tokens: higher cost and less content per window.","Ask about Polish regulations, names and institutions in Polish.","Run a small PL/EN eval on your own data instead of relying on media rankings."],
 s7_eye="7 · Method", s7_h="How this report was made",
 s7_p="17 Haiku subagent runs across 6 iterations. A lead agent managed the process, verified results, and delegated confirmation whenever something was doubtful. Only papers, preprints, official leaderboards and raw result files counted as evidence. Media served only as pointers.",
 s7_steps=[("Evidence map.","6 agents in parallel: article source, general benchmarks, RAG, reasoning and agents, long context and tokenization, other Slavic languages."),
  ("Number verification.","OneRuler LaTeX sources and figures, RAG tables, Global-MMLU and model cards, MCLM, MAPS and PolyMath. pypdf was installed to read PDF tables."),
  ("Citation audit.","11 arXiv IDs from 2025–2026: all exist. New data: Global PIQA, EuroEval."),
  ("Replications.","39 papers citing OneRuler: no replication for Polish."),
  ("Resolutions.","MASSIVE-Agents table columns read from a rendered page; devil's-advocate search for counter-evidence."),
  ("Local nuance.","Masked Advantage and PoVisLE (2026); OneRuler haystack books verified.")],
 s7_corr_h="Corrections made along the way",
 s7_corr=["Bielik 82.98 → 77.41 are two different models, not a before/after result.","Swahili 90.8 in the Yong et al. text does not appear in the table (it is 59.6).","Global-MMLU does not report per-language results; the brief's premise was wrong.","The CUS-QA numbers from the devil's-advocate agent did not match Tab. 11, so they were excluded.","The lead checked in PDF and CSV: OneRuler, MCLM, MASSIVE-Agents, the Anthropic page, Global PIQA, EU-24+, Masked Advantage."],
 s8_eye="8 · Sources", s8_h="Key publications",
 footer="Full reports: raport_PL.md and report_EN.md. Data as of 24 November 2026.",
 L=dict(dec=".",locale="en-US",pl="Polish",en="English",zh="Chinese",tokens="tokens",words="words",tpw="tok/word",verified="verified",partial="partial",
   s4_above="Polish higher",s4_below="English higher",s4_x="Accuracy in English (%)",s4_y="Accuracy in Polish (%)",s4_models="models",s4_mean="mean PL − EN (pts)",s4_plwin="higher in Polish",s4_enwin="higher in English",
   piqaf={"all":"All","instruct":"Instruct","base":"Base","frontier":"Selected 2025–26"},
   cats={"all":"All","longctx":"Long context","knowledge":"Knowledge","retrieval":"Retrieval and RAG","reasoning":"Reasoning","agents":"Agents and tools","local":"Questions about Poland"},
   fams={"all":"All","slavic":"Slavic","romance":"Romance","germanic":"Germanic","uralic":"Uralic","other":"Other"},
   famname={"slavic":"Slavic","romance":"Romance","germanic":"Germanic","uralic":"Uralic","other":"other"},
   langs={"pl":"Polish","ru":"Russian","fr":"French","it":"Italian","es":"Spanish","en":"English","uk":"Ukrainian","sv":"Swedish","pt":"Portuguese","de":"German","no":"Norwegian","nl":"Dutch","hu":"Hungarian","da":"Danish","ja":"Japanese","cs":"Czech","vi":"Vietnamese","fi":"Finnish","fa":"Persian","sr":"Serbian","hi":"Hindi","ko":"Korean","zh":"Chinese","sw":"Swahili","ta":"Tamil","st":"Sesotho"}),
 pts="pts"),
}
SOURCES = [("OneRuler — Kim et al., COLM 2025","2503.01996"),("Global PIQA, 2025–26","2510.24081"),("The Masked Advantage, 2026","2606.07422"),("PoVisLE, 2026","2608.07763"),
 ("Language Equality has a Price (EU-24+), 2026","2608.01395"),("MASSIVE-Agents, EMNLP Findings 2025",None),("MAPS multilingual agents, 2025–26","2505.15935"),("MCLM test-time scaling, 2025","2502.17407"),
 ("Reasoning-language control, 2025","2505.22888"),("PolyMath, 2025","2504.18428"),("MMLU-ProX, EMNLP 2025","2503.10497"),("EU21 benchmarks, 2024","2410.08928"),
 ("PL/EN medical exams, 2024","2412.00559"),("Park & Lee, multilingual RAG, ACL Findings 2025","2502.11175"),("Faux Polyglot, NAACL 2025","2407.05502"),("XRAG, EMNLP Findings 2025","2505.10089"),
 ("Linguistic Nepotism, 2025","2509.13930"),("BEIR-PL, 2023","2305.19840"),("PL-MTEB, ACL Findings 2026","2405.10138"),("Bielik v3 tokenizer, 2026","2604.10799"),
 ("Petrov et al., tokenizer unfairness, NeurIPS 2023","2305.15425"),("Do multilingual LLMs think in English?, 2025","2502.15603")]

for lang in ("pl","en"):
    c = C[lang]; L = c.pop("L"); L["langs"]=L["langs"]
    ev = [dict(cat=e[0],lang=e[1],label=html.escape(e[2][lang]),cmp=e[3],year=e[4],gap=e[5],nums=(e[6] if lang=="pl" else e[6].replace(",",".").replace("remis/tie","tie").replace("śr./avg 6 modeli, 11 języków","avg of 6 models, 11 languages")),
               note=html.escape(e[7][lang]),src=e[8],url=e[9],st=e[10]) for e in EV]
    if lang=="pl":
        for x in ev: x["nums"]=x["nums"].replace("remis/tie","remis").replace("śr./avg","śr.")
    v = "".join(f'<div class="verdict"><span class="tag"><i style="background:var(--{k})"></i>{t}</span><b>{h}</b><p>{p}</p></div>' for h,p,k,t in c["verdicts"])
    cards = "".join(f'<div class="card"><h3>{h}</h3><p>{p}</p></div>' for h,p in c["s6_cards"])
    tips = "".join(f"<li>{t}</li>" for t in c["s6_tips"])
    steps = "".join(f"<li><span><b>{h}</b> {p}</span></li>" for h,p in c["s7_steps"])
    corr = "".join(f"<li>{t}</li>" for t in c["s7_corr"])
    src = "".join(f'<div><a href="{"https://arxiv.org/abs/"+i if i else "https://aclanthology.org/2025.findings-emnlp.1099/"}" target="_blank" rel="noopener">{n}</a> <span class="num" style="color:var(--muted)">{("arXiv "+i) if i else "ACL Anthology"}</span></div>' for n,i in SOURCES)
    rep = dict(c, en=L["en"], pl=L["pl"], verdicts=v, s6_cards=cards, s6_tips=tips, s7_steps=steps, s7_corr=corr, sources=src,
               otherlink="#", L_JSON=json.dumps(L,ensure_ascii=False), RANK_JSON=json.dumps(RANK), TOK_JSON=json.dumps(TOK),
               EV_JSON=json.dumps(ev,ensure_ascii=False), PIQA_JSON=json.dumps(piqa,separators=(",",":")))
    out = tpl.replace("+30 pkt</span>", "+30 "+c["pts"]+"</span>")
    for k,val in rep.items():
        if isinstance(val,str): out = out.replace("{{"+k+"}}", val)
    assert "{{" not in out, [l for l in out.splitlines() if "{{" in l][:3]
    (OUT / ("artefakt_PL.html" if lang=="pl" else "artifact_EN.html")).write_text(out)
    print(lang, len(out))
