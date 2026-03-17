"""
Profile comparison benchmark: run datasets across all profiles, compare results,
evaluate Auto vs best, produce win summaries and tuning recommendations.
"""

from __future__ import annotations

import csv
import json
from collections import Counter
from pathlib import Path
from typing import Any

from infold.profiles.definitions import VALID_PROFILES

# Profile categories for representative dataset coverage
PROFILE_CATEGORIES = frozenset({
    "tiny",
    "balanced",
    "large_structured",
    "opaque_heavy",
    "version_like",
})

# Map dataset_id -> profile_category for reporting
DATASET_TO_PROFILE_CATEGORY: dict[str, str] = {
    "template-heavy": "tiny",
    "config-heavy": "tiny",
    "mixed-small": "tiny",
    "hierarchy-mirror": "tiny",
    "dependency-motif": "tiny",
    "duplicate-heavy-python": "balanced",
    "infold-workspace": "large_structured",
    "byte-fold-opaque": "opaque_heavy",
    "byte-fold-large-text": "version_like",
    "byte-fold-version-like": "version_like",
    "duplicate-stress": "version_like",
    "template-stress": "version_like",
}


def get_profile_category(dataset_id: str) -> str:
    """Return profile category for dataset. Default to 'balanced' if unknown."""
    return DATASET_TO_PROFILE_CATEGORY.get(dataset_id, "balanced")


def run_profile_comparison(
    datasets: list[tuple[Path, str, str]],
    config: dict[str, Any],
    base_path: Path,
    create_archives: bool = True,
) -> dict[str, Any]:
    """
    Run each dataset across all profiles (auto, sparrow, fox, dragon, golem, serpent).
    Returns comparison results with per-dataset per-profile metrics.
    """
    from infold.benchmark.campaign import run_benchmark_campaign

    profiles_to_run = ["auto"] + sorted(VALID_PROFILES)
    all_results: dict[str, dict[str, Any]] = {}

    for profile in profiles_to_run:
        cfg = dict(config)
        cfg["_benchmark_profile"] = profile
        results = run_benchmark_campaign(datasets, cfg, base_path, create_archives=create_archives)
        for r in results:
            did = r.get("dataset_id", "")
            if did not in all_results:
                all_results[did] = {
                    "dataset_id": did,
                    "category": r.get("category", ""),
                    "profile_category": get_profile_category(did),
                    "path": r.get("path", ""),
                    "raw_bytes": r.get("raw_bytes", 0),
                    "zip_bytes": r.get("zip_bytes", 0),
                    "gzip_bytes": r.get("gzip_bytes", 0),
                    "by_profile": {},
                }
            all_results[did]["by_profile"][profile] = {
                "infold_physical_folded_size": r.get("infold_physical_folded_size", 0),
                "infold_logical_gain": r.get("infold_logical_gain", 0),
                "archive_size_bytes": r.get("archive_size_bytes"),
                "fold_count": r.get("fold_count", 0),
                "exact_reconstruction_status": r.get("exact_reconstruction_status", ""),
                "archive_validate_status": r.get("archive_validate_status", ""),
                "fold_profile": r.get("fold_profile"),
                "fold_profile_reason": r.get("fold_profile_reason"),
            }

    return {"datasets": list(all_results.values()), "profiles_run": profiles_to_run}


