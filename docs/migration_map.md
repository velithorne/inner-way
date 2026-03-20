# Migration Map: Prototype 1 → Mk2

**Freeze tag:** `infold_proto1`

Matrix: **promote** | **rewrite clean** | **move to lab** | **discard**

---

## Operators

| Subsystem | Decision | Rationale |
|-----------|----------|-----------|
| exact_repetition | **promote** | Core win; whole-file duplicates always beneficial |
| template_skeleton | **promote** | Main structural win; proven on large projects |
| mutation_chain | **promote** | Template handoff, microscope-assisted; net-positive |
| fold_echo | **promote** | Recovers marginal passthrough; net-positive |
| byte_fold | **promote** | Helps version-like, opaque; chunk reuse proven |
| metadata_table_fold | **promote** | Path table saves bytes on path-heavy archives |
| anchor_file | **promote** | Operator guidance, context; reduces repeated metadata |
| structural_microscope | **promote** | Enables template on tiny files; 6 assisted matches |
| symbol_table | **move to lab** | Often blocked; conflict with template; re-evaluate scope |
| hierarchy_mirror | **move to lab** | Rare activation; narrow target; needs evidence |
| dependency_motif | **move to lab** | Narrow target; keep if dependency-heavy use case proven |

---

## Strategy Layers

| Subsystem | Decision | Rationale |
|-----------|----------|-----------|
| micro mode | **promote** | Size-first; beats gzip on large structure-heavy |
| lean mode | **promote** | Compact, no creature/Tesseract; size improvement |
| compact encoding (cb, sg, pr, _pa, _an) | **promote** | Proven byte savings |
| golem profile | **promote** | Wins physical size 12/12; size-first default |
| dragon profile | **rewrite clean** | Logical gain winner; simplify or merge with default |
| sparrow, fox, serpent | **discard** or **move to lab** | Auto picks golem; extra profiles add complexity |
| Creature adaptation | **discard** | Overhead, no size win; Phase 11 evidence |
| Tesseract Planner | **discard** | Overhead; no physical benefit |
| Tesseract Cooperation | **discard** | Hurts 12/12 datasets; Phase 10D |
| Planner v2 | **rewrite clean** | Keep scoring/decisions; simplify weights |
| Interaction policy | **promote** | Prevents double-fold; conflict resolution |

---

## Package / Archive

| Subsystem | Decision | Rationale |
|-----------|----------|-----------|
| manifest.json | **promote** | Required; compact micro encoding |
| ledger.json | **promote** | Required; compact gains-only in micro |
| maps/reconstruction.json | **promote** | Core; path_refs, family_membranes |
| shared/ (operator artifacts) | **promote** | Template, mutation, echo, exact, etc. |
| path_table | **promote** | Metadata table fold; net-positive |
| integrity.json | **promote** | Validation; checksums |
| reports/report.json | **rewrite clean** | Keep minimal; omit rich diagnostics in micro |
| snapshots/passthrough | **promote** | Required for reconstruction; array format in micro |
| anchors.json | **promote** | Compact format; operator guidance |

---

## CLI / Workflows

| Subsystem | Decision | Rationale |
|-----------|----------|-----------|
| archive create | **promote** | Core workflow |
| archive reconstruct | **promote** | Required |
| archive validate | **promote** | Required |
| archive explain | **promote** | Useful; keep compact in micro |
| archive inspect, stats, list | **promote** | Introspection |
| archive compare | **promote** | Useful |
| archive search | **promote** | Archive intelligence |
| --micro, --lean | **promote** | Size-first flags |
| --no-creature, --no-tesseract | **discard** | Remove layers entirely in Mk2 |
| sync (lineage) | **move to lab** | Keep if lineage use case proven |
| benchmark campaign | **promote** | Proven comparison framework |
| fair staged comparison | **promote** | Same source for all tools |

---

## Parsers / Intake

| Subsystem | Decision | Rationale |
|-----------|----------|-----------|
| Python parser | **promote** | Core |
| JSON, YAML, text | **promote** | Config/code |
| JS/TS, Markdown | **promote** | Broader coverage |
| File routing (structural_first, chunk_first) | **promote** | Byte fold targeting |

---

## Exit Criteria

For every **promoted** subsystem in Mk2:

1. **Why it deserves to exist**: Benchmark-proven win or required for correctness.
2. **Evidence**: Phase doc or benchmark result.
3. **No carry-forward of unproven assumptions**.

---

## Summary

| Category | Promote | Rewrite Clean | Move to Lab | Discard |
|----------|---------|---------------|-------------|---------|
| Operators | 8 | 0 | 3 | 0 |
| Strategy | 4 | 2 | 0 | 5 |
| Package | 8 | 1 | 0 | 0 |
| CLI | 10 | 0 | 1 | 2 |
| Parsers | 5 | 0 | 0 | 0 |

**Mk2 principle:** Rebuild only what earned survival.
