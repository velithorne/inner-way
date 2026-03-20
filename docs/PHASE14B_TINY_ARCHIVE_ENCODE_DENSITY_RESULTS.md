# Phase 14B — Tiny Archive Encode Density Results

## Summary

Phase 14B reduces structural encoding overhead in micro mode via compact manifest, reconstruction, inventory, and path_table representations. Micro archives are denser; exact reconstruction and strict validation are preserved.

## Micro-Mode Overhead Audit (Pre-Phase 14B)

| Component | Bytes | Notes |
|-----------|-------|-------|
| manifest | ~718 | Long keys, full compatibility dict |
| reconstruction | ~148 | records, path_table_ref, operator_ids |
| inventory | ~165 | files[].path_ref, size |
| path_table | ~123 | "paths" key |
| reports | ~998 | report.json |
| ledger | ~336 | |
| integrity | ~473 | |
| snapshots (passthrough) | ~9336 | Content, not overhead |

## Implemented

### 1. Compact Micro Manifest

- Short keys: `v`, `ps`, `c`, `src`, `cr`, `fc`, `fd`, `os`, `nf`, `lg`, `pf`, `fp`, `ce`, `te`, `tc`, `mt`, `mn`, `fm`, `pd`, `pdb`
- Compatibility: `c` = `{sv, mi, mr}` (minimal)
- Marker: `_m`: 1
- `_load_manifest` decodes when `_m` present

### 2. Compact Reconstruction

- `r` = array of `[index, gain, targets_refs, o]`
- `pt` = path_table_ref, `o` = operator_ids, `fm` = family_membranes
- `_load_reconstruction_data` decodes when `r` present

### 3. Compact Inventory

- `f` = array of `[path_ref, size]`
- `pt` = path_table_ref

### 4. Compact Path Table

- `p` instead of `paths` when not path_dna
- `load_path_table` returns `data.get("p", data.get("paths", []))`

## Benchmark Comparison (Phase 14B)

Small datasets: small_code, template_heavy, config_heavy, mixed-small.

| Dataset | zstd | gzip | zip | infold_micro | infold_lean | micro vs lean |
|---------|------|------|-----|--------------|-------------|---------------|
| duplicate-heavy-python | 3,119 | 3,340 | 8,188 | **6,133** | 6,578 | -445 |
| template-heavy | 2,701 | 2,819 | 4,209 | **5,130** | 5,556 | -426 |
| config-heavy | 2,733 | 2,856 | 4,713 | **5,089** | 5,519 | -430 |
| mixed-small | 2,679 | 2,797 | 4,446 | **4,772** | 5,173 | -401 |

Micro is ~400–450 bytes smaller than lean on these datasets.

## Overhead Reduction

- **Manifest**: ~150–200 bytes saved (short keys, minimal compatibility)
- **Reconstruction**: ~40–60 bytes saved (positional records, short keys)
- **Inventory**: ~20–30 bytes saved (array format)
- **Path table**: ~5 bytes saved (`p` vs `paths`)
- **Total structural**: ~250–350 bytes per small archive

## Did Micro Improve Again?

Yes. Phase 14B compact encoding reduces micro archive size by ~250–450 bytes vs Phase 14A micro on small datasets.

## Did the Gap to gzip/zstd Narrow?

Slightly. Micro is smaller, but zstd and gzip still win on raw size for small datasets. The gap narrowed by ~250–450 bytes.

## Validation

- Full test suite: 251 passed (including 5 Phase 14B tests)
- Verification sweep: all datasets PASS
- Exact reconstruction: ok
- Strict validation: ok

## Recommendation

Phase 14B compact encoding is active in `--micro` mode. Use `--micro` for tiny archives when size matters. The compact schema is transparent to readers; all archive operations decode automatically.
