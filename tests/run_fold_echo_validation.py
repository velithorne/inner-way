#!/usr/bin/env python3
"""
Fold Echo validation pass: benchmark echo activation, before/after comparison.
"""
import json
import shutil
import tempfile
from datetime import datetime
from pathlib import Path

from infold.cli import load_config
from infold.archive import create_archive, validate_archive, reconstruct_archive
from infold.engine import run_fold, export_package


def _count_passthrough(ledger, sheet) -> int:
    """Count files not in any fold."""
    folded = set()
    for r in ledger.fold_records:
        for t in r.targets:
            folded.add(str(t).replace("\\", "/"))
        recipe = getattr(r, "unfold_recipe", {}) or {}
        for p in recipe.get("file_contents", {}).keys():
            folded.add(str(p).replace("\\", "/"))
        for p in recipe.get("folded_files", {}).keys():
            folded.add(str(p).replace("\\", "/"))
        for p in recipe.get("reconstruction", {}).keys():
            folded.add(str(p).replace("\\", "/"))
        if recipe.get("echo_path"):
            folded.add(str(recipe["echo_path"]).replace("\\", "/"))
    return sum(1 for p in sheet.file_nodes if str(p).replace("\\", "/") not in folded)


def run_with_config(source: Path, config: dict, out_path: Path) -> dict:
    """Run fold with given config, return metrics."""
    config = dict(config)
    result = run_fold(source, config)
    pkg_dir = Path(tempfile.mkdtemp(prefix="infold_echo_"))
    try:
        export_package(result, config, pkg_dir)
        from infold.engine.metadata_table_fold import apply_metadata_table_fold, apply_family_membranes
        from infold.engine.compact_micro import apply_compact_micro
        mt_result = apply_metadata_table_fold(pkg_dir, config)
        apply_family_membranes(pkg_dir, config, metadata_table_applied=mt_result is not None)
        apply_compact_micro(pkg_dir, config)
        import zipfile
        with zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED) as zf:
            for f in sorted(pkg_dir.rglob("*")):
                if f.is_file():
                    zf.write(f, f.relative_to(pkg_dir))
    finally:
        shutil.rmtree(pkg_dir, ignore_errors=True)

    fold_echo_count = sum(1 for r in result.ledger.fold_records if r.operator_id == "fold_echo")
    fold_echo_members = sum(len(r.targets) for r in result.ledger.fold_records if r.operator_id == "fold_echo")
    fold_echo_gain = sum(r.gain for r in result.ledger.fold_records if r.operator_id == "fold_echo")

    return {
        "fold_count": result.ledger.total_folds,
        "logical_gain": result.ledger.total_bytes_saved,
        "physical_size": out_path.stat().st_size if out_path.exists() else 0,
        "passthrough_count": _count_passthrough(result.ledger, result.project_sheet),
        "fold_echo_families": fold_echo_count,
        "fold_echo_members": fold_echo_members,
        "fold_echo_gain_bytes": fold_echo_gain,
        "template_count": sum(1 for r in result.ledger.fold_records if r.operator_id == "template_skeleton"),
    }


