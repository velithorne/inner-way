"""
Phase 12: Competitive Benchmark Matrix and Comparison Pack.

Compares Infold against ZIP, gzip, zstd across dataset categories.
Exports matrix (csv, json, markdown) and feature-value comparison.
"""

from __future__ import annotations

import csv
import json
import shutil
import subprocess
import tempfile
from pathlib import Path
from typing import Any

# Dataset ID -> matrix category (for competitive report)
DATASET_TO_MATRIX_CATEGORY: dict[str, str] = {
    "duplicate-heavy-python": "small_code",
    "template-heavy": "template_heavy",
    "config-heavy": "config_heavy",
    "mixed-small": "mixed",
    "hierarchy-mirror": "small_code",
    "dependency-motif": "small_code",
    "infold-workspace": "medium_code",
    "byte-fold-opaque": "opaque_heavy",
    "byte-fold-large-text": "version_like",
    "byte-fold-version-like": "version_like",
    "duplicate-stress": "version_like",
    "template-stress": "template_heavy",
}


def get_matrix_category(dataset_id: str, pack_category: str) -> str:
    """Map dataset to competitive matrix category."""
    return DATASET_TO_MATRIX_CATEGORY.get(dataset_id, pack_category)


# Infold modes for competitive comparison
INFOLD_MODES = [
    ("infold_default", "default", {"_creature_adaptive": True, "_tesseract_planner": True, "_tesseract_cooperation": True}),
    ("infold_lean", "lean", {"_lean_mode": True, "_creature_adaptive": False, "_tesseract_planner": False, "_tesseract_cooperation": False}),
    ("infold_micro", "micro", {"_micro_mode": True, "_lean_mode": True, "_creature_adaptive": False, "_tesseract_planner": False, "_tesseract_cooperation": False}),
    ("infold_golem_static", "golem_static", {"_fold_profile": "golem", "_creature_adaptive": False, "_tesseract_planner": False, "_tesseract_cooperation": False}),
]

# Feature flags per tool/mode (honest assessment)
TOOL_FEATURES: dict[str, dict[str, bool | str]] = {
    "zip": {
        "exact_reconstruction": True,
        "searchable": False,
        "lineage_aware": False,
        "explainable": False,
        "structure_aware": False,
        "metadata_aware": False,
    },
    "gzip": {
        "exact_reconstruction": True,
        "searchable": False,
        "lineage_aware": False,
        "explainable": False,
        "structure_aware": False,
        "metadata_aware": False,
    },
    "zstd": {
        "exact_reconstruction": True,
        "searchable": False,
        "lineage_aware": False,
        "explainable": False,
        "structure_aware": False,
        "metadata_aware": False,
    },
    "infold_default": {
        "exact_reconstruction": True,
        "searchable": True,
        "lineage_aware": True,
        "explainable": True,
        "structure_aware": True,
        "metadata_aware": True,
    },
    "infold_lean": {
        "exact_reconstruction": True,
        "searchable": True,
        "lineage_aware": True,
        "explainable": True,
        "structure_aware": True,
        "metadata_aware": True,
    },
    "infold_micro": {
        "exact_reconstruction": True,
        "searchable": True,
        "lineage_aware": True,
        "explainable": True,
        "structure_aware": True,
        "metadata_aware": True,
    },
    "infold_golem_static": {
        "exact_reconstruction": True,
        "searchable": True,
        "lineage_aware": True,
        "explainable": True,
        "structure_aware": True,
        "metadata_aware": True,
    },
}


def _check_tool_available(name: str) -> bool:
    """Check if a compression tool is available."""
    if name == "zip":
        return shutil.which("zip") is not None
    if name == "gzip":
        return shutil.which("gzip") is not None
    if name == "zstd":
        return shutil.which("zstd") is not None
    return False


