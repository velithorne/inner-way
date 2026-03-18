# Phase 15 — Large Structure-Heavy Dominance

## Summary

Phase 15 strengthens Infold's performance on medium and large structure-heavy codebases, where Infold has the best chance to outperform traditional compressors.

## Implemented

### 1. Large-project opportunity audit

- **`audit_large_project_opportunities()`**: Audits medium_code, template_heavy, version_like datasets
- Returns operator breakdown, logical gain, top targets
- **`run_large_project_comparison()`**: Competitive comparison on structure-heavy datasets only
- **`infold_wins_report.md`**: Showcase report of where Infold beats zip/gzip/zstd

### 2. Structural fold improvements (Dragon profile)

- **Template skeleton**: `min_lines: 4` for dragon (was 5) — catches more config-like template families
- **Hierarchy mirror**: `min_structure_similarity: 0.82` for dragon (was 0.85) — more repeated layouts
- Use `--profile dragon` for large structure-rich projects when maximum logical gain is desired

### 3. Large-project metadata efficiency

- **Metadata table fold**: When path count ≥ 150, `min_net_gain_bytes` lowered to 24 — path table more often beneficial at scale
- Path DNA and Family Membranes already help; scale-aware threshold improves large-archive efficiency

### 4. Best-mode guidance for large projects

| Project class | Recommended mode | Reason |
|---------------|------------------|--------|
| **Medium codebase** | `--profile golem` or `--lean` | Best physical size; structure-heavy wins |
| **Large codebase** | `--profile golem` or `--lean` | Infold can beat zstd on large structure-heavy |
| **Template-heavy** | `--profile dragon` | More template families with min_lines 4 |
| **Config-heavy** | `--profile golem` | Balanced; metadata fold helps |
| **Analysis-rich** | default (auto) | Full creature, Tesseract, explainability |

### 5. Infold's competitive lane

- **Infold wins**: Medium/large structure-heavy codebases (e.g. infold-workspace ~79 MB)
- **zstd wins**: Small archives, opaque-heavy, most tiny datasets
- **Infold value**: Archive intelligence (search, sync, lineage, explain), structure-aware folding, exact reconstruction

### 6. CLI

```bash
# Large-project focused comparison (medium_code, template_heavy only)
python3 -m infold.cli --large-project-comparison --large-project-output results/large_project

# Dragon profile for maximum template/hierarchy detection
python3 -m infold.cli archive create --source . --output out.infold --profile dragon
```

### 7. Tests

- `test_phase15_large_structure.py`: Dragon profile, audit, comparison
