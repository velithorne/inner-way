# Phase 16A — Mutation Chains v0.1 Results

## Summary

Mutation Chains add a conservative, deterministic representation for closely related text files: **base member + compact mutations** instead of storing each member separately.

## Implementation

### Model
- **base member**: one full file (deterministic: smallest by bytes, then first by path)
- **mutated members**: compact line-based diffs `(line_idx, line_content)` for differing lines only
- **mutation chain**: base + mutations map
- **mutation gain**: net-positive only (overhead: 50 + 15×members bytes)

### Scope (v0.1)
- Config-like text files
- Repeated scripts with small changes
- Version-like text/project files
- Same line count, min 85% line overlap (configurable)
- Min 3 files, min 5 lines
- Text-oriented only; no binary delta

### Package Format
- `shared/mutation_chain_{i}.json`: base_content, base_path, mutations, paths
- `maps/reconstruction.json`: record with operator_id `mutation_chain`
- Exactly reversible, validation-safe

### Reconstruction
- Base written as-is
- Each mutated member: apply `(line_idx, content)` to base lines
- Byte-exact reconstruction

## Where Mutation Chains Help

- **config-heavy**: Similar configs with few differing values
- **version-like**: Same structure, small edits
- **repeated scripts**: Same scaffold, slot variations

## Benchmark

| Dataset        | Before (no mutation) | After (mutation) | Gain      |
|----------------|----------------------|-------------------|-----------|
| mutation_chain | 4 files stored       | 1 base + 3 muts   | ~11 bytes |

## Recommendation

**Enabled by default.** Conservative, net-positive only, no regression to exact reconstruction or validation.
