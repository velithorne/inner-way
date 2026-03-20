# Phase 16B — Mutation Chain Expansion and Real-World Targeting Results

## Summary

Phase 16B expands Mutation Chains from a conservative proof-of-concept into a more useful operator for similar-but-not-identical text/config/script families.

## Implemented Improvements

### 1. Stronger Mutation-Friendly Fixtures
- **mutation_version_like**: 4 JSON version configs (v1–v4) — template_skeleton typically wins
- **mutation_config_heavy**: 4 YAML configs (dev/staging/prod/test) — template_skeleton typically wins
- **mutation_script_like**: 4 Python modules with shared scaffold — template_skeleton typically wins
- **mutation_chain**: Original 4 config-like files — mutation_chain wins when template doesn’t apply

### 2. Better Candidate Grouping
- Group by `(extension, line_count)` when `group_by_extension=True`
- Same-extension files with same line count form candidate families
- Deterministic, conservative

### 3. Cost-Aware Base Selection
- Base chosen to minimize total mutation payload to the rest of the family
- Tie-break: smallest base size, then first by path
- Deterministic

### 4. Cheaper Mutation Encoding
- Contiguous block encoding: 2+ adjacent differing lines stored as `(start_idx, [lines])` instead of multiple `(idx, line)`
- Reduces per-line overhead for contiguous edits
- Exactly reversible

### 5. Chain Shape
- Star layout (base + direct mutations) kept as default
- Linear chain deferred for a later phase

### 6. Reporting / Explain
- `mutation_chain_metrics`: families_found, members_per_chain, gain_bytes, avg_changed_lines
- `mutation_chain_families`: targets, gain, avg_changed_lines
- Explain output: families, members, gain, avg changed lines per chain

## Where Mutation Chains Help

- **mutation_chain fixture**: 1 family, 4 members, ~1–91 bytes gain (when template doesn’t apply)
- **Config-like files** that don’t form template families (e.g. slot_ratio too high)
- **Version-like** and **script-like** files are often handled by template_skeleton first

## Operator Order

Mutation chain runs **after** template_skeleton. When template finds a family, those paths are committed and mutation_chain candidates that overlap are rejected. Mutation_chain is used when template_skeleton does not find a valid family (e.g. slot_ratio > 0.35, scaffold_similarity < 0.80).

## Recommendation

**Enabled by default.** Conservative, net-positive only, no regression to exact reconstruction or validation. Template_skeleton remains the primary operator for structure-heavy families; mutation_chain covers similar-but-not-template cases.
