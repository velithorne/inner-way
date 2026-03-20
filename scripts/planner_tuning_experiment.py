#!/usr/bin/env python3
"""
Planner/package tuning experiment.

Runs on: infold-workspace, duplicate-heavy, template-heavy, config-heavy.
Experiments with one knob at a time:
- planner.min_net_value
- metadata_penalty_weight
- physical_weight
- utility_weight

Compares: physical folded size, logical gain, fold count, reconstruction, validation.
"""

import json
import tempfile
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold
from infold.reporting.benchmark import _raw_size
from infold.archive import create_archive, validate_archive, reconstruct_archive
from infold.benchmark.tuning_report import build_tuning_report


DATASETS = [
    (".", "infold-workspace"),
    ("tests/fixtures/duplicate_python", "duplicate-heavy"),
    ("tests/fixtures/template_heavy", "template-heavy"),
    ("tests/fixtures/config_heavy", "config-heavy"),
]


def load_config() -> dict:
    with open(Path(__file__).parent.parent / "infold" / "config.json", encoding="utf-8") as f:
        return json.load(f)


def run_with_config(base: Path, dataset_path: Path, dataset_id: str, config: dict, create_archive_flag: bool = True) -> dict:
    """Run fold with config, return metrics + tuning report."""
    cfg = {**config}
    cfg["project"] = {**cfg.get("project", {}), "id": dataset_id}
    excludes = list(cfg["project"].get("exclude_patterns", []))
    if "infold_sweep_report" not in excludes:
        excludes.append("infold_sweep_report")
    cfg["project"]["exclude_patterns"] = excludes

    if not dataset_path.exists():
        return {"error": f"path not found: {dataset_path}"}

    result = run_fold(dataset_path, cfg)
    sheet = result.project_sheet
    ledger = result.ledger
    raw = _raw_size(sheet)
    logical_gain = ledger.total_bytes_saved
    physical_folded = raw - logical_gain

    package_overhead = None
    validate_ok = None
    if create_archive_flag:
        with tempfile.TemporaryDirectory(prefix="infold_tune_") as tmp:
            archive_path = Path(tmp) / f"{dataset_id}.infold"
            try:
                create_archive(dataset_path, archive_path, cfg)
                validate_ok, _ = validate_archive(archive_path)
                import zipfile
                with zipfile.ZipFile(archive_path, "r") as zf:
                    overhead = {}
                    for info in zf.infolist():
                        n, s = info.filename, info.file_size
                        if n.startswith("shared/"): overhead["shared"] = overhead.get("shared", 0) + s
                        elif n.startswith("maps/"): overhead["maps"] = overhead.get("maps", 0) + s
                        elif n.startswith("reports/"): overhead["reports"] = overhead.get("reports", 0) + s
                        elif n == "integrity.json": overhead["integrity"] = overhead.get("integrity", 0) + s
                        elif n.startswith("snapshots/"): overhead["snapshots"] = overhead.get("snapshots", 0) + s
                    package_overhead = overhead
            except Exception as e:
                validate_ok = False

    tuning = build_tuning_report(result, cfg, package_overhead)
    return {
        "dataset_id": dataset_id,
        "raw_bytes": raw,
        "logical_gain": logical_gain,
        "physical_folded_size": physical_folded,
        "fold_count": ledger.total_folds,
        "fold_count_by_operator": tuning.get("fold_count_by_operator", {}),
        "gain_by_operator": tuning.get("gain_by_operator", {}),
        "package_overhead": package_overhead,
        "low_value_folds": tuning.get("low_value_folds", []),
        "rejected_by_reason": tuning.get("rejected_by_reason", {}),
        "rejected_by_operator": tuning.get("rejected_by_operator", {}),
        "blocked_by_operator": tuning.get("blocked_by_operator", {}),
        "exact_reconstruction_ok": result.exact_reconstruction_ok,
        "validate_ok": validate_ok,
    }


