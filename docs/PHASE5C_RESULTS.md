# Phase 5C — Infold Byte Fold v0.3 — Results Summary

## Implementations

### 1. Byte Fold Search Visibility

- **Operator/family filters:** `--operator byte_fold`, `--family byte_fold`
- **Chunk filters:** `--min-chunk-reused-bytes`, `--max-chunk-reused-bytes`, `--min-chunk-reuse-ratio`, `--max-chunk-reuse-ratio`, `--min-files-chunk-folded`, `--max-files-chunk-folded`
- **Chunk-focused explain:** When `--explain` and byte_fold match with chunk_metrics, output includes chunk_reused, unique_chunk_count, reuse_ratio, dict_overhead, net
- **Enriched matches:** byte_fold matches get `chunk_metrics` from report.json

### 2. Byte Fold Sync Visibility

- **byte_fold_summary** in sync_report: added_count, removed_count, fold_count_diff, a_count, b_count
- **sync_report_to_text:** "Byte Fold changes:" section when byte_fold changes exist
- **sync_summary:** byte_fold_total across lineage (in JSON)

### 3. Realistic Byte Fold Benchmark Expansion

- **BYTE_FOLD_FIXTURES** in benchmark pack:
  - `tests/fixtures/byte_fold` → byte-fold-opaque
  - `tests/fixtures/byte_fold_large_text` → byte-fold-large-text
  - `tests/fixtures/byte_fold_version_like` → byte-fold-version-like
- Category `byte_fold` in campaign outputs
- Campaign records `byte_fold_metrics` when byte_fold folds exist

### 4. Byte Fold Reporting and Tuning Visibility

- **Tuning report:** byte_fold_metrics (files_chunk_folded, unique_chunk_count, reused_chunk_count, chunk_reused_bytes, chunk_dictionary_size_bytes)
- **Tuning report:** byte_fold_routing distribution (structural_first, chunk_first, passthrough_only, low_value)
- **Campaign markdown:** Byte Fold Metrics table when byte_fold_metrics present; "Top Byte Fold contributing datasets"

### 5. Architecture Prep for Next Byte Operator

- **byte_fold.py:** Docstring notes byte-operator family; future operators can coexist
- **interaction_policy.py:** Comment for byte_fold as first active byte operator; future byte_delta etc. can be added

### 6. Tests and Stability

- **test_search_by_operator_byte_fold**
- **test_search_by_family_byte_fold**
- **test_search_byte_fold_chunk_filters**
- **test_search_byte_fold_explain_when_present**
- **test_sync_report:** byte_fold_summary assertions
- **test_benchmark_pack_datasets:** byte_fold category when fixture exists

## Verification (2026-03-17)

- **Tests:** 125 passed
- **Verification sweep:** PASS (6 correctness datasets)
- **Benchmark campaign:** All datasets recon=ok, validate=ok, integrity=ok

## Benchmark Summary (Byte Fold Datasets)

| Dataset | Raw | ZIP | Gzip | Infold Phys | Infold Gain |
|---------|-----|-----|------|-------------|-------------|
| byte-fold-opaque | 7,095 | 2,319 | 633 | 4,309 | 2,786 |
| byte-fold-large-text | 2,589 | 1,914 | 594 | 970 | 1,619 |
| byte-fold-version-like | 1,050 | 814 | 204 | 504 | 546 |

Infold beats ZIP on byte-fold-large-text and byte-fold-version-like. Gzip wins on byte-fold-opaque (opaque content compresses well).
