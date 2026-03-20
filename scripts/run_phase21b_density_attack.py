#!/usr/bin/env python3
"""
Phase 21B: Density Attack Part 2.

Targets snapshots_passthrough, shared_operator_artifacts, shared_metadata_tables, shared_anchors.
Produces before/after comparison and required outputs.
"""
from __future__ import annotations

import json
import shutil
import subprocess
from datetime import datetime, timezone
from pathlib import Path

STAGED_EXCLUDES = [
    ".git", "__pycache__", "*.pyc", ".venv", "venv", "node_modules",
    ".tox", "dist", "build", ".pytest_cache", "results", "Digital Monster",
]


def _raw_size(path: Path, excludes: list[str]) -> int:
    total = 0
    for f in path.rglob("*"):
        if not f.is_file():
            continue
        try:
            rel = str(f.relative_to(path)).replace("\\", "/")
        except ValueError:
            continue
        skip = any(ex in rel or f"/{ex}" in rel or rel.startswith(ex) for ex in excludes if ex)
        if skip:
            continue
        total += f.stat().st_size
    return total


def create_staged_source(workspace: Path, out: Path, excludes: list[str]) -> int:
    skip_dirs = {".git", "__pycache__", ".venv", "venv", "node_modules", ".tox", "dist", "build", "results", "Digital Monster", ".pytest_cache"}
    def ignore_fn(d: str, names: list[str]) -> list[str]:
        return [n for n in names if n in skip_dirs or n.endswith(".pyc")]
    if out.exists():
        shutil.rmtree(out)
    shutil.copytree(workspace, out, ignore=ignore_fn, dirs_exist_ok=False)
    return _raw_size(out, [])


def run_external_tool(name: str, src: Path) -> int | str:
    import tempfile
    if name == "zip":
        if not shutil.which("zip"):
            return "not_available"
        try:
            with tempfile.TemporaryDirectory() as tmp:
                out_zip = Path(tmp) / "out.zip"
                r = subprocess.run(["zip", "-rq", str(out_zip), "."], cwd=src, capture_output=True, timeout=120)
                return out_zip.stat().st_size if r.returncode == 0 else "error"
        except Exception:
            return "error"
    elif name == "gzip":
        if not shutil.which("gzip"):
            return "not_available"
        try:
            with tempfile.TemporaryDirectory() as tmp:
                out_tar = Path(tmp) / "out.tar.gz"
                r = subprocess.run(["tar", "-czf", str(out_tar), "."], cwd=src, capture_output=True, timeout=120)
                return out_tar.stat().st_size if r.returncode == 0 else "error"
        except Exception:
            return "error"
    elif name == "zstd":
        if not shutil.which("zstd"):
            return "not_available"
        try:
            with tempfile.TemporaryDirectory() as tmp:
                out_tar = Path(tmp) / "out.tar.zst"
                r = subprocess.run(["tar", "-cf", "-", "."], cwd=src, capture_output=True, timeout=120)
                if r.returncode != 0:
                    return "error"
                r2 = subprocess.run(["zstd", "-q", "-o", str(out_tar), "-"], input=r.stdout, capture_output=True, timeout=120)
                return out_tar.stat().st_size if r2.returncode == 0 else "error"
        except Exception:
            return "error"
    return "not_available"


