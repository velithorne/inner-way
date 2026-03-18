"""
Phase 10D: Tesseract Evaluation and Tuning.

Compare baseline, Tesseract Planner-only, and Planner+Cooperation modes.
Threshold tuning experiments and cooperation win reporting.
"""

from __future__ import annotations

import csv
import json
from pathlib import Path
from typing import Any

# Tesseract evaluation modes
MODE_BASELINE = "baseline"
MODE_PLANNER_ONLY = "planner_only"
MODE_PLANNER_COOPERATION = "planner_cooperation"

# Dataset category for evaluation (maps to profile_comparison categories + tesseract-specific)
TESSERACT_DATASET_CATEGORY: dict[str, str] = {
    "template-heavy": "structure_heavy",
    "config-heavy": "metadata_heavy",
    "duplicate-heavy-python": "structure_heavy",
    "hierarchy-mirror": "structure_heavy",
    "dependency-motif": "structure_heavy",
    "mixed-small": "structure_heavy",
    "infold-workspace": "realistic_large",
    "byte-fold-opaque": "byte_heavy",
    "byte-fold-large-text": "byte_heavy",
    "byte-fold-version-like": "version_like",
    "duplicate-stress": "version_like",
    "template-stress": "version_like",
}


def get_tesseract_category(dataset_id: str) -> str:
    """Return Tesseract evaluation category for dataset."""
    return TESSERACT_DATASET_CATEGORY.get(dataset_id, "balanced")


def _config_for_mode(mode: str, base_config: dict[str, Any]) -> dict[str, Any]:
    """Build config for given mode."""
    cfg = dict(base_config)
    if mode == MODE_BASELINE:
        cfg["_tesseract_planner"] = False
        cfg["_tesseract_cooperation"] = False
    elif mode == MODE_PLANNER_ONLY:
        cfg["_tesseract_planner"] = True
        cfg["_tesseract_cooperation"] = False
    else:
        cfg["_tesseract_planner"] = True
        cfg["_tesseract_cooperation"] = True
    cfg["_fold_profile"] = "fox"
    return cfg


def run_tesseract_comparison(
    datasets: list[tuple[Path, str, str]],
    config: dict[str, Any],
    base_path: Path,
    create_archives: bool = True,
) -> dict[str, Any]:
    """
    Run benchmark across baseline, planner_only, planner_cooperation.
    Returns comparison with per-dataset per-mode metrics.
    """
    from infold.benchmark.campaign import run_benchmark_campaign

    modes = [MODE_BASELINE, MODE_PLANNER_ONLY, MODE_PLANNER_COOPERATION]
    all_results: dict[str, dict[str, Any]] = {}

    for mode in modes:
        cfg = _config_for_mode(mode, config)
        cfg["_benchmark_profile"] = "fox"
        results = run_benchmark_campaign(datasets, cfg, base_path, create_archives=create_archives)
        for r in results:
            did = r.get("dataset_id", "")
            if did not in all_results:
                all_results[did] = {
                    "dataset_id": did,
                    "category": r.get("category", ""),
                    "tesseract_category": get_tesseract_category(did),
                    "path": r.get("path", ""),
                    "raw_bytes": r.get("raw_bytes", 0),
                    "zip_bytes": r.get("zip_bytes", 0),
                    "gzip_bytes": r.get("gzip_bytes", 0),
                    "by_mode": {},
                }
            pfi = r.get("_tesseract_planner_info") if mode != MODE_BASELINE else None
            ep = r.get("_tesseract_execution_plan") if mode != MODE_BASELINE else None
            all_results[did]["by_mode"][mode] = {
                "archive_size_bytes": r.get("archive_size_bytes"),
                "infold_physical_folded_size": r.get("infold_physical_folded_size", 0),
                "infold_logical_gain": r.get("infold_logical_gain", 0),
                "fold_count": r.get("fold_count", 0),
                "fold_count_by_operator": r.get("fold_count_by_operator", {}),
                "metadata_table_fold_net": r.get("metadata_table_fold_net_bytes"),
                "package_overhead": r.get("package_overhead"),
                "exact_reconstruction_status": r.get("exact_reconstruction_status", ""),
                "archive_validate_status": r.get("archive_validate_status", ""),
                "integrity_status": r.get("integrity_status", ""),
                "dominant_dimension": pfi.get("dominant_dimension") if pfi else None,
                "secondary_dimension": pfi.get("secondary_dimension") if pfi else None,
                "cooperation_mode": ep.get("cooperation_mode") if ep else None,
                "execution_steps": ep.get("execution_steps") if ep else None,
            }

    return {
        "datasets": list(all_results.values()),
        "modes": modes,
    }


