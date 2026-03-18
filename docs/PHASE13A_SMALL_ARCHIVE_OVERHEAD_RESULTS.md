# Phase 13A — Small Archive Overhead Attack Results

## Summary

Phase 13A reduces archive overhead on small datasets via `--micro` mode, improving competitiveness against gzip/zstd on tiny archives. After fixing the lean-mode creature override regression, lean and micro modes now behave correctly.

## Lean Mode Fix (Post-Regression)

A bug in `_apply_create_flags` caused `--lean` to incorrectly re-enable creature adaptation when `micro=False`. The fix: apply non-lean defaults only when `if not lean:`, so lean/micro overrides are never overwritten. All tests pass; lean mode now correctly disables creature, Tesseract planner, and Tesseract cooperation.

## Implemented

1. **Overhead audit**
   - `overhead_audit.py`: breakdown of manifest, ledger, integrity, reports, maps, snapshots, shared_metadata

2. **--micro mode**
   - Explicit `--micro` flag (implies --lean)
   - Strips: required_files, required_dirs, fold_profile_mode, fold_profile_reason from manifest
   - Strips: duplicate_families, hierarchy_templates, dependency_motifs, byte_fold_families, per_operator_gain_share, fold_profile* from report
   - Ultra-minimal inventory (path + size only)

3. **Overhead reduction**
   - Manifest: ~175 bytes saved (uncompressed)
   - Report: ~270 bytes saved (uncompressed)
   - Total: ~180–200 bytes per small archive (compressed on-disk)

## Small-Dataset Comparison (bytes) — Post-Fix Rerun

Datasets: small_code, template_heavy, config_heavy, mixed-small.

| Dataset | zstd | gzip | zip | infold_micro | infold_lean | infold_default | infold_golem_static |
|---------|------|------|-----|--------------|-------------|----------------|---------------------|
| duplicate-heavy-python (small_code) | 3,150 | 3,366 | 8,185 | **5,822** | 6,012 | 7,501 | 6,551 |
| template-heavy | 2,704 | 2,829 | 4,214 | **5,356** | 5,538 | 6,980 | 6,043 |
| config-heavy | 2,731 | 2,857 | 4,710 | **5,301** | 5,495 | 6,876 | 6,000 |
| mixed-small | 2,667 | 2,792 | 4,437 | **4,959** | 5,142 | 6,585 | 5,632 |

## Overhead Bytes Removed

| Transition | duplicate-heavy | template-heavy | config-heavy | mixed-small |
|------------|-----------------|----------------|--------------|-------------|
| default → lean | 1,489 | 1,442 | 1,381 | 1,443 |
| lean → micro | 190 | 182 | 194 | 183 |

Lean mode correctly removes ~1,400 bytes vs default. Micro removes an additional ~185 bytes vs lean.

## Lean Mode Behavior (Verified)

- **Lean vs default**: Lean is consistently ~1,400 bytes smaller than default across small datasets. Creature, Tesseract planner, and Tesseract cooperation are correctly disabled.
- **Micro vs lean**: Micro is consistently ~180–195 bytes smaller than lean. Manifest and report trimming work as intended.

## Final Recommendation

**Smallest practical mode on small archives**: `--micro`

Use `--micro` when:
- Archive is tiny (< 50 KB raw)
- Size is critical
- Archive intelligence (search, lineage, explain) is still needed

Use `--lean` when:
- Size matters but some extra metadata is acceptable
- Default mode overhead is too high

Do not enable micro by default; lean is sufficient for most size-first use cases. zstd still wins on raw compression ratio; Infold's value remains archive intelligence (searchable, lineage-aware, explainable, structure-aware).

## Validation

- Full test suite: 239 passed
- Verification sweep: all datasets PASS
- Lean/micro tests: 12 passed
- Exact reconstruction: ok for all modes
- Strict validation: ok for all Infold archives
