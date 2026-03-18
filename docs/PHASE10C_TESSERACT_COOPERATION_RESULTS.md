# Phase 10C — Tesseract Cooperation / Orchestration v0.1 Results

## Summary

Tesseract Cooperation extends the planning layer into a conservative orchestration layer that generates short, safe execution plans for structural→metadata and byte→metadata cooperation. Deterministic, additive, no operator replacement.

## Execution Plans Supported

| Dominant | Secondary | Cooperation mode | Steps |
|---------|-----------|------------------|-------|
| structure | metadata (m≥0.25) | structural_then_metadata | structural → metadata |
| byte | metadata (m≥0.25) | byte_then_metadata | byte → metadata |
| structure | byte | primary_only | structural |
| byte | structure | primary_only | byte |
| metadata | * | primary_only | metadata |
| time (t≥0.6) | * | primary_only | primary |
| mixed/weak | * | balanced | primary |

## How Tesseract Cooperation Works

1. **Execution plan generation**: From Tesseract profile (dominant, secondary, strengths), generate a short plan with execution_steps, cooperation_mode, plan_reason.
2. **Metadata follow-up**: When cooperation_mode is structural_then_metadata or byte_then_metadata, metadata_table_fold uses a slightly lower min_net_gain threshold (max 8 bytes lower, floor 16) to allow more metadata compaction when beneficial.
3. **Planner support**: When cooperation allows metadata follow-up, add small favor_compactness (+0.03, cap 0.08) to planner bias.
4. **Fallback**: primary_only or balanced when cooperation is not appropriate (structure+byte, time-dominant, mixed signals).

## Where It Can Help Fold More Bytes

- **structural_then_metadata**: Structural folds create path refs; metadata_table_fold compacts repeated paths. Lower threshold allows metadata fold when gain is 24–31 bytes (vs default 32).
- **byte_then_metadata**: Byte fold creates path refs in chunk_reconstruction; metadata compaction reduces package overhead.
- **Large projects** (e.g. infold-workspace with 295 folds) trigger structural_then_metadata and benefit from metadata cleanup.

## Exact Reconstruction and Validation

- **Exact reconstruction**: Intact. Cooperation only affects metadata_table_fold threshold and planner bias; no changes to fold operators or correctness.
- **Validation**: Intact. All 207 tests pass, verification sweep passes.

## Visibility

- **Report**: tesseract_execution_plan (steps, cooperation_mode, plan_reason)
- **Explain**: Tesseract Execution Plan section
- **Manifest**: tesseract_execution_steps, tesseract_cooperation_mode, tesseract_plan_reason

## Example Output

```
Tesseract Execution Plan:
  steps=structural->metadata
  cooperation_mode=structural_then_metadata
  plan_reason=structure_metadata_cooperation
```