def compute_cooperation_wins(comparison: dict[str, Any]) -> dict[str, Any]:
    """Compute cooperation win summary."""
    datasets = comparison.get("datasets", [])
    wins = 0
    losses = 0
    neutral = 0
    total_saved = 0
    total_lost = 0
    by_category: dict[str, dict[str, int]] = {}

    for ds in datasets:
        cat = ds.get("tesseract_category", "balanced")
        if cat not in by_category:
            by_category[cat] = {"wins": 0, "losses": 0, "neutral": 0, "saved": 0, "lost": 0}
        by_mode = ds.get("by_mode", {})
        base = by_mode.get(MODE_BASELINE, {})
        coop = by_mode.get(MODE_PLANNER_COOPERATION, {})
        base_size = base.get("archive_size_bytes") or base.get("infold_physical_folded_size", 0)
        coop_size = coop.get("archive_size_bytes") or coop.get("infold_physical_folded_size", 0)
        if not base_size and not coop_size:
            neutral += 1
            by_category[cat]["neutral"] += 1
            continue
        diff = (coop_size or 0) - (base_size or 0)
        if diff < 0:
            wins += 1
            total_saved += abs(diff)
            by_category[cat]["wins"] += 1
            by_category[cat]["saved"] += abs(diff)
        elif diff > 0:
            losses += 1
            total_lost += diff
            by_category[cat]["losses"] += 1
            by_category[cat]["lost"] += diff
        else:
            neutral += 1
            by_category[cat]["neutral"] += 1

    return {
        "cooperation_improved_count": wins,
        "cooperation_hurt_count": losses,
        "cooperation_neutral_count": neutral,
        "total_bytes_saved_by_cooperation": total_saved,
        "total_bytes_lost_by_cooperation": total_lost,
        "by_category": by_category,
    }


def run_threshold_tuning(
    datasets: list[tuple[Path, str, str]],
    config: dict[str, Any],
    base_path: Path,
    *,
    metadata_strength_values: tuple[float, ...] = (0.20, 0.25, 0.30),
    metadata_lower_by_values: tuple[int, ...] = (4, 8, 12),
    compactness_bias_values: tuple[float, ...] = (0.02, 0.03, 0.05),
) -> dict[str, Any]:
    """
    Run conservative threshold tuning experiments.
    Returns tuning results per parameter.
    """
    from infold.benchmark.campaign import run_benchmark_campaign

    results: list[dict[str, Any]] = []
    cfg_base = dict(config)
    cfg_base["_fold_profile"] = "fox"
    cfg_base["_tesseract_planner"] = True
    cfg_base["_tesseract_cooperation"] = True

    for meta_cutoff in metadata_strength_values:
        cfg = {**cfg_base, "_tesseract_metadata_strength_cutoff": meta_cutoff}
        campaign = run_benchmark_campaign(datasets, cfg, base_path, create_archives=True)
        total_phys = sum(r.get("archive_size_bytes") or r.get("infold_physical_folded_size", 0) for r in campaign)
        results.append({
            "param": "metadata_strength_cutoff",
            "value": meta_cutoff,
            "total_archive_size": total_phys,
            "dataset_count": len(campaign),
        })

    for lower_by in metadata_lower_by_values:
        cfg = {**cfg_base, "_tesseract_metadata_threshold_lower_by": lower_by}
        campaign = run_benchmark_campaign(datasets, cfg, base_path, create_archives=True)
        total_phys = sum(r.get("archive_size_bytes") or r.get("infold_physical_folded_size", 0) for r in campaign)
        results.append({
            "param": "metadata_threshold_lower_by",
            "value": lower_by,
            "total_archive_size": total_phys,
            "dataset_count": len(campaign),
        })

    for bias in compactness_bias_values:
        cfg = {**cfg_base, "_tesseract_compactness_bias_increment": bias}
        campaign = run_benchmark_campaign(datasets, cfg, base_path, create_archives=True)
        total_phys = sum(r.get("archive_size_bytes") or r.get("infold_physical_folded_size", 0) for r in campaign)
        results.append({
            "param": "compactness_bias_increment",
            "value": bias,
            "total_archive_size": total_phys,
            "dataset_count": len(campaign),
        })

    return {"tuning_results": results}


