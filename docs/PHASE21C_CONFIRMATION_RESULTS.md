# Phase 21C — Confirmation Pass and Stability Check

## Goal
Confirm Phase 21B density improvements are stable, reproducible, and generalizable.

## Results (run 20260320_060516)

### Primary Dataset (infold-workspace)
- **Raw:** 1,825,906 bytes
- **3 repeated runs:** Infold micro 341,176–341,179 bytes; gzip 341,935 bytes
- **Beats gzip:** 3/3 runs
- **Stability:** Infold varied by 3 bytes across runs; gzip identical
- **Reconstruction:** PASS (all runs)
- **Validation:** PASS (all runs)

### Secondary Strong-Lane Datasets
| Dataset | Infold micro | gzip | Beats gzip |
|---------|--------------|------|------------|
| template-heavy | 4,784 | 2,987 | No |
| config-heavy | 4,752 | 3,022 | No |
| duplicate-heavy-python | 5,426 | 3,716 | No |

Gain does **not** generalize to these smaller structure-heavy datasets; gzip wins on small fixtures.

### Overhead (Post Phase 21B)
Dominant components (uncompressed):
- snapshots_passthrough: 1,761,254 bytes
- shared_operator_artifacts: 139,572 bytes
- shared_metadata_tables: 12,031 bytes
- shared_anchors: 6,708 bytes

### Conclusions
1. Infold micro **reliably beats gzip** on the primary large structure-heavy dataset (3/3 runs).
2. Archive size is **stable** (≤3 byte run-to-run variation).
3. Improvement **does not generalize** to small fixtures (template-heavy, config-heavy, duplicate-heavy); gzip wins there.
4. Best Infold mode: **micro** for size-first on large projects.
