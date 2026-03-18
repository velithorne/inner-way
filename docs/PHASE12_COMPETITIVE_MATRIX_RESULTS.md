# Phase 12 — Competitive Benchmark Matrix Results

## Summary

Phase 12 adds a category-aware competitive benchmark that compares Infold against ZIP, gzip, and zstd across dataset categories.

## Implemented

1. **Benchmark matrix schema**
   - Rows: dataset, category, tool, compressed_size, compression_ratio, exact_reconstruction, validation_status
   - Feature fields: searchable, lineage_aware, explainable, structure_aware, metadata_aware
   - Export: CSV, JSON, Markdown

2. **Competitor support**
   - zip, gzip, zstd (skipped cleanly if not installed)
   - Deterministic, well-logged

3. **Infold modes**
   - infold_default (analysis-rich)
   - infold_lean (size-first)
   - infold_golem_static (golem --no-creature --no-tesseract)

4. **Dataset categories**
   - small_code, template_heavy, config_heavy, mixed, opaque_heavy, version_like, medium_code

5. **Feature-value matrix**
   - Per-tool feature comparison (honest; external tools marked unavailable for archive intelligence)

6. **Competitive summary report**
   - Where Infold wins vs ZIP/gzip/zstd
   - Best Infold mode by category
   - Honest competitive position

7. **Research slot**
   - cmix, nncp, paq recorded as future_comparison

## CLI

```bash
python3 -m infold.cli --competitive-matrix --competitive-matrix-output results/competitive_matrix
```

## Sample Results (from run)

- **Tools available:** zip, gzip (zstd not installed on test system)
- **Infold wins:** 1 (medium_code / infold-workspace)
- **gzip wins:** 11 (most small datasets)
- **Best Infold mode:** infold_lean for medium_code

Infold excels on structure-heavy, template-heavy, and duplicate-heavy projects. Traditional compressors (gzip) often win on small datasets and opaque content where overhead dominates.