def compute_auto_vs_best(comparison: dict[str, Any]) -> dict[str, Any]:
    """Compute Auto vs best profile for each dataset."""
    auto_vs_best: list[dict[str, Any]] = []
    for ds in comparison.get("datasets", []):
        did = ds.get("dataset_id", "")
        by_profile = ds.get("by_profile", {})
        auto_result = by_profile.get("auto", {})
        auto_selected = auto_result.get("fold_profile", "")
        auto_reason = auto_result.get("fold_profile_reason", "")

        # Best by physical (smallest is best) - use archive_size_bytes when available, else infold_physical
        best_phys_profile = None
        best_phys_size = float("inf")
        for prof, res in by_profile.items():
            if prof == "auto":
                continue
            phys = res.get("archive_size_bytes")
            if phys is None:
                phys = res.get("infold_physical_folded_size", float("inf"))
            if phys is not None and phys < best_phys_size and res.get("exact_reconstruction_status") == "ok":
                best_phys_size = phys
                best_phys_profile = prof

        # Best by logical gain (largest is best) - among fixed profiles only
        best_gain_profile = None
        best_gain = -1
        for prof, res in by_profile.items():
            if prof == "auto":
                continue
            gain = res.get("infold_logical_gain", -1)
            if gain > best_gain and res.get("exact_reconstruction_status") == "ok":
                best_gain = gain
                best_gain_profile = prof

        auto_phys = auto_result.get("archive_size_bytes") or auto_result.get("infold_physical_folded_size", float("inf"))
        auto_gain = auto_result.get("infold_logical_gain", -1)
        auto_matched_phys = auto_selected == best_phys_profile if best_phys_profile else False
        auto_matched_gain = auto_selected == best_gain_profile if best_gain_profile else False

        # Near-best: within 5% of best
        near_best_phys = False
        if best_phys_size and best_phys_size < float("inf") and auto_phys is not None:
            near_best_phys = auto_phys <= best_phys_size * 1.05
        near_best_gain = auto_gain >= best_gain * 0.95 if best_gain > 0 else (best_gain == 0 and auto_gain == 0)

        auto_vs_best.append({
            "dataset_id": did,
            "profile_category": ds.get("profile_category", ""),
            "auto_selected": auto_selected,
            "auto_reason": auto_reason,
            "best_physical_profile": best_phys_profile,
            "best_logical_gain_profile": best_gain_profile,
            "auto_matched_best_physical": auto_matched_phys,
            "auto_matched_best_gain": auto_matched_gain,
            "auto_near_best_physical": near_best_phys,
            "auto_near_best_gain": near_best_gain,
        })

    return {"auto_vs_best": auto_vs_best}


def compute_profile_wins(comparison: dict[str, Any]) -> dict[str, Any]:
    """Compute which profile won most often by physical and by gain."""
    phys_wins: dict[str, int] = {}
    gain_wins: dict[str, int] = {}
    wins_by_category: dict[str, dict[str, int]] = {}

    for ds in comparison.get("datasets", []):
        by_profile = ds.get("by_profile", {})
        cat = ds.get("profile_category", "balanced")

        best_phys_p = None
        best_phys = float("inf")
        best_gain_p = None
        best_gain = -1
        for prof, res in by_profile.items():
            if prof == "auto" or res.get("exact_reconstruction_status") != "ok":
                continue
            phys = res.get("archive_size_bytes") or res.get("infold_physical_folded_size", float("inf"))
            gain = res.get("infold_logical_gain", -1)
            if phys < best_phys:
                best_phys = phys
                best_phys_p = prof
            if gain > best_gain:
                best_gain = gain
                best_gain_p = prof

        if best_phys_p:
            phys_wins[best_phys_p] = phys_wins.get(best_phys_p, 0) + 1
        if best_gain_p:
            gain_wins[best_gain_p] = gain_wins.get(best_gain_p, 0) + 1
        if cat not in wins_by_category:
            wins_by_category[cat] = {}
        if best_phys_p:
            wins_by_category[cat][f"phys_{best_phys_p}"] = wins_by_category[cat].get(f"phys_{best_phys_p}", 0) + 1

    return {
        "physical_wins": dict(sorted(phys_wins.items(), key=lambda x: -x[1])),
        "logical_gain_wins": dict(sorted(gain_wins.items(), key=lambda x: -x[1])),
        "wins_by_category": wins_by_category,
    }


