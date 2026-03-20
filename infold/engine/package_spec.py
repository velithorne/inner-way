"""
Package spec freeze v1: formalized structure, versioning, compatibility.

Required and optional files in the exported package.
Deterministic reconstruction, debug-friendly.
Explicit: logical_gain_bytes vs physical_folded_size_bytes.
"""

PACKAGE_SPEC_VERSION = "1.0"

REQUIRED_FILES = [
    "manifest.json",
    "ledger.json",
]

REQUIRED_DIRS = [
    "shared",
    "maps",
    "reports",
    "snapshots",
]

OPTIONAL_FILES = {
    "reports/report.json",
    "reports/report.txt",
}

OPTIONAL_IN_SHARED = [
    "exact_*.txt",
    "template_*.json",
    "symbols_*.json",
    "hierarchy_*.json",
    "dependency_motif_*.json",
]

MANIFEST_REQUIRED_KEYS = [
    "version",
    "project_id",
    "source_path",
    "created",
    "file_count",
    "fold_count",
    "logical_gain_bytes",
    "physical_folded_size_bytes",
]

from infold.engine.package_schema import build_compatibility

COMPATIBILITY_METADATA = build_compatibility(
    spec_version=PACKAGE_SPEC_VERSION,
    min_infold_version="0.2.0",
)
