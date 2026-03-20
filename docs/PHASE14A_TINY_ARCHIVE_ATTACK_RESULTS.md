# Phase 14A — Tiny Archive Attack Results

## Summary

Phase 14A reduces small-archive overhead via Path DNA Folding and Family Membranes, active primarily in `--micro` mode. Infold micro remains the smallest Infold mode; exact reconstruction and strict validation are preserved.

## Implemented

### 1. Path DNA Folding

- **Module**: `infold/engine/path_dna.py`
- **Behavior**: Encodes paths with shared roots; stores roots once, members as `(root_idx, leaf)`.
- **When used**: Micro mode, when `build_path_dna(path_table)` saves bytes vs flat path table.
- **Integration**: `metadata_table_fold` writes `path_dna` format to `path_table.json` when beneficial; `load_path_table` expands on read.

### 2. Family Membranes

- **Behavior**: Compacts `operator_id` in `maps/reconstruction.json` using `operator_ids` + `o` ref per record.
- **When used**: Micro mode, always when metadata_table_fold runs; membranes-only when metadata_table_fold does not run.
- **Integration**: `_load_reconstruction_data` in operations.py expands `o` → `operator_id` for all readers.

### 3. Micro Mode Integration

- Path DNA and Family Membranes are applied only when `_micro_mode` is True.
- Lean and default modes unchanged.

### 4. Report / Explain Visibility

- Manifest: `path_dna_folding`, `path_dna_bytes_saved`, `family_membranes`.
- Report: `metadata_table_fold_metrics.path_dna_folding`, `path_dna_bytes_saved`, `family_membranes`; `phase14a_tiny_archive` when membranes-only.

## Benchmark Comparison (Phase 14A run)

Small datasets: small_code, template_heavy, config_heavy, mixed-small.

| Dataset | zstd | gzip | zip | infold_micro | infold_lean |
|---------|------|------|-----|--------------|-------------|
| duplicate-heavy-python | 3,145 | 3,364 | 8,181 | **5,889** | 6,037 |
| template-heavy | 2,705 | 2,825 | 4,213 | **5,410** | 5,560 |
| config-heavy | 2,726 | 2,853 | 4,712 | **5,361** | 5,515 |
| mixed-small | 2,676 | 2,799 | 4,446 | **5,010** | 5,170 |

Micro is ~125–160 bytes smaller than lean on these datasets.

## Overhead Reduction

- **Path DNA**: Saves bytes when paths share long prefixes (e.g. `tests/fixtures/template_heavy/`). Not always used on very small path sets.
- **Family Membranes**: Saves ~15–20 bytes per reconstruction record by compacting `operator_id` to `o` ref.
- **Combined**: Micro archives are smaller; metadata_table_fold + Path DNA + Family Membranes work together.

## Did Micro Improve Again?

Yes. Micro remains smaller than lean. Path DNA and Family Membranes add extra compaction when metadata_table_fold runs.

## Did Infold Beat gzip/zstd on More Small Datasets?

No. zstd and gzip still win on raw size for small datasets. Infold’s strength remains archive intelligence and structure-heavy projects.

## Validation

- Full test suite: 239 + 6 Phase 14A tests passed
- Verification sweep: all datasets PASS
- Exact reconstruction: ok
- Strict validation: ok

## Recommendation

Use `--micro` for tiny archives when size matters. Path DNA and Family Membranes reduce overhead without changing correctness.
