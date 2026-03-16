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
    for r in ledger.fold_records:
        if r.operator_id not in op_breakdown:
            op_breakdown[r.operator_id] = {"count": 0, "gain": 0, "targets": 0}
        op_breakdown[r.operator_id]["count"] += 1
        op_breakdown[r.operator_id]["gain"] += r.gain
        op_breakdown[r.operator_id]["targets"] += len(r.targets)
        if r.operator_id == "template_skeleton" and hasattr(r, "unfold_recipe"):
            recipe = r.unfold_recipe
            template_metrics.setdefault("families_found", 0)
            template_metrics["families_found"] += 1
            template_metrics.setdefault("files_per_family", []).append(len(r.targets))
            template_metrics.setdefault("scaffold_lengths", []).append(
                sum(len(b) for b in recipe.get("const_blocks", []))
            )
            template_metrics.setdefault("slot_counts", []).append(len(recipe.get("slot_groups", [])))

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
