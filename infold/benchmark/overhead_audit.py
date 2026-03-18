"""
Phase 13A: Overhead audit for small archives.

Analyzes where bytes go in tiny archives: manifest, report, maps, snapshots, shared metadata.
"""

from __future__ import annotations

import zipfile
from pathlib import Path
from typing import Any


def audit_archive_overhead(archive_path: Path | str) -> dict[str, Any]:
    """
    Audit package overhead for an .infold archive.
    Returns breakdown: manifest_bytes, ledger_bytes, report_bytes, maps_bytes,
    snapshots_bytes, shared_metadata_bytes, total_overhead, total_archive.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        return {"error": "archive not found"}

    breakdown: dict[str, int] = {}
    total = 0
    with zipfile.ZipFile(archive, "r") as zf:
        for info in zf.infolist():
            name = info.filename
            size = info.file_size
            total += size
            if name == "manifest.json":
                breakdown["manifest"] = size
            elif name == "ledger.json":
                breakdown["ledger"] = size
            elif name == "integrity.json":
                breakdown["integrity"] = size
            elif name.startswith("reports/"):
                breakdown["reports"] = breakdown.get("reports", 0) + size
            elif name.startswith("maps/"):
                breakdown["maps"] = breakdown.get("maps", 0) + size
            elif name.startswith("snapshots/"):
                breakdown["snapshots"] = breakdown.get("snapshots", 0) + size
            elif name.startswith("shared/"):
                if "metadata_tables" in name or "chunk_index" in name:
                    breakdown["shared_metadata"] = breakdown.get("shared_metadata", 0) + size
                else:
                    breakdown["shared_content"] = breakdown.get("shared_content", 0) + size

    overhead = (
        breakdown.get("manifest", 0)
        + breakdown.get("ledger", 0)
        + breakdown.get("integrity", 0)
        + breakdown.get("reports", 0)
        + breakdown.get("maps", 0)
        + breakdown.get("snapshots", 0)
        + breakdown.get("shared_metadata", 0)
    )
    return {
        "breakdown": breakdown,
        "overhead_bytes": overhead,
        "total_archive_bytes": total,
        "overhead_pct": (overhead / total * 100) if total else 0,
    }


def audit_micro_overhead(archive_path: Path | str) -> dict[str, Any]:
    """
    Phase 14B: Detailed micro-mode overhead breakdown.
    Returns per-file sizes and guidance for compact encoding.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        return {"error": "archive not found"}
    breakdown: dict[str, int] = {}
    with zipfile.ZipFile(archive, "r") as zf:
        for info in zf.infolist():
            name = info.filename
            size = info.file_size
            breakdown[name] = size
    return {
        "breakdown": breakdown,
        "manifest_bytes": breakdown.get("manifest.json", 0),
        "ledger_bytes": breakdown.get("ledger.json", 0),
        "maps_bytes": sum(v for k, v in breakdown.items() if k.startswith("maps/")),
        "reports_bytes": sum(v for k, v in breakdown.items() if k.startswith("reports/")),
        "snapshots_bytes": sum(v for k, v in breakdown.items() if k.startswith("snapshots/")),
        "shared_metadata_bytes": sum(v for k, v in breakdown.items() if "metadata_tables" in k or "chunk_index" in k),
    }


def overhead_audit_to_text(audit: dict[str, Any]) -> str:
    """Human-readable overhead audit."""
    if "error" in audit:
        return f"Error: {audit['error']}"
    lines = [
        "Overhead Audit",
        "==============",
        "",
        f"Total archive: {audit.get('total_archive_bytes', 0):,} bytes",
        f"Overhead: {audit.get('overhead_bytes', 0):,} bytes ({audit.get('overhead_pct', 0):.1f}%)",
        "",
        "Breakdown:",
    ]
    for k, v in sorted(audit.get("breakdown", {}).items(), key=lambda x: -x[1]):
        lines.append(f"  {k}: {v:,} bytes")
    return "\n".join(lines)