def build_recommendation(
    comparison: dict[str, Any],
    cooperation_wins: dict[str, Any],
    tuning: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Build recommendation summary."""
    wins = cooperation_wins.get("cooperation_improved_count", 0)
    losses = cooperation_wins.get("cooperation_hurt_count", 0)
    saved = cooperation_wins.get("total_bytes_saved_by_cooperation", 0)
    lost = cooperation_wins.get("total_bytes_lost_by_cooperation", 0)

    structural_worthwhile = wins > losses or saved > lost
    byte_worthwhile = wins > 0 or saved > 0
    thresholds_ok = True
    if tuning:
        tr = tuning.get("tuning_results", [])
        if tr:
            best_by_param: dict[str, tuple[Any, int]] = {}
            for r in tr:
                p = r["param"]
                v = r["value"]
                s = r["total_archive_size"]
                if p not in best_by_param or s < best_by_param[p][1]:
                    best_by_param[p] = (v, s)
            thresholds_ok = all(
                best_by_param.get(p, (None, 0))[0] in (0.25, 8, 0.03)
                for p in ("metadata_strength_cutoff", "metadata_threshold_lower_by", "compactness_bias_increment")
                if any(r["param"] == p for r in tr)
            )

    return {
        "structural_metadata_cooperation_worthwhile": structural_worthwhile,
        "byte_metadata_cooperation_worthwhile": byte_worthwhile,
        "current_thresholds_adequate": thresholds_ok,
        "structure_byte_should_remain_deferred": True,
        "cooperation_should_remain_enabled_by_default": wins >= losses and saved >= lost,
        "summary": {
            "cooperation_wins": wins,
            "cooperation_losses": losses,
            "bytes_saved": saved,
            "bytes_lost": lost,
        },
    }


def export_evaluation_csv(comparison: dict[str, Any], out_path: Path) -> None:
    """Export comparison to CSV (one row per dataset per mode)."""
    rows = []
    for ds in comparison.get("datasets", []):
        did = ds.get("dataset_id", "")
        cat = ds.get("tesseract_category", "")
        for mode, m in ds.get("by_mode", {}).items():
            rows.append({
                "dataset_id": did,
                "tesseract_category": cat,
                "mode": mode,
                "archive_size_bytes": m.get("archive_size_bytes"),
                "infold_physical_folded_size": m.get("infold_physical_folded_size", 0),
                "infold_logical_gain": m.get("infold_logical_gain", 0),
                "fold_count": m.get("fold_count", 0),
                "cooperation_mode": m.get("cooperation_mode"),
                "exact_reconstruction": m.get("exact_reconstruction_status"),
            })
    if not rows:
        return
    keys = sorted(set().union(*(set(row.keys()) for row in rows)))
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=keys, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)


def export_evaluation_markdown(
    comparison: dict[str, Any],
    cooperation_wins: dict[str, Any],
    recommendation: dict[str, Any],
) -> str:
    """Export evaluation as markdown."""
    lines = [
        "# Tesseract Evaluation (Phase 10D)",
        "",
        "## Cooperation Win Summary",
        "",
        f"- Cooperation improved physical size: {cooperation_wins.get('cooperation_improved_count', 0)} datasets",
        f"- Cooperation hurt: {cooperation_wins.get('cooperation_hurt_count', 0)} datasets",
        f"- Neutral: {cooperation_wins.get('cooperation_neutral_count', 0)} datasets",
        f"- Total bytes saved: {cooperation_wins.get('total_bytes_saved_by_cooperation', 0):,}",
        f"- Total bytes lost: {cooperation_wins.get('total_bytes_lost_by_cooperation', 0):,}",
        "",
        "## Recommendation",
        "",
        f"- structural→metadata worthwhile: {recommendation.get('structural_metadata_cooperation_worthwhile')}",
        f"- byte→metadata worthwhile: {recommendation.get('byte_metadata_cooperation_worthwhile')}",
        f"- Current thresholds adequate: {recommendation.get('current_thresholds_adequate')}",
        f"- structure→byte remain deferred: {recommendation.get('structure_byte_should_remain_deferred')}",
        f"- Cooperation enabled by default: {recommendation.get('cooperation_should_remain_enabled_by_default')}",
        "",
        "## Per-Dataset Per-Mode",
        "",
        "| Dataset | Category | Mode | Archive Size | Logical Gain | Fold Count | Cooperation |",
        "|---------|----------|------|--------------|--------------|------------|-------------|",
    ]
    for ds in comparison.get("datasets", []):
        did = ds.get("dataset_id", "")
        cat = ds.get("tesseract_category", "")
        for mode in (MODE_BASELINE, MODE_PLANNER_ONLY, MODE_PLANNER_COOPERATION):
            m = ds.get("by_mode", {}).get(mode, {})
            sz = m.get("archive_size_bytes") or m.get("infold_physical_folded_size", 0)
            gain = m.get("infold_logical_gain", 0)
            fc = m.get("fold_count", 0)
            coop = m.get("cooperation_mode") or "-"
            lines.append(f"| {did} | {cat} | {mode} | {sz:,} | {gain:,} | {fc} | {coop} |")
    return "\n".join(lines)
