# COLLIDE — Phase 1: Code Collider

A native Android structural discovery prototype. Not a compression app. Not a fake sci-fi mockup. A real bounded code-collision engine that loads code inputs, transforms them, generates candidates, evaluates them with deterministic detectors, and archives only meaningful events.

---

## What Phase 1 Does

COLLIDE Phase 1 is a **Code Collider**. It:

1. **Accepts** one or two code/text snippets (pasted or imported from a file)
2. **Profiles** them with lightweight heuristic language detection
3. **Normalizes** them into internal structural representations (token stream, block tree, operator signature)
4. **Generates** bounded collision candidates by applying ordered recipe chains to the normalized inputs
5. **Evaluates** each candidate with a set of structural detectors
6. **Archives** only candidates that pass at least one detector threshold as "saved events"
7. **Replays** saved events deterministically to confirm they still appear

---

## What Phase 1 Does NOT Do

- Does **not** claim semantic correctness for arbitrary code
- Does **not** prove logical equivalence between inputs and outputs
- Does **not** execute imported code
- Does **not** use LLMs, cloud services, or network dependencies
- Does **not** implement a full compiler or standards-compliant parser for any language
- Does **not** invent "working software automatically" — it is a structural discovery tool

---

## Architecture

```
app/
  data/
    db/                  — Room entities, DAOs, database
    repository/          — EventRepository (save/load events, run summaries)
    settings/            — AppSettings via DataStore
  domain/
    model/               — Core data models (InputSample, NormalizedCodeModel, etc.)
    engine/
      normalize/         — InputProfiler, CodeNormalizer (tokenizer + block builder)
      collision/         — RecipeLibrary, TransformationEngine, CandidateGenerator, RecipeSerializer
      detectors/         — 6 structural detectors + DetectorPipeline
      evaluation/        — CandidateEvaluator (structural validity checks)
      replay/            — EventReplayer
    CodeColliderEngine   — Top-level coordinator
  ui/
    home/                — Home screen + HomeViewModel
    collider/            — Collider screen + ColliderViewModel
    archive/             — Archive screen + ArchiveViewModel
    eventdetail/         — Event detail + replay screen + EventDetailViewModel
    settings/            — Settings screen + SettingsViewModel
    theme/               — Dark lab theme (CollideTheme)
    navigation/          — Screen route definitions
  MainActivity           — Entry point, dependency wiring, NavHost
```

---

## How Code Normalization Works

Normalization is a three-stage heuristic pipeline. It does **not** produce a standards-compliant AST.

### Stage 1: Tokenization
The `CodeNormalizer.tokenize()` method walks character-by-character and classifies each token as one of:
- `KEYWORD` — recognized control/type keywords
- `IDENTIFIER` — user-defined names (also tracks an abstracted form for comparison)
- `OPERATOR` — arithmetic, comparison, logical, compound operators
- `LITERAL_NUMBER` / `LITERAL_STRING` — normalized literals
- `DELIMITER` — brackets, semicolons, punctuation
- `COMMENT` — single-line `//` and `#` comments
- `UNKNOWN` — anything else

### Stage 2: Block Tree Construction
`buildBlockTree()` applies indentation and keyword heuristics to identify structural blocks:
- `FUNCTION_DEF`, `CONDITION_BRANCH`, `LOOP_BODY`, `ASSIGNMENT_BLOCK`, etc.
- Block depth is estimated from indentation (4-space assumption)
- This is a practical approximation — it will be incorrect on unusual formatting

### Stage 3: Operator Signature
`computeSignature()` counts key structural indicators: arithmetic ops, comparison ops, conditionals, loops, return statements, unique identifiers. This produces a fingerprint string for quick comparison.

---

## What "Collision" Means in This App

A **collision** is the application of an ordered recipe of structural transformation operations to one or two normalized inputs, producing a **candidate**.

A **recipe** is a named, deterministic, serializable list of operations. Examples:
- `Strip & Normalize` — remove comments, normalize literals, abstract identifiers
- `Mirror Conditions` — flip comparison operators (`>` → `<=`, etc.)
- `Branch Splice` — graft a structural block from Input B into Input A
- `Shared Scaffold` — extract tokens present in both inputs

Recipes are **not** correctness-preserving transformations. They are structural search operations. The app explicitly does not claim any output is semantically equivalent to its input unless a detector provides evidence.

---

## What Detectors Actually Measure

Phase 1 includes six structural detectors:

| Detector | What It Measures | Event Type If Passed |
|---|---|---|
| **Structural Novelty** | Token-set Jaccard distance from parent + size change | `coherent_variant` |
| **Structural Compression** | Token count reduction while remaining coherent | `structural_simplification` |
| **Reusable Pattern** | Frequency of repeated n-gram sequences | `reusable_scaffold_extracted` |
| **Symmetry Hint** | Distribution similarity + size reduction | `symmetry_hint` |
| **Hybridization** | Token overlap from both parents in dual-input mode | `hybrid_structure_found` / `unexpected_merge` |
| **Contradiction/Tension** | Structural divergence vs. similarity of parents | `contradiction_detected` |

