"""
Phase 8: Productization workflow commands.

- analyze: project analysis and concise summary (no archive)
- archive_workflow: create + validate + summary in one
- sync_capture: add snapshot + timeline summary
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


def analyze_project(source_path: Path | str, config: dict[str, Any] | None = None) -> dict[str, Any]:
    """
    Analyze a project: run fold, produce concise summary.
    No archive creation. Deterministic.
    """
    from infold.engine import run_fold
    from infold.reporting.report import build_report
    from infold.reporting.benchmark import _raw_size, _zip_size, _gzip_size

    source = Path(source_path).resolve()
    if not source.exists():
        raise FileNotFoundError(f"Source not found: {source}")

    if config is None:
        from infold.cli import load_config
        config = load_config()

    result = run_fold(source, config)
    sheet = result.project_sheet
    ledger = result.ledger
    report = build_report(result, config)

    raw = _raw_size(sheet)
    zip_b = _zip_size(sheet)
    gz = _gzip_size(sheet)
    logical_gain = ledger.total_bytes_saved
    physical_folded = raw - logical_gain

    fold_by_op: dict[str, int] = {}
    for rec in ledger.fold_records:
        fold_by_op[rec.operator_id] = fold_by_op.get(rec.operator_id, 0) + 1

    gain_contributors = sorted(
        report.get("operator_breakdown", {}).items(),
        key=lambda x: -x[1].get("gain", 0),
    )[:5]

    pfi = config.get("_fold_profile_info") or {}
    return {
        "source_path": str(source),
        "file_count": sheet.metrics.get("file_count", 0),
        "folder_count": sheet.metrics.get("folder_count", 0),
        "raw_bytes": raw,
        "zip_bytes": zip_b,
        "gzip_bytes": gz,
        "logical_gain_bytes": logical_gain,
        "physical_folded_size_bytes": physical_folded,
        "fold_count": ledger.total_folds,
        "fold_counts_by_operator": fold_by_op,
        "biggest_gain_contributors": [{"operator_id": op, "gain": d.get("gain", 0)} for op, d in gain_contributors],
        "fold_profile": pfi.get("fold_profile"),
        "fold_profile_mode": pfi.get("fold_profile_mode"),
        "fold_profile_reason": pfi.get("fold_profile_reason"),
        "exact_reconstruction_ok": result.exact_reconstruction_ok,
    }


def analyze_to_text(data: dict[str, Any]) -> str:
    """Human-readable analyze summary."""
    lines = [
        "Project Analysis",
        "===============",
        "",
        f"Source: {data.get('source_path', '?')}",
        f"Files: {data.get('file_count', 0)}  Folders: {data.get('folder_count', 0)}",
        "",
        "Size:",
        f"  Raw: {data.get('raw_bytes', 0):,} bytes",
        f"  ZIP: {data.get('zip_bytes', 0):,} bytes",
        f"  gzip: {data.get('gzip_bytes', 0):,} bytes",
        "",
        "Fold:",
        f"  Logical gain: {data.get('logical_gain_bytes', 0):,} bytes",
        f"  Physical folded: {data.get('physical_folded_size_bytes', 0):,} bytes",
        f"  Fold count: {data.get('fold_count', 0)}",
        "",
        "Profile:",
        f"  {data.get('fold_profile', '?')} ({data.get('fold_profile_mode', '?')}, {data.get('fold_profile_reason', '?')})",
        "",
        "Fold counts by operator:",
    ]
    for op, cnt in sorted(data.get("fold_counts_by_operator", {}).items(), key=lambda x: -x[1]):
        lines.append(f"  {op}: {cnt}")
    lines.append("")
    lines.append("Biggest gain contributors:")
    for c in data.get("biggest_gain_contributors", [])[:5]:
        lines.append(f"  {c.get('operator_id', '?')}: {c.get('gain', 0):,} bytes")
    lines.append("")
    lines.append(f"Unfold validation: {'OK' if data.get('exact_reconstruction_ok') else 'failed'} (engine; use archive workflow for full validate+reconstruct)")
    return "\n".join(lines)


def archive_workflow(
    source_path: Path | str,
    output_path: Path | str,
    *,
    config: dict[str, Any] | None = None,
    profile: str = "auto",
) -> dict[str, Any]:
    """
    Create archive, validate, produce summary. All-in-one workflow.
    Returns {archive_path, created, validated, validation_ok, summary}.
    """
    from infold.archive import create_archive, validate_archive
    from infold.archive.operations import explain_archive, explain_to_text

    source = Path(source_path).resolve()
    out = Path(output_path).resolve()
    if out.suffix != ".infold":
        out = out.with_suffix(".infold")

    if config is None:
        from infold.cli import load_config
        config = load_config()

    create_archive(source, out, config, profile=profile)
    ok, errors = validate_archive(out, mode="strict")
    info = explain_archive(out)
    summary_text = explain_to_text(info)

    return {
        "archive_path": str(out),
        "created": True,
        "validated": True,
        "validation_ok": ok,
        "validation_errors": errors,
        "summary": summary_text,
        "explain": info,
    }


def real_project_showcase(
    source_path: Path | str,
    output_dir: Path | str,
    *,
    config: dict[str, Any] | None = None,
    profile: str = "auto",
) -> dict[str, Any]:
    """
    Run full workflow: create, validate, reconstruct, compare.
    Save results to output_dir. Deterministic.
    Returns summary dict.
    """
    from datetime import datetime, timezone
    from infold.archive import create_archive, validate_archive, reconstruct_archive
    from infold.archive.operations import explain_archive, explain_to_text, stats_archive, stats_to_text
    from infold.reporting.benchmark import _raw_size, _zip_size, _gzip_size
    from infold.intake import scan_project
    from infold.parsers import parse_project

    source = Path(source_path).resolve()
    out_dir = Path(output_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)
    archive_path = out_dir / "archive.infold"
    restored_dir = out_dir / "restored"

    if config is None:
        from infold.cli import load_config
        config = load_config()

    create_archive(source, archive_path, config, profile=profile)
    valid_ok, valid_errors = validate_archive(archive_path, mode="strict")
    info = explain_archive(archive_path)
    stats = stats_archive(archive_path)
    reconstruct_archive(archive_path, restored_dir)

    # Diff (exclude common exclusions)
    import subprocess
    diff_result = subprocess.run(
        ["diff", "-rq", str(source), str(restored_dir)],
        capture_output=True,
        text=True,
    )
    diff_lines = (diff_result.stdout + diff_result.stderr).strip().split("\n") if diff_result.stdout or diff_result.stderr else []
    recon_ok = diff_result.returncode == 0

    pkg = info.get("package_summary", {})
    raw = pkg.get("original_size_bytes", 0)
    sheet = scan_project(source, exclude_patterns=config.get("project", {}).get("exclude_patterns"))
    parse_project(sheet)
    zip_b = _zip_size(sheet)
    gz = _gzip_size(sheet)

    summary = {
        "source_path": str(source),
        "archive_path": str(archive_path),
        "output_dir": str(out_dir),
        "raw_bytes": raw,
        "zip_bytes": zip_b,
        "gzip_bytes": gz,
        "logical_gain_bytes": pkg.get("logical_gain_bytes", 0),
        "physical_folded_size_bytes": pkg.get("physical_folded_size_bytes", 0),
        "fold_count": pkg.get("fold_count", 0),
        "fold_profile": info.get("fold_profile"),
        "fold_profile_reason": info.get("fold_profile_reason"),
        "validation_ok": valid_ok,
        "exact_reconstruction_ok": recon_ok,
        "created": datetime.now(timezone.utc).isoformat(),
    }

    # Save outputs
    (out_dir / "explain.txt").write_text(explain_to_text(info), encoding="utf-8")
    (out_dir / "stats.txt").write_text(stats_to_text(stats), encoding="utf-8")
    (out_dir / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    summary_md = _showcase_summary_to_md(summary, valid_errors, diff_lines)
    (out_dir / "summary.md").write_text(summary_md, encoding="utf-8")

    return summary


def _showcase_summary_to_md(summary: dict, valid_errors: list, diff_lines: list) -> str:
    """Markdown summary for showcase."""
    lines = [
        "# Infold Real Project Showcase",
        "",
        f"**Source:** {summary.get('source_path', '?')}",
        f"**Created:** {summary.get('created', '?')}",
        "",
        "## Size",
        f"| Metric | Value |",
        f"|--------|-------|",
        f"| Raw | {summary.get('raw_bytes', 0):,} bytes |",
        f"| ZIP | {summary.get('zip_bytes', 0):,} bytes |",
        f"| gzip | {summary.get('gzip_bytes', 0):,} bytes |",
        f"| Infold logical gain | {summary.get('logical_gain_bytes', 0):,} bytes |",
        f"| Infold physical folded | {summary.get('physical_folded_size_bytes', 0):,} bytes |",
        "",
        "## Status",
        f"- **Profile:** {summary.get('fold_profile', '?')} ({summary.get('fold_profile_reason', '?')})",
        f"- **Fold count:** {summary.get('fold_count', 0)}",
        f"- **Validation:** {'OK' if summary.get('validation_ok') else 'FAILED'}",
        f"- **Exact reconstruction:** {'OK' if summary.get('exact_reconstruction_ok') else 'FAILED'}",
        "",
    ]
    if valid_errors:
        lines.append("### Validation errors")
        for e in valid_errors:
            lines.append(f"- {e}")
        lines.append("")
    if diff_lines and not summary.get("exact_reconstruction_ok"):
        lines.append("### Diff (source vs restored)")
        for ln in diff_lines[:20]:
            lines.append(f"  {ln}")
        if len(diff_lines) > 20:
            lines.append(f"  ... +{len(diff_lines) - 20} more")
    return "\n".join(lines)


def sync_capture(
    sync_dir: Path | str,
    source_path: Path | str,
    *,
    config: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """
    Add snapshot to lineage, then produce timeline summary.
    Returns {snapshot, timeline, timeline_text}.
    """
    from infold.sync import create_and_add_snapshot, sync_timeline, sync_timeline_to_text

    sync = Path(sync_dir).resolve()
    source = Path(source_path).resolve()

    if config is None:
        from infold.cli import load_config
        config = load_config()

    add_result = create_and_add_snapshot(sync, source, config=config)
    timeline_data = sync_timeline(sync)
    timeline_text = sync_timeline_to_text(timeline_data)

    return {
        "snapshot": add_result.get("snapshot", {}),
        "timeline": timeline_data,
        "timeline_text": timeline_text,
    }
