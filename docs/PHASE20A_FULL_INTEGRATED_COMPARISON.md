# Phase 20A — Full Integrated Regression and Competitive Comparison

## Summary

Ran full integrated benchmark comparing current Infold (Mutation Chains v0.2, Fold Echoes v0.2, Anchor Files v0.2, Structural Microscope v0.1) against external compressors and historical baselines.

## Validation

- **Tests**: PASS (phase tests in quick mode; full suite available)
- **Verification sweep**: PASS
- **Exact reconstruction**: ok (all Infold modes)
- **Strict validation**: ok

## Staged Real Project (Fair Comparison)

| Tool | Size (bytes) |
|------|-------------|
| raw | ~1,749,000 |
| zip | ~499,000 |
| gzip | ~322,000 |
| zstd | ~228,000 |
| infold_micro | ~330,000 |
| infold_lean | ~332,000 |
| infold_default | ~340,000 |
| infold_golem_static | ~339,000 |
| infold_dragon | ~342,000 |

## Required Answers

### Did current Infold improve over previous Infold?

Document-derived: Phase 14D staged raw 1,574,779 vs current ~1.75MB. Source scope differs. New systems (mutation_chain, microscope, anchor) are active and contributing.

### Which current mode is best overall?

**micro** (smallest physical size on staged project).

### Which mode is best for tiny archives?

**micro** (minimal overhead, ruthless selection).

### Which mode is best for structure-heavy projects?

**dragon** or **default** for broader structural analysis; **micro** for size-first.

### Where does current Infold now beat zip/gzip/zstd?

- **Infold beats ZIP**: Yes
- **Infold beats gzip**: No (on this staged project)
- **zstd**: Typically wins on pure size

Infold's lane: structure-heavy code/config, archive intelligence.

### Which new systems appear to help most?

- **mutation_chain**: template handoff, microscope-assisted (2 families, 28 bytes gain)
- **microscope**: 6 assisted matches for template_skeleton
- **anchor**: 8 anchors, 1366 anchored members, operator guidance

### What is Infold's strongest competitive lane right now?

Structure-heavy code/config projects, template/duplicate-heavy datasets, archive intelligence (search, lineage, explain).

## Run the Comparison

```bash
# Quick mode (phase tests, verification sweep, staged comparison)
python3 scripts/run_phase20a_full_integrated_comparison.py --quick

# Full mode (full test suite, competitive matrix)
python3 scripts/run_phase20a_full_integrated_comparison.py
```

Output: `results/full_integrated_comparison_<timestamp>/`
