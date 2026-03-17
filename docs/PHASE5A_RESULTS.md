# Phase 5A — Infold Byte Fold v0.1 — Results Summary

## A. Test Pass

| Metric | Result |
|--------|--------|
| Total tests | 111 |
| Passed | 111 |
| Failed | 0 |
| Status | **PASS** |

## B. Verification Sweep

| Dataset | Raw | Folded | Logical Gain | Fold Count | Recon | Status |
|---------|-----|--------|--------------|------------|-------|--------|
| duplicate-heavy-python | 4,478 | 4,288 | 190 | 1 | ok | PASS |
| template-heavy | 422 | 295 | 127 | 1 | ok | PASS |
| config-heavy | 442 | 363 | 79 | 1 | ok | PASS |
| mixed-small | 340 | 340 | 0 | 0 | ok | PASS |
| hierarchy-mirror | 249 | 229 | 20 | 1 | ok | PASS |
| dependency-motif | 320 | 270 | 50 | 1 | ok | PASS |

**Engine invariant checks:** All 6 datasets PASS

## C. Benchmark Suite

| Dataset | Raw | ZIP | Gzip | Infold logical gain | Infold physical | Fold count | Recon |
|---------|-----|-----|------|---------------------|-----------------|------------|-------|
| duplicate-heavy-python | 4,478 | 2,440 | 1,317 | 190 | 4,288 | 1 | ok |
| template-heavy | 422 | 660 | 149 | 127 | 295 | 1 | ok |
| config-heavy | 442 | 1,016 | 192 | 79 | 363 | 1 | ok |
| infold-workspace | 1,130,182 | 275,928 | 178,514 | 52,207 | 1,077,975 | 23 | ok |
| mixed-small | 340 | 823 | 254 | 0 | 340 | 0 | ok |
| hierarchy-mirror | 249 | 1,019 | 122 | 20 | 229 | 1 | ok |
| dependency-motif | 320 | 558 | 123 | 50 | 270 | 1 | ok |

## D. Benchmark Campaign

| Dataset | Raw | ZIP | Gzip | Infold Phys | Infold Gain | Fold | Recon | Validate | Integrity |
|---------|-----|-----|------|-------------|-------------|------|-------|----------|-----------|
| duplicate-heavy-python | 4,478 | 2,440 | 1,317 | 4,288 | 190 | 1 | ok | ok | ok |
| template-heavy | 422 | 660 | 149 | 295 | 127 | 1 | ok | ok | ok |
| config-heavy | 442 | 1,016 | 192 | 363 | 79 | 1 | ok | ok | ok |
| mixed-small | 340 | 823 | 254 | 340 | 0 | 0 | ok | ok | ok |
| hierarchy-mirror | 249 | 1,019 | 122 | 229 | 20 | 1 | ok | ok | ok |
| dependency-motif | 320 | 558 | 123 | 270 | 50 | 1 | ok | ok | ok |
| infold-workspace | 1,137,277 | 278,741 | 178,709 | 1,081,061 | 56,216 | 28 | ok | ok | ok |
| duplicate-stress | 10,550 | 5,672 | 95 | 2,661 | 7,889 | 1 | ok | ok | ok |
| template-stress | 1,940 | 4,962 | 249 | 1,940 | 0 | 0 | ok | ok | ok |

## E. Archive Workflow Test (byte_fold fixture)

| Command | Result |
|---------|--------|
| `archive create --source tests/fixtures/byte_fold` | Created |
| `archive inspect` | Files: 6, Folds: 1, Logical gain: 2,786 bytes |
| `archive explain` | template_skeleton: 1 fold, 2,786 bytes |
| `archive validate --mode strict` | Valid |
| `archive reconstruct --output byte_fold_restored` | Reconstructed 6 files |
| `diff -rq original restored` | No differences (exact reconstruction) |

## F. Phase 5A Byte Fold Additions

| Component | Status |
|-----------|--------|
| File routing policy | Done |
| Chunking subsystem | Done |
| ByteFoldOperator | Done |
| Package integration | Done |
| Archive reconstruct | Done |
| Integrity hashing for chunks | Done |
| Report metrics | Done |
| Tests | 9 new tests |

## G. Conclusion

- **Tests:** 111 passed, 0 failed
- **Verification sweep:** All datasets PASS
- **Benchmark campaign:** All datasets ok (recon, validate, integrity)
- **Archive validation:** Strict mode passes
- **Exact reconstruction:** Confirmed
- **Phase 5A Byte Fold v0.1:** Foundation complete

**Build status:** Ready for broader testing
