# Phase 5B — Infold Byte Fold v0.2 — Results Summary

## Implemented

### 1. Routing v2
- `route_file_v2()`: deterministic routing with `RouteResult` (route, reason)
- Uses: extension, size, parser_confidence, line_count, committed_paths
- Routes: structural_first, chunk_first, passthrough_only, low_value
- `get_chunk_eligible_paths_with_diagnostics()`: returns routing diagnostics
- Diagnostics stored in `config["_run_diagnostics"]["byte_fold_routing"]`

### 2. Chunk tuning controls
- `min_chunk_reuse_count`: chunks must appear ≥ this to count toward gross (default: 2)
- `max_chunks_per_file`: skip files with > this many chunks (default: 1024)
- `low_structure_confidence`: parser confidence below this → chunk_first (default: 0.5)
- All in `config.json` thresholds.byte_fold

### 3. Chunk dictionary and map compaction
- Compact format: `paths` + `seqs` (chunk indices) instead of path → chunk_ids
- `chunk_index.json`: `{"ids": [...], "record_index": i}`
- `chunk_reconstruction.json`: `{"paths": [...], "seqs": [[...], ...]}`
- Backward compatible: reconstruct supports legacy path_to_chunk_ids

### 4. Byte Fold reporting visibility
- Report: files_chunk_folded, unique_chunk_count, reused_chunk_count, chunk_reused_bytes, chunk_dictionary_size_bytes, net_bytes_saved, chunk_reuse_ratio, chunk_folded_paths
- Routing diagnostics: by_route counts and sample paths
- archive explain, stats, benchmark campaign include Byte Fold metrics

### 5. Search / Sync exposure
- archive compare: byte_fold_families, byte_fold_families_changed
- sync report: byte_fold_families in added/removed, by_operator
- compare_to_text: byte_fold family counts and changes

### 6. Tests
- 6 new tests: route_file_v2, routing classification, chunk tuning, compact reconstruction
- 121 total tests (115 + 6)

## Verification

| Check | Result |
|-------|--------|
| Full test suite | 121 passed |
| Verification sweep | All 6 datasets PASS |
| Benchmark campaign | All ok (recon, validate, integrity) |
| Exact reconstruction | Confirmed |
| Strict validation | Passes |

## Benchmark Campaign (Phase 5B)

| Dataset | Raw | ZIP | Gzip | Infold Phys | Infold Gain | Fold | Recon |
|---------|-----|-----|------|-------------|-------------|------|-------|
| duplicate-heavy-python | 5,528 | 2,727 | 1,537 | 5,338 | 190 | 1 | ok |
| template-heavy | 422 | 660 | 149 | 295 | 127 | 1 | ok |
| config-heavy | 442 | 1,016 | 192 | 363 | 79 | 1 | ok |
| mixed-small | 340 | 823 | 254 | 340 | 0 | 0 | ok |
| hierarchy-mirror | 249 | 1,019 | 122 | 229 | 20 | 1 | ok |
| dependency-motif | 320 | 558 | 123 | 270 | 50 | 1 | ok |
| infold-workspace | 1,271,237 | 309,688 | 197,305 | 1,206,061 | 65,176 | 30 | ok |
| duplicate-stress | 10,550 | 5,672 | 95 | 2,661 | 7,889 | 1 | ok |
| template-stress | 1,940 | 4,962 | 249 | 1,940 | 0 | 0 | ok |

## Conclusion

- **Byte Fold v0.2:** Routing v2, chunk tuning, compact format, improved reporting, Search/Sync exposure
- **Exact reconstruction:** Preserved
- **Package compatibility:** Backward compatible (legacy chunk format supported)
- **Physical size:** Compact format reduces chunk_reconstruction.json size (integer indices vs 32-char ids)
