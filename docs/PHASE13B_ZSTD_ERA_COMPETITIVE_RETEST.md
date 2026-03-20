# Phase 13B — zstd-Era Competitive Retest Results

## Summary

Phase 13B reruns Infold's competitive position after small-archive overhead reduction (Phase 13A) and with zstd available. This document provides an updated, honest benchmark story across ZIP, gzip, zstd, and Infold modes.

## 1. Competitive Benchmark Matrix (Re-run)

Tools: zip, gzip, zstd, infold_default, infold_lean, infold_micro, infold_golem_static.

### Size Wins (smallest per dataset)

- **Infold wins**: 0–1 (medium_code when workspace is large and structure-rich)
- **ZIP wins**: 0
- **gzip wins**: 0
- **zstd wins**: 11–12

zstd wins most datasets on raw size. On large structure-heavy codebases (e.g. infold-workspace ~79 MB raw), Infold micro can occasionally beat zstd. Infold's primary value is archive intelligence.

### Best Infold Mode by Category (smallest Infold size)

| Category | Best Infold Mode |
|----------|------------------|
| small_code | infold_micro |
| template_heavy | infold_micro |
| config_heavy | infold_micro |
| mixed | infold_micro |
| medium_code | infold_micro |
| opaque_heavy | infold_micro |
| version_like | infold_micro |

Micro is the smallest Infold mode across all categories.

---

## 2. Focused Category Comparison

### small_code (duplicate-heavy-python, hierarchy-mirror, dependency-motif)

| Tool | Size (bytes) | Ratio |
|------|--------------|-------|
| zstd | 2,746–3,115 | 0.16–0.30 |
| gzip | 2,867–3,338 | 0.17–0.33 |
| zip | 4,192–8,189 | 0.41–0.58 |
| infold_micro | 5,474–6,342 | 0.29–0.32 |
| infold_lean | 5,434–6,542 | 0.33–0.33 |
| infold_default | 6,978–8,051 | 0.41–0.73 |

**Verdict**: zstd wins. Micro improves small-archive competitiveness vs lean/default.

### template_heavy (template-heavy, template-stress)

| Tool | Size (bytes) | Ratio |
|------|--------------|-------|
| zstd | 508–2,699 | 0.26–0.30 |
| gzip | 676–2,827 | 0.35–0.32 |
| zip | 4,216–7,002 | 0.47–3.61 |
| infold_micro | 2,788–5,352 | 0.60–1.44 |
| infold_lean | 2,969–5,538 | 0.62–1.53 |
| infold_default | 4,434–6,981 | 0.78–2.29 |

**Verdict**: zstd wins. Micro is best Infold mode.

### config_heavy

| Tool | Size (bytes) | Ratio |
|------|--------------|-------|
| zstd | 2,730 | 0.31 |
| gzip | 2,856 | 0.32 |
| zip | 4,712 | 0.53 |
| infold_micro | 5,295 | 0.60 |
| infold_lean | 5,493 | 0.62 |
| infold_default | 6,861 | 0.77 |

**Verdict**: zstd wins.

### mixed_small (mixed-small)

| Tool | Size (bytes) | Ratio |
|------|--------------|-------|
| zstd | 2,671 | 0.33 |
| gzip | 2,793 | 0.35 |
| zip | 4,443 | 0.55 |
| infold_micro | 4,960 | 0.62 |
| infold_lean | 5,145 | 0.64 |
| infold_default | 6,582 | 0.82 |

**Verdict**: zstd wins. Micro is closest Infold mode to zip.

### medium_code (infold-workspace)

Results vary with workspace size. On ~31 MB raw: zstd wins. On ~79 MB raw (structure-heavy): Infold micro can beat zstd.

| Tool | Size (bytes) ~31 MB raw | Size (bytes) ~79 MB raw |
|------|-------------------------|--------------------------|
| zstd | 7,235,662 | 22,391,943 |
| gzip | 17,588,517 | 52,018,071 |
| zip | 19,599,734 | 56,323,328 |
| infold_micro | 8,074,892 | **22,344,105** |
| infold_lean | 8,076,982 | 22,346,472 |
| infold_default | 8,107,351 | 22,400,602 |

**Verdict**: On large structure-heavy codebases, Infold micro can beat zstd. Infold beats ZIP and gzip in both cases.

### opaque_heavy (byte-fold-opaque)

