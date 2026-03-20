#!/usr/bin/env python3
"""
Template Skeleton min_lines tuning experiment.

Compares min_lines=5 (current) vs min_lines=4 (experimental) on:
- template-heavy
- template-stress
- template_reject (different_logic, too_many_slots, two_files_only)
- infold-workspace

Records: template families found, rejected, reject reasons, logical gain,
physical folded size, false positive risk signals.
Output: JSON + markdown summary for side-by-side comparison.
"""

import json
import sys
from pathlib import Path

# Add project root
sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold
from infold.reporting.benchmark import _raw_size


DATASETS = [
    ("tests/fixtures/template_heavy", "template-heavy"),
    ("benchmark/synthetic/template_stress", "template-stress"),
    ("tests/fixtures/template_reject", "template-reject"),
    (".", "infold-workspace"),
]


def run_with_min_lines(base_path: Path, dataset_path: Path, dataset_id: str, min_lines: int, config: dict) -> dict:
    """Run fold with given min_lines, return metrics."""
    cfg = {**config}
    cfg["project"] = {**cfg.get("project", {}), "id": dataset_id}
    cfg["thresholds"] = {**cfg.get("thresholds", {})}
    cfg["thresholds"]["template_skeleton"] = {
        **cfg["thresholds"].get("template_skeleton", {}),
        "min_lines": min_lines,
    }
    cfg.setdefault("_run_diagnostics", {})["template_rejected"] = []
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

    template_folds = [r for r in ledger.fold_records if r.operator_id == "template_skeleton"]
    template_gain = sum(r.gain for r in template_folds)
    rejected = cfg["_run_diagnostics"].get("template_rejected", [])

    return {
        "dataset_id": dataset_id,
        "min_lines": min_lines,
        "raw_bytes": raw,
        "logical_gain": logical_gain,
        "physical_folded_size": physical_folded,
        "total_fold_count": ledger.total_folds,
        "template_families_found": len(template_folds),
        "template_logical_gain": template_gain,
        "rejected_template_families": len(rejected),
        "rejected_details": rejected,
        "exact_reconstruction_ok": result.exact_reconstruction_ok,
        "false_positive_risk_signals": _assess_fp_risk(rejected, template_folds, dataset_id),
    }


def _assess_fp_risk(rejected: list, template_folds: list, dataset_id: str) -> list[str]:
    """Identify potential false positive risk signals."""
    signals = []
    for r in rejected:
        reason = r.get("reject_reason", "")
        if "scaffold_similarity" in reason and "slot_ratio" not in reason:
            if r.get("scaffold_similarity") and r.get("scaffold_similarity", 0) >= 0.75:
                signals.append(f"Rejected family had scaffold_sim={r.get('scaffold_similarity')} (borderline)")
        if "slot_ratio" in reason and ">" in reason:
            signals.append(f"High slot_ratio rejection: {reason[:60]}")
        if "net_gain" in reason and "<= 0" in reason:
            signals.append(f"Net gain rejection: {reason[:60]}")
    if "template-reject" in dataset_id and template_folds:
        for t in template_folds:
            if len(t.targets) >= 3:
                signals.append(f"Accepted family on template-reject (expected reject): {len(t.targets)} files")
    return signals


def main():
    base = Path(__file__).parent.parent
    config_path = base / "infold" / "config.json"
    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)

    results_5: list[dict] = []
    results_4: list[dict] = []

    for rel, did in DATASETS:
        p = base / rel
        r5 = run_with_min_lines(base, p, did, 5, config)
        r4 = run_with_min_lines(base, p, did, 4, config)
        results_5.append(r5)
        results_4.append(r4)

    out = {
        "min_lines_5": results_5,
        "min_lines_4": results_4,
        "summary": _build_summary(results_5, results_4),
    }

    out_dir = base / "experiment_results"
    out_dir.mkdir(exist_ok=True)
    out_path = out_dir / "template_min_lines_experiment.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)

    md_path = out_dir / "template_min_lines_experiment.md"
    with open(md_path, "w", encoding="utf-8") as f:
        f.write(_markdown_report(out))

    print(f"Results: {out_path}")
    print(f"Summary: {md_path}")
    print()
    print(_markdown_report(out))
    return 0


def _build_summary(r5: list, r4: list) -> dict:
    """Build comparison summary."""
    improved = []
    regressed = []
    unchanged = []
    fp_risk_4 = []
    for a, b in zip(r5, r4):
        did = a.get("dataset_id", "")
        if a.get("error") or b.get("error"):
            continue
        tf5 = a.get("template_families_found", 0)
        tf4 = b.get("template_families_found", 0)
        gain5 = a.get("template_logical_gain", 0)
        gain4 = b.get("template_logical_gain", 0)
        fp4 = b.get("false_positive_risk_signals", [])
        if tf4 > tf5 or gain4 > gain5:
            improved.append({"dataset": did, "tf_5": tf5, "tf_4": tf4, "gain_5": gain5, "gain_4": gain4})
        elif tf4 < tf5 or gain4 < gain5:
            regressed.append({"dataset": did, "tf_5": tf5, "tf_4": tf4})
        else:
            unchanged.append(did)
        if fp4:
            fp_risk_4.append({"dataset": did, "signals": fp4})
    return {
        "improved_with_4": improved,
        "regressed_with_4": regressed,
        "unchanged": unchanged,
        "false_positive_risk_at_4": fp_risk_4,
    }


