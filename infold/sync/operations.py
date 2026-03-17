"""
Infold Sync v0.1: versioned archive snapshots, lineage manifest.

- init_sync: create sync directory with lineage manifest
- add_snapshot: create archive, add to lineage
- list_snapshots: list snapshots from lineage
- sync_compare: compare two snapshots (uses compare_archives)
- sync_report: report added/removed/changed fold families
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

LINEAGE_VERSION = "1.0"
LINEAGE_FILENAME = "lineage.json"
SNAPSHOTS_DIR = "snapshots"


def _lineage_path(sync_dir: Path) -> Path:
    return sync_dir / LINEAGE_FILENAME


def _snapshots_dir(sync_dir: Path) -> Path:
    return sync_dir / SNAPSHOTS_DIR


def init_sync(
    sync_dir: Path | str,
    *,
    source_path: Path | str | None = None,
    project_id: str | None = None,
) -> dict[str, Any]:
    """
    Initialize sync directory with lineage manifest.
    Creates sync_dir/lineage.json and sync_dir/snapshots/.
    """
    sync = Path(sync_dir).resolve()
    sync.mkdir(parents=True, exist_ok=True)
    _snapshots_dir(sync).mkdir(exist_ok=True)
    lineage = {
        "version": LINEAGE_VERSION,
        "project_id": project_id or "",
        "source_path": str(source_path) if source_path else "",
        "snapshots": [],
        "created": datetime.now(timezone.utc).isoformat(),
    }
    _lineage_path(sync).write_text(json.dumps(lineage, indent=2), encoding="utf-8")
    return {"sync_dir": str(sync), "lineage": lineage}


def _load_lineage(sync_dir: Path) -> dict[str, Any]:
    """Load lineage manifest. Raises if missing."""
    p = _lineage_path(sync_dir)
    if not p.exists():
        raise FileNotFoundError(f"Lineage not found: {p}. Run sync init first.")
    return json.loads(p.read_text(encoding="utf-8"))


def _save_lineage(sync_dir: Path, lineage: dict[str, Any]) -> None:
    _lineage_path(sync_dir).write_text(json.dumps(lineage, indent=2), encoding="utf-8")


def add_snapshot(
    sync_dir: Path | str,
    archive_path: Path | str,
    *,
    snapshot_id: str | None = None,
) -> dict[str, Any]:
    """
    Add an existing archive as a snapshot to the lineage.
    Returns updated lineage entry for the new snapshot.
    """
    from infold.archive.operations import explain_archive

    sync = Path(sync_dir).resolve()
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    lineage = _load_lineage(sync)
    info = explain_archive(archive)
    pkg = info["package_summary"]
    snapshots = lineage.get("snapshots", [])
    sid = snapshot_id or f"v{len(snapshots) + 1}"
    created = datetime.now(timezone.utc).isoformat()
    entry = {
        "id": sid,
        "path": str(archive),
        "created": created,
        "logical_gain_bytes": pkg.get("logical_gain_bytes", 0),
        "physical_folded_size_bytes": pkg.get("physical_folded_size_bytes", 0),
        "fold_count": pkg.get("fold_count", 0),
        "fold_counts_by_operator": info.get("fold_counts_by_operator", {}),
    }
    snapshots.append(entry)
    lineage["snapshots"] = snapshots
    _save_lineage(sync, lineage)
    return {"snapshot": entry, "lineage": lineage}


def create_and_add_snapshot(
    sync_dir: Path | str,
    source_path: Path | str,
    *,
    snapshot_id: str | None = None,
    config: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """
    Create archive from source, store in sync_dir/snapshots/, add to lineage.
    Deterministic. Reuses create_archive.
    """
    from infold.archive import create_archive

    sync = Path(sync_dir).resolve()
    source = Path(source_path).resolve()
    if not source.exists():
        raise FileNotFoundError(f"Source not found: {source}")
    lineage = _load_lineage(sync)
    snapshots = lineage.get("snapshots", [])
    sid = snapshot_id or f"v{len(snapshots) + 1}"
    ts = datetime.now(timezone.utc).strftime("%Y%m%d_%H%M%S")
    out_archive = _snapshots_dir(sync) / f"{sid}_{ts}.infold"
    create_archive(source, out_archive, config)
    return add_snapshot(sync, out_archive, snapshot_id=sid)


def list_snapshots(sync_dir: Path | str) -> dict[str, Any]:
    """List snapshots from lineage. Deterministic."""
    sync = Path(sync_dir).resolve()
    lineage = _load_lineage(sync)
    snapshots = lineage.get("snapshots", [])
    return {
        "sync_dir": str(sync),
        "source_path": lineage.get("source_path", ""),
        "snapshot_count": len(snapshots),
        "snapshots": snapshots,
    }


def sync_compare(
    archive_a: Path | str,
    archive_b: Path | str,
) -> dict[str, Any]:
    """
    Compare two snapshots structurally. Uses existing compare_archives.
    """
    from infold.archive.operations import compare_archives

    return compare_archives(archive_a, archive_b)


def sync_report(
    archive_a: Path | str,
    archive_b: Path | str,
) -> dict[str, Any]:
    """
    Report added/removed/changed fold families between two snapshots.
    Uses compare_archives and formats for human/script consumption.
    """
    diff = sync_compare(archive_a, archive_b)
    return {
        "archive_a": diff["archive_a"],
        "archive_b": diff["archive_b"],
        "added": {
            "template_families": diff.get("template_families_changed", {}).get("added", []),
            "hierarchy_templates": diff.get("hierarchy_templates_changed", {}).get("added", []),
            "dependency_motifs": diff.get("dependency_motifs_changed", {}).get("added", []),
        },
        "removed": {
            "template_families": diff.get("template_families_changed", {}).get("removed", []),
            "hierarchy_templates": diff.get("hierarchy_templates_changed", {}).get("removed", []),
            "dependency_motifs": diff.get("dependency_motifs_changed", {}).get("removed", []),
        },
        "counts": {
            "template_families": diff.get("template_families", {}),
            "duplicate_families": diff.get("duplicate_families", {}),
            "hierarchy_templates": diff.get("hierarchy_templates", {}),
            "dependency_motifs": diff.get("dependency_motifs", {}),
        },
        "logical_gain_diff": diff.get("logical_gain", {}).get("diff", 0),
        "fold_counts_diff": diff.get("fold_counts_by_operator", {}).get("diff", {}),
    }


def sync_report_to_text(report: dict[str, Any]) -> str:
    """Human-readable sync report."""
    lines = [
        "Sync Report",
        "===========",
        "",
        f"From: {report.get('archive_a', '?')}",
        f"To:   {report.get('archive_b', '?')}",
        "",
        f"Logical gain diff: {report.get('logical_gain_diff', 0):+,} bytes",
        "",
        "Added:",
    ]
    for kind, items in report.get("added", {}).items():
        lines.append(f"  {kind}: {len(items)}")
        for s in items[:5]:
            lines.append(f"    - {s}")
        if len(items) > 5:
            lines.append(f"    ... +{len(items) - 5} more")
    lines.append("")
    lines.append("Removed:")
    for kind, items in report.get("removed", {}).items():
        lines.append(f"  {kind}: {len(items)}")
        for s in items[:5]:
            lines.append(f"    - {s}")
        if len(items) > 5:
            lines.append(f"    ... +{len(items) - 5} more")
    return "\n".join(lines)


def sync_list_to_text(data: dict[str, Any]) -> str:
    """Human-readable sync list."""
    lines = [
        "Sync Snapshots",
        "=============",
        "",
        f"Sync dir: {data.get('sync_dir', '?')}",
        f"Source:   {data.get('source_path', '?')}",
        f"Count:    {data.get('snapshot_count', 0)}",
        "",
    ]
    for s in data.get("snapshots", []):
        lines.append(f"  [{s.get('id', '?')}] {s.get('path', '?')}")
        lines.append(f"    created={s.get('created', '?')} gain={s.get('logical_gain_bytes', 0)} folds={s.get('fold_count', 0)}")
    return "\n".join(lines)
