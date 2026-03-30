# COLLIDE — Phase 1: Compression Collider

COLLIDE is a native Android experimental lab that generates and tests reversible compression recipe candidates on-device.

---

## What Phase 1 Does

Phase 1 implements a single focused workflow called the **Compression Collider**:

1. **Load** a small input file from device storage or a built-in fixture
2. **Generate** candidate compression recipe chains deterministically
3. **Encode** each candidate: run transform chain on input bytes, then apply a backend compressor
4. **Decode** each candidate: decompress, then apply inverse transforms in reverse order
5. **Verify** that the decoded bytes exactly match the original input (byte-for-byte)
6. **Compare** the total encoded size (compressed bytes + all metadata overhead) against the selected baseline
7. **Archive** only strict winning events — candidates that beat the baseline AND pass exact reconstruction
8. **Inspect** winning events and their full recipe chains

The result is an honest accounting of what worked, what failed, and by how much.

---

## What Phase 1 Does NOT Do

- No World-Law Collider or general invention engine
- No neural compression or ML guidance
- No cloud sync, accounts, or authentication
- No GPU or native C++ (pure Kotlin/JVM)
- No large-file benchmarking
- No production file replacement on device
- No root or system-level access
- No fabricated results — if the data says no improvement, the UI says no improvement

---

## Architecture Summary

```
app/
  data/
    db/               Room database (EventEntity, RunSummaryEntity, DAOs, AppDatabase)
    repository/       EventRepository: domain ↔ database mapping
    settings/         DataStore settings (CollideSettingsStore)
  domain/
    model/            Pure data classes (InputSample, RecipeSpec, CandidateResult, RunStats, etc.)
    engine/           CompressionColliderEngine, BackendCompressorEngine
    transforms/       ReversibleTransform interface + 7 implementations
    recipes/          CandidateGenerator, RecipeSerializer
    evaluator/        CandidateEvaluator, BaselineEvaluator, ExactnessVerifier
    detector/         EventRecorder
  ui/
    home/             HomeScreen + HomeViewModel
    collider/         ColliderScreen + ColliderViewModel
    archive/          ArchiveScreen + ArchiveViewModel
    eventdetail/      EventDetailScreen + EventDetailViewModel
    settings/         SettingsScreen + SettingsViewModel
    navigation/       CollideNavGraph + Routes
    theme/            CollideTheme, CollideColors, CollideTypography
  util/
```

**Pattern**: MVVM with a clean domain layer. ViewModels consume domain engine classes via coroutines/Flow. The UI is pure Jetpack Compose with no business logic. Room persists events and run summaries. DataStore holds settings.

---

## Transform Library

All transforms are exactly reversible. Each implements `ReversibleTransform`:

| ID | Name | Description |
|----|------|-------------|
| `identity` | Identity | Pass-through, no change |
| `byte_run_rle` | Byte-Run RLE | Run-length encoding for arbitrary byte runs (min run 3) |
| `zero_run_rle` | Zero-Run RLE | Specialised RLE for zero-byte runs |
| `delta8` | Delta-8 | Replace each byte with difference from previous byte |
| `xor_prev` | XOR-Prev-Byte | Replace each byte with XOR against previous byte |
| `block_shuffle_4` | Block-Shuffle-4 | Interleave 4-byte block byte-planes (column-major reorder) |
| `block_shuffle_8` | Block-Shuffle-8 | Interleave 8-byte block byte-planes |
| `move_to_front` | Move-To-Front | MTF rank transform; groups recently-seen symbols near zero |

Transforms that cannot apply to the current data (e.g. Zero-Run RLE on data with no zero bytes) return `NOT_APPLICABLE` without attempting to encode.

---

## How Exactness Verification Works

For every candidate recipe:

1. The transform chain encodes the input bytes and collects metadata from each transform.
2. The backend compressor compresses the final transform output.
3. The backend decompresses the result.
4. The transforms are applied **in reverse order** using the saved metadata.
5. `ExactnessVerifier.verify()` performs a byte-for-byte comparison between the decoded bytes and the original input.
6. If any byte differs, or the lengths differ, the candidate is classified as `EXACTNESS_FAILED` and is never saved as a winner.

