# Lessons from Prototype 1

**Freeze tag:** `infold_proto1`

Knowledge baseline before Mk2. Do not rebuild history. Rebuild only what earned survival.

---

## 1. Proven Benchmark Truths

### Where Infold Wins
- **Large structure-heavy projects** (infold-workspace scale): Infold micro reliably beats gzip (3/3 runs, Phase 21C).
- **vs ZIP**: Infold beats ZIP on structure-heavy datasets.
- **Best mode for size**: `micro` (minimal overhead, ruthless selection).
- **Best profile for physical size**: `golem` (12/12 wins in Phase 6B); `dragon` for logical gain.

### Where Infold Loses
- **Small fixtures** (template-heavy, config-heavy, duplicate-heavy): gzip wins. Gain does not generalize.
- **vs zstd**: zstd typically wins on pure size.
- **Tiny archives**: gzip/zstd win; Infold overhead dominates.

### Stability
- Archive size stable across runs (≤3 byte variation).
- Exact reconstruction: preserved in all modes.
- Strict validation: passes when archive is valid.

### Fair Comparison Rules
- Same staged source for all tools (zip, gzip, zstd, Infold).
- Exclude: .git, __pycache__, .venv, node_modules, .tox, dist, build, results.

---

## 2. Confirmed Operator Wins

| Operator | Proven Win | Evidence |
|----------|------------|----------|
| **exact_repetition** | Whole-file duplicates | Core; always beneficial when duplicates exist |
| **template_skeleton** | Repeated scaffold + slots | Main structural win; 9 families typical on workspace |
| **metadata_table_fold** | Path-heavy archives | Replaces repeated paths with refs; net-positive when paths ≥ ~32 |
| **byte_fold** | Repeated opaque, version-like | Chunk reuse on mixed/binary; helps version-like datasets |
| **mutation_chain** | Similar configs/scripts | Template handoff, microscope-assisted; 2 families, ~28 bytes (Phase 20A) |
| **fold_echo** | Weak near-family passthrough | Attaches marginal files to templates; net-positive when structure matches |
| **anchor_file** | Operator guidance, context | 8 anchors, 1366 members; metadata reduction, operator guidance |
| **structural_microscope** | Tiny weak-structure files | 6 assisted matches for template; enables folds below min_lines |

---

## 3. Operator Losses / Marginal

| Operator | Status | Evidence |
|----------|--------|----------|
| **symbol_table** | Often blocked | Conflict with template; metadata-only when it fires |
| **hierarchy_mirror** | Rare activation | Needs repeated subtree depth; narrow target |
| **dependency_motif** | Narrow target | Min 3 deps, 2 occurrences; helps when present |
| **Creature adaptation** | Overhead, no size win | Phase 11: ~1.2 KB overhead when enabled |
| **Tesseract Planner** | Overhead | Adds manifest/report metadata |
| **Tesseract Cooperation** | Hurts size | Phase 10D: 12/12 datasets worse, ~31 KB lost |

---

## 4. Failed Experiments and Why

| Experiment | Outcome | Why |
|------------|---------|-----|
| **Tesseract Cooperation default-on** | Reverted / off by default | Metadata overhead exceeds metadata-table-fold savings; 12/12 datasets hurt |
| **Auto profile for gain** | Switched to physical | Phase 6C: Auto now optimizes for physical; golem wins size |
| **Tiny-archive parity with gzip** | Never achieved | Package overhead dominates on small datasets; zstd/gzip win |
| **Symbol table as content fold** | Blocked by conflict | Competes with template; often reject_conflict |
| **Structure→byte chaining** | Deferred | Phase 10D: remain deferred; not proven safe/beneficial |

---

## 5. Overhead Dominance (Post Phase 21B)

Uncompressed component sizes on primary dataset:
1. **snapshots_passthrough** — largest (file content)
2. **shared_operator_artifacts** — template, mutation, echo, etc.
3. **shared_metadata_tables** — path table
4. **shared_anchors** — anchor metadata

Passthrough is content; operator artifacts and metadata are the compressible overhead.

---

## 6. What Earned Survival

- **Exact reconstruction** — non-negotiable.
- **Strict validation** — required.
- **Deterministic behavior** — no randomness.
- **Micro mode** — size-first; beats gzip on large structure-heavy.
- **Path table / metadata_table_fold** — net-positive on path-heavy archives.
- **Template skeleton** — core structural win.
- **Compact encoding** — cb, sg, pr, _pa, _an; saves bytes.
- **Fair staged comparison** — same source for all tools.

---

## 7. Assumptions to Question in Mk2

- Do we need all 10+ operators? Hierarchy and dependency_motif rarely fire.
- Is symbol_table worth keeping if it's usually blocked?
- Do we need Creature/Tesseract at all for size-first users?
- Is the profile system (sparrow, fox, dragon, golem, serpent) necessary if golem wins everywhere?
- Can we simplify to: micro (size) vs default (analysis) without five profiles?

---

## 8. Rule from Prototype 1

**Do not rebuild history. Rebuild only what earned survival.**

Every promoted subsystem in Mk2 must have a benchmark-proven reason to exist.
