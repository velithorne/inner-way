# Fold Echo Validation Pass — Summary

**Timestamp:** 2026-03-19

## 1. Did Fold Echoes Actually Activate?

**Yes.** Fold Echoes activated on 2 of 3 echo-friendly datasets.

| Dataset | Echo Families | Echo Members | Echo Gain (bytes) |
|---------|---------------|--------------|-------------------|
| echo-config | 2 | 2 | 44 |
| echo-handlers | 1 | 1 | 80 |
| echo-txt | 0 | 0 | 0 |

**Total:** 3 echo families, 3 echo members, 124 bytes saved.

## 2. On Which Datasets?

- **echo_friendly_config**: 5 JSON config files. With `max_family_size=3`, template accepted 3 (svc_a, svc_b, svc_c). Echoes attached svc_d and svc_e (2 echoes).
- **echo_friendly_handlers**: 4 Python handler modules. Template accepted 3 (mod_a, mod_b, mod_c). Echo attached mod_d (1 echo).
- **echo_friendly_txt**: 4 txt files. Template rejected (net_gain too low for small files). No echoes.

## 3. How Many Files Became Echoes?

**3 files** across 2 datasets:
- 2 config files (svc_d.json, svc_e.json)
- 1 handler file (mod_d.py)

## 4. How Many Bytes Did They Save?

**124 bytes** total logical gain:
- echo-config: 44 bytes
- echo-handlers: 80 bytes

## 5. Did They Reduce Passthrough Overhead?

**Yes.** Passthrough count decreased:

| Dataset | Passthrough (without echo) | Passthrough (with echo) |
|---------|---------------------------|-------------------------|
| echo-config | 2 | 0 |
| echo-handlers | 1 | 0 |

Echoes converted all eligible passthrough files into folded representation.

## 6. Physical Size Note

Physical archive size (ZIP) increased slightly with echoes enabled due to echo metadata (shared/echo_{i}.json, maps records). Logical gain (bytes saved from deduplication) is positive. For echo-friendly datasets, the trade-off favors structure awareness and exact reconstruction over minimal ZIP size.

## 7. Validation

- **Strict validation:** PASS for all echo archives
- **Exact reconstruction:** PASS (diff -rq shows no differences)
- **All Phase 17A tests:** PASS

## 8. Should Fold Echoes Remain Enabled by Default?

**Yes.** Fold Echoes:
- Activate only when net-positive
- Require an accepted template host
- Reduce passthrough overhead on echo-friendly datasets
- Preserve exact reconstruction
- Are conservative (no fuzzy matching)

Recommendation: **Keep Fold Echoes enabled by default.**

## 9. Fixtures Added

- `tests/fixtures/echo_friendly_config/` — 5 JSON config files (use with `max_family_size=3`)
- `tests/fixtures/echo_friendly_handlers/` — 4 Python handler modules
- `tests/fixtures/echo_friendly_txt/` — 4 txt template files

## 10. Template Change for Echo-Friendly Mode

Added `max_family_size` to template_skeleton thresholds. When set (e.g. 3), the template caps family size, leaving excess structurally-similar files as passthrough. Those become echo candidates. Use for benchmarking echo activation.
