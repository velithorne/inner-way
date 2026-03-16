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