def _raw_size_path(path: Path, excludes: list[str]) -> int:
    """Compute raw size of directory (sum of file sizes)."""
    total = 0
    for f in path.rglob("*"):
        if f.is_file():
            try:
                rel = str(f.relative_to(path)).replace("\\", "/")
            except ValueError:
                continue
            skip = False
            for ex in excludes:
                if not ex:
                    continue
                if ex.startswith("*") and ex.endswith("*") and ex[1:-1] in rel:
                    skip = True
                    break
                if ex.startswith("*") and rel.endswith(ex[1:]):
                    skip = True
                    break
                if ex in rel or f"/{ex}" in rel or rel.startswith(ex):
                    skip = True
                    break
            if skip:
                continue
            total += f.stat().st_size
    return total


def _run_zip(path: Path, out_archive: Path, excludes: list[str]) -> int | str:
    """Create ZIP archive. Returns size in bytes or 'not_available'."""
    if not _check_tool_available("zip"):
        return "not_available"
    try:
        with tempfile.TemporaryDirectory() as tmp:
            # zip from path; exclude patterns via -x
            exclude_args = []
            for ex in excludes:
                if ex and ex != "*":
                    exclude_args.extend(["-x", f"*{ex}*"])
            cmd = ["zip", "-rq", "-", "."] + exclude_args
            result = subprocess.run(
                cmd,
                cwd=path,
                capture_output=True,
                timeout=60,
            )
            if result.returncode != 0:
                return "error"
            out_archive.write_bytes(result.stdout)
            return len(result.stdout)
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return "not_available"


def _run_gzip(path: Path, out_archive: Path, excludes: list[str]) -> int | str:
    """Create gzip archive (tar | gzip). Returns size in bytes or 'not_available'."""
    if not _check_tool_available("gzip"):
        return "not_available"
    try:
        exclude_args = []
        for ex in excludes:
            if ex:
                exclude_args.extend(["--exclude", ex])
        cmd_tar = ["tar", "-cf", "-"] + exclude_args + ["."]
        proc_tar = subprocess.Popen(cmd_tar, cwd=path, stdout=subprocess.PIPE)
        cmd_gz = ["gzip", "-9", "-c"]
        proc_gz = subprocess.Popen(cmd_gz, stdin=proc_tar.stdout, stdout=subprocess.PIPE)
        proc_tar.stdout.close()
        out, _ = proc_gz.communicate(timeout=60)
        proc_tar.wait(timeout=5)
        out_archive.write_bytes(out)
        return len(out)
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return "not_available"


def _run_zstd(path: Path, out_archive: Path, excludes: list[str]) -> int | str:
    """Create zstd archive (tar | zstd). Returns size in bytes or 'not_available'."""
    if not _check_tool_available("zstd"):
        return "not_available"
    try:
        exclude_args = []
        for ex in excludes:
            if ex:
                exclude_args.extend(["--exclude", ex])
        cmd_tar = ["tar", "-cf", "-"] + exclude_args + ["."]
        proc_tar = subprocess.Popen(cmd_tar, cwd=path, stdout=subprocess.PIPE)
        cmd_zstd = ["zstd", "-19", "-c"]
        proc_zstd = subprocess.Popen(cmd_zstd, stdin=proc_tar.stdout, stdout=subprocess.PIPE)
        proc_tar.stdout.close()
        out, _ = proc_zstd.communicate(timeout=120)
        proc_tar.wait(timeout=5)
        out_archive.write_bytes(out)
        return len(out)
    except (subprocess.TimeoutExpired, FileNotFoundError, OSError):
        return "not_available"