| Tool | Size (bytes) | Ratio |
|------|--------------|-------|
| zstd | 770 | 0.11 |
| gzip | 866 | 0.12 |
| zip | 2,727 | 0.38 |
| infold_micro | 3,328 | 0.47 |
| infold_lean | 3,513 | 0.50 |
| infold_default | 4,917 | 0.69 |

**Verdict**: zstd wins. Opaque content favors byte compressors.

### version_like (duplicate-stress, byte-fold-version-like, byte-fold-large-text)

| Tool | Size (bytes) | Ratio |
|------|--------------|-------|
| zstd | 292–699 | 0.27–0.28 |
| gzip | 343–747 | 0.29–0.33 |
| zip | 1,018–2,118 | 0.82–0.97 |
| infold_micro | 2,504–2,906 | 0.28–1.11 |
| infold_lean | 2,691–3,098 | 0.29–1.18 |
| infold_default | 3,992–4,681 | 0.44–1.69 |

**Verdict**: zstd wins. Tiny version-like datasets have high Infold overhead.

---

## 3. Real-Project Retest (Current Codebase)

Source: `.` (infold-workspace). Raw: 31,134,026 bytes.

| Mode | Size (bytes) | Ratio | Exact Reconstruction | Validation |
|------|--------------|-------|------------------------|------------|
| zip | 19,599,734 | 0.63 | yes | n/a |
| gzip | 17,588,517 | 0.56 | yes | n/a |
| zstd | 7,235,662 | 0.23 | yes | n/a |
| infold_default | 8,107,351 | 0.26 | yes | ok |
| infold_lean | 8,076,982 | 0.26 | yes | ok |
| infold_micro | 8,074,892 | 0.26 | yes | ok |
| infold_golem_static | 8,106,190 | 0.26 | yes | ok |

**Best Infold mode by size**: `--micro` (8,074,892 bytes)

**Best Infold mode by practical value**: `--micro` or `--lean` — nearly identical size, lean retains slightly more metadata. For structure-heavy codebases, default or golem offer richer analysis at ~30 KB overhead.

---

## 4. Best-Mode Guidance by Project Class

| Project Class | Best Infold Mode | Rationale |
|---------------|------------------|-----------|
| **Tiny archive** (&lt; 50 KB raw) | `--micro` | Minimizes overhead; zstd still wins on size |
| **Structure-heavy codebase** | `--lean` or default | Good fold yield; lean reduces overhead, default adds analysis |
| **Opaque-heavy content** | `--micro` or `--lean` | Byte Fold helps; minimize metadata overhead |
| **Version-like content** | `--micro` | Small archives; overhead dominates |
| **Analysis-rich usage** | default | Full creature, Tesseract, explain, lineage |

---

## 5. Competitive Summary Refresh

### Where Infold beats ZIP/gzip/zstd

- **Medium structure-heavy codebases**: Infold beats ZIP and gzip. On large codebases (~79 MB raw), Infold micro can beat zstd.
- **Archive intelligence**: Search, lineage, explain, structure-aware, metadata-aware — Infold only.

### Where zstd or gzip still win

- **All 12 datasets**: zstd has smallest size.
- **Small datasets**: gzip and zstd dominate; Infold overhead is significant.
- **Opaque/binary content**: zstd and gzip excel; Infold Byte Fold helps but overhead remains.

### Did micro mode improve small-archive competitiveness?

Yes. Micro is ~180–200 bytes smaller than lean on small datasets. It narrows the gap to zip but does not beat zstd or gzip on raw size.

### Best Infold mode for tiny archives

`--micro`

### Best Infold mode for medium/structure-heavy archives

`--micro` or `--lean` — nearly identical. Use `--lean` if slightly richer metadata is acceptable.

### Infold's honest current lane

- **Structure-aware folding** for code/config projects
- **Archive intelligence**: searchable, lineage-aware, explainable
- **Competitive on medium_code** vs ZIP/gzip (beats both); can beat zstd on large structure-heavy codebases
- **zstd wins** on small datasets and opaque content
- **Use case**: When archive intelligence matters, or when structure folding yields better ratio than byte compression on large codebases

---

## 6. Validation

- Full test suite: 239 passed
- Verification sweep: all datasets PASS
- Competitive matrix: exported to results/phase13b_competitive/
- Real-project: exact reconstruction ok, strict validation ok
