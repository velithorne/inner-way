"""Base interface for fold operators."""

from abc import ABC, abstractmethod
from typing import Any

from core.types import ProjectSheet


class BaseOperator(ABC):
    """Abstract base class for all fold operators."""

    @abstractmethod
    def operator_id(self) -> str:
        """Unique identifier for this operator."""
        ...

    @abstractmethod
    def operator_name(self) -> str:
        """Human-readable name."""
        ...

    @abstractmethod
    def scope(self) -> str:
        """Scope: 'file', 'project', or 'cross_project'."""
        ...

    @abstractmethod
    def detect_candidates(self, sheet: ProjectSheet) -> list[dict]:
        """Identify candidate regions for folding."""
        ...

    @abstractmethod
    def estimate_gain(self, candidate: dict) -> int:
        """Estimate compression gain in bytes."""
        ...

    @abstractmethod
    def estimate_stress(self, candidate: dict) -> float:
        """Estimate stress (0.0–1.0)."""
        ...

    def simulate(self, sheet: ProjectSheet, candidate: dict) -> dict:
        """Dry-run fold simulation. Default: return candidate metadata."""
        return {"candidate": candidate, "estimated_gain": self.estimate_gain(candidate)}

    @abstractmethod
    def validate(self, sheet: ProjectSheet, candidate: dict) -> bool:
        """Check if fold is valid per invariants."""
        ...

    @abstractmethod
    def apply(self, sheet: ProjectSheet, candidate: dict) -> tuple[ProjectSheet, dict]:
        """Apply fold; return (modified_sheet, fold_record)."""
        ...

    @abstractmethod
    def unfold(self, fold_record: dict) -> Any:
        """Reconstruct original from fold record."""
        ...

    def metrics(self) -> dict[str, Any]:
        """Return operator-specific metrics. Default: empty."""
        return {}