def run_competitive_matrix(
    datasets: list[tuple[Path, str, str]],
    config: dict[str, Any],
    base_path: Path,
) -> dict[str, Any]:
    """
    Run competitive benchmark: Infold modes + zip/gzip/zstd per dataset.
    Returns matrix with rows per (dataset, category, tool, size, ratio, features).
    Skips unavailable competitors cleanly.
    """
    from infold.archive import create_archive, validate_archive
    from infold.cli import load_config

    base_cfg = load_config() if not config else dict(config)
    excludes = list(base_cfg.get("project", {}).get("exclude_patterns", [".git", "__pycache__", "*.pyc", ".venv", "node_modules", ".tox", "dist", "build"]))
    matrix_rows: list[dict[str, Any]] = []

    for path, dataset_id, pack_category in datasets:
        if not path.exists():
            continue
        matrix_cat = get_matrix_category(dataset_id, pack_category)
        raw = _raw_size_path(path, excludes)
        if raw == 0:
            continue

        # Competitors
        with tempfile.TemporaryDirectory(prefix="infold_comp_") as tmp:
            tmp_p = Path(tmp)
            for tool_name, runner in [
                ("zip", lambda: _run_zip(path, tmp_p / "out.zip", excludes)),
                ("gzip", lambda: _run_gzip(path, tmp_p / "out.tar.gz", excludes)),
                ("zstd", lambda: _run_zstd(path, tmp_p / "out.tar.zst", excludes)),
            ]:
                size = runner()
                if isinstance(size, str):
                    size_val = None
                    ratio = None
                else:
                    size_val = size
                    ratio = size / raw if raw else None
                features = TOOL_FEATURES.get(tool_name, {})
                matrix_rows.append({
                    "dataset_id": dataset_id,
                    "category": pack_category,
                    "matrix_category": matrix_cat,
                    "tool": tool_name,
                    "raw_bytes": raw,
                    "compressed_size_bytes": size_val,
                    "compression_ratio": ratio,
                    "exact_reconstruction": features.get("exact_reconstruction", True),
                    "validation_status": "n/a",
                    "searchable": features.get("searchable", False),
                    "lineage_aware": features.get("lineage_aware", False),
                    "explainable": features.get("explainable", False),
                    "structure_aware": features.get("structure_aware", False),
                    "metadata_aware": features.get("metadata_aware", False),
                    "status": "ok" if isinstance(size, int) else size,
                })

        # Infold modes
        for mode_id, mode_name, overrides in INFOLD_MODES:
            cfg = dict(base_cfg)
            cfg.update(overrides)
            if "golem_static" in mode_id:
                cfg["_fold_profile"] = "golem"
            cfg["project"] = {**cfg.get("project", {}), "exclude_patterns": excludes}
            with tempfile.TemporaryDirectory(prefix="infold_comp_") as tmp2:
                arc = Path(tmp2) / f"{dataset_id}_{mode_id}.infold"
                try:
                    create_archive(path, arc, cfg, profile=cfg.get("_fold_profile", "auto"))
                    size_val = arc.stat().st_size
                    ok, _ = validate_archive(arc, mode="strict")
                    val_status = "ok" if ok else "failed"
                except Exception:
                    size_val = None
                    val_status = "error"
                ratio = size_val / raw if size_val and raw else None
                features = TOOL_FEATURES.get(mode_id, {})
                matrix_rows.append({
                    "dataset_id": dataset_id,
                    "category": pack_category,
                    "matrix_category": matrix_cat,
                    "tool": mode_id,
                    "raw_bytes": raw,
                    "compressed_size_bytes": size_val,
                    "compression_ratio": ratio,
                    "exact_reconstruction": True,
                    "validation_status": val_status,
                    "searchable": features.get("searchable", True),
                    "lineage_aware": features.get("lineage_aware", True),
                    "explainable": features.get("explainable", True),
                    "structure_aware": features.get("structure_aware", True),
                    "metadata_aware": features.get("metadata_aware", True),
                    "status": "ok" if size_val else "error",
                })

    return {
        "matrix": matrix_rows,
        "tools_available": {k: _check_tool_available(k) for k in ["zip", "gzip", "zstd"]},
        "research_slots": get_research_slots_status(),
    }


def export_matrix_csv(matrix: dict[str, Any], out_path: Path) -> None:
    """Export competitive matrix to CSV."""
    rows = matrix.get("matrix", [])
    if not rows:
        return
    keys = sorted(set().union(*(set(r.keys()) for r in rows)))
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=keys, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)


