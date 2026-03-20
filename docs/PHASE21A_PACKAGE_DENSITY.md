# Phase 21A — Package Density Attack / Overhead Reduction

## Goal
Reduce archive/package overhead so Infold micro can close more of the remaining gap to gzip on structure-heavy project datasets, while preserving exact reconstruction, validation, determinism, and current operator behavior.

## Implemented Changes

### 1. Overhead Audit (Phase 21A)
- **`audit_archive_overhead_phase21a()`** in `infold/benchmark/overhead_audit.py`
- Granular breakdown: manifest, ledger, integrity, maps_reconstruction, maps_chunk_reconstruction, shared_operator_artifacts, shared_anchors, shared_metadata_tables, reports_json, reports_txt, snapshots_inventory, snapshots_passthrough
- Returns `breakdown`, `per_file`, `dominant` (top 8 components)

### 2. Manifest Density
- **`encode_manifest_micro()`** in `infold/engine/compact_micro.py`
- Omit optional empty fields: `fold_profile_mode`, `fold_profile_reason`, `scope_accounting`
- Always include MANIFEST_REQUIRED_KEYS (project_id, etc.)
- Added short key `pid` for `project_id`

### 3. Shared Artifact Path Ref Compaction
- **`_apply_shared_artifact_path_refs()`** in `infold/engine/metadata_table_fold.py`
- When metadata_table_fold applies, rewrite `template_*.json` and `mutation_chain_*.json` to use `path_refs` instead of full `paths`
- Saves bytes when paths are long and repeated
- Reconstruction resolves `path_refs` via path_table when loading template artifacts

### 4. Reconstruction Support
- **`infold/archive/operations.py`**: Template reconstruction now resolves `path_refs` when path_table exists and artifact has `path_refs`

## Required Outputs
- `overhead_audit.md` / `overhead_audit.json` — from `scripts/run_phase21a_package_density.py`
- `package_density_before_after.md` / `package_density_before_after.json`
- `micro_density_summary.md` / `micro_density_summary.json`
- `summary.md` — answers required questions

## Usage
```bash
python3 scripts/run_phase21a_package_density.py
```

## Validation
- All changes preserve exact reconstruction
- Strict validation passes
- Deterministic behavior maintained
