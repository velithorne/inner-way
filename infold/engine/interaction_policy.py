"""
Cross-operator interaction policy: precedence and conflict rules.

- Precedence: operators run in fixed order (exact_repetition, symbol_table, template_skeleton, hierarchy_mirror, dependency_motif)
- Conflict: prevent double-folding of same file paths
- Hierarchy and dependency folds are metadata-level only (no file content change)
- Content-modifying operators (exact, template, symbol_table) claim file paths
"""

from pathlib import Path
from typing import Any

from infold.models.candidate import CandidateCrease

# Operators that modify file content (claim paths)
CONTENT_OPERATORS = {"exact_repetition", "template_skeleton", "symbol_table"}

# Operators that are metadata-only (do not claim paths for conflict)
METADATA_OPERATORS = {"hierarchy_mirror", "dependency_motif"}


def get_candidate_paths(candidate: CandidateCrease) -> set[str]:
    """Extract normalized path strings from candidate targets."""
    paths: set[str] = set()
    for t in candidate.targets:
        p = str(t).replace("\\", "/") if t else ""
        if p:
            paths.add(p)
    return paths


def conflicts_with_committed(
    candidate: CandidateCrease,
    committed_paths: set[str],
) -> tuple[bool, str | None]:
    """
    Check if candidate conflicts with already-committed folds.
    Content operators: reject if any target path is in committed_paths.
    Metadata operators: never conflict (they don't modify content).
    """
    if candidate.operator_id in METADATA_OPERATORS:
        return False, None
    cand_paths = get_candidate_paths(candidate)
    overlap = cand_paths & committed_paths
    if overlap:
        return True, f"paths already folded: {sorted(overlap)[:3]}"
    return False, None


def update_committed_paths(
    committed_paths: set[str],
    candidate: CandidateCrease,
    record_targets: list[Any],
) -> None:
    """Add paths from committed fold to committed_paths (in-place)."""
    if candidate.operator_id not in CONTENT_OPERATORS:
        return
    for t in record_targets:
        p = str(t).replace("\\", "/") if t else ""
        if p:
            committed_paths.add(p)