def export_matrix_json(matrix: dict[str, Any], out_path: Path) -> None:
    """Export competitive matrix to JSON."""
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(matrix, f, indent=2)


def export_matrix_markdown(matrix: dict[str, Any], out_path: Path) -> None:
    """Export competitive matrix to Markdown."""
    rows = matrix.get("matrix", [])
    tools_avail = matrix.get("tools_available", {})
    lines = [
        "# Competitive Benchmark Matrix",
        "",
        "## Tools Available",
        "",
        "| Tool | Available |",
        "|------|-----------|",
    ]
    for t, av in tools_avail.items():
        lines.append(f"| {t} | {'yes' if av else 'no' if av is False else '?'} |")
    lines.extend(["", "## Size Comparison (bytes)", ""])
    # Group by dataset
    by_ds: dict[str, list[dict]] = {}
    for r in rows:
        did = r.get("dataset_id", "")
        by_ds.setdefault(did, []).append(r)
    for did in sorted(by_ds.keys()):
        rs = by_ds[did]
        raw = rs[0].get("raw_bytes", 0) if rs else 0
        lines.append(f"### {did} (raw: {raw:,})")
        lines.append("")
        lines.append("| Tool | Size | Ratio | Status |")
        lines.append("|------|------|-------|--------|")
        for r in sorted(rs, key=lambda x: (x.get("compressed_size_bytes") or 999999999, x.get("tool", ""))):
            sz = r.get("compressed_size_bytes")
            sz_str = f"{sz:,}" if isinstance(sz, int) else str(sz or "n/a")
            ratio = r.get("compression_ratio")
            ratio_str = f"{ratio:.3f}" if ratio is not None else "n/a"
            status = r.get("status", "?")
            lines.append(f"| {r.get('tool', '?')} | {sz_str} | {ratio_str} | {status} |")
        lines.append("")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def build_feature_matrix(matrix: dict[str, Any]) -> tuple[dict[str, Any], str]:
    """Build feature-value matrix and return (dict, markdown)."""
    tools = ["zip", "gzip", "zstd", "infold_default", "infold_lean", "infold_micro", "infold_golem_static"]
    features = ["exact_reconstruction", "searchable", "lineage_aware", "explainable", "structure_aware", "metadata_aware"]
    out: dict[str, dict[str, bool]] = {}
    for t in tools:
        out[t] = dict(TOOL_FEATURES.get(t, {}))
    lines = [
        "# Feature-Value Matrix",
        "",
        "| Tool | Exact reconstruction | Searchable | Lineage | Explainable | Structure-aware | Metadata-aware |",
        "|------|---------------------|------------|---------|-------------|-----------------|----------------|",
    ]
    for t in tools:
        row = out.get(t, {})
        cells = [f"{'✓' if row.get(f) else '✗'}" for f in features]
        lines.append(f"| {t} | " + " | ".join(cells) + " |")
    return {"matrix": out}, "\n".join(lines)


