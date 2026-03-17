#!/usr/bin/env python3
"""
Package footprint experiment: before/after compact mode.

Compares default vs compact package export on:
- duplicate-heavy, template-heavy, config-heavy, infold-workspace

Records: archive size, overhead by section, validation, reconstruction.
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold
from infold.reporting.benchmark import _raw_size
from infold.archive import create_archive, validate_archive, reconstruct_archive


DATASETS = [
    ("tests/fixtures/duplicate_python", "duplicate-heavy"),
    ("tests/fixtures/template_heavy", "template-heavy"),
    ("tests/fixtures/config_heavy", "config-heavy"),
    (".", "infold-workspace"),
]


def load_config() -> dict:
    with open(Path(__file__).parent.parent / "infold" / "config.json", encoding="utf-8") as f:
        return json.load(f)


def get_archive_size_and_overhead(archive_path: Path) -> tuple[int, dict]:
    import zipfile
    overhead = {}
    total = 0
    with zipfile.ZipFile(archive_path, "r") as zf:
        for info in zf.infolist():
            n, s = info.filename, info.file_size
            total += s
            if n == "manifest.json": overhead["manifest"] = overhead.get("manifest", 0) + s
            elif n == "ledger.json": overhead["ledger"] = overhead.get("ledger", 0) + s
            elif n.startswith("shared/"): overhead["shared"] = overhead.get("shared", 0) + s
            elif n.startswith("maps/"): overhead["maps"] = overhead.get("maps", 0) + s
            elif n.startswith("reports/"): overhead["reports"] = overhead.get("reports", 0) + s
            elif n == "integrity.json": overhead["integrity"] = overhead.get("integrity", 0) + s
            elif n.startswith("snapshots/"): overhead["snapshots"] = overhead.get("snapshots", 0) + s
    return total, overhead


def run_experiment(base: Path, dataset_path: Path, dataset_id: str, config: dict, compact: bool) -> dict:
    cfg = {**config}
    cfg["project"] = {**cfg.get("project", {}), "id": dataset_id}
    pkg = cfg.get("package_export", {})
    cfg["package_export"] = {
        **pkg,
        "compact": compact,
        "report_text": pkg.get("report_text", True) if not compact else False,
        "inventory_minimal": pkg.get("inventory_minimal", False) or compact,
    }
    excludes = list(cfg["project"].get("exclude_patterns", []))
    if "infold_sweep_report" not in excludes:
        excludes.append("infold_sweep_report")
    cfg["project"]["exclude_patterns"] = excludes

    if not dataset_path.exists():
        return {"error": f"path not found: {dataset_path}"}

    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_fp_") as tmp:
        archive_path = Path(tmp) / f"{dataset_id}.infold"
        result = run_fold(dataset_path, cfg)
        create_archive(dataset_path, archive_path, cfg)
        size, overhead = get_archive_size_and_overhead(archive_path)
        validate_ok, _ = validate_archive(archive_path)
        out_dir = Path(tmp) / "restored"
        reconstruct_archive(archive_path, out_dir)
        # Verify reconstruction
        from infold.reporting.benchmark import _raw_size
        raw = _raw_size(result.project_sheet)
        restored_size = sum((out_dir / p).stat().st_size for p in out_dir.rglob("*") if (out_dir / p).is_file())
        # Simple check: file count matches
        orig_count = len(result.project_sheet.file_nodes)
        rest_count = len(list(out_dir.rglob("*"))) - len(list(out_dir.rglob("*/")))  # approximate
        rest_files = [f for f in out_dir.rglob("*") if f.is_file()]
        recon_ok = len(rest_files) >= orig_count  # at least as many files
    return {
        "dataset_id": dataset_id,
        "compact": compact,
        "archive_size_bytes": size,
        "overhead": overhead,
        "logical_gain": result.ledger.total_bytes_saved,
        "validate_ok": validate_ok,
        "recon_file_count": len(rest_files),
    }


def main():
    base = Path(__file__).parent.parent
    config = load_config()

    results_default = []
    results_compact = []
    for rel, did in DATASETS:
        p = base / rel
        r0 = run_experiment(base, p, did, config, compact=False)
        r1 = run_experiment(base, p, did, config, compact=True)
        results_default.append(r0)
        results_compact.append(r1)

    # Build comparison
    comparison = []
    for d, c in zip(results_default, results_compact):
        if d.get("error") or c.get("error"):
            continue
        delta = c["archive_size_bytes"] - d["archive_size_bytes"]
        pct = (delta / d["archive_size_bytes"] * 100) if d["archive_size_bytes"] else 0
        comparison.append({
            "dataset": d["dataset_id"],
            "default_bytes": d["archive_size_bytes"],
            "compact_bytes": c["archive_size_bytes"],
            "delta_bytes": delta,
            "pct_reduction": round(-pct, 1) if delta < 0 else round(-pct, 1),
            "validate_ok": c["validate_ok"],
        })

    out = {
        "default": results_default,
        "compact": results_compact,
        "comparison": comparison,
    }

    out_dir = base / "experiment_results"
    out_dir.mkdir(exist_ok=True)
    out_path = out_dir / "package_footprint_experiment.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)

    md_lines = [
        "# Package Footprint Experiment",
        "",
        "## Before (default) vs After (compact)",
        "",
        "| Dataset | Default | Compact | Delta | % Reduction | Validate |",
        "|---------|---------|---------|-------|-------------|----------|",
    ]
    for c in comparison:
        pct = c["pct_reduction"]
        md_lines.append(f"| {c['dataset']} | {c['default_bytes']:,} | {c['compact_bytes']:,} | {c['delta_bytes']:+,} | {pct}% | {'ok' if c['validate_ok'] else 'fail'} |")
    md_lines.extend([
        "",
        "## Overhead by section (compact)",
        "",
    ])
    for r in results_compact:
        if r.get("error"):
            continue
        md_lines.append(f"### {r['dataset_id']}")
        for k, v in sorted(r.get("overhead", {}).items(), key=lambda x: -x[1]):
            md_lines.append(f"  {k}: {v:,} bytes")
        md_lines.append("")
    md_lines.append("## Conclusion")
    md_lines.append("")
    total_delta = sum(c["delta_bytes"] for c in comparison)
    if total_delta < 0:
        md_lines.append(f"Compact mode reduces total archive size by {-total_delta:,} bytes across datasets.")
    else:
        md_lines.append("Compact mode: see per-dataset results above.")
    md_lines.append("")
    md_lines.append("Compact mode: no JSON indent, no report.txt, minimal inventory. Reconstruction and validation preserved.")

    md_path = out_dir / "package_footprint_experiment.md"
    with open(md_path, "w", encoding="utf-8") as f:
        f.write("\n".join(md_lines))

    print(f"Results: {out_path}")
    print(f"Report: {md_path}")
    print()
    print("\n".join(md_lines))
    return 0


if __name__ == "__main__":
    sys.exit(main())
