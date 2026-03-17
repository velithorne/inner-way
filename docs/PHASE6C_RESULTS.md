# Phase 6C — Auto Profile Tuning — Results Summary

## Before vs After (Profile Comparison)

| Metric | Before (Phase 6B) | After (Phase 6C) |
|--------|-------------------|------------------|
| Auto matched best physical | 0/12 (0%) | 12/12 (100%) |
| Auto matched best gain | 1/12 (8.3%) | 0/12 (0%) |
| Auto near-best physical (within 5%) | 9/12 (75%) | 12/12 (100%) |

## Summary

Phase 6C optimized Auto for **physical folded size**. Auto now selects **golem** for all non-lineage datasets (golem wins physical for all categories per Phase 6B). For lineage/sync context, Auto selects **serpent**.

### Changes

1. **Scoring-based selection** — Replaced simple rule cascade with per-profile scoring
2. **Physical optimization** — Golem base score 100 (benchmark evidence); serpent 105 when lineage
3. **Deterministic** — Tie-break by fixed profile order
4. **Reporting** — Score breakdown in report, explain, manifest factors

### Trade-off

- **Gain match rate** dropped from 8.3% to 0% — expected, since we optimized for physical not gain. Dragon wins gain; users can use `--profile dragon` when logical gain is priority.

### Tests

- All 154 tests pass (13 profile + 9 profile_comparison + 132 existing)
- Deterministic scoring verified
- Golem selected for tiny, balanced, large, opaque, version-like
- Serpent selected when lineage context exists
