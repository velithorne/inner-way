# Phase 14C — Ruthless Small-Archive Selection and Scope Alignment

## Summary

Phase 14C tightens micro-mode selection, adds scope accounting for fair comparisons, and supports `--fair-scope` for staged benchmark runs.

## Implemented

### 1. Micro-mode selection audit

- `_micro_skip_diagnostics` list records when folds/metadata are skipped:
  - **planner**: reject_low_value, reject_high_stress, reject_conflict, etc.
  - **metadata_table_fold**: net_saved < min_net
  - **path_dna**: dna_saved < _micro_path_dna_min_bytes_saved (100)
- Lightweight, deterministic, surfaced in report and explain.

### 2. Ruthless micro thresholds

When `--micro` is used:

- `planner.min_net_value`: 0.5 (was -0.5 default)
- `metadata_table_fold.min_net_gain_bytes`: 64 (was 32)
- `_micro_path_dna_min_bytes_saved`: 100 (Path DNA only if saves ≥ 100 bytes)

### 3. Scope accounting / inclusion transparency

- `compute_scope_metrics()` in scanner returns:
  - source_file_count, source_bytes
  - included_file_count, included_bytes
  - excluded_file_count, excluded_bytes
  - excluded_by_extension, excluded_by_pattern
- Stored in `_scope_metrics` and written to:
  - report.json `scope_accounting`
  - manifest.json `scope_accounting` (when source ≠ included)
  - archive explain output

### 4. Fair-scope support

- `--fair-scope` extends `include_extensions` with `.csv`, `.html`, `.toml`, `.cfg`, `.ini`
- Improves alignment with staged benchmark inputs
- Optional; default behavior unchanged

### 5. Benchmark rerun (fair staged comparison)

| Tool | Size (bytes) |
|------|--------------|
| zstd | 266,827 |
| gzip | 282,250 |
| **infold_micro** | **285,123** |
| infold_lean | 285,807 |
| infold_golem | 290,371 |
| infold_default | 291,459 |
| zip | 429,014 |

**Staged raw:** 1,567,773 bytes (350 files)  
**Infold included:** 345 files (1,561,322 bytes), 5 excluded

### 6. Results

- **Size reduction from stricter micro:** Micro is ~2.8 KB smaller than lean (285,123 vs 285,807). Ruthless thresholds reduce low-value folds.
- **Micro vs gzip:** Micro is ~2.9 KB behind gzip (285,123 vs 282,250).
- **Scope clarity:** Scope accounting shows source/included/excluded counts and bytes.
- **Remaining gap:** Package overhead (manifest, maps, reports, integrity) plus structural fold metadata. zstd/gzip remain smaller on raw size.

### 7. Exact reconstruction

All Infold modes (default, lean, micro, golem) validated and reconstructed exactly for the 345 archived files.

## CLI

```bash
# Micro with ruthless thresholds
python3 -m infold.cli archive create --source . --output out.infold --micro

# Fair-scope for staged benchmarks (include .csv, .html, .toml)
python3 -m infold.cli archive create --source staged_source --output out.infold --fair-scope
```

## Tests

- `test_phase14c_micro_scope.py`: scope metrics, micro thresholds, fair-scope, exact reconstruction
