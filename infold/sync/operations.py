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
    Includes by_operator grouping for clearer output.
    """
    diff = sync_compare(archive_a, archive_b)
    added = {
        "duplicate_families": diff.get("duplicate_families_changed", {}).get("added", []),
        "template_families": diff.get("template_families_changed", {}).get("added", []),
        "hierarchy_templates": diff.get("hierarchy_templates_changed", {}).get("added", []),
        "dependency_motifs": diff.get("dependency_motifs_changed", {}).get("added", []),
        "byte_fold_families": diff.get("byte_fold_families_changed", {}).get("added", []),
    }
    removed = {
        "duplicate_families": diff.get("duplicate_families_changed", {}).get("removed", []),
        "template_families": diff.get("template_families_changed", {}).get("removed", []),
        "hierarchy_templates": diff.get("hierarchy_templates_changed", {}).get("removed", []),
        "dependency_motifs": diff.get("dependency_motifs_changed", {}).get("removed", []),
        "byte_fold_families": diff.get("byte_fold_families_changed", {}).get("removed", []),
    }
    fold_diff = diff.get("fold_counts_by_operator", {}).get("diff", {})
    by_operator: dict[str, dict[str, Any]] = {}
    for op, delta in fold_diff.items():
        if delta != 0:
            by_operator[op] = {"fold_count_diff": delta}
    for kind, items in added.items():
        op = kind.replace("_families", "").replace("_templates", "").replace("_motifs", "")
        op_map = {"duplicate": "exact_repetition", "template": "template_skeleton", "hierarchy": "hierarchy_mirror", "dependency": "dependency_motif", "byte_fold": "byte_fold"}
        op_id = op_map.get(op, op)
        if op_id not in by_operator:
            by_operator[op_id] = {}
        if items:
            by_operator[op_id]["added"] = items
    for kind, items in removed.items():
        op = kind.replace("_families", "").replace("_templates", "").replace("_motifs", "")
        op_map = {"duplicate": "exact_repetition", "template": "template_skeleton", "hierarchy": "hierarchy_mirror", "dependency": "dependency_motif", "byte_fold": "byte_fold"}
        op_id = op_map.get(op, op)
        if op_id not in by_operator:
            by_operator[op_id] = {}
        if items:
            by_operator[op_id]["removed"] = items

    bf_added = diff.get("byte_fold_families_changed", {}).get("added", [])
    bf_removed = diff.get("byte_fold_families_changed", {}).get("removed", [])
    bf_counts = diff.get("byte_fold_families", {})
    byte_fold_summary = {
        "added_count": len(bf_added),
        "removed_count": len(bf_removed),
        "a_count": bf_counts.get("a_count", 0),
        "b_count": bf_counts.get("b_count", 0),
        "fold_count_diff": fold_diff.get("byte_fold", 0),
    }

    return {
        "archive_a": diff["archive_a"],
        "archive_b": diff["archive_b"],
        "added": added,
        "removed": removed,
        "by_operator": dict(sorted(by_operator.items())),
        "counts": {
            "template_families": diff.get("template_families", {}),
            "duplicate_families": diff.get("duplicate_families", {}),
            "hierarchy_templates": diff.get("hierarchy_templates", {}),
            "dependency_motifs": diff.get("dependency_motifs", {}),
            "byte_fold_families": diff.get("byte_fold_families", {}),
        },
        "byte_fold_summary": byte_fold_summary,
        "logical_gain_diff": diff.get("logical_gain", {}).get("diff", 0),
        "fold_counts_diff": fold_diff,
    }


def sync_report_to_text(report: dict[str, Any]) -> str:
    """Human-readable sync report. Grouped by operator/family type. Deterministic."""
    lines = [
        "Sync Report",
        "===========",
        "",
        f"From: {report.get('archive_a', '?')}",
        f"To:   {report.get('archive_b', '?')}",
        "",
        f"Logical gain diff: {report.get('logical_gain_diff', 0):+,} bytes",
        "",
    ]
    bf_sum = report.get("byte_fold_summary", {})
    if bf_sum and (bf_sum.get("added_count", 0) or bf_sum.get("removed_count", 0) or bf_sum.get("fold_count_diff", 0)):
        lines.append("Byte Fold changes:")
        lines.append(f"  added: {bf_sum.get('added_count', 0)} removed: {bf_sum.get('removed_count', 0)}")
        if bf_sum.get("fold_count_diff"):
            lines.append(f"  fold count diff: {bf_sum['fold_count_diff']:+,}")
        lines.append("")
    fold_diff = report.get("fold_counts_diff", {})
    if fold_diff:
        lines.append("Fold count changes by operator:")
        for op, delta in sorted(fold_diff.items()):
            if delta != 0:
                lines.append(f"  {op}: {delta:+,}")
        lines.append("")

    by_op = report.get("by_operator", {})
    if by_op:
        lines.append("Changes by operator/family:")
        for op, ch in sorted(by_op.items()):
            has_changes = ch.get("added") or ch.get("removed") or ch.get("fold_count_diff")
            if not has_changes:
                continue
            lines.append(f"  --- {op} ---")
            if ch.get("added"):
                lines.append(f"    Added: {len(ch['added'])}")
                for s in (ch["added"] or [])[:5]:
                    lines.append(f"      - {s}")
                if len(ch.get("added", [])) > 5:
                    lines.append(f"      ... +{len(ch['added']) - 5} more")
            if ch.get("removed"):
                lines.append(f"    Removed: {len(ch['removed'])}")
                for s in (ch["removed"] or [])[:5]:
                    lines.append(f"      - {s}")
                if len(ch.get("removed", [])) > 5:
                    lines.append(f"      ... +{len(ch['removed']) - 5} more")
            if ch.get("fold_count_diff"):
                lines.append(f"    Fold count diff: {ch['fold_count_diff']:+,}")
            lines.append("")
    else:
        lines.append("Added:")
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
    return "\n".join(lines).rstrip()


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


def validate_sync(sync_dir: Path | str) -> dict[str, Any]:
    """
    Validate lineage structure, snapshot archive existence, metadata consistency.
    Returns {valid: bool, errors: [...], warnings: [...]}.
    """
    sync = Path(sync_dir).resolve()
    errors: list[str] = []
    warnings: list[str] = []

    # Structure
    if not _lineage_path(sync).exists():
        return {"valid": False, "errors": ["lineage.json not found"], "warnings": []}
    try:
        lineage = _load_lineage(sync)
    except json.JSONDecodeError as e:
        return {"valid": False, "errors": [f"lineage.json invalid JSON: {e}"], "warnings": []}

    if "snapshots" not in lineage:
        errors.append("lineage missing 'snapshots'")
    snapshots = lineage.get("snapshots", [])
    if not isinstance(snapshots, list):
        errors.append("'snapshots' must be a list")

    # Snapshot archives exist
    missing: list[str] = []
    for i, s in enumerate(snapshots):
        if not isinstance(s, dict):
            errors.append(f"snapshot[{i}] is not a dict")
            continue
        path = s.get("path")
        if not path:
            errors.append(f"snapshot[{i}] missing 'path'")
            continue
        if not Path(path).exists():
            missing.append(path)

    if missing:
        errors.extend([f"archive not found: {p}" for p in missing])

    # Metadata consistency (where archives exist)
    for i, s in enumerate(snapshots):
        if not isinstance(s, dict) or not s.get("path"):
            continue
        p = Path(s["path"])
        if not p.exists():
            continue
        try:
            from infold.archive.operations import explain_archive
            info = explain_archive(p)
            pkg = info["package_summary"]
            if s.get("logical_gain_bytes") != pkg.get("logical_gain_bytes"):
                warnings.append(f"snapshot[{i}] {s.get('id','?')}: lineage logical_gain {s.get('logical_gain_bytes')} != manifest {pkg.get('logical_gain_bytes')}")
            if s.get("fold_count") != pkg.get("fold_count"):
                warnings.append(f"snapshot[{i}] {s.get('id','?')}: lineage fold_count {s.get('fold_count')} != manifest {pkg.get('fold_count')}")
        except Exception as e:
            warnings.append(f"snapshot[{i}] {s.get('path','?')}: could not verify manifest: {e}")

    return {
        "valid": len(errors) == 0,
        "errors": errors,
        "warnings": warnings,
        "sync_dir": str(sync),
        "snapshot_count": len(snapshots),
    }


def sync_search(
    sync_dir: Path | str,
    *,
    snapshot_id: str | None = None,
    snapshot_path: str | None = None,
    created_after: str | None = None,
    created_before: str | None = None,
    with_lineage: bool = False,
    **search_kw: Any,
) -> dict[str, Any]:
    """
    Search across all snapshots in lineage. Reuses search_archives.
    Filters: snapshot_id, snapshot_path (substring), created_after, created_before.
    Pass-through search_kw to search_archives (operator, path, family, etc.).
    When with_lineage=True, adds lineage_tracking and snapshot_id to each match.
    """
    from infold.archive import search_archives

    sync = Path(sync_dir).resolve()
    lineage = _load_lineage(sync)
    snapshots = lineage.get("snapshots", [])
    path_to_snap: dict[str, dict] = {}
    paths: list[Path] = []
    for s in snapshots:
        if not isinstance(s, dict):
            continue
        path = s.get("path")
        if not path or not Path(path).exists():
            continue
        if snapshot_id and s.get("id") != snapshot_id:
            continue
        if snapshot_path and (snapshot_path or "").lower() not in (path or "").lower():
            continue
        created = s.get("created", "")
        if created_after and created < created_after:
            continue
        if created_before and created > created_before:
            continue
        paths.append(Path(path))
        path_to_snap[str(Path(path).resolve())] = s

    if not paths:
        return {
            "paths": [],
            "query": {"snapshot_id": snapshot_id, "snapshot_path": snapshot_path, "with_lineage": with_lineage, **search_kw},
            "match_count": 0,
            "matches": [],
            "summary": {"total_archives_searched": 0, "total_matches": 0, "matches_by_operator": {}, "matches_by_family": {}},
        }

    result = search_archives(paths, **search_kw)
    sorted_snaps = sorted(
        [s for s in lineage.get("snapshots", []) if isinstance(s, dict) and s.get("created")],
        key=lambda x: x.get("created", ""),
    )
    latest_id = sorted_snaps[-1].get("id") if sorted_snaps else None
    for m in result.get("matches", []):
        ap = m.get("archive_path", "")
        snap = path_to_snap.get(str(Path(ap).resolve())) if ap else None
        if snap:
            m["snapshot_id"] = snap.get("id")
            m["snapshot_created"] = snap.get("created")
            m["present_in_latest"] = snap.get("id") == latest_id
    if with_lineage:
        from infold.sync.lineage_insights import compute_lineage_tracking
        tracking = compute_lineage_tracking(sync)
        result["lineage_tracking"] = tracking
        # Enrich each match with first_seen, last_seen when we can compute signature
        import hashlib
        ops = tracking.get("operators", {})
        for m in result.get("matches", []):
            if m.get("match_type") != "fold":
                continue
            op = m.get("operator_id", "")
            paths = m.get("paths") or m.get("roots_or_paths") or []
            if not paths or op not in ops:
                continue
            paths_tuple = tuple(sorted(str(p) for p in paths))
            sig = hashlib.sha256(json.dumps(paths_tuple).encode()).hexdigest()[:16]
            entry = ops.get(op, {}).get(sig)
            if entry:
                m["first_seen"] = entry.get("first_seen_snapshot")
                m["last_seen"] = entry.get("last_seen_snapshot")
                m["snapshots_seen_in"] = sorted(entry.get("snapshots_seen_in", []))
                m["snapshot_count"] = entry.get("snapshot_count", 0)
    return result


def sync_timeline(sync_dir: Path | str) -> dict[str, Any]:
    """
    Lineage timeline: total snapshots, gain/size/fold over time, operator usage over time.
    Phase 7: timeline intelligence.
    """
    sync = Path(sync_dir).resolve()
    lineage = _load_lineage(sync)
    snapshots = lineage.get("snapshots", [])
    sorted_snaps = sorted(
        [s for s in snapshots if isinstance(s, dict) and s.get("created")],
        key=lambda x: x.get("created", ""),
    )
    total_gain = sum(s.get("logical_gain_bytes", 0) for s in sorted_snaps)
    phys_sizes = [s.get("physical_folded_size_bytes", 0) for s in sorted_snaps]
    avg_phys = sum(phys_sizes) / len(phys_sizes) if phys_sizes else 0
    fold_over_time = [{"id": s.get("id"), "created": s.get("created"), "fold_count": s.get("fold_count"), "logical_gain_bytes": s.get("logical_gain_bytes"), "physical_folded_size_bytes": s.get("physical_folded_size_bytes")} for s in sorted_snaps]
    op_over_time: dict[str, list[dict[str, Any]]] = {}
    for s in sorted_snaps:
        for op, cnt in (s.get("fold_counts_by_operator") or {}).items():
            if op not in op_over_time:
                op_over_time[op] = []
            op_over_time[op].append({"id": s.get("id"), "created": s.get("created"), "count": cnt})
    return {
        "sync_dir": str(sync),
        "source_path": lineage.get("source_path", ""),
        "total_snapshots": len(snapshots),
        "total_logical_gain_bytes": total_gain,
        "avg_physical_folded_size_bytes": round(avg_phys, 0),
        "fold_over_time": fold_over_time,
        "logical_gain_over_time": [{"id": s.get("id"), "created": s.get("created"), "logical_gain_bytes": s.get("logical_gain_bytes")} for s in sorted_snaps],
        "physical_size_over_time": [{"id": s.get("id"), "created": s.get("created"), "physical_folded_size_bytes": s.get("physical_folded_size_bytes")} for s in sorted_snaps],
        "operator_usage_over_time": op_over_time,
    }


def sync_timeline_to_text(data: dict[str, Any]) -> str:
    """Human-readable timeline with highlights."""
    lines = [
        "Sync Timeline",
        "=============",
        "",
        f"Sync dir: {data.get('sync_dir', '?')}",
        f"Source: {data.get('source_path', '?')}",
        f"Snapshots: {data.get('total_snapshots', 0)}",
        f"Total logical gain: {data.get('total_logical_gain_bytes', 0):,} bytes",
        f"Avg physical size: {data.get('avg_physical_folded_size_bytes', 0):,.0f} bytes",
        "",
    ]
    fold_over = data.get("fold_over_time", [])
    if fold_over:
        newest = fold_over[-1]
        oldest = fold_over[0]
        lines.append("Highlights:")
        lines.append(f"  Newest: [{newest.get('id','?')}] {newest.get('created','?')[:19]} folds={newest.get('fold_count',0)} gain={newest.get('logical_gain_bytes',0):,}")
        if len(fold_over) > 1:
            lines.append(f"  Oldest: [{oldest.get('id','?')}] {oldest.get('created','?')[:19]} folds={oldest.get('fold_count',0)} gain={oldest.get('logical_gain_bytes',0):,}")
        lines.append("")
    lines.append("Fold count over time:")
    for row in fold_over[:15]:
        lines.append(f"  [{row.get('id','?')}] {row.get('created','?')[:19]} folds={row.get('fold_count',0)} gain={row.get('logical_gain_bytes',0):,}")
    if len(fold_over) > 15:
        lines.append(f"  ... +{len(fold_over) - 15} more")
    return "\n".join(lines)


def sync_trace(
    sync_dir: Path | str,
    *,
    family: str | None = None,
    operator: str | None = None,
) -> dict[str, Any]:
    """
    Trace family lifecycle: first_seen, last_seen, present_in_latest, snapshots_seen_in.
    Filter by family type (duplicate, template, hierarchy, dependency, byte_fold) or operator.
    """
    from infold.sync.lineage_insights import compute_lineage_tracking

    sync = Path(sync_dir).resolve()
    tracking = compute_lineage_tracking(sync)
    ops = tracking.get("operators", {})
    op_map = {"duplicate": "exact_repetition", "template": "template_skeleton", "hierarchy": "hierarchy_mirror", "dependency": "dependency_motif", "byte_fold": "byte_fold"}
    target_op = None
    if family:
        target_op = op_map.get(family, family)
    elif operator:
        target_op = operator
    items: list[dict[str, Any]] = []
    for op, sig_map in ops.items():
        if target_op and op != target_op:
            continue
        for sig, entry in sig_map.items():
            items.append({
                "operator": op,
                "signature": sig,
                "first_seen_snapshot": entry.get("first_seen_snapshot"),
                "last_seen_snapshot": entry.get("last_seen_snapshot"),
                "snapshot_count": entry.get("snapshot_count", 0),
                "present_in_latest": entry.get("present_in_latest", False),
                "snapshots_seen_in": sorted(entry.get("snapshots_seen_in", [])),
            })
    items.sort(key=lambda x: (x["operator"], x["signature"]))
    return {
        "sync_dir": str(sync),
        "filter_family": family,
        "filter_operator": operator,
        "items": items,
        "latest_snapshot_id": tracking.get("latest_snapshot_id", ""),
    }


def sync_trace_to_text(data: dict[str, Any]) -> str:
    """Human-readable trace."""
    lines = [
        "Sync Trace",
        "==========",
        "",
        f"Sync dir: {data.get('sync_dir', '?')}",
        f"Filter: family={data.get('filter_family', '')} operator={data.get('filter_operator', '')}",
        f"Latest: {data.get('latest_snapshot_id', '')}",
        "",
    ]
    for item in data.get("items", [])[:20]:
        lines.append(f"  [{item.get('operator','?')}] sig={item.get('signature','?')[:12]}... first={item.get('first_seen_snapshot')} last={item.get('last_seen_snapshot')} count={item.get('snapshot_count')} in_latest={item.get('present_in_latest')}")
    if len(data.get("items", [])) > 20:
        lines.append(f"  ... +{len(data['items']) - 20} more")
    return "\n".join(lines)


def sync_lineage_report(sync_dir: Path | str) -> dict[str, Any]:
    """
    Change-focused lineage report: added/removed/stable families by snapshot interval.
    Phase 7: change-focused reporting.
    """
    sync = Path(sync_dir).resolve()
    lineage = _load_lineage(sync)
    snapshots = lineage.get("snapshots", [])
    sorted_snaps = sorted(
        [s for s in snapshots if isinstance(s, dict) and s.get("path")],
        key=lambda x: x.get("created", ""),
    )
    intervals: list[dict[str, Any]] = []
    for i in range(1, len(sorted_snaps)):
        a_path = Path(sorted_snaps[i - 1]["path"])
        b_path = Path(sorted_snaps[i]["path"])
        if not a_path.exists() or not b_path.exists():
            continue
        diff = sync_compare(a_path, b_path)
        added = {
            "duplicate": len(diff.get("duplicate_families_changed", {}).get("added", [])),
            "template": len(diff.get("template_families_changed", {}).get("added", [])),
            "hierarchy": len(diff.get("hierarchy_templates_changed", {}).get("added", [])),
            "dependency": len(diff.get("dependency_motifs_changed", {}).get("added", [])),
            "byte_fold": len(diff.get("byte_fold_families_changed", {}).get("added", [])),
        }
        removed = {
            "duplicate": len(diff.get("duplicate_families_changed", {}).get("removed", [])),
            "template": len(diff.get("template_families_changed", {}).get("removed", [])),
            "hierarchy": len(diff.get("hierarchy_templates_changed", {}).get("removed", [])),
            "dependency": len(diff.get("dependency_motifs_changed", {}).get("removed", [])),
            "byte_fold": len(diff.get("byte_fold_families_changed", {}).get("removed", [])),
        }
        intervals.append({
            "from_id": sorted_snaps[i - 1].get("id"),
            "to_id": sorted_snaps[i].get("id"),
            "from_created": sorted_snaps[i - 1].get("created"),
            "to_created": sorted_snaps[i].get("created"),
            "added_by_operator": added,
            "removed_by_operator": removed,
            "logical_gain_diff": diff.get("logical_gain", {}).get("diff", 0),
        })
    return {
        "sync_dir": str(sync),
        "intervals": intervals,
        "total_intervals": len(intervals),
    }


def sync_lineage_report_to_text(data: dict[str, Any]) -> str:
    """Human-readable lineage report."""
    lines = [
        "Lineage Report (Change-focused)",
        "===============================",
        "",
        f"Sync dir: {data.get('sync_dir', '?')}",
        f"Intervals: {data.get('total_intervals', 0)}",
        "",
    ]
    for iv in data.get("intervals", [])[:10]:
        lines.append(f"  {iv.get('from_id','?')} -> {iv.get('to_id','?')}:")
        a = iv.get("added_by_operator", {})
        r = iv.get("removed_by_operator", {})
        for op in ["duplicate", "template", "hierarchy", "dependency", "byte_fold"]:
            if a.get(op, 0) or r.get(op, 0):
                lines.append(f"    {op}: +{a.get(op,0)} -{r.get(op,0)}")
        lines.append(f"    gain_diff: {iv.get('logical_gain_diff',0):+,}")
        lines.append("")
    return "\n".join(lines).rstrip()


def sync_summary(sync_dir: Path | str) -> dict[str, Any]:
    """
    Summary of lineage: total snapshots, total logical gain, fold counts over time,
    top operators, newest/oldest snapshot.
    """
    sync = Path(sync_dir).resolve()
    lineage = _load_lineage(sync)
    snapshots = lineage.get("snapshots", [])
    total_gain = sum(s.get("logical_gain_bytes", 0) for s in snapshots if isinstance(s, dict))
    op_totals: dict[str, int] = {}
    for s in snapshots:
        if not isinstance(s, dict):
            continue
        for op, cnt in (s.get("fold_counts_by_operator") or {}).items():
            op_totals[op] = op_totals.get(op, 0) + cnt

    sorted_snaps = sorted(
        [s for s in snapshots if isinstance(s, dict) and s.get("created")],
        key=lambda x: x.get("created", ""),
    )
    newest = sorted_snaps[-1] if sorted_snaps else None
    oldest = sorted_snaps[0] if sorted_snaps else None

    byte_fold_total = op_totals.get("byte_fold", 0)
    return {
        "sync_dir": str(sync),
        "source_path": lineage.get("source_path", ""),
        "total_snapshots": len(snapshots),
        "total_logical_gain_bytes": total_gain,
        "fold_counts_over_time": [{"id": s.get("id"), "created": s.get("created"), "fold_count": s.get("fold_count"), "logical_gain_bytes": s.get("logical_gain_bytes")} for s in sorted_snaps],
        "top_operators": sorted([{"operator_id": k, "total": v} for k, v in op_totals.items()], key=lambda x: -x["total"])[:10],
        "byte_fold_total": byte_fold_total,
        "newest_snapshot": newest,
        "oldest_snapshot": oldest,
    }


def sync_summary_to_text(data: dict[str, Any]) -> str:
    """Human-readable sync summary."""
    lines = [
        "Sync Summary",
        "============",
        "",
        f"Sync dir:    {data.get('sync_dir', '?')}",
        f"Source:      {data.get('source_path', '?')}",
        f"Snapshots:   {data.get('total_snapshots', 0)}",
        f"Total gain:  {data.get('total_logical_gain_bytes', 0):,} bytes",
        "",
        "Top operators:",
    ]
    for t in data.get("top_operators", [])[:5]:
        lines.append(f"  {t.get('operator_id', '?')}: {t.get('total', 0)}")
    ns = data.get("newest_snapshot")
    if ns:
        lines.append("")
        lines.append(f"Newest: [{ns.get('id','?')}] {ns.get('created','?')} gain={ns.get('logical_gain_bytes',0)} folds={ns.get('fold_count',0)}")
    os = data.get("oldest_snapshot")
    if os and os != ns:
        lines.append(f"Oldest: [{os.get('id','?')}] {os.get('created','?')} gain={os.get('logical_gain_bytes',0)} folds={os.get('fold_count',0)}")
    return "\n".join(lines)


def _get_latest_and_previous(sync_dir: Path) -> tuple[dict | None, dict | None]:
    """Get latest and previous snapshots by created timestamp. Deterministic."""
    lineage = _load_lineage(sync_dir)
    snapshots = [s for s in lineage.get("snapshots", []) if isinstance(s, dict) and s.get("created")]
    if not snapshots:
        return None, None
    sorted_snaps = sorted(snapshots, key=lambda x: x.get("created", ""))
    latest = sorted_snaps[-1]
    previous = sorted_snaps[-2] if len(sorted_snaps) >= 2 else None
    return latest, previous


def resolve_snapshot_ref(sync_dir: Path | str, ref: str) -> Path | None:
    """
    Resolve 'latest' or 'previous' to archive path. Returns None if not found.
    """
    sync = Path(sync_dir).resolve()
    ref_lower = (ref or "").lower()
    if ref_lower not in ("latest", "previous"):
        return Path(ref) if ref else None
    latest, previous = _get_latest_and_previous(sync)
    if ref_lower == "latest" and latest:
        return Path(latest["path"])
    if ref_lower == "previous" and previous:
        return Path(previous["path"])
    return None