def build_competitive_summary(matrix: dict[str, Any]) -> dict[str, Any]:
    """Build competitive summary report."""
    rows = matrix.get("matrix", [])
    if not rows:
        return {"summary": "No data"}
    by_ds: dict[str, list[dict]] = {}
    for r in rows:
        did = r.get("dataset_id", "")
        by_ds.setdefault(did, []).append(r)
    infold_wins = 0
    zip_wins = 0
    gzip_wins = 0
    zstd_wins = 0
    best_infold_by_cat: dict[str, str] = {}
    best_infold_mode_by_cat: dict[str, tuple[str, int]] = {}  # cat -> (mode, size)
    for did, rs in by_ds.items():
        valid = [r for r in rs if isinstance(r.get("compressed_size_bytes"), (int, float))]
        if not valid:
            continue
        best = min(valid, key=lambda x: x.get("compressed_size_bytes") or 999999999)
        best_tool = best.get("tool", "")
        mat_cat = rs[0].get("matrix_category", "") if rs else ""
        if "infold" in best_tool:
            infold_wins += 1
            best_infold_by_cat[mat_cat] = best_infold_by_cat.get(mat_cat, best_tool)
        elif best_tool == "zip":
            zip_wins += 1
        elif best_tool == "gzip":
            gzip_wins += 1
        elif best_tool == "zstd":
            zstd_wins += 1
        # Best Infold mode per category (smallest Infold size) regardless of overall winner
        infold_rows = [r for r in rs if "infold" in str(r.get("tool", "")) and isinstance(r.get("compressed_size_bytes"), (int, float))]
        if infold_rows and mat_cat:
            best_infold = min(infold_rows, key=lambda x: x.get("compressed_size_bytes") or 999999999)
            prev = best_infold_mode_by_cat.get(mat_cat, (None, 999999999))
            if (best_infold.get("compressed_size_bytes") or 999999999) < prev[1]:
                best_infold_mode_by_cat[mat_cat] = (best_infold.get("tool", ""), best_infold.get("compressed_size_bytes") or 0)
    return {
        "infold_wins_count": infold_wins,
        "zip_wins_count": zip_wins,
        "gzip_wins_count": gzip_wins,
        "zstd_wins_count": zstd_wins,
        "best_infold_by_category": best_infold_by_cat,
        "best_infold_mode_by_category": {k: v[0] for k, v in best_infold_mode_by_cat.items()},
        "tools_available": matrix.get("tools_available", {}),
        "dataset_count": len(by_ds),
    }


def export_competitive_summary_markdown(summary: dict[str, Any], matrix: dict[str, Any]) -> str:
    """Export competitive summary as markdown."""
    lines = [
        "# Competitive Benchmark Summary",
        "",
        "## Tools Tested",
        "",
    ]
    for t, av in summary.get("tools_available", {}).items():
        lines.append(f"- **{t}**: {'available' if av else 'not available'}")
    lines.extend([
        "",
        "## Size Wins (smallest archive per dataset)",
        "",
        f"- Infold wins: {summary.get('infold_wins_count', 0)}",
        f"- ZIP wins: {summary.get('zip_wins_count', 0)}",
        f"- gzip wins: {summary.get('gzip_wins_count', 0)}",
        f"- zstd wins: {summary.get('zstd_wins_count', 0)}",
        "",
        "## Best Infold Mode by Category",
        "",
    ])
    for cat, mode in sorted(summary.get("best_infold_by_category", {}).items()):
        lines.append(f"- {cat}: {mode}")
    lines.extend([
        "",
        "## Best Infold Mode by Category (smallest Infold size)",
        "",
    ])
    for cat, mode in sorted(summary.get("best_infold_mode_by_category", {}).items()):
        lines.append(f"- {cat}: {mode}")
    lines.extend([
        "",
        "## Research / Extreme-Ratio Tools (future)",
        "",
    ])
    for slot, status in matrix.get("research_slots", {}).items():
        lines.append(f"- {slot}: {status}")
    lines.extend([
        "",
        "## Honest Competitive Position",
        "",
        "Infold is structure-aware folding, not a general-purpose compressor. It competes on:",
        "- structure-heavy code/config projects",
        "- template-heavy and duplicate-heavy datasets",
        "- archive intelligence (search, lineage, explain, metadata-aware)",
        "",
        "Traditional compressors (ZIP, gzip, zstd) may win on:",
        "- opaque/binary-heavy content",
        "- small datasets where overhead dominates",
        "- pure byte-level compression",
        "",
    ])
    return "\n".join(lines)


# Research/extreme-ratio tools (placeholder)
RESEARCH_SLOTS = ["cmix", "nncp", "paq"]


def get_research_slots_status() -> dict[str, str]:
    """Return status of research comparison tools."""
    return {s: "future_comparison" for s in RESEARCH_SLOTS}
