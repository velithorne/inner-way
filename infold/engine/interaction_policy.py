"""
Cross-operator interaction policy: precedence, conflict, ownership, overlap resolution.

Operator classes:
- content folds: exact_repetition, template_skeleton, symbol_table (modify file content)
- structural metadata folds: hierarchy_mirror, dependency_motif (metadata only)

Ownership scopes:
- file-region: (future) line/byte ranges within a file
- file: whole file path
- family: group of files (template family, duplicate family)
- metadata-family: hierarchy/dependency family (no content change)

Overlap resolution: deterministic rules. Reuse/synergy: later operators can reference
earlier fold artifacts. Diagnostics: blocked_folds, superseded_folds, reused_artifacts.
"""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from infold.models.candidate import CandidateCrease

# Content operators: modify file content, claim file ownership
# byte_fold is the first active byte-operator; metadata_table_fold is the second (package-level)
# Future byte operators (e.g. byte_delta) can be added here
CONTENT_OPERATORS = {"exact_repetition", "template_skeleton", "symbol_table", "byte_fold"}

# Structural metadata operators: no content change, metadata-family ownership
METADATA_OPERATORS = {"hierarchy_mirror", "dependency_motif"}

# Ownership scope types
SCOPE_FILE = "file"
SCOPE_FILE_REGION = "file_region"
SCOPE_FAMILY = "family"
SCOPE_METADATA_FAMILY = "metadata_family"


@dataclass
class FoldOwnership:
    """Ownership claimed by a committed fold."""

    operator_id: str
    scope_type: str  # file, family, metadata_family
    paths: set[str] = field(default_factory=set)
    family_id: str | None = None


def get_operator_class(operator_id: str) -> str:
    """Return 'content' or 'metadata'."""
    return "content" if operator_id in CONTENT_OPERATORS else "metadata"


def get_ownership_scope(operator_id: str) -> str:
    """Return scope type for operator."""
    if operator_id in CONTENT_OPERATORS:
        if operator_id in ("template_skeleton", "exact_repetition"):
            return SCOPE_FAMILY
        if operator_id == "byte_fold":
            return SCOPE_FAMILY  # one candidate covers multiple files
        return SCOPE_FILE
    return SCOPE_METADATA_FAMILY


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


def resolve_overlap(
    candidate: CandidateCrease,
    committed_ownerships: list[FoldOwnership],
) -> tuple[bool, str | None]:
    """
    Deterministic overlap resolution. Returns (has_overlap, reason).
    Content vs content: reject if paths overlap.
    Metadata: never overlaps with content.
    """
    if candidate.operator_id in METADATA_OPERATORS:
        return False, None
    cand_paths = get_candidate_paths(candidate)
    for own in committed_ownerships:
        if own.operator_id not in CONTENT_OPERATORS:
            continue
        overlap = cand_paths & own.paths
        if overlap:
            return True, f"overlap with {own.operator_id}: {sorted(overlap)[:2]}"
    return False, None


def record_ownership(
    operator_id: str,
    targets: list[Any],
) -> FoldOwnership:
    """Create FoldOwnership for a committed fold."""
    paths = {str(t).replace("\\", "/") for t in targets if t}
    scope = get_ownership_scope(operator_id)
    return FoldOwnership(
        operator_id=operator_id,
        scope_type=scope,
        paths=paths,
        family_id=None,
    )


# Diagnostics
@dataclass
class InteractionDiagnostics:
    """Diagnostics for blocked, superseded, reused folds."""

    blocked_folds: list[dict[str, Any]] = field(default_factory=list)
    superseded_folds: list[dict[str, Any]] = field(default_factory=list)
    reused_artifacts: list[dict[str, Any]] = field(default_factory=list)
