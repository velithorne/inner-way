"""Whole-project state."""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from infold.models.file_node import FileNode
from infold.models.candidate import CandidateCrease
from infold.models.fold_record import FoldRecord


@dataclass
class ProjectSheet:
    """Whole-project state: file nodes, folder nodes, dependency graph, symbol index, candidates, fold history, metrics."""

    source_path: Path
    file_nodes: dict[Path, FileNode] = field(default_factory=dict)
    folder_nodes: list[Path] = field(default_factory=list)
    dependency_graph: dict[str, list[str]] = field(default_factory=dict)  # node -> edges
    symbol_index: dict[str, list[Any]] = field(default_factory=dict)
    candidates: list[CandidateCrease] = field(default_factory=list)
    fold_history: list[FoldRecord] = field(default_factory=list)
    metrics: dict[str, Any] = field(default_factory=dict)