This check is mandatory and cannot be bypassed.

---

## How Winners Are Classified

A **strict winner** requires all of the following:

- `CandidateClassification.STRICT_WINNER`
- `reconstructionPassed == true`
- `encodedSize < baselineSize` (strictly less than, not equal)

`encodedSize` includes all overhead:
- Backend-compressed bytes
- Per-transform metadata (length-prefixed)
- Header: number of transforms (4 bytes) + original size (4 bytes)

Only these events are persisted to the archive.

---

## Candidate Classification

| Classification | Meaning |
|----------------|---------|
| `STRICT_WINNER` | Beats baseline, exact reconstruction passed |
| `NO_STRICT_IMPROVEMENT` | Exact reconstruction passed, but no size gain |
| `EXACTNESS_FAILED` | Decoded bytes differ from input |
| `NOT_APPLICABLE` | One or more transforms cannot process this data |
| `PRUNED_PRE_EVAL` | Skipped before full evaluation (future pruning logic) |
| `ENCODE_ERROR` | An error occurred during encoding |
| `DECODE_ERROR` | An error occurred during decoding |

---

## Honest Accounting

Every run tracks:
- `candidates_seen` — total generated
- `candidates_pruned_pre_eval` — skipped before evaluation
- `candidates_evaluated` — actually run through encode/decode/verify
- `exactness_failures` — failed reconstruction
- `not_applicable_count` — not applicable to input
- `no_gain_count` — correct but did not beat baseline
- `strict_winner_count` — actual wins

Failed volume is always visible in the Live Run Panel. The app never hides the failure count.

---

## Run Modes

| Mode | Max Candidates | Max Chain Length | Notes |
|------|---------------|-----------------|-------|
| Safe | 30 | 2 | Light, fast exploration |
| Balanced | 80 | 3 | Default recommended mode |
| Burst | 200 | 3 | Maximum depth for Phase 1 |

---

## Built-in Test Fixtures

| File | Type | Notes |
|------|------|-------|
| `fixtures/sample_text.txt` | Plain text | Repetitive sentences |
| `fixtures/sample_data.json` | JSON | Sensor measurement data |
| `fixtures/sample_table.csv` | CSV | Tabular structured data |
| `fixtures/sample_binary.bin` | Binary | Zero runs, delta sequences, block patterns |
| `fixtures/sample_mixed.txt` | Mixed | Log-format structured text |

---

## Building

Requires:
- Android Studio Hedgehog or newer (or Gradle 8.9+ CLI)
- JDK 17+
- Android SDK 35
- Min SDK 26 (Android 8.0)

```bash
./gradlew assembleDebug
./gradlew test
```

---

## Phase 2 Recommendation

After validating the Phase 1 loop, the next recommended expansion is:

### Phase 2: Guided Search + Transform Composition Extension

1. **Adaptive candidate pruning**: Use early-exit heuristics based on partial encode size estimates to prune candidates before full evaluation. This allows much larger search spaces without proportional cost.

2. **Transform feedback signal**: After each run, record which transform positions in winning chains tended to help. Use this as a lightweight frequency-based guide (no ML required) to bias candidate ordering in future runs.

3. **Extended transform library**: Add reversible transforms such as:
   - BWT (Burrows-Wheeler Transform) — requires known block size in metadata
   - Huffman symbol reordering (reversible)
   - Run-length + move-to-front pipeline as a named compound step
   - Word-level delta for structured integer sequences

4. **Multi-file batch mode**: Accept a small directory of files and run the collider on each, accumulating a cross-file winner database to identify recipes that generalize.

5. **Export recipe as standalone Kotlin snippet**: Let the user export a winning recipe as a self-contained Kotlin function that can be pasted into any JVM project.

Phase 2 should still remain on-device, deterministic, and honest. It extends depth of search, not the deception budget.
