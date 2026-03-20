"""
Lineage insights: first-seen, last-seen tracking, timeline summaries.

Phase 7: Turns sync from snapshot storage into lineage intelligence.
"""

from __future__ import annotations

import json
import zipfile
from pathlib import Path
from typing import Any

from infold.archive.operations import _family_signatures


def _extract_root(tmp: Path) -> Path:
    """Find package root (where manifest.json lives)."""
    manifest = tmp / "manifest.json"
    if manifest.exists():
        return tmp
    found = list(tmp.rglob("manifest.json"))
    return found[0].parent if found else tmp


def _signatures_from_archive(archive_path: Path) -> dict[str, dict[str, set[str]]]:
    """Extract family signatures from archive. Uses _family_signatures."""
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive_path, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        maps_path = root / "maps" / "reconstruction.json"
        if not maps_path.exists():
            return {}
        maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
        records = maps_data.get("records", [])
        return _family_signatures(root, records)


def compute_lineage_tracking(sync_dir: Path) -> dict[str, Any]:
    """
    Compute first_seen, last_seen, snapshot_count, present_in_latest for each family.
    Returns {operators: {op: {sig: {first_seen_snapshot, last_seen_snapshot, snapshot_count, present_in_latest, snapshots_seen_in}}}, snapshots, latest_snapshot_id}.
    """
    from infold.sync.operations import _load_lineage

    lineage = _load_lineage(sync_dir)
    snapshots = lineage.get("snapshots", [])
    sorted_snaps = sorted(
        [s for s in snapshots if isinstance(s, dict) and s.get("path")],
        key=lambda x: x.get("created", ""),
    )
    if not sorted_snaps:
        return {"operators": {}, "snapshots": [], "latest_snapshot_id": ""}

    all_sigs: dict[str, dict[str, dict[str, Any]]] = {
        "exact_repetition": {},
        "template_skeleton": {},
        "hierarchy_mirror": {},
        "dependency_motif": {},
        "byte_fold": {},
    }
    latest_id = sorted_snaps[-1].get("id", "")

    for snap in sorted_snaps:
        path = Path(snap.get("path", ""))
        if not path.exists():
            continue
        sid = snap.get("id", "")
        try:
            sigs = _signatures_from_archive(path)
        except Exception:
            sigs = {}
        for op, sig_map in sigs.items():
            if op not in all_sigs:
                all_sigs[op] = {}
            for sig, paths in sig_map.items():
                if sig not in all_sigs[op]:
                    all_sigs[op][sig] = {
                        "first_seen_snapshot": sid,
                        "last_seen_snapshot": sid,
                        "snapshot_count": 0,
                        "snapshots_seen_in": [],
                    }
                entry = all_sigs[op][sig]
                if sid not in entry["snapshots_seen_in"]:
                    entry["snapshots_seen_in"].append(sid)
                entry["last_seen_snapshot"] = sid
                entry["snapshot_count"] = len(entry["snapshots_seen_in"])

    for op, sig_map in all_sigs.items():
        for entry in sig_map.values():
            entry["present_in_latest"] = latest_id in (entry.get("snapshots_seen_in") or [])

    return {
        "operators": all_sigs,
        "snapshots": [{"id": s.get("id"), "created": s.get("created")} for s in sorted_snaps],
        "latest_snapshot_id": latest_id,
    }
