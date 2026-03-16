"""
Reporting: JSON and text summaries, diagnostics, fold breakdown.
"""

import json
from pathlib import Path
from typing import Any

from infold.engine.ledger import FoldLedger
from infold.engine.orchestrator import FoldResult
from infold.models.project_sheet import ProjectSheet


def build_report(result: FoldResult, config: dict[str, Any]) -> dict[str, Any]:
    """Build report dict from FoldResult."""
    sheet = result.project_sheet
    ledger = result.ledger
    metrics = sheet.metrics

    # Operator breakdown
    op_breakdown: dict[str, dict[str, Any]] = {}
    template_metrics: dict[str, Any] = {}
    symbol_table_metrics: dict[str, Any] = {}
    hierarchy_metrics: dict[str, Any] = {}
    dependency_metrics: dict[str, Any] = {}
    for r in ledger.fold_records:
        if r.operator_id not in op_breakdown:
            op_breakdown[r.operator_id] = {"count": 0, "gain": 0, "targets": 0}
        op_breakdown[r.operator_id]["count"] += 1
        op_breakdown[r.operator_id]["gain"] += r.gain
        op_breakdown[r.operator_id]["targets"] += len(r.targets)
        if r.operator_id == "symbol_table":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            symbol_table_metrics.setdefault("shared_symbol_count", 0)
            symbol_table_metrics["shared_symbol_count"] = symbol_table_metrics.get("shared_symbol_count", 0) + recipe.get("shared_symbol_count", 0)
            symbol_table_metrics.setdefault("symbol_reuse_ratio", []).append(recipe.get("symbol_reuse_ratio"))
            symbol_table_metrics.setdefault("net_bytes_saved", 0)
            symbol_table_metrics["net_bytes_saved"] = symbol_table_metrics.get("net_bytes_saved", 0) + r.gain
        if r.operator_id == "hierarchy_mirror":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            hierarchy_metrics.setdefault("hierarchy_templates_found", 0)
            hierarchy_metrics["hierarchy_templates_found"] += 1
            hierarchy_metrics.setdefault("instances_per_template", []).append(recipe.get("instance_count", 0))
            hierarchy_metrics.setdefault("net_bytes_saved", 0)
            hierarchy_metrics["net_bytes_saved"] = hierarchy_metrics.get("net_bytes_saved", 0) + r.gain
            hierarchy_metrics.setdefault("structural_reuse_ratio", []).append(recipe.get("structural_reuse_ratio"))
            hierarchy_metrics.setdefault("path_reconstruction_accuracy", []).append(recipe.get("path_reconstruction_accuracy"))
            hierarchy_metrics.setdefault("file_membership_accuracy", []).append(recipe.get("file_membership_accuracy"))
        if r.operator_id == "dependency_motif":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            dependency_metrics.setdefault("dependency_motifs_found", 0)
            dependency_metrics["dependency_motifs_found"] += 1
            dependency_metrics.setdefault("motif_instances_per_family", []).append(recipe.get("instance_count", 0))
            dependency_metrics.setdefault("average_motif_size", []).append(recipe.get("motif_size", 0))
            dependency_metrics.setdefault("net_bytes_saved", 0)
            dependency_metrics["net_bytes_saved"] = dependency_metrics.get("net_bytes_saved", 0) + r.gain
            dependency_metrics.setdefault("dependency_recovery_accuracy", []).append(recipe.get("dependency_recovery_accuracy"))
            dependency_metrics.setdefault("structural_reuse_ratio", []).append(recipe.get("structural_reuse_ratio"))
        if r.operator_id == "template_skeleton":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            template_metrics.setdefault("families_found", 0)
            template_metrics["families_found"] += 1
            template_metrics.setdefault("files_per_family", []).append(len(r.targets))
            template_metrics.setdefault("scaffold_lengths", []).append(
                sum(len(b) for b in recipe.get("const_blocks", []))
            )
            template_metrics.setdefault("slot_counts", []).append(len(recipe.get("slot_groups", [])))
            template_metrics.setdefault("family_purity", []).append(recipe.get("family_purity"))
            template_metrics.setdefault("slot_ambiguity", []).append(recipe.get("slot_ambiguity"))
            template_metrics.setdefault("avg_slot_size", []).append(recipe.get("avg_slot_size"))

    raw = metrics.get("original_size_bytes", 0)
    logical_gain = ledger.total_bytes_saved  # bytes saved by deduplication
    physical_folded_size = raw - logical_gain  # size of folded representation

    # Per-operator gain share
    total_gain = ledger.total_bytes_saved
    per_operator_gain: dict[str, float] = {}
    for op_id, data in op_breakdown.items():
        g = data.get("gain", 0)
        per_operator_gain[op_id] = g / total_gain if total_gain else 0

    # Family summaries from fold records
    duplicate_families = [r for r in ledger.fold_records if r.operator_id == "exact_repetition"]
    template_families = [r for r in ledger.fold_records if r.operator_id == "template_skeleton"]
    shared_symbol_families = [r for r in ledger.fold_records if r.operator_id == "symbol_table"]
    hierarchy_templates = [r for r in ledger.fold_records if r.operator_id == "hierarchy_mirror"]
    dependency_motifs = [r for r in ledger.fold_records if r.operator_id == "dependency_motif"]

    return {
        "project_id": config.get("project", {}).get("id"),
        "source_path": str(sheet.source_path),
        "file_count": metrics.get("file_count", 0),
        "folder_count": metrics.get("folder_count", 0),
        "original_size_bytes": raw,
        "fold_count": ledger.total_folds,
        "operator_breakdown": op_breakdown,
        "total_bytes_saved": logical_gain,
        "logical_gain_bytes": logical_gain,
        "physical_folded_size_bytes": physical_folded_size,
        "candidate_counts": getattr(result, "candidate_counts", {}),
        "exact_reconstruction_ok": getattr(result, "exact_reconstruction_ok", True),
        "fold_records": ledger.to_dict().get("fold_records", []),
        "reconstruction_status": "ok" if not result.errors and getattr(result, "exact_reconstruction_ok", True) else "errors",
        "errors": result.errors,
        "validation_failures": len(result.errors),
        "template_skeleton_metrics": template_metrics if template_metrics else None,
        "symbol_table_metrics": symbol_table_metrics if symbol_table_metrics else None,
        "hierarchy_metrics": hierarchy_metrics if hierarchy_metrics else None,
        "dependency_metrics": dependency_metrics if dependency_metrics else None,
        "duplicate_families": [{"targets": len(r.targets), "gain": r.gain} for r in duplicate_families],
        "template_families": [
            {
                "targets": len(r.targets),
                "gain": r.gain,
                "file_count": (r.unfold_recipe or {}).get("file_count", len(r.targets)),
                "scaffold_similarity": (r.unfold_recipe or {}).get("scaffold_similarity"),
                "slot_ratio": (r.unfold_recipe or {}).get("slot_ratio"),
                "family_purity": (r.unfold_recipe or {}).get("family_purity"),
                "reject_reason": None,
            }
            for r in template_families
        ],
        "template_rejected_families": config.get("_run_diagnostics", {}).get("template_rejected", []),
        "shared_symbol_families": [{"targets": len(r.targets), "gain": r.gain} for r in shared_symbol_families],
        "hierarchy_templates": [{"targets": len(r.targets), "gain": r.gain} for r in hierarchy_templates],
        "dependency_motifs": [{"targets": len(r.targets), "gain": r.gain} for r in dependency_motifs],
        "per_operator_gain_share": per_operator_gain,
        "rejected_candidates_summary": getattr(result, "rejected_candidates", []),
        "planner_decisions": getattr(result, "planner_decisions", []),
    }