def _best_profile_by_category(comparison: dict[str, Any]) -> dict[str, dict[str, str]]:
    """For each profile_category, determine best profile by physical and by gain."""
    by_cat: dict[str, dict[str, list[tuple[str, str]]]] = {}
    for ds in comparison.get("datasets", []):
        cat = ds.get("profile_category", "balanced")
        if cat not in by_cat:
            by_cat[cat] = {"phys": [], "gain": []}
        best_phys_p = None
        best_phys = float("inf")
        best_gain_p = None
        best_gain = -1
        for prof, res in ds.get("by_profile", {}).items():
            if prof == "auto" or res.get("exact_reconstruction_status") != "ok":
                continue
            phys = res.get("archive_size_bytes") or res.get("infold_physical_folded_size", float("inf"))
            gain = res.get("infold_logical_gain", -1)
            if phys < best_phys:
                best_phys = phys
                best_phys_p = prof
            if gain > best_gain:
                best_gain = gain
                best_gain_p = prof
        if best_phys_p:
            by_cat[cat]["phys"].append((ds.get("dataset_id", ""), best_phys_p))
        if best_gain_p:
            by_cat[cat]["gain"].append((ds.get("dataset_id", ""), best_gain_p))
    result: dict[str, dict[str, str]] = {}
    for cat, data in by_cat.items():
        phys_winners = [p for _, p in data["phys"]]
        gain_winners = [p for _, p in data["gain"]]
        result[cat] = {
            "best_physical": Counter(phys_winners).most_common(1)[0][0] if phys_winners else "",
            "best_gain": Counter(gain_winners).most_common(1)[0][0] if gain_winners else "",
        }
    return result


def build_comparison_summary(comparison: dict[str, Any]) -> dict[str, Any]:
    """Build full comparison summary with auto-vs-best and profile wins."""
    avb = compute_auto_vs_best(comparison)
    wins = compute_profile_wins(comparison)
    best_by_cat = _best_profile_by_category(comparison)
    avb_list = avb.get("auto_vs_best", [])

    matched_phys = sum(1 for x in avb_list if x.get("auto_matched_best_physical"))
    matched_gain = sum(1 for x in avb_list if x.get("auto_matched_best_gain"))
    near_phys = sum(1 for x in avb_list if x.get("auto_near_best_physical"))
    near_gain = sum(1 for x in avb_list if x.get("auto_near_best_gain"))
    total = len(avb_list)

    misclassified: dict[str, list[str]] = {}
    for x in avb_list:
        cat = x.get("profile_category", "")
        if not x.get("auto_matched_best_physical"):
            misclassified.setdefault(cat, []).append(x.get("dataset_id", ""))

    return {
        "comparison": comparison,
        "auto_vs_best": avb,
        "profile_wins": wins,
        "best_by_category": best_by_cat,
        "summary": {
            "total_datasets": total,
            "auto_matched_best_physical_count": matched_phys,
            "auto_matched_best_gain_count": matched_gain,
            "auto_near_best_physical_count": near_phys,
            "auto_near_best_gain_count": near_gain,
            "auto_matched_physical_pct": round(100 * matched_phys / total, 1) if total else 0,
            "auto_matched_gain_pct": round(100 * matched_gain / total, 1) if total else 0,
            "misclassified_by_category": misclassified,
        },
    }


def export_comparison_csv(summary: dict[str, Any], out_path: Path) -> None:
    """Export per-dataset per-profile comparison to CSV."""
    comparison = summary.get("comparison", {})
    rows = []
    for ds in comparison.get("datasets", []):
        did = ds.get("dataset_id", "")
        cat = ds.get("profile_category", "")
        for prof, res in ds.get("by_profile", {}).items():
            rows.append({
                "dataset_id": did,
                "profile_category": cat,
                "profile": prof,
                "infold_physical": res.get("infold_physical_folded_size", 0),
                "infold_gain": res.get("infold_logical_gain", 0),
                "fold_count": res.get("fold_count", 0),
                "recon": res.get("exact_reconstruction_status", ""),
                "auto_selected": res.get("fold_profile") if prof == "auto" else "",
            })
    if not rows:
        return
    keys = ["dataset_id", "profile_category", "profile", "infold_physical", "infold_gain", "fold_count", "recon", "auto_selected"]
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=keys, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)


