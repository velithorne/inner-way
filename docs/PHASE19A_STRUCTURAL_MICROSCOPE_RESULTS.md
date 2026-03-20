# Phase 19A — Structural Microscope v0.1 Results

## Summary

The Structural Microscope is an analysis-only layer that magnifies weak structure in tiny text/config/script files to improve template, mutation-chain, or fold-echo matching. It does not store magnified representations; it uses them only during candidate discovery.

## Implementation

### 1. Microscope Model (`infold/engine/structural_microscope.py`)

- **Eligible tiny file**: File below `max_bytes` (512) and `max_lines` (10), text/config/script type
- **Magnified structural view**: Temporary expanded representation (e.g. JSON one key-value per line)
- **Microscope transform type**: JSON expansion, INI normalization, key-value expansion
- **Microscope-assisted candidate**: Group of files identified by magnified structure similarity
- **Microscope gain**: Net effect only when fold leads to net-positive archive result

### 2. Eligibility Rules

- `max_bytes`: 512 (configurable)
- `max_lines`: 10 (configurable)
- `min_family`: 3 (same as template)
- Text extensions: `.json`, `.yaml`, `.yml`, `.toml`, `.ini`, `.cfg`, `.py`, `.js`, `.ts`, `.txt`, `.md`

### 3. Magnified Structural View

- **JSON**: `{"a":1,"b":2}` → one key-value per line (sorted keys)
- **INI**: Normalized key=value lines
- **Generic key-value**: One pair per line

### 4. Operator Integration

- **Template Skeleton**: Primary integration. Microscope groups tiny files (< min_lines) by magnified structure; template processes them with relaxed thresholds (min_similarity - 0.05, max_slot_ratio + 0.05)
- **Mutation Chain / Fold Echo**: Not yet integrated in v0.1; architecture supports future extension

### 5. Net-Positive Gating

- Template still applies net_gain check; microscope-assisted families are rejected when meta_cost exceeds gross gain
- No magnified form stored; only original content used for reconstruction

### 6. Reporting

- `microscope_metrics`: `assisted_matches`, `operators_benefited`
- `microscope_assisted`: List of `{operator, paths}` for each assisted match
- Shown in archive explain output

## Benchmark Results

- **microscope_tiny fixture**: 3 JSON configs (3–4 lines, 1 slot)
  - Without microscope: Normal template skips (min_lines=5)
  - With microscope: Template finds family, fold_count=1, logical_gain=12
  - Exact reconstruction: confirmed

## Validation

- Full test suite: pass
- Verification sweep: pass
- Exact reconstruction: pass
- Strict validation: pass

## Recommendation

Structural Microscope should remain enabled by default. It activates on tiny config/script files that would otherwise remain passthrough, with no impact on correctness or reconstruction.
