"""
Infold Archive v0.1: create, inspect, validate, reconstruct, explain, compare.

Deterministic validation. Preserves exact-mode guarantees.
"""

from infold.archive.operations import (
    create_archive,
    inspect_archive,
    validate_archive,
    reconstruct_archive,
    explain_archive,
    compare_archives,
)

__all__ = [
    "create_archive",
    "inspect_archive",
    "validate_archive",
    "reconstruct_archive",
    "explain_archive",
    "compare_archives",
]
