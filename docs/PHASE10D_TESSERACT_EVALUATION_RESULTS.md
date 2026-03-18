# Phase 10D — Tesseract Evaluation and Tuning Results

## Summary

Phase 10D adds a benchmark comparison framework to measure the real effect of Tesseract Planner and Cooperation on physical folded size, logical gain, and metadata cleanup.

## What Was Implemented

1. **Comparison framework**
   - Modes: `baseline` (no Tesseract), `planner_only`, `planner_cooperation`
   - Runs benchmark campaign for each mode on representative datasets
   - Exports JSON, CSV, Markdown

2. **Representative dataset coverage**
   - Structure-heavy, metadata-heavy, byte-heavy, version-like, realistic large
   - Category labels in outputs

3. **Metrics**
   - Raw, ZIP, gzip, Infold physical/logical
   - Fold counts by operator
   - Metadata table fold contribution
   - Package overhead
   - Exact reconstruction, validation, integrity
   - Tesseract-specific: dominant_dimension, cooperation_mode, execution_steps

4. **Cooperation win reporting**
   - Datasets where cooperation improved physical size
   - Datasets where cooperation hurt
   - Total bytes saved/lost by cooperation
   - By-category breakdown

5. **Threshold tuning experiments**
   - `metadata_strength_cutoff`: 0.20, 0.25, 0.30
   - `metadata_threshold_lower_by`: 4, 8, 12 bytes
   - `compactness_bias_increment`: 0.02, 0.03, 0.05

6. **Recommendation output**
   - structural→metadata worthwhile
   - byte→metadata worthwhile
   - Current thresholds adequate
   - structure→byte remain deferred
   - Cooperation enabled by default

## CLI

```bash
python3 -m infold.cli --tesseract-evaluate --tesseract-evaluate-output results/tesseract_evaluate
```

## Evidence from Initial Run

On the benchmark pack (12 datasets):

- **Cooperation improved**: 0 datasets
- **Cooperation hurt**: 12 datasets
- **Total bytes lost**: ~31 KB

Baseline (no Tesseract) produces smaller archives because:
- Tesseract Planner adds manifest/report metadata (route, dominant dimension, execution plan)
- Cooperation adds execution plan and cooperation mode to manifest
- The metadata overhead exceeds any metadata-table-fold savings on these datasets

**Recommendation**: Cooperation should not be enabled by default for physical-size optimization. The evaluation framework provides evidence for tuning decisions.

## Exact Reconstruction and Validation

All modes preserve exact reconstruction and strict validation. The evaluation compares physical size and logical gain; correctness is unchanged.

## Tests

- `test_tesseract_evaluation.py`: config modes, comparison structure, cooperation wins, recommendation, CSV/Markdown export, threshold tuning
