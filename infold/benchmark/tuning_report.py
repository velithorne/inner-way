"""
Tuning report: logical gain vs physical size, metadata overhead, low-value folds, blocked/rejected.

Analyzes fold results to identify:
- operator contributions to logical gain
- package overhead by directory
- overhead hotspots (which sections dominate)
- accepted folds with low physical benefit
- rejected candidates by planner reason
- operators most often blocked or rejected
"""

import json
from pathlib import Path
from typing import Any

from infold.engine.package_audit import audit_package_overhead


def build_tuning_report(
    result: Any,
    config: dict[str, Any],
    package_overhead: dict[str, int] | None = None,
    total_archive_bytes: int | None = None,
) -> dict[str, Any]:
    """
    Build tuning-focused report from FoldResult.
    """
    ledger = result.ledger
    sheet = result.project_sheet
    raw = sheet.metrics.get("original_size_bytes", 0)
    logical_gain = ledger.total_bytes_saved
    physical_folded = raw - logical_gain

    # Operator contributions to logical gain
    gain_by_op: dict[str, int] = {}
    for r in ledger.fold_records:
        gain_by_op[r.operator_id] = gain_by_op.get(r.operator_id, 0) + r.gain

    # Per-fold gain (for low-value detection)
    folds_with_gain: list[dict[str, Any]] = []
    for r in ledger.fold_records:
        folds_with_gain.append({
            "operator_id": r.operator_id,
            "gain": r.gain,
            "target_count": len(r.targets),
            "gain_per_target": r.gain / len(r.targets) if r.targets else 0,
        })
    low_value_threshold = 50  # bytes
    low_value_folds = [f for f in folds_with_gain if f["gain"] < low_value_threshold]

    # Rejected by planner reason
    rejected_by_reason: dict[str, int] = {}
    rejected_by_operator: dict[str, int] = {}
    for rc in getattr(result, "rejected_candidates", []):
        reason = rc.get("reason", "unknown")
        planner_dec = rc.get("planner_decision", "")
        key = f"{reason}:{planner_dec}" if planner_dec else reason
        rejected_by_reason[key] = rejected_by_reason.get(key, 0) + 1
        op = rc.get("operator_id", "unknown")
        rejected_by_operator[op] = rejected_by_operator.get(op, 0) + 1

    # Blocked (from interaction diagnostics)
    blocked = getattr(result, "interaction_diagnostics", None)
    blocked_folds = getattr(blocked, "blocked_folds", []) if blocked else []
    blocked_by_op: dict[str, int] = {}
    for b in blocked_folds:
        op = b.get("operator_id", "unknown")
        blocked_by_op[op] = blocked_by_op.get(op, 0) + 1

    byte_fold_metrics: dict[str, Any] = {}
    byte_fold_routing: dict[str, int] = {}
    for r in ledger.fold_records:
        if r.operator_id == "byte_fold":
            recipe = getattr(r, "unfold_recipe", {}) or {}
            byte_fold_metrics["files_chunk_folded"] = byte_fold_metrics.get("files_chunk_folded", 0) + len(recipe.get("reconstruction", {}))
            byte_fold_metrics["unique_chunk_count"] = byte_fold_metrics.get("unique_chunk_count", 0) + len(recipe.get("chunk_dict_b64", {}))
            byte_fold_metrics["reused_chunk_count"] = byte_fold_metrics.get("reused_chunk_count", 0) + recipe.get("reused_chunk_count", 0)
            byte_fold_metrics["chunk_reused_bytes"] = byte_fold_metrics.get("chunk_reused_bytes", 0) + recipe.get("chunk_reused_bytes", 0)
            byte_fold_metrics["chunk_dictionary_size_bytes"] = byte_fold_metrics.get("chunk_dictionary_size_bytes", 0) + recipe.get("chunk_dictionary_size_bytes", 0)
    run_diag = config.get("_run_diagnostics", {})
    bf_routing = run_diag.get("byte_fold_routing") or {}
    for path, route_result in bf_routing.items():
        if isinstance(route_result, dict):
            route = route_result.get("route", "unknown")
        elif hasattr(route_result, "route"):
            route = route_result.route
        else:
            route = "unknown"
        byte_fold_routing[route] = byte_fold_routing.get(route, 0) + 1

    report: dict[str, Any] = {
        "logical_gain_bytes": logical_gain,
        "physical_folded_size_bytes": physical_folded,
        "raw_bytes": raw,
        "gain_by_operator": gain_by_op,
        "fold_count": ledger.total_folds,
        "fold_count_by_operator": {},
        "byte_fold_metrics": byte_fold_metrics if byte_fold_metrics else None,
        "byte_fold_routing": byte_fold_routing if byte_fold_routing else None,
        "low_value_folds": low_value_folds,
        "low_value_threshold_bytes": low_value_threshold,
        "rejected_by_reason": rejected_by_reason,
        "rejected_by_operator": rejected_by_operator,
        "blocked_by_operator": blocked_by_op,
        "exact_reconstruction_ok": getattr(result, "exact_reconstruction_ok", True),
    }
    for r in ledger.fold_records:
        report["fold_count_by_operator"][r.operator_id] = report["fold_count_by_operator"].get(r.operator_id, 0) + 1
    if package_overhead:
        report["package_overhead"] = package_overhead
        report["total_overhead_bytes"] = sum(package_overhead.values())
        report["package_audit"] = audit_package_overhead(
            package_overhead, total_archive_bytes
        )
    return report


