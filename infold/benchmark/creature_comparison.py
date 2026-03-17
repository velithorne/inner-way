"""
Phase 6D: Creature comparison benchmark.
Compare static (no adaptation) vs adaptive creature behavior.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from infold.benchmark.campaign import run_benchmark_campaign
from infold.benchmark.pack import ensure_stress_datasets, get_benchmark_datasets


def run_creature_comparison(
    config: dict[str, Any],
    base_path: Path,
    datasets: list[tuple[Path, str, str]] | None = None,
    create_archives: bool = True,
) -> dict[str, Any]:
    """
    Run benchmark with creature adaptive ON vs OFF (static).
    Uses fox profile for both to isolate adaptation effect.
    Returns comparison with physical size, logical gain, reconstruction, validation.
    """
    if datasets is None:
        ensure_stress_datasets(base_path)
        datasets = get_benchmark_datasets(base_path)[:6]

    results_static: list[dict[str, Any]] = []
    results_adaptive: list[dict[str, Any]] = []

    cfg_static = dict(config)
    cfg_static["_fold_profile"] = "fox"
    cfg_static["_creature_adaptive"] = False
    results_static = run_benchmark_campaign(datasets, cfg_static, base_path, create_archives)

    cfg_adaptive = dict(config)
    cfg_adaptive["_fold_profile"] = "fox"
    cfg_adaptive["_creature_adaptive"] = True
    results_adaptive = run_benchmark_campaign(datasets, cfg_adaptive, base_path, create_archives)

    by_dataset: dict[str, dict[str, Any]] = {}
    for r in results_static:
        did = r.get("dataset_id", "")
        by_dataset[did] = {
            "dataset_id": did,
            "static": {
                "physical": r.get("archive_size_bytes") or r.get("infold_physical_folded_size", 0),
                "logical_gain": r.get("infold_logical_gain", 0),
                "fold_count": r.get("fold_count", 0),
                "reconstruction": r.get("exact_reconstruction_status", ""),
                "validation": r.get("archive_validate_status", ""),
            },
            "adaptive": {},
        }
    for r in results_adaptive:
        did = r.get("dataset_id", "")
        if did not in by_dataset:
            by_dataset[did] = {"dataset_id": did, "static": {}, "adaptive": {}}
        by_dataset[did]["adaptive"] = {
            "physical": r.get("archive_size_bytes") or r.get("infold_physical_folded_size", 0),
            "logical_gain": r.get("infold_logical_gain", 0),
            "fold_count": r.get("fold_count", 0),
            "reconstruction": r.get("exact_reconstruction_status", ""),
            "validation": r.get("archive_validate_status", ""),
        }

    summary = {
        "adaptive_wins_physical": 0,
        "static_wins_physical": 0,
        "tie_physical": 0,
        "both_reconstruction_ok": 0,
        "both_validation_ok": 0,
    }
    for ds in by_dataset.values():
        s = ds.get("static", {})
        a = ds.get("adaptive", {})
        sp = s.get("physical", 0)
        ap = a.get("physical", 0)
        if sp and ap:
            if ap < sp:
                summary["adaptive_wins_physical"] += 1
            elif sp < ap:
                summary["static_wins_physical"] += 1
            else:
                summary["tie_physical"] += 1
        if s.get("reconstruction") == "ok" and a.get("reconstruction") == "ok":
            summary["both_reconstruction_ok"] += 1
        if s.get("validation") in ("ok", "skipped") and a.get("validation") in ("ok", "skipped"):
            summary["both_validation_ok"] += 1

    return {
        "datasets": list(by_dataset.values()),
        "summary": summary,
        "profiles_used": "fox (static vs adaptive)",
    }


def creature_comparison_to_markdown(data: dict[str, Any]) -> str:
    """Format creature comparison as markdown."""
    lines = [
        "# Creature Comparison (Static vs Adaptive)",
        "",
        f"Profiles: {data.get('profiles_used', '?')}",
        "",
        "## Summary",
        "",
        f"- Adaptive wins (smaller physical): {data.get('summary', {}).get('adaptive_wins_physical', 0)}",
        f"- Static wins (smaller physical): {data.get('summary', {}).get('static_wins_physical', 0)}",
        f"- Tie: {data.get('summary', {}).get('tie_physical', 0)}",
        f"- Both reconstruction OK: {data.get('summary', {}).get('both_reconstruction_ok', 0)}",
        f"- Both validation OK: {data.get('summary', {}).get('both_validation_ok', 0)}",
        "",
        "## Per-dataset",
        "",
        "| Dataset | Static physical | Adaptive physical | Static gain | Adaptive gain | S recon | A recon |",
        "|---------|------------------|--------------------|-------------|---------------|---------|---------|",
    ]
    for ds in data.get("datasets", []):
        s = ds.get("static", {})
        a = ds.get("adaptive", {})
        lines.append(
            f"| {ds.get('dataset_id', '?')} | "
            f"{s.get('physical', 0):,} | {a.get('physical', 0):,} | "
            f"{s.get('logical_gain', 0):,} | {a.get('logical_gain', 0):,} | "
            f"{s.get('reconstruction', '?')} | {a.get('reconstruction', '?')} |"
        )
    return "\n".join(lines)
