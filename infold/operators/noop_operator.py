"""
No-op operator for testing the base interface.

Implements all abstract methods with empty/minimal behavior.
"""

from typing import Any

from infold.models.candidate import CandidateCrease
from infold.models.estimates import GainEstimate, StressEstimate
from infold.models.fold_record import FoldRecord
from infold.models.project_sheet import ProjectSheet
from infold.models.simulation import FoldSimulationResult
from infold.models.validation_result import ValidationResult
from infold.operators.base import BaseOperator


class NoOpOperator(BaseOperator):
    """Operator that finds no candidates and does nothing. Used to verify the interface."""

    def operator_id(self) -> str:
        return "noop"

    def operator_name(self) -> str:
        return "No-Op (test)"

    def scope(self) -> str:
        return "none"

    def invariants(self) -> list[str]:
        return []

    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict,
    ) -> list[CandidateCrease]:
        return []

    def estimate_gain(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict,
    ) -> GainEstimate:
        return GainEstimate(0, 0, 0, 0.0, 0.0)

    def estimate_stress(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict,
    ) -> StressEstimate:
        return StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0)

    def simulate(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict,
    ) -> FoldSimulationResult:
        return FoldSimulationResult(
            folded_state=None,
            unfold_attempt=None,
            success=True,
            error=None,
        )

    def validate(
        self,
        simulation_result: FoldSimulationResult,
        project_sheet: ProjectSheet,
        config: dict,
    ) -> ValidationResult:
        return ValidationResult(
            accepted=True,
            hard_failures=[],
            warnings=[],
            fidelity_ok=True,
            stress_ok=True,
            utility_ok=True,
        )

    def apply(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict,
    ) -> FoldRecord:
        return FoldRecord(
            operator_id=self.operator_id(),
            shared_representation=None,
            invariants=[],
            gain=0,
            utility=0.0,
            unfold_recipe={},
            targets=candidate.targets,
            validation_summary="noop",
            dependencies=[],
        )

    def unfold(
        self,
        fold_record: FoldRecord,
        folded_project: Any,
        config: dict,
    ) -> Any:
        return None
