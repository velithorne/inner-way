# Template Skeleton min_lines Experiment

## Comparison: min_lines=5 (current) vs min_lines=4 (experimental)

| Dataset | min_lines | Template Families | Template Gain | Rejected | Physical | Recon OK |
|---------|-----------|-------------------|---------------|----------|----------|----------|
| template-heavy | 5 | 1 | 127 | 0 | 295 | yes |
| template-heavy | 4 | 1 | 127 | 0 | 295 | yes |
| template-stress | 5 | 0 | 0 | 1 | 1,940 | yes |
| template-stress | 4 | 0 | 0 | 1 | 1,940 | yes |
| template-reject | 5 | 0 | 0 | 3 | 1,161 | yes |
| template-reject | 4 | 0 | 0 | 3 | 1,161 | yes |
| infold-workspace | 5 | 0 | 0 | 107 | 875,511 | yes |
| infold-workspace | 4 | 0 | 0 | 109 | 875,511 | yes |

## Reject Reasons (min_lines=5)

### template-heavy
- (none)

### template-stress
- files skipped: 30 with < 5 lines (min for template analysis)

### template-reject
- files skipped: 5 with < 5 lines (min for template analysis)
- scaffold_similarity 0.091 < 0.8; slot_ratio 0.909 > 0.35
- scaffold_similarity 0.500 < 0.8; slot_ratio 0.500 > 0.35

### infold-workspace
- files skipped: 121 with < 5 lines (min for template analysis)
- file_count 1 < min_family_size 3
- file_count 1 < min_family_size 3
- file_count 1 < min_family_size 3
- file_count 1 < min_family_size 3


## Reject Reasons (min_lines=4)

### template-heavy
- (none)

### template-stress
- files skipped: 30 with < 4 lines (min for template analysis)

### template-reject
- scaffold_similarity 0.000 < 0.8; slot_ratio 1.000 > 0.35
- scaffold_similarity 0.091 < 0.8; slot_ratio 0.909 > 0.35
- scaffold_similarity 0.500 < 0.8; slot_ratio 0.500 > 0.35

### infold-workspace
- files skipped: 112 with < 4 lines (min for template analysis)
- file_count 1 < min_family_size 3
- file_count 1 < min_family_size 3
- file_count 1 < min_family_size 3
- file_count 1 < min_family_size 3


## False Positive Risk Signals (min_lines=4)

### template-reject
- High slot_ratio rejection: scaffold_similarity 0.000 < 0.8; slot_ratio 1.000 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.091 < 0.8; slot_ratio 0.909 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.500 < 0.8; slot_ratio 0.500 > 0.35

### infold-workspace
- High slot_ratio rejection: scaffold_similarity 0.000 < 0.8; slot_ratio 1.000 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.077 < 0.8; slot_ratio 0.923 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.625 < 0.8; slot_ratio 0.375 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.000 < 0.8; slot_ratio 1.000 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.000 < 0.8; slot_ratio 1.000 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.000 < 0.8; slot_ratio 1.000 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.000 < 0.8; slot_ratio 1.000 > 0.35
- High slot_ratio rejection: scaffold_similarity 0.500 < 0.8; slot_ratio 0.500 > 0.35


## Summary

**Improved with min_lines=4:** []
**Regressed with min_lines=4:** []
**Unchanged:** ['template-heavy', 'template-stress', 'template-reject', 'infold-workspace']
**FP risk at 4:** 2 datasets

## Conclusion

Lowering min_lines to 4 does **not** increase template families or gain in this experiment.

**Recommendation:** Keep default min_lines=5. Lowering to 4 did not yield useful new folds in this experiment. template-stress files have 3 lines, so min_lines=4 still skips them. No false positives observed with min_lines=4.