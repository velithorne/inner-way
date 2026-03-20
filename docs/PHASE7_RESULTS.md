# Phase 7 — Lineage Insights and Timeline Intelligence — Results Summary

## New Lineage Questions Infold Can Now Answer

1. **When did this family first appear?** — `sync trace --family template` shows `first_seen_snapshot`
2. **When did it disappear?** — `last_seen_snapshot` plus `present_in_latest` (false = gone)
3. **Which snapshots contain it?** — `snapshots_seen_in` in trace output
4. **Which operator/family changed most over time?** — `sync lineage-report` shows added/removed by interval
5. **Fold count / gain / physical size over time?** — `sync timeline`
6. **Search with lineage context?** — `sync search --with-lineage` adds first_seen, last_seen, snapshot_id

## Implementation Summary

### 1. First-seen / last-seen tracking
- `compute_lineage_tracking()` in `infold/sync/lineage_insights.py`
- Tracks exact_repetition, template_skeleton, hierarchy_mirror, dependency_motif, byte_fold
- Per-family: first_seen_snapshot, last_seen_snapshot, snapshot_count, present_in_latest, snapshots_seen_in
- Resolves targets_refs from metadata_table_fold for signature extraction

### 2. Lineage trend summary
- `sync timeline` — fold_over_time, logical_gain_over_time, physical_size_over_time, operator_usage_over_time
- `sync_summary` unchanged; timeline is the extended view

### 3. Search + lineage integration
- `sync search --with-lineage` — adds lineage_tracking, snapshot_id, present_in_latest, first_seen, last_seen to matches

### 4. Timeline / trace reporting
- `archive sync timeline --dir .infold-sync`
- `archive sync trace --dir .infold-sync --family template`
- `archive sync lineage-report --dir .infold-sync`

### 5. Change-focused reporting
- `sync lineage-report` — added/removed families by snapshot interval, grouped by operator
- `sync report` — now includes duplicate_families (exact_repetition)

### 6. Tests
- 6 new tests: sync_timeline, sync_trace, sync_trace_filter_family, sync_lineage_report, sync_search_with_lineage, lineage_tracking_first_last_seen

## Exact Reconstruction and Sync Behavior

- **Exact reconstruction:** Intact. No changes to fold pipeline or package format.
- **Sync behavior:** Intact. init, add, list, compare, report, reconstruct, validate, search, summary unchanged.
- **New commands:** timeline, trace, lineage-report are additive.

## CLI Usage

```bash
# Timeline
python3 -m infold.cli archive sync timeline --dir .infold-sync

# Trace family lifecycle
python3 -m infold.cli archive sync trace --dir .infold-sync --family template

# Change-focused report
python3 -m infold.cli archive sync lineage-report --dir .infold-sync

# Search with lineage
python3 -m infold.cli archive sync search --dir .infold-sync --operator exact_repetition --with-lineage
```
