"""
Phase 15: Large Structure-Heavy Project Opportunity Audit.

Identifies highest-value targets for medium/large codebases:
- template-heavy patterns
- repeated folder/layout structures
- repeated dependency motifs
- path-heavy metadata
- where Infold wins or nearly wins
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from infold.benchmark.pack import get_benchmark_datasets
from infold.benchmark.competitive_matrix import (
    run_competitive_matrix,
    get_matrix_category,
)
from infold.archive import create_archive, validate_archive, explain_archive
from infold.cli import load_config


def audit_large_project_opportunities(
    base_path: Path,
    config: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """
    Audit benchmark datasets to identify large-project opportunities.
    Returns summary of best targets, operator contributions, and competitive position.
    """
    cfg = config or load_config()
    datasets = get_benchmark_datasets(base_path)

    # Filter to medium/large structure-heavy: infold-workspace, template-stress, duplicate-stress
    structure_heavy = [
        (p, did, cat)
        for p, did, cat in datasets
        if get_matrix_category(did, cat) in ("medium_code", "template_heavy", "version_like")
        and p.exists()
    ]

    results: list[dict[str, Any]] = []
    for path, dataset_id, pack_category in structure_heavy:
        raw = _raw_size(path, cfg.get("project", {}).get("exclude_patterns", []))
        if raw == 0:
            continue
        matrix_cat = get_matrix_category(dataset_id, pack_category)

        # Create Infold archive and extract operator breakdown
        import tempfile
        with tempfile.TemporaryDirectory(prefix="infold_audit_") as tmp:
            arc = Path(tmp) / "audit.infold"
            try:
                create_archive(path, arc, cfg, profile="auto")
                ok, _ = validate_archive(arc, mode="strict")
                size = arc.stat().st_size
                info = _explain_archive(arc)
            except Exception as e:
                size = None
                info = {}
                ok = False

        results.append({
            "dataset_id": dataset_id,
            "category": pack_category,
            "matrix_category": matrix_cat,
            "raw_bytes": raw,
            "infold_size": size,
            "validation_ok": ok,
            "fold_count": info.get("fold_count", 0),
            "logical_gain": info.get("logical_gain_bytes", 0),
            "operator_breakdown": info.get("fold_counts_by_operator", {}),
            "gain_by_operator": info.get("gain_by_operator", {}),
        })

    # Build opportunity summary
    medium = [r for r in results if r.get("matrix_category") == "medium_code"]
    template = [r for r in results if r.get("matrix_category") == "template_heavy"]
    version = [r for r in results if r.get("matrix_category") == "version_like"]

    return {
        "audit_results": results,
        "medium_code": medium,
        "template_heavy": template,
        "version_like": version,
        "summary": _build_opportunity_summary(results),
    }


def _raw_size(path: Path, excludes: list[str]) -> int:
    """Compute raw size with exclusions."""
    import fnmatch
    total = 0
    excludes = excludes or []
    for f in path.rglob("*"):
        if not f.is_file():
            continue
        rel = str(f.relative_to(path)).replace("\\", "/")
        skip = False
        for ex in excludes:
            if not ex:
                continue
            if "*" in ex and fnmatch.fnmatch(f.name, ex):
                skip = True
                break
            if ex in rel or f"/{ex}" in rel or rel.startswith(ex):
                skip = True
                break
        if skip:
            continue
        try:
            total += f.stat().st_size
        except OSError:
            pass
    return total


def _explain_archive(arc: Path) -> dict[str, Any]:
    """Extract explain info from archive via explain_archive."""
    try:
        info = explain_archive(arc)
        return {
            "fold_count": info.get("package_summary", {}).get("fold_count", 0),
            "logical_gain_bytes": info.get("package_summary", {}).get("logical_gain_bytes", 0),
            "fold_counts_by_operator": info.get("fold_counts_by_operator", {}),
            "gain_by_operator": info.get("gain_by_operator", {}),
        }
    except Exception:
        return {}


def _build_opportunity_summary(results: list[dict[str, Any]]) -> dict[str, Any]:
    """Build concise opportunity summary."""
    if not results:
        return {"targets": [], "best_categories": [], "operator_priorities": []}

    # Top datasets by logical gain
    by_gain = sorted(results, key=lambda r: r.get("logical_gain", 0), reverse=True)
    top_gain = by_gain[:5] if by_gain else []

    # Operator contribution across all
    op_totals: dict[str, int] = {}
    op_gain: dict[str, int] = {}
    for r in results:
        for op, cnt in r.get("operator_breakdown", {}).items():
            op_totals[op] = op_totals.get(op, 0) + cnt
        for op, g in r.get("gain_by_operator", {}).items():
            op_gain[op] = op_gain.get(op, 0) + g

    # Best categories (where Infold has most folds)
    by_folds = sorted(results, key=lambda r: r.get("fold_count", 0), reverse=True)
    best_cats = list({r.get("matrix_category") for r in by_folds[:5] if r.get("fold_count", 0) > 0})

    return {
        "top_datasets_by_gain": [
            {"dataset_id": r["dataset_id"], "logical_gain": r.get("logical_gain", 0), "raw_bytes": r.get("raw_bytes", 0)}
            for r in top_gain
        ],
        "operator_totals": op_totals,
        "operator_gain_totals": op_gain,
        "best_categories": best_cats,
        "total_datasets_audited": len(results),
    }


def run_large_project_comparison(
    base_path: Path,
    config: dict[str, Any] | None = None,
    output_dir: Path | None = None,
) -> dict[str, Any]:
    """
    Run competitive comparison on medium/large structure-heavy datasets only.
    Returns matrix and summary. Optionally writes to output_dir.
    """
    cfg = config or load_config()
    datasets = get_benchmark_datasets(base_path)
    structure_heavy = [
        (p, did, cat)
        for p, did, cat in datasets
        if get_matrix_category(did, cat) in ("medium_code", "template_heavy")
        and p.exists()
    ]
    if not structure_heavy:
        return {"matrix": [], "summary": {}, "infold_wins": []}

    matrix = run_competitive_matrix(structure_heavy, cfg, base_path)
    rows = matrix.get("matrix", [])

    # Build Infold wins report
    by_dataset: dict[str, list[dict]] = {}
    for r in rows:
        did = r.get("dataset_id", "")
        by_dataset.setdefault(did, []).append(r)

    infold_wins: list[dict[str, Any]] = []
    for did, tools in by_dataset.items():
        raw = tools[0].get("raw_bytes", 0) if tools else 0
        infold_sizes = [r for r in tools if r.get("tool", "").startswith("infold_")]
        zip_sizes = [r for r in tools if r.get("tool") in ("zip", "gzip", "zstd")]
        best_infold = min((r for r in infold_sizes if r.get("compressed_size_bytes")), key=lambda x: x.get("compressed_size_bytes", 999999999)) if infold_sizes else None
        best_baseline = min((r for r in zip_sizes if r.get("compressed_size_bytes")), key=lambda x: x.get("compressed_size_bytes", 999999999)) if zip_sizes else None
        if best_infold and best_baseline:
            infold_size = best_infold.get("compressed_size_bytes", 0)
            baseline_size = best_baseline.get("compressed_size_bytes", 0)
            if infold_size and baseline_size and infold_size < baseline_size:
                infold_wins.append({
                    "dataset_id": did,
                    "raw_bytes": raw,
                    "best_infold": best_infold.get("tool"),
                    "infold_size": infold_size,
                    "beaten_tool": best_baseline.get("tool"),
                    "beaten_size": baseline_size,
                })

    summary = {
        "datasets_compared": len(by_dataset),
        "infold_wins_count": len(infold_wins),
        "infold_wins": infold_wins,
    }

    if output_dir:
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        (output_dir / "large_project_matrix.json").write_text(json.dumps(matrix, indent=2), encoding="utf-8")
        (output_dir / "large_project_summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
        (output_dir / "infold_wins_report.md").write_text(
            _format_infold_wins_report(summary, matrix),
            encoding="utf-8",
        )

    return {"matrix": matrix, "summary": summary, "infold_wins": infold_wins}


def _format_infold_wins_report(summary: dict[str, Any], matrix: dict[str, Any]) -> str:
    """Format Infold wins showcase report."""
    lines = [
        "# Infold Wins — Large Structure-Heavy Projects",
        "",
        "## Summary",
        "",
        f"Datasets compared: {summary.get('datasets_compared', 0)}",
        f"Infold wins (smallest): {summary.get('infold_wins_count', 0)}",
        "",
    ]
    wins = summary.get("infold_wins", [])
    if wins:
        lines.extend([
            "## Where Infold Beats zip/gzip/zstd",
            "",
            "| Dataset | Raw | Best Infold | Infold Size | Beaten |",
            "|---------|-----|-------------|-------------|--------|",
        ])
        for w in wins:
            lines.append(f"| {w['dataset_id']} | {w.get('raw_bytes', 0):,} | {w.get('best_infold', '?')} | {w.get('infold_size', 0):,} | {w.get('beaten_tool', '?')} ({w.get('beaten_size', 0):,}) |")
        lines.extend(["", "## Why Infold Wins Here", "", "- **Structure-aware folding**: Repeated templates, hierarchies, dependency motifs", "- **Metadata reuse**: Path tables, path DNA reduce overhead at scale", "- **Exact reconstruction**: Byte-for-byte recovery with searchable lineage", ""])
    else:
        lines.append("Infold did not beat zip/gzip/zstd on any compared dataset in this run.")
        lines.append("Best mode for large structure-heavy: `--profile golem` or `--lean`.")
    return "\n".join(lines)
