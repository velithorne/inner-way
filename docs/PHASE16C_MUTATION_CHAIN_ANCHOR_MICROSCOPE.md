# Phase 16C — Mutation Chains v0.2 (Anchor- and Microscope-Aware)

## Summary

Upgrades Mutation Chains to activate more intelligently on real project-style files using Anchor File scope, Structural Microscope-assisted tiny-file structure, and cleaner handoff from template rejection zones.

## Implementation

### 1. Anchor-Aware Candidate Grouping

- Buckets use `(ext, line_count, anchor_id)` when anchors available
- Same-anchor scope is a strong positive signal: paths in same anchor form separate buckets
- Fallback to `(ext, line_count)` for paths without anchor or cross-anchor

### 2. Microscope-Aware Tiny-File Activation

- Microscope groups (from `find_microscope_assisted_groups`) passed to mutation
- Tried with relaxed `min_lines=2` for tiny config/script files
- Only when structural_microscope operator enabled
- Net-positive only; no magnified forms stored

### 3. Template Rejection Handoff

- Template appends `(paths, reason)` to `config["_template_rejected_groups"]` when rejecting for:
  - file_count < min_family
  - scaffold_similarity / slot_ratio thresholds
  - reconstruction_fidelity_failed
- Mutation tries these groups with relaxed `min_lines=2`
- Skips exact_duplicates (leave to Exact Repetition)
- Only when `len(paths) >= min_family_size`

### 4. Deterministic Base Selection

- `_pick_base_min_mutation_payload` tie-breakers:
  1. Total mutation cost (lower better)
  2. Anchor locality (0 if base shares anchor with all, else 1)
  3. Smallest base size
  4. First path lexicographically

### 5. Reporting

- `mutation_chain_metrics`: anchor_assisted_count, microscope_assisted_count, template_handoff_count
- Shown in archive explain: "context-aware: anchor-assisted: N, microscope-assisted: N, template-handoff: N"

## Validation

- All Phase 16A/16B tests pass
- New tests: test_anchor_aware_grouping, test_base_selection_anchor_locality
- Verification sweep: pass
- Exact reconstruction: preserved

## Recommendation

Mutation Chains should remain enabled by default. Anchor scope, microscope, and template handoff improve activation on config-heavy and version-like datasets without weakening correctness.