All detectors return:
- A score from 0.0 to 1.0
- A threshold (adjusted by sensitivity setting)
- A pass/fail result
- A concise reason string
- An `EventType` suggestion if the threshold is passed

Detectors measure structural properties of text representations. They do **not** execute code, prove correctness, or guarantee the output is useful.

---

## What Saved Events Do and Do Not Imply

A **saved event** means:
- A candidate passed at least one detector threshold
- The candidate was produced by a fully deterministic, serialized recipe
- The event can be replayed (the same recipe will be re-run)

A saved event does **not** mean:
- The output is semantically correct or equivalent to the input
- The output is compilable
- The "discovery" is useful for any real software task
- The engine found a bug, proof, or optimization

The Event Archive is a **structural discovery log**, not a code improvement tool.

---

## Run Modes

| Mode | Max Candidates | Search Depth | Pruning | Expensive Detectors |
|---|---|---|---|---|
| **Safe** | 20 | 2 | Aggressive | No (2 detectors) |
| **Balanced** | 60 | 4 | Normal | Yes (all 6) |
| **Burst** | 200 | 6 | Normal | Yes (all 6) |

All modes are fully deterministic and cancellable.

---

## Fixture Files

Built-in fixtures are bundled in `app/src/main/assets/fixtures/`:

| File | Language Hint | Purpose |
|---|---|---|
| `arithmetic_simple.txt` | Python-like | Basic function set |
| `arithmetic_variant.txt` | Python-like | Same shape, different names — tests normalization |
| `string_utils.txt` | JavaScript-like | String manipulation functions |
| `branch_heavy.txt` | Kotlin-like | Condition-heavy code for branching detectors |
| `pseudo_workflow.txt` | Pseudo-code | Algorithm-style structured prose |
| `rule_dsl.txt` | Rule snippet | Business rule DSL |
| `kotlin_data_pipeline.txt` | Kotlin-like | Functional pipeline pattern |
| `c_like_struct.txt` | C-like | Struct + pointer operations |

---

## Testing

Run unit tests with:
```
./gradlew :app:test
```

Test coverage:
- **`InputProfilerTest`** — heuristic language detection predictability
- **`CodeNormalizerTest`** — tokenization, identifier abstraction, operator counting, determinism
- **`DetectorTest`** — each detector on curated inputs; threshold sensitivity; edge cases
- **`CandidateGeneratorTest`** — deterministic ordering, bounded search, sequential indexing, dual-input mode
- **`RecipeSerializerTest`** — round-trip serialization for all library recipes; param preservation
- **`ColliderEngineIntegrationTest`** — full pipeline; honest accounting; dual-input; sensitivity effects

---

## Known Limitations

1. **Tokenizer limitations**: Does not handle multi-line strings, raw strings, or template literals correctly across all languages
2. **Block tree accuracy**: Relies on indentation (4-space) and keyword heuristics; unusual formatting will produce incorrect tree structure
3. **Language detection is an estimate**: Confidence values reflect heuristic strength, not parser certainty
4. **No semantic evaluation**: No code is executed; behavioral correctness is never tested
5. **Phase 1 input size limit**: 50KB max input to keep mobile processing feasible
6. **No full parser for any language**: Language profiles are labeled "estimated" throughout the UI
7. **Event relevance is context-dependent**: A structurally novel candidate is not necessarily a practically useful one

---

## Phase 2 Recommendation

The natural next phase expands the engine's representational power and adds a second collision chamber.

### Recommended Phase 2 scope:

1. **Richer parsing layer**: Add optional tree-sitter bindings (via JNI) for Python and JavaScript. This would replace the heuristic block tree with a proper CST for supported languages, enabling accurate structure-aware operations.

2. **Native C++ engine core**: Move the candidate generator and transformation operations to a C++ layer via the NDK. This unlocks larger candidate budgets and faster evaluation on-device, making Burst mode meaningfully more powerful.

3. **Lightweight toy interpreter**: For arithmetic-only sub-expressions, implement a small evaluator that can confirm whether two structurally different forms produce identical outputs on a test vector. This would allow the Symmetry Hint detector to upgrade from "hinting" to "confirmed".

4. **World-Law Collider chamber**: Add a second chamber (non-code) that accepts physics/rule system definitions as inputs and applies similar structural collision passes — the natural extension of the Code Collider architecture.

5. **Richer event taxonomy**: Use the detector results to auto-generate structured hypotheses about why an event was found (e.g., "this simplification preserves operator count but reduces branching"), shown in the Event Detail screen.

6. **Export and sharing**: Allow events to be exported as structured JSON for downstream use in research or tooling pipelines.

---

*COLLIDE Phase 1 is a bounded structural discovery prototype. It is honest about what it measures, honest about what it does not prove, and designed to be a clean foundation for expansion.*
