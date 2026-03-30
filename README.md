# COLLIDE — Phase 2: Honest Compression Research Upgrade

COLLIDE is a native Android experimental compression lab. Phase 2 extends Phase 1 by tightening every aspect of result honesty: verification, size accounting, reproducibility, candidate diagnostics, corpus testing, and search depth.

---

## What Phase 2 Adds

### A. Stronger Exactness Verification (SHA-256)
Every candidate evaluation now performs dual verification:
1. Byte-for-byte equality between original input and reconstructed output.
2. SHA-256 hash match between original and reconstructed.

Both must pass. A candidate that fails either check is never archived as a winner.

The `VerificationResult` domain object records both hashes and the failure reason if any.

### B. Honest Total Encoded Size Accounting
A formal `SizeBreakdown` model accounts for every byte required to reconstruct the original file:

| Component | What it is |
|-----------|-----------|
| `transformedPayloadSize` | Bytes after transform chain, before backend compression |
| `backendCompressedPayloadSize` | Bytes after Deflate backend compression |
| `transformMetadataSize` | All per-transform metadata blobs + their 4-byte length prefixes |
| `containerHeaderSize` | Fixed 8-byte header (num_transforms + original_size) |
| **`totalEncodedSize`** | **The canonical comparison number** |

The candidate's `totalEncodedSize` is always compared against the baseline (which is raw compressed bytes only), making the comparison deliberately conservative — the candidate must beat the baseline even after paying all per-transform metadata costs.

### C. Deterministic Reproducibility and Replay
Every winning event now stores:
- Full recipe chain (transform IDs + backend)
- `RunConfigSnapshot` (run mode, baseline, max candidates, chain length, enabled transforms, engine version)
- `candidateIndex` (position in the deterministic generation order)
- SHA-256 hashes of input and reconstruction

The **Replay Event** action re-runs the exact recipe on the same input bytes and verifies:
- Total encoded size matches the archived value
- Exactness + SHA-256 verification passes
- Input SHA-256 matches archived original SHA-256

Replay status is tracked per event: `MATCHED`, `SIZE_MISMATCH`, `VERIFY_FAILED`, `ERROR`, `PENDING`.

### D. Expanded Classification + Diagnostics
Two new candidate classifications, plus `reason` string on every result:

| Classification | Meaning |
|----------------|---------|
| `HASH_MISMATCH` | Bytes matched but SHA-256 hashes differ (internal consistency failure) |
| `METADATA_ACCOUNTING_FAILURE` | Size accounting produced a non-positive total (should not occur in practice) |

All classifications now carry a human-readable `reason` string. Run summaries track all classification counts.

### E. Rich Event Detail Screen
The Event Detail screen now shows:
- Full size breakdown card
- Original SHA-256 and reconstructed SHA-256
- Verification method
- Candidate index
- Run config summary
- Engine version
- Replay status + replay button + replay result inline

### F. Run Summary / Lab Report
After every run the Collider shows an expandable lab report with:
- Classification counts (winners, no-gain, exactness failures, hash mismatches, errors, not-applicable)
- Top 5 candidates by total encoded size (color-coded for winners)
- Baseline size and best winner size

### G. Fixture Corpus Run
A dedicated Corpus screen runs all 7 built-in fixtures through the collider and produces a corpus summary showing:
- Wins and losses per fixture
- Average savings percentage across winning fixtures
- Best case and worst case fixtures
- Per-fixture candidate counts and elapsed time

### H. Expanded Search
- **Enabled transform toggles**: any subset of transforms can be enabled/disabled via Settings
- **Chain length 4**: opt-in setting for 4-step transform chains
- **Consecutive deduplication**: same transform cannot appear at consecutive positions in a chain
- All generation remains deterministic and bounded

---

## What Phase 1 Did

Phase 1 proved the basic loop: load → generate → encode → decode → verify (byte equality only) → compare to baseline → archive strict winners.

Phase 2 does not change Phase 1's core logic — it strengthens it.

---

## What Phase 2 Does NOT Do

- No World-Law Collider, GPU compute, or native C++
- No cloud, auth, ads, or social features
- No neural compression or ML guidance
- No production file replacement
- No fabricated wins

---

## Exactness and Hash Verification Policy

A candidate passes verification if and only if:
1. `original.size == reconstructed.size`
2. `original.contentEquals(reconstructed)` (byte-for-byte)
3. `SHA-256(original) == SHA-256(reconstructed)`

Conditions (1) and (2) guarantee byte equality. Condition (3) provides an independent cryptographic check. In theory they cannot disagree; if they did, it would indicate an internal error in the verifier. This dual-check approach is documented in `ExactnessVerifier.verifyFull()`.

---

## Total Encoded Size Accounting Policy

```
totalEncodedSize = backendCompressedPayloadSize
                 + transformMetadataSize        (sum of 4-byte prefix + metadata per transform)
                 + containerHeaderSize          (always 8 bytes)
```

