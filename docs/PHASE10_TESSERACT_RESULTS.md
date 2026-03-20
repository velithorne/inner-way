# Phase 10 — Tesseract Fold v0.1 Results

## Summary

Tesseract Fold adds a **multi-dimensional family identity layer** that unifies structure, byte reuse, metadata reuse, and temporal persistence as linked dimensions of a shared family core. It enriches search, sync, and explain/report outputs without replacing current fold operators or the archive format.

## What Tesseract Fold Adds

- **Tesseract Signature schema**: `family_type`, `operator`, `structure_sig`, `byte_sig`, `metadata_sig`, `time_sig`, `dimensions_present`, optional strength helpers
- **Deterministic signature generation** for exact_repetition, template_skeleton, hierarchy_mirror, dependency_motif, byte_fold
- **Sync trace enrichment**: each trace item includes `tesseract` and `tesseract_summary` (e.g. `structural+temporal`)
- **Search enrichment**: `--with-tesseract` adds `tesseract`, `dimensions_present`, `tesseract_summary` to fold matches
- **Explain/report integration**: archive explain shows "Tesseract (multi-dimensional family identity)" with families by dimension (mainly structural, byte reuse, metadata reuse, temporal persistence)

## Dimensions Currently Supported

| Dimension | Source | When present |
|-----------|--------|--------------|
| **structure** | Family/operator signature (paths, roots, scaffold) | All fold families |
| **byte** | Byte Fold chunk metrics (chunk_reuse_ratio) | byte_fold families |
| **metadata** | Path table / metadata_table_fold participation | When path_table exists |
| **time** | Lineage tracking (first_seen, last_seen, snapshot_count) | Sync context with lineage |

## How It Improves Search/Sync/Explain

- **Search**: `--with-tesseract` shows whether a match is mainly structural, has byte reuse, metadata reuse, or temporal persistence
- **Sync trace**: Each family lifecycle item shows `dims=[structural+temporal]` (or similar)
- **Explain**: "Tesseract" section lists families by dimension (mainly structural, byte reuse, metadata reuse, temporal persistence)

## Exact Reconstruction and Sync Behavior

- **Exact reconstruction**: Intact. Tesseract is additive metadata only; no changes to fold operators or archive format.
- **Sync behavior**: Intact. Lineage tracking, compare, report unchanged. Trace output extended with tesseract.
- **Package compatibility**: Preserved. No new required package files.

## CLI Examples

```bash
# Archive search with tesseract
python3 -m infold.cli archive search archive.infold --with-tesseract

# Sync search with tesseract
python3 -m infold.cli archive sync search --dir .infold-sync --with-tesseract

# Sync trace (always includes tesseract)
python3 -m infold.cli archive sync trace --dir .infold-sync

# Archive explain (always includes tesseract summary)
python3 -m infold.cli archive explain archive.infold
```

## Validation

- Full test suite: 186 passed
- Verification sweep: PASS
- Sync trace with tesseract: OK (dims=[structural+temporal])
- Archive search --with-tesseract: OK (tesseract: structural+metadata_reuse)
- Archive explain: OK (Tesseract section with families by dimension)