def report_to_json(result: FoldResult, config: dict[str, Any]) -> str:
    """Export report as JSON string."""
    report = build_report(result, config)
    return json.dumps(report, indent=2)


def report_to_text(result: FoldResult, config: dict[str, Any]) -> str:
    """Export report as human-readable text."""
    report = build_report(result, config)
    lines = [
        "Infold Report",
        "=============",
        "",
        f"Project: {report['source_path']}",
        f"Files: {report['file_count']}",
        f"Folders: {report['folder_count']}",
        f"Original size (raw): {report['original_size_bytes']:,} bytes",
        "",
        f"Folds: {report['fold_count']}",
        f"Logical gain (bytes saved): {report['total_bytes_saved']:,}",
        f"Physical folded size: {report.get('physical_folded_size_bytes', report['original_size_bytes'] - report['total_bytes_saved']):,} bytes",
        f"Exact reconstruction: {'OK' if report.get('exact_reconstruction_ok', True) else 'FAILED'}",
        "",
        "Candidate counts:",
    ]
    for op_id, count in report.get("candidate_counts", {}).items():
        lines.append(f"  {op_id}: {count}")
    lines.append("")
    lines.append("Operator breakdown:")
    for op_id, data in report["operator_breakdown"].items():
        lines.append(f"  {op_id}: {data['count']} folds, {data['gain']} bytes saved, {data['targets']} targets")
    lines.append("")
    lines.append("Families:")
    for name, fams in [
        ("duplicate_families", report.get("duplicate_families", [])),
        ("template_families", report.get("template_families", [])),
        ("shared_symbol_families", report.get("shared_symbol_families", [])),
        ("hierarchy_templates", report.get("hierarchy_templates", [])),
        ("dependency_motifs", report.get("dependency_motifs", [])),
    ]:
        if fams:
            lines.append(f"  {name}: {len(fams)} families")
            for i, f in enumerate(fams[:5]):
                lines.append(f"    [{i}] targets={f.get('targets', 0)}, gain={f.get('gain', 0)}")
            if len(fams) > 5:
                lines.append(f"    ... and {len(fams) - 5} more")
    if report.get("template_skeleton_metrics"):
        tm = report["template_skeleton_metrics"]
        lines.append("")
        lines.append("Template Skeleton metrics:")
        lines.append(f"  families_found: {tm.get('families_found', 0)}")
        if tm.get("files_per_family"):
            lines.append(f"  files_per_family: {tm['files_per_family']}")
        if tm.get("scaffold_lengths"):
            lines.append(f"  scaffold_lengths: {tm['scaffold_lengths']}")
        if tm.get("slot_counts"):
            lines.append(f"  slot_counts: {tm['slot_counts']}")
        if tm.get("family_purity"):
            lines.append(f"  family_purity: {tm['family_purity']}")
        if tm.get("slot_ambiguity"):
            lines.append(f"  slot_ambiguity: {tm['slot_ambiguity']}")
        if tm.get("avg_slot_size"):
            lines.append(f"  avg_slot_size: {tm['avg_slot_size']}")
    rejected_tm = report.get("template_rejected_families", [])
    if rejected_tm:
        lines.append("")
        lines.append("Template rejected families (diagnostics):")
        for i, r in enumerate(rejected_tm[:10]):
            lines.append(
                f"  [{i}] file_count={r.get('file_count')} "
                f"scaffold_sim={r.get('scaffold_similarity')} slot_ratio={r.get('slot_ratio')} "
                f"purity={r.get('family_purity')} -> {r.get('reject_reason', '?')}"
            )
        if len(rejected_tm) > 10:
            lines.append(f"  ... and {len(rejected_tm) - 10} more")
    if report.get("symbol_table_metrics"):
        stm = report["symbol_table_metrics"]
        lines.append("")
        lines.append("Symbol Table metrics:")
        lines.append(f"  shared_symbol_count: {stm.get('shared_symbol_count', 0)}")
        lines.append(f"  net_bytes_saved: {stm.get('net_bytes_saved', 0)}")
        if stm.get("symbol_reuse_ratio"):
            lines.append(f"  symbol_reuse_ratio: {stm['symbol_reuse_ratio']}")
    if report.get("hierarchy_metrics"):
        hm = report["hierarchy_metrics"]
        lines.append("")
        lines.append("Hierarchy Mirror metrics:")
        lines.append(f"  hierarchy_templates_found: {hm.get('hierarchy_templates_found', 0)}")
        lines.append(f"  instances_per_template: {hm.get('instances_per_template', [])}")
        lines.append(f"  net_bytes_saved: {hm.get('net_bytes_saved', 0)}")
        lines.append(f"  structural_reuse_ratio: {hm.get('structural_reuse_ratio', [])}")
        lines.append(f"  path_reconstruction_accuracy: {hm.get('path_reconstruction_accuracy', [])}")
        lines.append(f"  file_membership_accuracy: {hm.get('file_membership_accuracy', [])}")
    if report.get("dependency_metrics"):
        dm = report["dependency_metrics"]
        lines.append("")
        lines.append("Dependency Motif metrics:")
        lines.append(f"  dependency_motifs_found: {dm.get('dependency_motifs_found', 0)}")
        lines.append(f"  motif_instances_per_family: {dm.get('motif_instances_per_family', [])}")
        lines.append(f"  average_motif_size: {dm.get('average_motif_size', [])}")
        lines.append(f"  net_bytes_saved: {dm.get('net_bytes_saved', 0)}")
        lines.append(f"  dependency_recovery_accuracy: {dm.get('dependency_recovery_accuracy', [])}")
        lines.append(f"  structural_reuse_ratio: {dm.get('structural_reuse_ratio', [])}")
    lines.append("")
    lines.append("Per-operator gain share:")
    for op_id, share in report.get("per_operator_gain_share", {}).items():
        lines.append(f"  {op_id}: {share:.1%}")
    lines.append("")
    lines.append("Rejected candidates summary:")
    for rc in report.get("rejected_candidates_summary", [])[:20]:
        decision = rc.get("planner_decision", rc.get("reason", "?"))
        lines.append(f"  {rc.get('operator_id', '?')}: {decision} - {rc.get('detail', '')[:50]}")
    if len(report.get("rejected_candidates_summary", [])) > 20:
        lines.append(f"  ... and {len(report['rejected_candidates_summary']) - 20} more")
    planner = report.get("planner_decisions", [])
    if planner:
        accept_count = sum(1 for p in planner if p.get("planner_decision") == "accept")
        lines.append("")
        lines.append("Planner decisions:")
        lines.append(f"  total: {len(planner)}, accepted: {accept_count}")
        for pd in planner[:5]:
            lines.append(f"  {pd.get('operator_id', '?')}: {pd.get('planner_decision', '?')} net={pd.get('final_net_value', 0):.3f}")
        if len(planner) > 5:
            lines.append(f"  ... and {len(planner) - 5} more")
    lines.extend(["", f"Reconstruction: {report['reconstruction_status']}"])
    if report["errors"]:
        lines.append("Errors:")
        for e in report["errors"]:
            lines.append(f"  - {e}")
    return "\n".join(lines)


def export_report(
    result: FoldResult,
    config: dict[str, Any],
    output_dir: Path | str | None = None,
) -> tuple[str, str]:
    """
    Export JSON and text reports. Returns (json_path, text_path).
    Creates output_dir if needed. Uses config report.output_dir if not specified.
    """
    out = output_dir or config.get("report", {}).get("output_dir", "infold_reports")
    out_path = Path(out)
    out_path.mkdir(parents=True, exist_ok=True)

    json_path = out_path / "report.json"
    text_path = out_path / "report.txt"

    if config.get("report", {}).get("export_json", True):
        json_path.write_text(report_to_json(result, config), encoding="utf-8")
    if config.get("report", {}).get("export_text", True):
        text_path.write_text(report_to_text(result, config), encoding="utf-8")

    return str(json_path), str(text_path)
