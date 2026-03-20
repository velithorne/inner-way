# Phase 21B — Density Attack Part 2

## Goal
Target the four dominant overhead sources in micro mode and reduce them safely:
- snapshots_passthrough
- shared_operator_artifacts
- shared_metadata_tables
- shared_anchors

## Implemented Changes

### 1. Overhead Audit Refinement
- **`audit_archive_overhead_phase21b()`** — Phase 21B audit with before/after comparison
- **`focus_components`** in phase21a audit — snapshots_passthrough, shared_operator_artifacts, shared_metadata_tables, shared_anchors
- **`component_before_after`** — per-component delta when before_audit provided

### 2. snapshots_passthrough Compaction
- **`_compact_passthrough_micro()`** — When path_table_ref (all keys numeric), convert to array format `{"_pa": 1, "e": [[ref, content], ...]}`
- Saves JSON key overhead (each `"N":` replaced by `[N,` in array)
- Reconstruction handles both `_pa` format and legacy object format

### 3. shared_operator_artifacts Compaction
- **`_compact_shared_artifacts_micro()`** — Template short keys: `const_blocks`→`cb`, `slot_groups`→`sg`, `path_refs`→`pr`
- Reconstruction decodes `cb`, `sg`, `pr` when present

### 4. shared_anchors Compaction
- Compact anchor records: `path`→`p`, `anchor_type`→`t`, `anchored_count`→`c`, `anchored_paths`→`ap` (capped at 30)
- Format: `{"_an": 1, "a": [compact_anchors]}`

### 5. Config Flag
- **`_skip_phase21b_compaction`** — When True, skip Phase 21B compaction for baseline comparison

## Results (infold-workspace)
- **shared_operator_artifacts:** 369 bytes saved (template short keys)
- **shared_anchors:** 3,271 bytes saved (compact format)
- **snapshots_passthrough:** +14 bytes (array format added overhead in this run; path_refs may vary)
- **Infold micro:** 336,784 bytes
- **gzip:** 339,106 bytes
- **Infold beats gzip** on this dataset

## Usage
```bash
python3 scripts/run_phase21b_density_attack.py
```

## Validation
- Exact reconstruction preserved
- Strict validation passes
- All changes deterministic and default-on in micro mode
