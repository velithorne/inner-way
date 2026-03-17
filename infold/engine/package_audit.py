"""
Package overhead audit: measure archive size by section, identify hotspots.

Sections: shared/, maps/, reports/, integrity, snapshots/, manifest, ledger.
Reports which sections dominate per dataset.
"""

from typing import Any


def audit_package_overhead(overhead: dict[str, int], total_archive_bytes: int | None = None) -> dict[str, Any]:
    """
    Analyze package overhead. overhead maps section name -> bytes.
    Returns audit with sizes, percentages, dominant sections.
    """
    total = sum(overhead.values())
    by_section: dict[str, dict[str, Any]] = {}
    for name, size in overhead.items():
        pct = (size / total * 100) if total else 0
        by_section[name] = {"bytes": size, "pct_of_overhead": round(pct, 1)}
    # Dominant = sections that contribute most
    sorted_sections = sorted(overhead.items(), key=lambda x: -x[1])
    dominant = [s[0] for s in sorted_sections[:3]] if sorted_sections else []
    pct_of_archive = None
    if total_archive_bytes and total_archive_bytes > 0:
        pct_of_archive = round(total / total_archive_bytes * 100, 1)
    return {
        "by_section": by_section,
        "total_overhead_bytes": total,
        "dominant_sections": dominant,
        "pct_of_archive": pct_of_archive,
        "hotspots": _identify_hotspots(overhead, total),
    }


def _identify_hotspots(overhead: dict[str, int], total: int) -> list[str]:
    """Identify potential hotspots (sections that dominate)."""
    hotspots = []
    if total == 0:
        return hotspots
    # Reports often dominate on small datasets
    reports = overhead.get("reports", 0)
    if reports > total * 0.4:
        hotspots.append("reports: >40% of overhead (consider compact/minimal report)")
    # Snapshots (passthrough) can be large
    snapshots = overhead.get("snapshots", 0)
    if snapshots > total * 0.5:
        hotspots.append("snapshots: >50% of overhead (passthrough + inventory)")
    # Maps redundancy
    maps_b = overhead.get("maps", 0)
    if maps_b > total * 0.2 and total < 10000:
        hotspots.append("maps: significant share on small package (consider compact JSON)")
    return hotspots
