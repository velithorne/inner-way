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


def audit_archive_overhead_phase21a(archive_path: Path | str) -> dict[str, Any]:
    """
    Phase 21A: Granular package overhead audit for density attack.

    Breaks archive size into:
    - manifest
    - ledger
    - integrity
    - maps_reconstruction
    - maps_chunk_reconstruction
    - shared_operator_artifacts (template, mutation_chain, echo, exact, hierarchy, dependency, symbols)
    - shared_anchors
    - shared_metadata_tables (path_table, chunk_index)
    - shared_chunks (binary)
    - reports_json
    - reports_txt
    - snapshots_inventory
    - snapshots_passthrough
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        return {"error": "archive not found"}

    per_file: dict[str, int] = {}
    components: dict[str, int] = {}
    total = 0

    with zipfile.ZipFile(archive, "r") as zf:
        for info in zf.infolist():
            name = info.filename
            size = info.file_size
            total += size
            per_file[name] = size

            if name == "manifest.json":
                components["manifest"] = components.get("manifest", 0) + size
            elif name == "ledger.json":
                components["ledger"] = components.get("ledger", 0) + size
            elif name == "integrity.json":
                components["integrity"] = components.get("integrity", 0) + size
            elif name == "maps/reconstruction.json":
                components["maps_reconstruction"] = components.get("maps_reconstruction", 0) + size
            elif name == "maps/chunk_reconstruction.json":
                components["maps_chunk_reconstruction"] = components.get("maps_chunk_reconstruction", 0) + size
            elif name.startswith("maps/"):
                components["maps_other"] = components.get("maps_other", 0) + size
            elif name.startswith("shared/"):
                if "metadata_tables" in name or "path_table" in name:
                    components["shared_metadata_tables"] = components.get("shared_metadata_tables", 0) + size
                elif "chunk_index" in name:
                    components["shared_chunk_index"] = components.get("shared_chunk_index", 0) + size
                elif "chunks/" in name and name.endswith(".bin"):
                    components["shared_chunks"] = components.get("shared_chunks", 0) + size
                elif "anchors.json" in name:
                    components["shared_anchors"] = components.get("shared_anchors", 0) + size
                elif "template_" in name or "mutation_chain_" in name or "echo_" in name:
                    components["shared_operator_artifacts"] = components.get("shared_operator_artifacts", 0) + size
                elif "exact_" in name or "hierarchy_" in name or "dependency_motif_" in name or "symbols_" in name:
                    components["shared_operator_artifacts"] = components.get("shared_operator_artifacts", 0) + size
                else:
                    components["shared_other"] = components.get("shared_other", 0) + size
            elif name == "reports/report.json":
                components["reports_json"] = components.get("reports_json", 0) + size
            elif name == "reports/report.txt":
                components["reports_txt"] = components.get("reports_txt", 0) + size
            elif name.startswith("reports/"):
                components["reports_other"] = components.get("reports_other", 0) + size
            elif name == "snapshots/inventory.json":
                components["snapshots_inventory"] = components.get("snapshots_inventory", 0) + size
            elif name == "snapshots/passthrough.json":
                components["snapshots_passthrough"] = components.get("snapshots_passthrough", 0) + size
            elif name.startswith("snapshots/"):
                components["snapshots_other"] = components.get("snapshots_other", 0) + size

    overhead = (
        components.get("manifest", 0)
        + components.get("ledger", 0)
        + components.get("integrity", 0)
        + components.get("maps_reconstruction", 0)
        + components.get("maps_chunk_reconstruction", 0)
        + components.get("maps_other", 0)
        + components.get("shared_metadata_tables", 0)
        + components.get("shared_chunk_index", 0)
        + components.get("shared_anchors", 0)
        + components.get("shared_operator_artifacts", 0)
        + components.get("shared_other", 0)
        + components.get("reports_json", 0)
        + components.get("reports_txt", 0)
        + components.get("reports_other", 0)
        + components.get("snapshots_inventory", 0)
        + components.get("snapshots_passthrough", 0)
        + components.get("snapshots_other", 0)
    )
    # shared_chunks is content, not overhead
    content_bytes = components.get("shared_chunks", 0)

    return {
        "breakdown": components,
        "per_file": per_file,
        "overhead_bytes": overhead,
        "content_bytes": content_bytes,
        "total_archive_bytes": total,
        "overhead_pct": (overhead / total * 100) if total else 0,
        "dominant": sorted(
            [(k, v) for k, v in components.items() if v > 0],
            key=lambda x: -x[1],
        )[:8],
        "focus_components": {
            "snapshots_passthrough": components.get("snapshots_passthrough", 0),
            "shared_operator_artifacts": components.get("shared_operator_artifacts", 0),
            "shared_metadata_tables": components.get("shared_metadata_tables", 0),
            "shared_anchors": components.get("shared_anchors", 0),
        },
    }


def audit_archive_overhead_phase21b(
    archive_path: Path | str,
    before_audit: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """
    Phase 21B: Overhead-source-focused audit with before/after comparison.

    Returns audit plus component deltas when before_audit is provided.
    """
    audit = audit_archive_overhead_phase21a(archive_path)
    if "error" in audit:
        return audit
    if before_audit is not None and "error" not in before_audit:
        focus = ["snapshots_passthrough", "shared_operator_artifacts", "shared_metadata_tables", "shared_anchors"]
        before_breakdown = before_audit.get("breakdown", {})
        after_breakdown = audit.get("breakdown", {})
        component_deltas = {}
        for k in focus:
            b = before_breakdown.get(k, 0)
            a = after_breakdown.get(k, 0)
            component_deltas[k] = {"before": b, "after": a, "delta": a - b}
        audit["component_before_after"] = component_deltas
        audit["total_delta"] = audit.get("total_archive_bytes", 0) - before_audit.get("total_archive_bytes", 0)
    return audit
