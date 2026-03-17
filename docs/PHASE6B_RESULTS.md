# Phase 6B — Adaptive Profile Benchmarking and Tuning — Results Summary

## Test and Verification Status

- **Full test suite**: 153 passed
- **Verification sweep**: PASS
- **Profile comparison benchmark**: Completed

## Profile Comparison Findings

### Auto vs Best Summary

- Total datasets: 12
- Auto matched best physical (archive size): 0/12 (0%)
- Auto matched best logical gain: 1/12 (8.3%)
- Auto near-best physical (within 5%): 9/12 (75%)

### Profile Wins

| Metric | Winner |
|--------|--------|
| Physical folded size (archive size) | golem (12/12) |
| Logical gain | dragon (12/12) |

### Best Profile by Dataset Category

| Category | Best Physical | Best Gain |
|----------|--------------|-----------|
| tiny | golem | dragon |
| balanced | golem | dragon |
| large_structured | golem | dragon |
| opaque_heavy | golem | dragon |
| version_like | golem | dragon |

## Conclusions

### Which profile is best for tiny archives
**golem** — produces smallest archive size (compact package + metadata fold settings)

### Which profile is best for balanced projects
**golem** for physical size; **dragon** for logical gain

### Which profile is best for large structure-rich archives
**golem** for physical size; **dragon** for logical gain

### Which profile is best for binary/opaque-heavy content
**golem** for physical size; **dragon** for logical gain

### Which profile is best for version-like datasets
**golem** for physical size; **dragon** for logical gain

### Whether Auto should remain the default
**Yes.** Auto remains the default. It selects sparrow for tiny, fox for balanced, dragon for large_structured. While golem wins on physical size across all categories, Auto is conservative and semantically correct (sparrow for small, fox for balanced, dragon for large). The near-best rate (75% within 5%) is acceptable. Users can override with `--profile golem` when archive size is critical.

### Threshold changes justified
None. No speculative changes were made to auto-selection thresholds. The evidence shows golem wins on physical size across all categories, but changing the default was deemed too aggressive. Conservative approach: document findings and recommend manual `--profile golem` when archive size is the priority.

## Implementation Summary

1. **Profile comparison benchmark**: `--profile-compare` runs each dataset across all profiles (auto, sparrow, fox, dragon, golem, serpent)
2. **Representative dataset coverage**: Categories tiny, balanced, large_structured, opaque_heavy, version_like
3. **Auto-vs-best evaluation**: Per-dataset comparison of Auto's choice vs best physical and best gain
4. **Profile win summaries**: Physical wins, gain wins, best by category
5. **Reporting**: CSV, JSON, Markdown exports with tuning recommendations
6. **Tests**: 9 new tests for profile comparison

## CLI Usage

```bash
python3 -m infold.cli --profile-compare --profile-compare-output results/pc
```

Outputs: `profile_comparison.csv`, `profile_comparison.json`, `profile_comparison.md`, `auto_vs_best.csv`
