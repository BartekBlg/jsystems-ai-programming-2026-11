# Prescreen evaluation: header

- Date: 2026-11-26
- Commit: c754c66 (`git rev-parse --short HEAD`)
- Decisions model: `typesafe/jev-1.14`
- Confidence threshold: 0.80 (`DECISIONS_CONFIDENCE_THRESHOLD` default)
- Run: `PrescreenEvaluationIT` via failsafe, real Decisions endpoint and real vision LLM, over `assets/example-images/*` x RETURN/COMPLAINT
- Ground truth: `app/backend/src/test/resources/prescreen/expected.yaml` is still TODO (human labelling pending), so "Agreement with expected" is not computed and shows `pending expected.yaml`. Agreement with LLM is computed.
- Calls per prescreen: 2 Decisions calls (request A and B); per image and case: 1 vision LLM call. Total: 20 Decisions calls and 10 vision LLM calls.

---

# Prescreen evaluation

Real API evaluation; Decisions and vision LLM each run once per image and case type.

| Image | Case | Gate | Reasons | Agreement with LLM | Agreement with expected | Decisions ms | LLM ms | Cost USD |
|---|---|---|---|---|---|---:|---:|---:|
| laptop-1.png | RETURN | UNCERTAIN | LOW_CONFIDENCE:photo_usable, LOW_CONFIDENCE:use_scratches, LOW_CONFIDENCE:use_dirt, LOW_CONFIDENCE:use_wear, LOW_CONFIDENCE:missing_protective_film, LOW_CONFIDENCE:missing_accessories | n/a | pending expected.yaml | 3409 | 12050 | 5.428919999999999E-4 |
| laptop-1.png | COMPLAINT | UNCERTAIN | LOW_CONFIDENCE:photo_usable | n/a | pending expected.yaml | 718 | 12283 | 5.56164E-4 |
| laptop-2.webp | RETURN | UNCERTAIN | LOW_CONFIDENCE:photo_usable, LOW_CONFIDENCE:use_scratches, LOW_CONFIDENCE:use_dirt, LOW_CONFIDENCE:use_wear, LOW_CONFIDENCE:missing_protective_film, LOW_CONFIDENCE:missing_accessories, LOW_CONFIDENCE:condition | n/a | pending expected.yaml | 445 | 5972 | 2.1462E-4 |
| laptop-2.webp | COMPLAINT | UNCERTAIN | LOW_CONFIDENCE:photo_usable, LOW_CONFIDENCE:condition, LOW_CONFIDENCE:damage_cause, LOW_CONFIDENCE:damage_type, LOW_CONFIDENCE:damage_location | n/a | pending expected.yaml | 551 | 3146 | 2.27934E-4 |
| phone-1.jpg | RETURN | UNCERTAIN | LOW_CONFIDENCE:use_scratches, LOW_CONFIDENCE:use_dirt, LOW_CONFIDENCE:use_wear, LOW_CONFIDENCE:missing_protective_film, LOW_CONFIDENCE:missing_accessories | n/a | pending expected.yaml | 451 | 3916 | 2.9181599999999997E-4 |
| phone-1.jpg | COMPLAINT | CONFIDENT |  | photo=true, confidence=false, damage=true, cause=true | pending expected.yaml | 444 | 5017 | 3.05046E-4 |
| phone-2.jpeg | RETURN | UNCERTAIN | LOW_CONFIDENCE:photo_usable, LOW_CONFIDENCE:visible_damage, LOW_CONFIDENCE:use_scratches, LOW_CONFIDENCE:use_dirt, LOW_CONFIDENCE:use_wear, LOW_CONFIDENCE:missing_protective_film, LOW_CONFIDENCE:missing_accessories, LOW_CONFIDENCE:condition | n/a | pending expected.yaml | 276 | 5664 | 9.815400000000001E-5 |
| phone-2.jpeg | COMPLAINT | UNCERTAIN | LOW_CONFIDENCE:photo_usable, LOW_CONFIDENCE:visible_damage, LOW_CONFIDENCE:condition, LOW_CONFIDENCE:damage_cause, LOW_CONFIDENCE:damage_type, LOW_CONFIDENCE:damage_location | n/a | pending expected.yaml | 318 | 6430 | 1.11426E-4 |
| phone-3.jpeg | RETURN | UNCERTAIN | LOW_CONFIDENCE:use_dirt, LOW_CONFIDENCE:missing_protective_film, LOW_CONFIDENCE:missing_accessories, LOW_CONFIDENCE:condition | n/a | pending expected.yaml | 287 | 6550 | 9.9036E-5 |
| phone-3.jpeg | COMPLAINT | UNCERTAIN | LOW_CONFIDENCE:condition, LOW_CONFIDENCE:damage_cause, LOW_CONFIDENCE:damage_type, LOW_CONFIDENCE:damage_location | n/a | pending expected.yaml | 348 | 5602 | 1.12476E-4 |

Totals: Decisions 7247 ms; vision LLM 66630 ms; Decisions cost USD 0.0025595639999999994.
