"""
Package/ledger formalization: manifest, ledger, integrity, maps schemas.

Formalized structure with compatibility/versioning:
- spec_version
- min_reader_version
- compatibility_status
- upgrade_path_available

Explicit distinction: logical_gain_bytes vs physical_folded_size_bytes.
"""

from typing import Any

# Schema versions
MANIFEST_SCHEMA_VERSION = "1.0"
LEDGER_SCHEMA_VERSION = "1.0"
INTEGRITY_SCHEMA_VERSION = "1.0"
MAPS_SCHEMA_VERSION = "1.0"

# Compatibility
COMPATIBILITY_STATUS_OK = "ok"
COMPATIBILITY_STATUS_UPGRADE_AVAILABLE = "upgrade_available"
COMPATIBILITY_STATUS_INCOMPATIBLE = "incompatible"
UPGRADE_PATH_AVAILABLE = True  # v1 -> v2 when defined


def manifest_schema() -> dict[str, Any]:
    """Formal manifest schema (reference)."""
    return {
        "version": "string",
        "package_spec": "string",
        "compatibility": {
            "spec_version": "string",
            "min_infold_version": "string",
            "min_reader_version": "string",
            "reconstruction_mode": "string",
            "debug_friendly": "bool",
            "compatibility_status": "string",
            "upgrade_path_available": "bool",
        },
        "project_id": "any",
        "source_path": "string",
        "created": "string",
        "file_count": "int",
        "folder_count": "int",
        "original_size_bytes": "int",
        "fold_count": "int",
        "logical_gain_bytes": "int",
        "physical_folded_size_bytes": "int",
        "required_files": "list",
        "required_dirs": "list",
    }


def ledger_schema() -> dict[str, Any]:
    """Formal ledger schema (reference)."""
    return {
        "version": "string",
        "project_id": "any",
        "engine_version": "string",
        "created": "string",
        "source_fingerprint": "string",
        "mode": "string",
        "total_folds": "int",
        "total_bytes_saved": "int",
        "fold_records": "list",
    }


def integrity_schema() -> dict[str, Any]:
    """Formal integrity schema (reference)."""
    return {
        "checksums": "dict[str, str]",
    }


def maps_schema() -> dict[str, Any]:
    """Formal maps/reconstruction schema (reference)."""
    return {
        "records": "list[dict]",
        "record_fields": ["index", "operator_id", "targets", "gain"],
    }


def build_compatibility(
    spec_version: str = "1.0",
    min_infold_version: str = "0.2.0",
    min_reader_version: str | None = None,
    compatibility_status: str = COMPATIBILITY_STATUS_OK,
    upgrade_path_available: bool = UPGRADE_PATH_AVAILABLE,
) -> dict[str, Any]:
    """Build compatibility block for manifest."""
    return {
        "spec_version": spec_version,
        "min_infold_version": min_infold_version,
        "min_reader_version": min_reader_version or min_infold_version,
        "reconstruction_mode": "deterministic",
        "debug_friendly": True,
        "compatibility_status": compatibility_status,
        "upgrade_path_available": upgrade_path_available,
    }


def check_manifest_ledger_consistency(manifest: dict, ledger: dict) -> list[str]:
    """Check consistency between manifest and ledger. Returns list of errors."""
    errors: list[str] = []
    m_fold = manifest.get("fold_count", -1)
    l_fold = ledger.get("total_folds", -1)
    if m_fold != l_fold:
        errors.append(f"manifest fold_count {m_fold} != ledger total_folds {l_fold}")
    m_gain = manifest.get("logical_gain_bytes", -1)
    l_gain = ledger.get("total_bytes_saved", -1)
    if m_gain != l_gain:
        errors.append(f"manifest logical_gain_bytes {m_gain} != ledger total_bytes_saved {l_gain}")
    return errors


def check_logical_vs_physical(manifest: dict) -> list[str]:
    """Verify logical_gain_bytes and physical_folded_size_bytes are distinct and consistent."""
    errors: list[str] = []
    raw = manifest.get("original_size_bytes", 0)
    logical = manifest.get("logical_gain_bytes", 0)
    physical = manifest.get("physical_folded_size_bytes", 0)
    if raw - logical != physical:
        errors.append(f"physical_folded_size_bytes {physical} != original_size_bytes {raw} - logical_gain_bytes {logical}")
    return errors