def export_auto_vs_best_csv(summary: dict[str, Any], out_path: Path) -> None:
    """Export auto-vs-best evaluation to CSV."""
    rows = summary.get("auto_vs_best", {}).get("auto_vs_best", [])
    if not rows:
        return
    keys = ["dataset_id", "profile_category", "auto_selected", "auto_reason", "best_physical_profile", "best_logical_gain_profile", "auto_matched_best_physical", "auto_matched_best_gain"]
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=keys, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)


def export_comparison_markdown(summary: dict[str, Any], out_path: Path) -> None:
    """Export comparison summary to Markdown."""
    lines = [
        "# Profile Comparison Benchmark",
        "",
        "## Auto vs Best Summary",
        "",
    ]
    s = summary.get("summary", {})
    total = s.get("total_datasets", 0)
    lines.append(f"- Total datasets: {total}")
    lines.append(f"- Auto matched best physical: {s.get('auto_matched_best_physical_count', 0)} ({s.get('auto_matched_physical_pct', 0)}%)")
    lines.append(f"- Auto matched best logical gain: {s.get('auto_matched_best_gain_count', 0)} ({s.get('auto_matched_gain_pct', 0)}%)")
    lines.append(f"- Auto near-best physical (within 5%): {s.get('auto_near_best_physical_count', 0)}")
    lines.append("")
    lines.append("## Profile Wins (by physical folded size)")
    for prof, cnt in summary.get("profile_wins", {}).get("physical_wins", {}).items():
        lines.append(f"- {prof}: {cnt}")
    lines.append("")
    lines.append("## Profile Wins (by logical gain)")
    for prof, cnt in summary.get("profile_wins", {}).get("logical_gain_wins", {}).items():
        lines.append(f"- {prof}: {cnt}")
    lines.append("")
    lines.append("## Per-Dataset Auto vs Best")
    lines.append("")
    lines.append("| Dataset | Category | Auto | Best Phys | Best Gain | Matched Phys | Matched Gain |")
    lines.append("|---------|----------|------|-----------|-----------|--------------|--------------|")
    for x in summary.get("auto_vs_best", {}).get("auto_vs_best", []):
        did = x.get("dataset_id", "")
        cat = x.get("profile_category", "")
        auto = x.get("auto_selected", "")
        bp = x.get("best_physical_profile", "")
        bg = x.get("best_logical_gain_profile", "")
        mp = "yes" if x.get("auto_matched_best_physical") else "no"
        mg = "yes" if x.get("auto_matched_best_gain") else "no"
        lines.append(f"| {did} | {cat} | {auto} | {bp} | {bg} | {mp} | {mg} |")
    mis = s.get("misclassified_by_category", {})
    if mis:
        lines.append("")
        lines.append("## Misclassified by Category")
        for cat, ids in mis.items():
            lines.append(f"- {cat}: {ids}")

    best_by_cat = summary.get("best_by_category", {})
    lines.extend([
        "",
        "## Best Profile by Category",
        "",
        "| Category | Best Physical | Best Gain |",
        "|----------|----------------|-----------|",
    ])
    for cat in sorted(best_by_cat.keys()):
        bc = best_by_cat.get(cat, {})
        lines.append(f"| {cat} | {bc.get('best_physical', '')} | {bc.get('best_gain', '')} |")
    lines.extend([
        "",
        "## Tuning Recommendations",
        "",
        "Based on comparison results:",
    ])
    for cat in sorted(best_by_cat.keys()):
        bc = best_by_cat.get(cat, {})
        bp = bc.get("best_physical", "")
        bg = bc.get("best_gain", "")
        if bp or bg:
            lines.append(f"- Best for {cat}: physical={bp}, gain={bg}")
    lines.append("- Auto default: keep auto; consider tuning thresholds if mismatch rate is high")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
