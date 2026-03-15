"""Core type definitions for the Origami folding engine."""

from dataclasses import dataclass, field
from typing import Any


@dataclass
class FileSheet:
    """Internal representation of a parsed file."""

    path: str
    language: str
    raw_text: str
    tokens: list[Any] = field(default_factory=list)
    ast: Any = None
    symbols: dict[str, Any] = field(default_factory=dict)
    hashes: dict[str, str] = field(default_factory=dict)
    repeated_regions: list[dict] = field(default_factory=list)
    template_signature: str | None = None
    parse_success: bool = False
    diagnostics: list[str] = field(default_factory=list)


@dataclass
class ProjectSheet:
    """Internal representation of a project before folding."""

    project_id: str
    file_nodes: list[FileSheet] = field(default_factory=list)
    folder_nodes: list[str] = field(default_factory=list)
    dependency_graph: dict[str, list[str]] = field(default_factory=dict)
    symbol_index: dict[str, list[str]] = field(default_factory=dict)
    fold_candidates: list[dict] = field(default_factory=list)
    fold_history: list[dict] = field(default_factory=list)


@dataclass
class FoldRecord:
    """Record of a single fold operation."""

    fold_id: str
    operator_type: str
    source_targets: list[dict]
    shared_representation: str = ""
    invariants: list[str] = field(default_factory=list)
    stress_score: float = 0.0
    compression_gain: int = 0
    unfold_recipe: dict = field(default_factory=dict)
