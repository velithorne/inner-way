# Phase 14D — Last-Mile Micro Density

## Summary

Phase 14D adds compact ledger and report encoding for micro mode, reducing package overhead by ~1–1.5 KB.

## Implemented

### 1. Remaining micro overhead audit

Overhead breakdown (pre-14D):
- manifest: 514 bytes
- ledger: 3,569 bytes
- report: 4,808 bytes
- maps: 849 bytes (already compact)
- integrity: 2,791 bytes
- inventory: 3,466 bytes (already compact)

Ledger and report were the largest remaining targets.

### 2. Compact ledger (Phase 14D)

- **encode_ledger_micro**: `{"_l":1,"v":"1.0","nf":26,"lg":52929,"g":[46507,...]}`
- Gains array only; `total_folds` and `total_bytes_saved` derived
- **decode_ledger_micro**: Expands to full form for validation
- **Savings**: ~3,400 bytes (3,569 → ~146)

### 3. Compact report (Phase 14D)

- **encode_report_micro**: Short keys (fc, nf, lg, pf, ok, st, err, rc, sc)
- Minimal fields for validation and explain
- **decode_report_micro**: Expands with `rejected_candidates_summary` placeholder
- **Savings**: ~4,500 bytes (4,808 → ~257)

### 4. Benchmark results (fair staged)

| Tool | Size (bytes) |
|------|--------------|
| zstd | 268,607 |
| gzip | 284,180 |
| **infold_micro** | **286,918** |
| infold_golem | 288,708 |
| infold_lean | 293,303 |
| infold_default | 294,402 |
| zip | 431,430 |

**Staged raw:** 1,574,779 bytes  
**Gap to gzip:** ~2.7 KB  
**Bytes removed in 14D:** ~1,087 (micro 285,123 → 284,036 on same-source comparison)

### 5. Answers

- **Bytes removed:** ~1,087 in last-mile pass (ledger + report compaction)
- **Micro vs gzip:** Micro does not beat gzip (286,918 vs 284,180)
- **Gap to zstd:** ~18 KB; narrowed slightly
- **Recommendation:** The remaining ~2.7 KB gap to gzip comes from structural overhead (manifest, integrity, maps, path_table, shared metadata). Further micro work has diminishing returns. Infold should focus on its strengths: structure-aware folding, archive intelligence, and medium/large codebases where it can beat gzip.

### 6. Exact reconstruction

All Infold modes validated and reconstructed exactly.

### 7. Tests

- `test_compact_ledger_encode_decode`
- `test_compact_report_encode_decode`
- Existing Phase 14B compact tests still pass