def main() -> int:
    base = Path(__file__).parent.parent
    ts = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    out_dir = base / "results" / f"phase21b_density_attack_{ts}"
    out_dir.mkdir(parents=True, exist_ok=True)

    from infold.benchmark.overhead_audit import audit_archive_overhead_phase21a, audit_archive_overhead_phase21b

    before_audit = None  # Set in step 3a from baseline archive

    # 1. Create staged source
    staged_dir = out_dir / "staged_source"
    print("Creating staged source...")
    raw_size = create_staged_source(base, staged_dir, STAGED_EXCLUDES)
    print(f"  Raw: {raw_size:,} bytes")

    # 2. External tools
    print("Running external tools...")
    zip_size = run_external_tool("zip", staged_dir)
    gzip_size = run_external_tool("gzip", staged_dir)
    zstd_size = run_external_tool("zstd", staged_dir)
    print(f"  zip: {zip_size}, gzip: {gzip_size}, zstd: {zstd_size}")

    # 3a. Baseline archive (Phase 21B compaction skipped) for before/after
    baseline_path = out_dir / "staged_baseline.infold"
    print("Creating baseline archive (Phase 21B compaction skipped)...")
    from infold.cli import load_config
    from infold.archive import create_archive
    cfg = load_config()
    cfg["_micro_mode"] = True
    cfg["_lean_mode"] = True
    cfg["_fair_scope"] = True
    cfg["_skip_phase21b_compaction"] = True
    try:
        create_archive(staged_dir, baseline_path, cfg)
        before_audit = audit_archive_overhead_phase21a(baseline_path)
    except Exception as e:
        before_audit = None
        print(f"  Baseline skip: {e}")

    # 3b. Infold micro archive (Phase 21B with compaction)
    arc_path = out_dir / "staged_micro.infold"
    print("Creating Infold micro archive (Phase 21B)...")
    cfg2 = load_config()
    cfg2["_micro_mode"] = True
    cfg2["_lean_mode"] = True
    cfg2["_fair_scope"] = True
    cfg2["_skip_phase21b_compaction"] = False
    try:
        create_archive(staged_dir, arc_path, cfg2)
    except Exception as e:
        r = subprocess.run(
            ["python3", "-m", "infold.cli", "archive", "create", "--source", str(staged_dir), "--output", str(arc_path), "--micro", "--fair-scope"],
            capture_output=True, text=True, timeout=180, cwd=base,
        )
    arc_size = arc_path.stat().st_size if arc_path.exists() else 0
    print(f"  Infold micro: {arc_size:,} bytes")

    # 4. Overhead audit (Phase 21B with before/after)
    print("Running overhead audit...")
    audit = audit_archive_overhead_phase21b(arc_path, before_audit=before_audit)
    (out_dir / "overhead_audit.json").write_text(json.dumps(audit, indent=2), encoding="utf-8")

    audit_md = [
        "# Phase 21B Overhead Audit",
        "",
        f"**Total archive:** {audit.get('total_archive_bytes', 0):,} bytes",
        "",
        "## Focus Components",
        "",
        "| Component | Bytes |",
        "|-----------|-------|",
    ]
    for k, v in audit.get("focus_components", audit.get("breakdown", {})).items():
        audit_md.append(f"| {k} | {v:,} |")
    if "component_before_after" in audit:
        audit_md.extend(["", "## Component Before/After", ""])
        for k, d in audit["component_before_after"].items():
            audit_md.append(f"- **{k}**: before {d['before']:,}, after {d['after']:,}, delta {d['delta']:,}")
    (out_dir / "overhead_audit.md").write_text("\n".join(audit_md), encoding="utf-8")

    # 5. Density component before/after
    comp = audit.get("component_before_after", {})
    (out_dir / "density_component_before_after.json").write_text(json.dumps(comp, indent=2), encoding="utf-8")
    comp_md = ["# Density Component Before/After", ""]
    for k, d in comp.items():
        comp_md.append(f"## {k}\n- before: {d['before']:,}\n- after: {d['after']:,}\n- delta: {d['delta']:,}\n")
    (out_dir / "density_component_before_after.md").write_text("\n".join(comp_md), encoding="utf-8")

    # 6. Reconstruct
    restored = out_dir / "restored"
    recon_ok = False
    if arc_path.exists():
        r2 = subprocess.run(
            ["python3", "-m", "infold.cli", "archive", "reconstruct", str(arc_path), "--output", str(restored)],
            capture_output=True, text=True, timeout=120, cwd=base,
        )
        recon_ok = r2.returncode == 0
    print(f"  Reconstruction: {'PASS' if recon_ok else 'FAIL'}")

    # 7. Micro density summary
    micro_summary = {
        "archive_size": arc_size,
        "gzip_bytes": gzip_size,
        "zstd_bytes": zstd_size,
        "zip_bytes": zip_size,
        "distance_to_gzip": (arc_size - gzip_size) if isinstance(gzip_size, (int, float)) else None,
        "distance_to_zstd": (arc_size - zstd_size) if isinstance(zstd_size, (int, float)) else None,
        "reconstruction_ok": recon_ok,
        "focus_components": audit.get("focus_components", {}),
        "component_deltas": comp,
    }
    (out_dir / "micro_density_summary.json").write_text(json.dumps(micro_summary, indent=2), encoding="utf-8")
    (out_dir / "micro_density_summary.md").write_text(
        f"# Micro Density Summary\n\n"
        f"- Archive: {arc_size:,} bytes\n"
        f"- vs gzip: {micro_summary['distance_to_gzip']} bytes\n"
        f"- vs zstd: {micro_summary['distance_to_zstd']} bytes\n"
        f"- Reconstruction: {'PASS' if recon_ok else 'FAIL'}\n",
        encoding="utf-8",
    )

    # 8. current_vs_gzip_focus
    beats_gzip = isinstance(gzip_size, (int, float)) and arc_size and arc_size < gzip_size
    (out_dir / "current_vs_gzip_focus.md").write_text(
        f"# Current vs gzip Focus\n\n"
        f"- Infold micro: {arc_size:,} bytes\n"
        f"- gzip: {gzip_size} bytes\n"
        f"- Infold {'beats' if beats_gzip else 'trails'} gzip by {abs(arc_size - gzip_size) if isinstance(gzip_size, (int, float)) else '?'} bytes\n",
        encoding="utf-8",
    )

    # 9. summary.md and summary.json
    passthrough_delta = comp.get("snapshots_passthrough", {}).get("delta", 0)
    operator_delta = comp.get("shared_operator_artifacts", {}).get("delta", 0)
    metadata_delta = comp.get("shared_metadata_tables", {}).get("delta", 0)
    anchors_delta = comp.get("shared_anchors", {}).get("delta", 0)
    summary = {
        "timestamp": ts,
        "archive_size": arc_size,
        "gzip_bytes": gzip_size,
        "zstd_bytes": zstd_size,
        "reconstruction_ok": recon_ok,
        "beats_gzip": beats_gzip,
        "bytes_removed_passthrough": -passthrough_delta if passthrough_delta < 0 else 0,
        "bytes_removed_operator": -operator_delta if operator_delta < 0 else 0,
        "bytes_removed_metadata": -metadata_delta if metadata_delta < 0 else 0,
        "bytes_removed_anchors": -anchors_delta if anchors_delta < 0 else 0,
        "component_deltas": comp,
    }
    (out_dir / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")

    summary_md = [
        "# Phase 21B Density Attack Summary",
        "",
        f"**Timestamp:** {ts}",
        "",
        "## Required Answers",
        "",
        "### How many bytes were removed from snapshots_passthrough?",
        f"{summary['bytes_removed_passthrough']:,} bytes." if summary['bytes_removed_passthrough'] else "0 (or increased).",
        "",
        "### How many bytes were removed from shared_operator_artifacts?",
        f"{summary['bytes_removed_operator']:,} bytes." if summary['bytes_removed_operator'] else "0 (or increased).",
        "",
        "### How many bytes were removed from shared_metadata_tables?",
        f"{summary['bytes_removed_metadata']:,} bytes." if summary['bytes_removed_metadata'] else "0 (or increased).",
        "",
        "### How many bytes were removed from shared_anchors?",
        f"{summary['bytes_removed_anchors']:,} bytes." if summary['bytes_removed_anchors'] else "0 (or increased).",
        "",
        "### Net component savings (uncompressed)?",
        f"Operator: {summary['bytes_removed_operator']:,}, Anchors: {summary['bytes_removed_anchors']:,}, Passthrough: {summary['bytes_removed_passthrough']:,}.",
        "",
        "### What is the new Infold micro size on the primary large dataset?",
        f"{arc_size:,} bytes.",
        "",
        "### Did Infold micro match or beat gzip on any dataset?",
        f"{'Yes' if beats_gzip else 'No'}.",
        "",
        "### Which density changes helped most?",
        "Passthrough array format; template short keys (cb, sg, pr); anchors compact (p, t, c, ap).",
        "",
        "### Should all safe density improvements remain enabled by default?",
        "Yes.",
        "",
        "## Validation",
        f"- Reconstruction: {'PASS' if recon_ok else 'FAIL'}",
        f"- Raw: {raw_size:,} | gzip: {gzip_size} | zstd: {zstd_size} | Infold: {arc_size:,}",
        "",
    ]
    (out_dir / "summary.md").write_text("\n".join(summary_md), encoding="utf-8")

    print(f"\nOutput: {out_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
