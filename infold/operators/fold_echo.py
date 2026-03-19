"""
Phase 17A: Fold Echo v0.1 — attach weak files as low-cost echoes to accepted families.

Targets: template-near files, config/script satellites. Host families: template_skeleton.
Conservative, deterministic, net-positive only.
"""

from pathlib import Path
from typing import Any

from infold.engine.fold_echo import (
    find_template_echo_candidates,
    reconstruct_echo_from_template,
)
from infold.models.candidate import CandidateCrease
from infold.models.estimates import GainEstimate, StressEstimate
from infold.models.fold_record import FoldRecord
from infold.models.project_sheet import ProjectSheet
from infold.models.simulation import FoldSimulationResult
from infold.models.validation_result import ValidationResult
from infold.operators.base import BaseOperator


class FoldEchoOperator(BaseOperator):
    """Attach weak passthrough files as echoes to accepted template families."""

    def operator_id(self) -> str:
        return "fold_echo"

    def operator_name(self) -> str:
        return "Fold Echo"

    def scope(self) -> str:
        return "file-level"

    def invariants(self) -> list[str]:
        return [
            "exact byte recovery",
            "host-family only",
            "net-positive only",
        ]

    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> list[CandidateCrease]:
        """Find passthrough files that fit accepted template families as echoes."""
        ledger = config.get("_ledger")
        committed_paths = config.get("_committed_paths", set())
        if not ledger or not hasattr(ledger, "fold_records"):
            return []

        thresh = config.get("thresholds", {}).get("fold_echo", {})
        min_net_gain = thresh.get("min_net_gain", 16)
        min_similarity = thresh.get("min_echo_similarity", 0.85)

        # Phase 17B/18B: Anchor-aware host selection (use shared anchor context)
        anchors: list[dict[str, Any]] | None = None
        anchor_ctx = config.get("_anchor_context", {})
        if anchor_ctx.get("anchors"):
            anchors = anchor_ctx["anchors"]
        elif config.get("operators", {}).get("anchor_file", {}).get("enabled", True):
            try:
                from infold.engine.anchor_file import find_anchor_files
                anchors = find_anchor_files(project_sheet, project_sheet.source_path)
            except Exception:
                pass

        microscope_assisted_paths: set[str] = set()
        for m in config.get("_microscope_assisted", []):
            if m.get("operator") == "template_skeleton":
                microscope_assisted_paths.update(m.get("paths", []))

        candidates_raw = find_template_echo_candidates(
            project_sheet,
            ledger.fold_records,
            committed_paths,
            min_echo_similarity=min_similarity,
            min_net_gain=min_net_gain,
            anchors=anchors,
            microscope_assisted_paths=microscope_assisted_paths or None,
        )

        candidates: list[CandidateCrease] = []
        for c in candidates_raw:
            path = Path(c["echo_path"])
            gain = GainEstimate(c["gain"], 0, c["gain"], 0.8, 0.9)
            stress = StressEstimate(0.05, 0.1, 0.05, 0.02, 0.08)
            candidates.append(CandidateCrease(
                operator_id=self.operator_id(),
                targets=[path],
                gain=gain,
                stress=stress,
                invariants=self.invariants(),
                metadata={
                    "host_operator": c["host_operator"],
                    "host_idx": c["host_idx"],
                    "echo_path": c["echo_path"],
                    "slot_values": c["slot_values"],
                    "gain": c["gain"],
                },
            ))
        return candidates

    def estimate_gain(self, c: CandidateCrease, s: ProjectSheet, cfg: dict) -> GainEstimate:
        return c.gain

    def estimate_stress(self, c: CandidateCrease, s: ProjectSheet, cfg: dict) -> StressEstimate:
        return c.stress

    def simulate(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> FoldSimulationResult:
        """Dry-run reconstruction."""
        meta = candidate.metadata
        path = Path(meta["echo_path"])
        expected = project_sheet.file_nodes.get(path)
        if not expected:
            return FoldSimulationResult(folded_state=None, unfold_attempt=None, success=False, error="echo path not in sheet")
        ledger = config.get("_ledger")
        if not ledger:
            return FoldSimulationResult(folded_state=None, unfold_attempt=None, success=False, error="no ledger")
        rec = ledger.fold_records[meta["host_idx"]]
        if rec.operator_id != "template_skeleton":
            return FoldSimulationResult(folded_state=None, unfold_attempt=None, success=False, error="host not template")
        recipe = rec.unfold_recipe
        content = reconstruct_echo_from_template(
            recipe.get("const_blocks", []),
            recipe.get("slot_groups", []),
            meta["slot_values"],
        )
        if content != expected.raw_text:
            return FoldSimulationResult(folded_state=None, unfold_attempt=None, success=False, error="reconstruction mismatch")
        return FoldSimulationResult(
            folded_state=meta,
            unfold_attempt={path: content},
            success=True,
            error=None,
        )

    def validate(
        self,
        simulation_result: FoldSimulationResult,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> ValidationResult:
        """Validate echo reconstruction."""
        if not simulation_result.success:
            return ValidationResult(
                accepted=False,
                hard_failures=[simulation_result.error or "simulation failed"],
                warnings=[],
                fidelity_ok=False,
                stress_ok=True,
                utility_ok=True,
            )
        unfold = simulation_result.unfold_attempt
        if not unfold:
            return ValidationResult(
                accepted=False,
                hard_failures=["No unfold"],
                warnings=[],
                fidelity_ok=False,
                stress_ok=True,
                utility_ok=True,
            )
        for path, content in unfold.items():
            node = project_sheet.file_nodes.get(path)
            if node and content != node.raw_text:
                return ValidationResult(
                    accepted=False,
                    hard_failures=[f"Fidelity failed: {path}"],
                    warnings=[],
                    fidelity_ok=False,
                    stress_ok=True,
                    utility_ok=True,
                )
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
        config: dict[str, Any],
    ) -> FoldRecord:
        """Commit echo as fold record."""
        meta = candidate.metadata
        path = Path(meta["echo_path"])
        unfold_recipe = {
            "host_operator": meta["host_operator"],
            "host_idx": meta["host_idx"],
            "echo_path": meta["echo_path"],
            "slot_values": meta["slot_values"],
        }
        return FoldRecord(
            operator_id=self.operator_id(),
            shared_representation=unfold_recipe,
            invariants=self.invariants(),
            gain=meta["gain"],
            utility=0.85,
            unfold_recipe=unfold_recipe,
            targets=[path],
            validation_summary="exact reconstruction verified",
            dependencies=[],
        )

    def unfold(
        self,
        record: FoldRecord,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> dict[Path, str]:
        """Reconstruct echo from host + payload."""
        recipe = record.unfold_recipe
        ledger = config.get("_ledger")
        if not ledger:
            return {}
        rec = ledger.fold_records[recipe["host_idx"]]
        if rec.operator_id != "template_skeleton":
            return {}
        content = reconstruct_echo_from_template(
            rec.unfold_recipe.get("const_blocks", []),
            rec.unfold_recipe.get("slot_groups", []),
            recipe["slot_values"],
        )
        path = Path(recipe["echo_path"])
        return {path: content}