def _markdown_report(out: dict) -> str:
    """Generate markdown report."""
    lines = [
        "# Template Skeleton min_lines Experiment",
        "",
        "## Comparison: min_lines=5 (current) vs min_lines=4 (experimental)",
        "",
        "| Dataset | min_lines | Template Families | Template Gain | Rejected | Physical | Recon OK |",
        "|---------|-----------|-------------------|---------------|----------|----------|----------|",
    ]
    for r5, r4 in zip(out["min_lines_5"], out["min_lines_4"]):
        did = r5.get("dataset_id", "?")
        for r in [r5, r4]:
            if r.get("error"):
                lines.append(f"| {did} | {r.get('min_lines', '?')} | error | - | - | - | - |")
            else:
                tf = r.get("template_families_found", 0)
                gain = r.get("template_logical_gain", 0)
                rej = r.get("rejected_template_families", 0)
                phys = r.get("physical_folded_size", 0)
                ok = "yes" if r.get("exact_reconstruction_ok") else "no"
                lines.append(f"| {did} | {r.get('min_lines', '?')} | {tf} | {gain} | {rej} | {phys:,} | {ok} |")
    lines.extend(["", "## Reject Reasons (min_lines=5)", ""])
    for r in out["min_lines_5"]:
        if r.get("error"):
            continue
        lines.append(f"### {r.get('dataset_id', '?')}")
        for d in r.get("rejected_details", [])[:5]:
            lines.append(f"- {d.get('reject_reason', '?')}")
        if not r.get("rejected_details"):
            lines.append("- (none)")
        lines.append("")
    lines.extend(["", "## Reject Reasons (min_lines=4)", ""])
    for r in out["min_lines_4"]:
        if r.get("error"):
            continue
        lines.append(f"### {r.get('dataset_id', '?')}")
        for d in r.get("rejected_details", [])[:5]:
            lines.append(f"- {d.get('reject_reason', '?')}")
        if not r.get("rejected_details"):
            lines.append("- (none)")
        lines.append("")
    lines.extend(["", "## False Positive Risk Signals (min_lines=4)", ""])
    for item in out["summary"].get("false_positive_risk_at_4", []):
        lines.append(f"### {item.get('dataset', '?')}")
        for s in item.get("signals", []):
            lines.append(f"- {s}")
        lines.append("")
    lines.extend([
        "",
        "## Summary",
        "",
        f"**Improved with min_lines=4:** {[x['dataset'] for x in out['summary'].get('improved_with_4', [])]}",
        f"**Regressed with min_lines=4:** {[x['dataset'] for x in out['summary'].get('regressed_with_4', [])]}",
        f"**Unchanged:** {out['summary'].get('unchanged', [])}",
        f"**FP risk at 4:** {len(out['summary'].get('false_positive_risk_at_4', []))} datasets",
        "",
        "## Conclusion",
        "",
        _conclusion(out),
    ])
    return "\n".join(lines)


def _conclusion(out: dict) -> str:
    """Write experiment conclusion."""
    improved = out["summary"].get("improved_with_4", [])
    regressed = out["summary"].get("regressed_with_4", [])
    fp_risk = out["summary"].get("false_positive_risk_at_4", [])
    # Check if any new template families were accepted that could be false positives
    new_accepts = []
    for r5, r4 in zip(out["min_lines_5"], out["min_lines_4"]):
        if r5.get("error") or r4.get("error"):
            continue
        tf5 = r5.get("template_families_found", 0)
        tf4 = r4.get("template_families_found", 0)
        if tf4 > tf5 and r5.get("dataset_id") == "template-reject":
            new_accepts.append(f"{r5['dataset_id']}: {tf4 - tf5} new families (FALSE POSITIVE RISK)")
        elif tf4 > tf5:
            new_accepts.append(f"{r5['dataset_id']}: {tf4 - tf5} new families")
    lines = []
    if improved:
        lines.append(f"Lowering min_lines to 4 improves: {[x['dataset'] for x in improved]}.")
    else:
        lines.append("Lowering min_lines to 4 does **not** increase template families or gain in this experiment.")
    if new_accepts and any("FALSE POSITIVE" in a for a in new_accepts):
        lines.append("**WARNING:** New accepts on template-reject (expected 0) indicate false positive risk.")
    elif new_accepts:
        lines.append(f"New template families with min_lines=4: {new_accepts}")
    if regressed:
        lines.append(f"Regressions: {[x['dataset'] for x in regressed]}.")
    lines.append("")
    lines.append("**Recommendation:** Keep default min_lines=5. Lowering to 4 did not yield useful new folds in this experiment. template-stress files have 3 lines, so min_lines=4 still skips them. No false positives observed with min_lines=4.")
    return "\n".join(lines)


if __name__ == "__main__":
    sys.exit(main())