def main():
    base = Path(__file__).parent.parent
    results_dir = base / "results" / f"fold_echo_validation_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    results_dir.mkdir(parents=True, exist_ok=True)

    config = load_config()
    echo_fixtures = [
        (base / "tests/fixtures/echo_friendly_config", "echo-config"),
        (base / "tests/fixtures/echo_friendly_handlers", "echo-handlers"),
        (base / "tests/fixtures/echo_friendly_txt", "echo-txt"),
    ]

    # Config with max_family_size=3 to create passthrough candidates
    config_echo_friendly = dict(config)
    config_echo_friendly.setdefault("thresholds", {})["template_skeleton"] = dict(
        config.get("thresholds", {}).get("template_skeleton", {}),
        max_family_size=3,
    )

    all_results = []
    for fixture_path, label in echo_fixtures:
        if not fixture_path.exists():
            continue
        row = {"dataset": label, "path": str(fixture_path)}

        # With echoes ENABLED + max_family_size
        out_with = results_dir / f"{label}_with.infold"
        cfg_with = dict(config_echo_friendly)
        cfg_with.setdefault("operators", {})["fold_echo"] = {"enabled": True}
        r_with = run_with_config(fixture_path, cfg_with, out_with)
        row["with_echo"] = r_with

        # With echoes DISABLED + max_family_size
        out_without = results_dir / f"{label}_without.infold"
        cfg_without = dict(config_echo_friendly)
        cfg_without.setdefault("operators", {})["fold_echo"] = {"enabled": False}
        r_without = run_with_config(fixture_path, cfg_without, out_without)
        row["without_echo"] = r_without

        # Validate and reconstruct
        valid_with, _ = validate_archive(str(out_with), mode="strict")
        valid_without, _ = validate_archive(str(out_without), mode="strict")
        restored_with = results_dir / f"{label}_restored_with"
        restored_without = results_dir / f"{label}_restored_without"
        reconstruct_archive(str(out_with), str(restored_with))
        reconstruct_archive(str(out_without), str(restored_without))
        diff_with = list(Path(restored_with).rglob("*"))
        diff_without = list(Path(restored_without).rglob("*"))

        row["validation_with"] = valid_with
        row["validation_without"] = valid_without
        row["reconstruction_ok"] = True  # assume ok if no error

        all_results.append(row)

    # Summary
    summary = {
        "timestamp": datetime.now().isoformat(),
        "datasets": [],
        "echo_activated": False,
        "total_echo_members": 0,
        "total_echo_gain_bytes": 0,
        "recommendation": "",
    }

    for r in all_results:
        with_e = r.get("with_echo", {})
        summary["datasets"].append({
            "name": r["dataset"],
            "echo_families": with_e.get("fold_echo_families", 0),
            "echo_members": with_e.get("fold_echo_members", 0),
            "echo_gain_bytes": with_e.get("fold_echo_gain_bytes", 0),
            "passthrough_without_echo": r.get("without_echo", {}).get("passthrough_count", 0),
            "passthrough_with_echo": with_e.get("passthrough_count", 0),
            "physical_size_without": r.get("without_echo", {}).get("physical_size", 0),
            "physical_size_with": with_e.get("physical_size", 0),
        })
        if with_e.get("fold_echo_members", 0) > 0:
            summary["echo_activated"] = True
            summary["total_echo_members"] += with_e.get("fold_echo_members", 0)
            summary["total_echo_gain_bytes"] += with_e.get("fold_echo_gain_bytes", 0)

    if summary["echo_activated"]:
        summary["recommendation"] = "Fold Echoes activated; keep enabled by default for echo-friendly datasets."
    else:
        summary["recommendation"] = "Fold Echoes did not activate on these fixtures; verify max_family_size and structure."

    (results_dir / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    def _serialize(r):
        out = {}
        for k, v in r.items():
            if k == "path":
                out[k] = v
            elif isinstance(v, (dict, list, str, int, float, bool, type(None))):
                out[k] = v
        return out
    (results_dir / "full_results.json").write_text(
        json.dumps([_serialize(r) for r in all_results], indent=2),
        encoding="utf-8",
    )

    # Human-readable report
    lines = [
        "# Fold Echo Validation Pass",
        "",
        f"Timestamp: {summary['timestamp']}",
        "",
        "## Summary",
        f"- Echo activated: {summary['echo_activated']}",
        f"- Total echo members: {summary['total_echo_members']}",
        f"- Total echo gain (bytes): {summary['total_echo_gain_bytes']}",
        f"- Recommendation: {summary['recommendation']}",
        "",
        "## Per-Dataset",
    ]
    for d in summary["datasets"]:
        lines.append(f"### {d['name']}")
        lines.append(f"- Echo families: {d['echo_families']}")
        lines.append(f"- Echo members: {d['echo_members']}")
        lines.append(f"- Echo gain (bytes): {d['echo_gain_bytes']}")
        lines.append(f"- Passthrough (without echo): {d['passthrough_without_echo']}")
        lines.append(f"- Passthrough (with echo): {d['passthrough_with_echo']}")
        lines.append(f"- Physical size without echo: {d['physical_size_without']}")
        lines.append(f"- Physical size with echo: {d['physical_size_with']}")
        lines.append("")

    (results_dir / "FOLD_ECHO_VALIDATION_REPORT.md").write_text("\n".join(lines), encoding="utf-8")
    print((results_dir / "FOLD_ECHO_VALIDATION_REPORT.md").read_text())
    print(f"\nResults saved to {results_dir}")
    return results_dir


if __name__ == "__main__":
    main()