This is the number compared to the baseline. The comparison is intentionally unfavourable to the candidate — every overhead byte counts against it. A candidate only wins if its `totalEncodedSize < baseline.encodedSize`.

The baseline is measured as raw Deflate output on the unmodified input bytes, with no metadata overhead, because a baseline has no transform chain to account for.

---

## Replay Policy

Replay is a re-execution of the saved recipe on the input bytes. It is considered `MATCHED` if:
- `replayEncodedSize == savedEvent.winningSize`
- Exactness + SHA-256 verification passes on the re-run output
- `SHA-256(replay_input) == savedEvent.originalSha256` (if archived)

A `SIZE_MISMATCH` does not necessarily mean the event was wrong — it may indicate the input bytes used for replay were different from the original. An `ERROR` means the recipe could not be executed (e.g., the input changed, or the transform is unavailable). Events are not retroactively invalidated by a replay failure; the archived result stands as measured.

---

## How to Interpret Wins Honestly

A "Verified Exact-Reconstruction Winner" means exactly:
- The recipe was applied to the specific input file bytes.
- The output was decoded step-by-step in reverse.
- The decoded bytes are byte-for-byte identical to the input and their SHA-256 matches.
- The total encoded size (including all metadata overhead) is strictly smaller than Deflate applied directly to the input.

It does **not** mean:
- The recipe will win on other files.
- The recipe is better than production compressors.
- The savings generalise beyond this specific input.

---

## Known Limitations

1. **Baseline conservatism**: The baseline has no overhead; candidates pay full overhead. This is intentional and honest.
2. **Small file sizes**: Phase 2 is validated for files up to ~5 MB. Large files may take many seconds.
3. **Transform metadata cost**: Transforms with non-trivial metadata (e.g., `FixedBlockShuffleTransform` with 8 bytes) will not win on small inputs where the metadata overhead exceeds any compression savings.
4. **Replay requires same input bytes**: Replay currently only works automatically for built-in fixture files. User-imported files require re-importing the same file before replay.
5. **No persistence of full candidate result list**: Only the winning events are persisted to Room; the full per-run candidate list lives only in memory.

---

## Architecture Summary

```
app/
  data/
    db/           Room v2: EventEntity (Phase 2 fields), RunSummaryEntity (Phase 2 fields),
                           CorpusRunSummaryEntity (new), MIGRATION_1_2
    repository/   EventRepository with full Phase 2 field mapping + updateReplayStatus
    settings/     CollideSettingsStore: enabledTransformIds, allowChainLength4
  domain/
    model/        + SizeBreakdown, VerificationResult, ReplayResult, RunConfigSnapshot,
                    CorpusResult (FixtureResult + CorpusSummary)
                    Updated: CandidateResult, CandidateClassification, RunStats, SavedEvent
    engine/       + ReplayEngine, CorpusRunner
                    Updated: CompressionColliderEngine (RunConfigSnapshot, candidateIndex)
    evaluator/    Updated: ExactnessVerifier (SHA-256), CandidateEvaluator (SizeBreakdown),
                           BaselineEvaluator, EventRecorder (Phase 2 gate)
    recipes/      Updated: CandidateGenerator (enabled transforms, chain 4)
    transforms/   Unchanged (all 7 transforms still valid)
  ui/
    + corpus/     CorpusScreen + CorpusViewModel
    Updated:      ColliderScreen (RunSummaryCard), ColliderViewModel (RunResult),
                  EventDetailScreen (breakdown, replay, SHA-256), ArchiveScreen (language),
                  HomeScreen (Corpus entry), SettingsScreen (transform toggles, chain 4)
```

---

## Build

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest   # 151 tests, 0 failures
```

Min SDK: 26 (Android 8.0) | Target SDK: 35 | Engine version: 2.0.0-phase2

---

## Recommended Phase 3

### Phase 3: Guided Search + Multi-file Generalization

1. **Frequency-based candidate ordering**: After each corpus run, record which transform positions in winning chains tended to contribute positively. Use simple frequency tables (no ML) to bias the generation order in future runs — place historically productive transforms earlier in the enumeration.

2. **BWT (Burrows-Wheeler Transform)**: Implement a reversible BWT with explicit permutation metadata stored as the decode key. BWT tends to cluster similar bytes together, creating long runs that RLE and MTF benefit from. It is the foundation of bzip2-class compression.

3. **Cross-file recipe generalization score**: After a corpus run, compute a "generalization score" for each winning recipe: `wins_across_files / total_fixtures`. Recipes with higher generalization scores are promoted in the next run's search order.

4. **Streaming evaluation**: Process candidates in a producer/consumer coroutine pipeline rather than sequentially. On multi-core devices this allows encoding/decoding to overlap with baseline computation for the next candidate.

5. **Persistent recipe performance history**: Add a `RecipeHistoryEntity` table that stores the win rate of each recipe across all historical runs. Use this to show which recipes have been most consistently useful.

Phase 3 should still require zero ML, zero cloud, and zero fabrication. It extends intelligent search based solely on honest empirical evidence.
