"""
Package spec freeze v1: formalized structure, versioning, compatibility.

Required and optional files in the exported package.
Deterministic reconstruction, debug-friendly.
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

COMPATIBILITY_METADATA = {
    "spec_version": PACKAGE_SPEC_VERSION,
    "min_infold_version": "0.1.0",
    "reconstruction_mode": "deterministic",
    "debug_friendly": True,
}
