# Infold Package Spec v1.0

**Core v0.2.0 freeze.** This document defines the physical folded package format.

## Overview

An Infold package is a directory containing folded project data. It enables deterministic reconstruction of the original project from the folded representation.

## Required Contents

### Required Files

| File | Description |
|------|-------------|
| `manifest.json` | Package metadata, version, compatibility, fold summary |
| `ledger.json` | Full fold ledger with all records and unfold recipes |

### Required Directories

| Directory | Description |
|-----------|-------------|
| `shared/` | Canonical representations from each fold (exact content, templates, symbol maps, etc.) |
| `maps/` | Reconstruction mappings (record index → targets) |
| `reports/` | Fold reports (JSON and text) |
| `snapshots/` | File inventory snapshot |

## Optional Contents

| Path | Description |
|------|-------------|
| `reports/report.json` | Full report as JSON |
| `reports/report.txt` | Human-readable report |
| `shared/exact_*.txt` | Canonical content for exact-repetition folds |
| `shared/template_*.json` | Template scaffold + slots for template-skeleton folds |
| `shared/symbols_*.json` | Symbol dictionary for symbol-table folds |
| `shared/hierarchy_*.json` | Structure metadata for hierarchy-mirror folds |
| `shared/dependency_motif_*.json` | Import motif data for dependency-motif folds |
| `snapshots/passthrough.json` | Files not in any fold (for full reconstruction) |

## Manifest Structure

`manifest.json` must contain:

```json
{
  "version": "1.0",
  "package_spec": "1.0",
  "compatibility": {
    "spec_version": "1.0",
    "min_infold_version": "0.2.0",
    "reconstruction_mode": "deterministic",
    "debug_friendly": true
  },
  "project_id": "...",
  "source_path": "...",
  "created": "ISO8601",
  "file_count": 0,
  "folder_count": 0,
  "original_size_bytes": 0,
  "fold_count": 0,
  "logical_gain_bytes": 0,
  "physical_folded_size_bytes": 0,
  "required_files": ["manifest.json", "ledger.json"],
  "required_dirs": ["shared", "maps", "reports", "snapshots"]
}
```

## Exact-Mode Guarantees

When `reconstruction_mode` is `deterministic`:

1. **Byte-for-byte recovery**: Unfolding produces identical content to the original files.
2. **Deterministic order**: Fold records are applied in ledger order; unfold order is fixed.
3. **No lossy transforms**: All operators preserve exact spelling, boundaries, and encoding.
4. **Validation**: The engine verifies exact reconstruction before commit.

## Operator Set (Core v0.2.0)

| Operator | Scope | Rejection Behavior |
|----------|-------|--------------------|
| **exact_repetition** | file-level | min 64 bytes, min 2 occurrences; exact hash match only |
| **symbol_table** | project-wide | min 5 symbol reuse; rejects ambiguous scope; conflicts with prior content folds |
| **template_skeleton** | file-level | min 3 files, scaffold_similarity ≥ 0.80, slot_ratio ≤ 0.35; rejects exact duplicates |
| **hierarchy_mirror** | metadata | min depth 2, min 2 instances; structure-only, no content change |
| **dependency_motif** | metadata | min 3 deps, min 2 instances; import-signature match only |

Runtime order: exact_repetition → symbol_table → template_skeleton → hierarchy_mirror → dependency_motif.

## Known Limitations

1. **No cross-operator content overlap**: Content operators (exact, template, symbol_table) cannot fold the same file path; conflict policy rejects.
2. **Symbol table runs after exact/template**: May be rejected if paths already folded.
3. **Template requires same line count**: Files grouped by language + line count; structural similarity is line-based.
4. **Hierarchy/dependency are metadata-only**: No file content change; gain is metadata overhead reduction.
5. **No binary support**: Text/code files only; extensions: .py, .js, .ts, .json, .yaml, .yml, .md, .txt.
6. **No fuzzy matching**: All matching is exact or threshold-based deterministic.
