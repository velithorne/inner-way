# Phase 18B — Anchor Files v0.2 (Context Compression and Operator Guidance)

## Summary

Upgrades Anchor Files from metadata-only reporting into a conservative context-compression and shared operator-guidance layer.

## Implementation

### 1. Shared Anchor Context Utilities (`infold/engine/anchor_file.py`)

- **build_path_to_anchor_scopes(anchors)**: Map path → set of anchor ids. Reusable by Fold Echo, Mutation Chain, package, report.
- **paths_share_anchor(paths, path_to_scopes)**: Return anchor id if all paths share scope, else None.
- **path_in_anchor_scope(path, anchor_id, path_to_scopes)**: Membership test.
- **get_nearest_anchor_for_path(path, path_to_scopes, anchors)**: Nearest anchor for a path.
- **anchor_relative_path(path, anchor_dir)**: Compact relative path (reversible).
- **expand_anchor_relative_path(rel, anchor_path)**: Expand back to full path.

### 2. Orchestrator: Shared Anchor Context

- Before operator loop: `config["_anchor_context"] = {anchors, path_to_scopes}`
- Operators (Fold Echo, Mutation Chain) use `_anchor_context` when available
- Reduces duplicate find_anchor_files / build_path_to_anchor_scopes calls

### 3. Operator Guidance Cleanup

- **Fold Echo**: Uses `paths_share_anchor` from anchor_file; uses `build_path_to_anchor_scopes`
- **Mutation Chain**: Uses `build_path_to_anchor_scopes` via `_path_to_anchor_scopes`
- Both prefer `config["_anchor_context"]` over re-computing

### 4. Metrics / Explain Improvements

- **anchor_metrics**: anchor_assisted_mutation_chain (count of mutation families that used anchor scope)
- **explain output**: metadata reduction estimate, operator guidance line when applicable

### 5. Anchor-Scoped Compaction (Conservative)

- anchor_relative_path / expand_anchor_relative_path available for future use
- No change to stored package paths (exact reconstruction preserved)
- Report uses anchor context for grouping/display only

## Validation

- All Phase 18A tests pass
- New tests: build_path_to_anchor_scopes, paths_share_anchor, anchor_relative_path, path_in_anchor_scope
- Fold Echo and Mutation Chain tests pass
- Verification sweep: pass
- Exact reconstruction: preserved

## Recommendation

Anchor Files should remain enabled by default. Shared utilities improve consistency; operator guidance is conservative and metadata-first.
