# Phase 5D — Infold Metadata Table Fold v0.4 — Results Summary

## Implementations

### 1. Metadata Table Fold as Second Byte Operator

- **New module:** `infold/engine/metadata_table_fold.py`
- **Operator:** metadata_table_fold (package-level, runs after export, before integrity)
- **Config:** `operators.metadata_table_fold.enabled`, `thresholds.metadata_table_fold.min_net_gain_bytes`

### 2. Target Metadata Domains

- maps/reconstruction.json targets
- maps/chunk_reconstruction.json paths
- snapshots/inventory.json files[].path
- snapshots/passthrough.json keys

### 3. Shared Metadata Tables

- **shared/metadata_tables/path_table.json:** Ordered list of unique path strings
- **Compact refs:** maps use targets_refs, path_refs; passthrough uses integer keys
- **Deterministic:** Ordered by sorted path strings

### 4. Package Export and Reconstruction

- **create_archive:** Calls apply_metadata_table_fold after export_package
- **reconstruct_archive:** Resolves path_table refs when path_table exists
- **Backward compatibility:** Archives without path_table use raw paths
- **Integrity:** metadata_tables hashed in integrity.json
- **Validation:** Strict mode requires path_table when path_table_ref is set

### 5. Planner and Gain Logic

- **Estimate:** gross_saved (path bytes - ref bytes) - table_overhead
- **Skip:** When net_saved < min_net_gain_bytes (default 32)
- **Config:** min_net_gain_bytes tunable

### 6. Reporting Visibility

- **archive explain:** Metadata Table Fold section (unique_paths, table_size_bytes, net_bytes_saved)
- **archive stats:** metadata_table_fold_metrics
- **report.json:** metadata_table_fold_metrics added when applied

### 7. Search / Sync Compatibility

- **search_archive:** Resolves targets_refs via path_table for human-readable paths
- **list_archive:** Resolves targets_refs
- **compare_archives:** _family_signatures resolves path_refs for byte_fold
- **Sync:** Unchanged; compare/report work with resolved paths

### 8. Tests

- test_metadata_table_fold_collects_paths
- test_build_path_table_deterministic
- test_load_path_table_missing_returns_none
- test_metadata_table_fold_creates_path_table
- test_metadata_table_fold_skips_low_gain
- test_archive_with_metadata_table_fold_reconstructs
- test_archive_validate_strict_with_metadata_table_fold
- test_metadata_table_fold_disabled

## Verification (2026-03-17)

- **Tests:** 133 passed (125 existing + 8 new)
- **Verification sweep:** PASS (6 correctness datasets)
- **Benchmark campaign:** All datasets recon=ok, validate=ok, integrity=ok

## Example: duplicate_python

- **Metadata Table Fold:** unique_paths=8, table_size_bytes=174, net_bytes_saved=88
- **Exact reconstruction:** OK
- **Strict validation:** OK

## Conclusion

- **Exact reconstruction:** Preserved
- **Strict validation:** Passes
- **Metadata Table Fold:** Should remain enabled by default
- **Package overhead:** Reduced on path-heavy archives