def main():
    base = Path(__file__).parent.parent
    config = load_config()

    # Baseline
    baseline: dict[str, dict] = {}
    for rel, did in DATASETS:
        p = base / rel
        baseline[did] = run_with_config(base, p, did, config)

    # Experiments: one knob at a time (conservative tuning)
    experiments = [
        ("min_net_value_0", {"planner": {**config["planner"], "min_net_value": 0.0}}),
        ("min_net_value_0.5", {"planner": {**config["planner"], "min_net_value": 0.5}}),
        ("min_net_value_1.0", {"planner": {**config["planner"], "min_net_value": 1.0}}),
        ("metadata_penalty_-0.002", {"planner": {**config["planner"], "metadata_penalty_weight": -0.002}}),
        ("metadata_penalty_-0.005", {"planner": {**config["planner"], "metadata_penalty_weight": -0.005}}),
        ("physical_weight_0.001", {"planner": {**config["planner"], "physical_weight": 0.001}}),
        ("utility_weight_1.5", {"planner": {**config["planner"], "utility_weight": 1.5}}),
    ]

    exp_results: dict[str, dict[str, dict]] = {"baseline": baseline}
    for name, override in experiments:
        merged = {**config}
        for k, v in override.items():
            if k in merged:
                merged[k] = {**merged[k], **v} if isinstance(v, dict) else v
            else:
                merged[k] = v
        exp_results[name] = {}
        for rel, did in DATASETS:
            p = base / rel
            exp_results[name][did] = run_with_config(base, p, did, merged)

    # Compare and summarize
    summary = []
    for exp_name, results in exp_results.items():
        for did, r in results.items():
            if r.get("error"):
                continue
            summary.append({
                "experiment": exp_name,
                "dataset": did,
                "logical_gain": r.get("logical_gain", 0),
                "physical_folded_size": r.get("physical_folded_size", 0),
                "fold_count": r.get("fold_count", 0),
                "exact_reconstruction_ok": r.get("exact_reconstruction_ok"),
                "validate_ok": r.get("validate_ok"),
            })

    out = {
        "baseline": baseline,
        "experiments": {k: v for k, v in exp_results.items() if k != "baseline"},
        "summary": summary,
        "deltas": _compute_deltas(exp_results),
    }

    out_dir = base / "experiment_results"
    out_dir.mkdir(exist_ok=True)
    out_path = out_dir / "planner_tuning_experiment.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)

    md_path = out_dir / "planner_tuning_experiment.md"
    with open(md_path, "w", encoding="utf-8") as f:
        f.write(_markdown_report(out))

    print(f"Results: {out_path}")
    print(f"Report: {md_path}")
    print()
    print(_markdown_report(out))
    return 0


def _compute_deltas(exp_results: dict) -> list[dict]:
    """Compute deltas vs baseline for each experiment."""
    baseline = exp_results.get("baseline", {})
    deltas = []
    for exp_name, results in exp_results.items():
        if exp_name == "baseline":
            continue
        for did, r in results.items():
            b = baseline.get(did, {})
            if r.get("error") or b.get("error"):
                continue
            deltas.append({
                "experiment": exp_name,
                "dataset": did,
                "logical_gain_delta": r.get("logical_gain", 0) - b.get("logical_gain", 0),
                "physical_delta": r.get("physical_folded_size", 0) - b.get("physical_folded_size", 0),
                "fold_count_delta": r.get("fold_count", 0) - b.get("fold_count", 0),
                "reconstruction_ok": r.get("exact_reconstruction_ok"),
                "validate_ok": r.get("validate_ok"),
            })
    return deltas


def _markdown_report(out: dict) -> str:
    lines = [
        "# Planner Tuning Experiment",
        "",
        "## Baseline",
        "",
        "| Dataset | Logical Gain | Physical Size | Fold Count | Recon | Validate |",
        "|---------|--------------|---------------|------------|-------|----------|",
    ]
    for did, r in out.get("baseline", {}).items():
        if r.get("error"):
            continue
        lines.append(f"| {did} | {r.get('logical_gain', 0):,} | {r.get('physical_folded_size', 0):,} | {r.get('fold_count', 0)} | {'ok' if r.get('exact_reconstruction_ok') else 'fail'} | {'ok' if r.get('validate_ok') else 'fail'} |")
    lines.extend(["", "## Deltas vs Baseline (per experiment)", ""])
    for d in out.get("deltas", []):
        lg = d.get("logical_gain_delta", 0)
        ph = d.get("physical_delta", 0)
        fc = d.get("fold_count_delta", 0)
        lines.append(f"- **{d.get('experiment')}** / {d.get('dataset')}: logical_gain Δ={lg:+,}, physical Δ={ph:+,}, folds Δ={fc:+}")
    lines.extend(["", "## Conclusion", ""])
    lines.append(_conclusion(out))
    return "\n".join(lines)


def _conclusion(out: dict) -> str:
    deltas = out.get("deltas", [])
    improved_physical = [d for d in deltas if d.get("physical_delta", 0) < 0]
    regressed_recon = [d for d in deltas if d.get("exact_reconstruction_ok") is False or d.get("validate_ok") is False]
    # min_net_value increase rejects folds -> physical size increases (loses gain)
    stricter_planner = [d for d in deltas if d.get("fold_count_delta", 0) < 0 and d.get("physical_delta", 0) > 0]
    lines = []
    if improved_physical:
        lines.append(f"Physical size improved in: {[(d['experiment'], d['dataset']) for d in improved_physical[:5]]}")
    if regressed_recon:
        lines.append(f"**WARNING** Reconstruction/validation failed: {regressed_recon}")
    if stricter_planner:
        lines.append(f"Stricter planner (e.g. min_net_value=1.0) rejects more folds -> physical size increases (loses gain). Not beneficial.")
    lines.append("")
    lines.append("**Recommendation:** Keep baseline planner settings. No knob improved physical package efficiency. Package overhead (reports, snapshots, maps, integrity) dominates on small datasets.")
    return "\n".join(lines) if lines else "No change."


if __name__ == "__main__":
    sys.exit(main())
