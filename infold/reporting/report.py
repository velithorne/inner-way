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
    for r in ledger.fold_records:
        if r.operator_id not in op_breakdown:
            op_breakdown[r.operator_id] = {"count": 0, "gain": 0, "targets": 0}
        op_breakdown[r.operator_id]["count"] += 1
        op_breakdown[r.operator_id]["gain"] += r.gain
        op_breakdown[r.operator_id]["targets"] += len(r.targets)

    return {
        "project_id": config.get("project", {}).get("id"),
        "source_path": str(sheet.source_path),
        "file_count": metrics.get("file_count", 0),
        "folder_count": metrics.get("folder_count", 0),
        "original_size_bytes": metrics.get("original_size_bytes", 0),
        "fold_count": ledger.total_folds,
        "operator_breakdown": op_breakdown,
        "total_bytes_saved": ledger.total_bytes_saved,
        "fold_records": ledger.to_dict().get("fold_records", []),
        "reconstruction_status": "ok" if not result.errors else "errors",
        "errors": result.errors,
        "validation_failures": len(result.errors),
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
        f"Original size: {report['original_size_bytes']:,} bytes",
        "",
        f"Folds: {report['fold_count']}",
        f"Bytes saved: {report['total_bytes_saved']:,}",
        "",
        "Operator breakdown:",
    ]
    for op_id, data in report["operator_breakdown"].items():
        lines.append(f"  {op_id}: {data['count']} folds, {data['gain']} bytes saved, {data['targets']} targets")
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
