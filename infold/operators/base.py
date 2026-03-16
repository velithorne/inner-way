"""
Base operator contract for fold operators.

Operators must be deterministic in exact mode, declare invariants,
generate serializable unfold recipes, and not mutate project state
during candidate detection or simulation.
"""

from abc import ABC, abstractmethod
from typing import Any

from infold.models.candidate import CandidateCrease
from infold.models.fold_record import FoldRecord
from infold.models.project_sheet import ProjectSheet
from infold.models.estimates import GainEstimate, StressEstimate
from infold.models.simulation import FoldSimulationResult
from infold.models.validation_result import ValidationResult


class BaseOperator(ABC):
    """Base operator contract. All fold operators implement this interface."""

    @abstractmethod
    def operator_id(self) -> str:
        """Unique identifier for this operator (e.g. 'exact_repetition')."""
        ...

    @abstractmethod
    def operator_name(self) -> str:
        """Human-readable name (e.g. 'Exact Repetition Fold')."""
        ...

    @abstractmethod
    def scope(self) -> str:
        """Scope of operation: 'token-level', 'file-level', 'cross-file', 'graph-level', etc."""
        ...

    @abstractmethod
    def invariants(self) -> list[str]:
        """Declared invariants that must hold during fold and unfold."""
        ...

    @abstractmethod
    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> list[CandidateCrease]:
        """
        Find potential fold candidates. Must not mutate project_sheet.
        """
        ...

    @abstractmethod
    def estimate_gain(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> GainEstimate:
        """Estimate bytes saved and utility. Must not mutate state."""
        ...

    @abstractmethod
    def estimate_stress(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> StressEstimate:
        """Estimate stress (ambiguity, reconstruction cost). Must not mutate state."""
        ...

    @abstractmethod
    def simulate(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> FoldSimulationResult:
        """
        Dry-run: simulate fold and unfold without mutating project state.
        Returns folded state and reconstruction attempt.
        """
        ...

    @abstractmethod
    def validate(
        self,
        simulation_result: FoldSimulationResult,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> ValidationResult:
        """Validate simulation: fidelity, stress, utility, hard rules."""
        ...

    @abstractmethod
    def apply(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> FoldRecord:
        """
        Commit the fold: update project state and return FoldRecord for ledger.
        """
        ...

    @abstractmethod
    def unfold(
        self,
        fold_record: FoldRecord,
        folded_project: Any,
        config: dict[str, Any],
    ) -> Any:
        """
        Reconstruct original from fold record. Returns restored content/structure.
        """
        ...

    def metrics(self, fold_record: FoldRecord) -> dict[str, Any]:
        """Optional: operator-specific metrics for reporting."""
        return {
            "operator_id": fold_record.operator_id,
            "gain": fold_record.gain,
            "utility": fold_record.utility,
            "target_count": len(fold_record.targets),
        }