def tuning_report_to_text(report: dict[str, Any], dataset_id: str = "") -> str:
    """Human-readable tuning report."""
    lines = [
        "# Tuning Report",
        "",
        f"Dataset: {dataset_id}" if dataset_id else "",
        "",
        "## Logical vs Physical",
        f"  Raw: {report.get('raw_bytes', 0):,} bytes",
        f"  Logical gain: {report.get('logical_gain_bytes', 0):,} bytes",
        f"  Physical folded size: {report.get('physical_folded_size_bytes', 0):,} bytes",
        "",
        "## Operator contributions (logical gain)",
    ]
    for op, gain in sorted(report.get("gain_by_operator", {}).items(), key=lambda x: -x[1]):
        lines.append(f"  {op}: {gain:,} bytes")
    lines.extend([
        "",
        "## Package overhead",
    ])
    for k, v in (report.get("package_overhead") or {}).items():
        lines.append(f"  {k}: {v:,} bytes")
    if report.get("total_overhead_bytes"):
        lines.append(f"  total: {report['total_overhead_bytes']:,} bytes")
    audit = report.get("package_audit", {})
    if audit:
        lines.append("")
        lines.append("## Overhead hotspots")
        lines.append(f"  Dominant: {', '.join(audit.get('dominant_sections', []))}")
        for h in audit.get("hotspots", []):
            lines.append(f"  - {h}")
    lines.extend([
        "",
        "## Low-value accepted folds (gain < 50 bytes)",
    ])
    low = report.get("low_value_folds", [])
    for f in low[:10]:
        lines.append(f"  {f.get('operator_id', '?')}: gain={f.get('gain', 0)} targets={f.get('target_count', 0)}")
    if not low:
        lines.append("  (none)")
    lines.extend([
        "",
        "## Rejected by planner reason",
    ])
    for k, v in sorted(report.get("rejected_by_reason", {}).items(), key=lambda x: -x[1]):
        lines.append(f"  {k}: {v}")
    lines.extend([
        "",
        "## Rejected/blocked by operator",
    ])
    for op in sorted(set(report.get("rejected_by_operator", {})) | set(report.get("blocked_by_operator", {}))):
        rej = report.get("rejected_by_operator", {}).get(op, 0)
        blk = report.get("blocked_by_operator", {}).get(op, 0)
        lines.append(f"  {op}: rejected={rej} blocked={blk}")
    bfm = report.get("byte_fold_metrics")
    if bfm:
        lines.extend([
            "",
            "## Byte Fold metrics",
            f"  files_chunk_folded: {bfm.get('files_chunk_folded', 0)}",
            f"  unique_chunk_count: {bfm.get('unique_chunk_count', 0)}",
            f"  reused_chunk_count: {bfm.get('reused_chunk_count', 0)}",
            f"  chunk_reused_bytes: {bfm.get('chunk_reused_bytes', 0):,}",
            f"  chunk_dictionary_size_bytes: {bfm.get('chunk_dictionary_size_bytes', 0):,}",
        ])
    bfr = report.get("byte_fold_routing")
    if bfr:
        lines.append("")
        lines.append("## Byte Fold routing distribution")
        for route, cnt in sorted(bfr.items()):
            lines.append(f"  {route}: {cnt}")
    return "\n".join(lines)
