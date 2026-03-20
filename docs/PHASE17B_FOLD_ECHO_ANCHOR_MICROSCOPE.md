# Phase 17B — Fold Echoes v0.2 (Anchor- and Microscope-Aware)

## Summary

Upgrades Fold Echoes so weak near-family files attach more intelligently using Anchor File scope and Structural Microscope-assisted structure.

## Implementation

### 1. Anchor-Aware Host Selection

When multiple template hosts could accept an echo, prefer hosts that are:
- **In the same anchor scope**: Echo path and host paths share at least one anchor (e.g. both under `package.json` scope)
- **Closer to the weak file**: Path proximity via common prefix length (same directory > sibling > distant)

### 2. Path Proximity

- `_common_prefix_len(a, b)`: Length of common path prefix
- `_path_proximity(echo_path, host_paths)`: Max common prefix over all host paths
- Higher = closer (same dir preferred)

### 3. Anchor Scope Index

- `_build_path_to_anchor_scopes(anchors)`: Maps each path to set of anchor identifiers that scope it
- `_anchor_scope_match(echo_path, host_paths, path_to_anchor_scopes)`: True if echo and host share an anchor

### 4. Microscope-Aware Ranking

When echo is microscope-eligible (tiny) and host was microscope-assisted:
- Prefer that host (tiny echo → tiny host family)
- Ranking: anchor_match > microscope_match > proximity > gain

### 5. Host Selection Flow

- Iterate passthrough files first (not hosts)
- For each file, find all matching template hosts
- Rank by (anchor_match, microscope_match, proximity, gain)
- Pick best host per echo
- Deterministic, conservative, explainable

## Validation

- All Phase 17A tests pass
- New tests: `test_anchor_scope_match`, `test_path_proximity`
- Verification sweep: pass
- Exact reconstruction: preserved
