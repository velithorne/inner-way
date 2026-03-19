"""
Phase 16A: Mutation Chain Fold v0.1 — base + compact mutations for similar files.

Targets: config-like text files, repeated scripts with small changes, version-like files.
Conservative, deterministic, net-positive only.
"""

from pathlib import Path
from typing import Any

from infold.models.candidate import CandidateCrease
from infold.models.estimates import GainEstimate, StressEstimate
from infold.models.fold_record import FoldRecord
from infold.models.project_sheet import ProjectSheet
from infold.models.simulation import FoldSimulationResult
from infold.models.validation_result import ValidationResult
from infold.operators.base import BaseOperator
from infold.engine.mutation_chain import (
    build_mutation_chain,
    reconstruct_from_chain,
    mutation_chain_to_unfold_recipe,
    find_mutation_chain_candidates,
)


class MutationChainOperator(BaseOperator):
    """Represent similar files as base + compact mutations."""

    def operator_id(self) -> str:
        return "mutation_chain"

    def operator_name(self) -> str:
        return "Mutation Chain Fold"

    def scope(self) -> str:
        return "file-level"

    def invariants(self) -> list[str]:
        return [
            "exact byte recovery",
            "deterministic base selection",
            "line-based mutation format",
        ]

    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> list[CandidateCrease]:
        """Find groups of similar files that form net-positive mutation chains."""
        thresh = config.get("thresholds", {}).get("mutation_chain", {})
        min_family = thresh.get("min_family_size", 3)
        min_lines = thresh.get("min_lines", 5)
        min_overlap = thresh.get("min_line_overlap_ratio", 0.85)
        committed_paths = config.get("_committed_paths", set())

        path_to_content: dict[Path, str] = {}
        for path, node in project_sheet.file_nodes.items():
            if node.diagnostics or not node.raw_text:
                continue
            path_to_content[path] = node.raw_text

        # Phase 16C: Anchor-aware, microscope-aware, template handoff
        anchors: list[dict[str, Any]] | None = None
        if config.get("operators", {}).get("anchor_file", {}).get("enabled", True):
            try:
                from infold.engine.anchor_file import find_anchor_files
                anchors = find_anchor_files(project_sheet, project_sheet.source_path)
            except Exception:
                pass

        template_rejected = config.get("_template_rejected_groups", [])

        microscope_groups: list[tuple[list[Path], list[list[str]]]] | None = None
        if config.get("operators", {}).get("structural_microscope", {}).get("enabled", True):
            try:
                from infold.engine.structural_microscope import find_microscope_assisted_groups
                scope_thresh = config.get("thresholds", {}).get("structural_microscope", {})
                micro_groups = find_microscope_assisted_groups(
                    project_sheet,
                    max_bytes=scope_thresh.get("max_bytes", 512),
                    max_lines=scope_thresh.get("max_lines", 10),
                    min_family=min_family,
                )
                # Exclude paths already committed
                microscope_groups = [
                    (paths, lines_list)
                    for paths, lines_list in micro_groups
                    if not any(str(p).replace("\\", "/") in committed_paths for p in paths)
                ] or None
            except Exception:
                pass

        chains = find_mutation_chain_candidates(
            path_to_content,
            min_family_size=min_family,
            min_lines=min_lines,
            min_line_overlap_ratio=min_overlap,
            anchors=anchors,
            template_rejected_groups=template_rejected,
            microscope_groups=microscope_groups,
            committed_paths=committed_paths,
        )

        candidates: list[CandidateCrease] = []
        for chain in chains:
            paths = [Path(p) for p in chain["paths"]]
            gain_val = chain["gain_bytes"]
            gain = GainEstimate(gain_val, 0, gain_val, 0.7, 0.8)
            stress = StressEstimate(0.1, 0.2, 0.1, 0.05, 0.15)
            candidates.append(CandidateCrease(
                operator_id=self.operator_id(),
                targets=paths,
                gain=gain,
                stress=stress,
                invariants=self.invariants(),
                metadata={
                    "chain": chain,
                    "paths": chain["paths"],
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
        chain = candidate.metadata.get("chain", {})
        unfold_attempt = reconstruct_from_chain(chain)
        return FoldSimulationResult(
            folded_state={"chain": chain},
            unfold_attempt={Path(p): c for p, c in unfold_attempt.items()},
            success=True,
            error=None,
        )

    def validate(
        self,
        simulation_result: FoldSimulationResult,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> ValidationResult:
        """Byte-for-byte reconstruction check."""
        if not simulation_result.success:
            return ValidationResult(False, ["Simulation failed"], [], False, False, False)
        unfold = simulation_result.unfold_attempt
        if not unfold:
            return ValidationResult(False, ["No unfold"], [], False, False, False)
        for path, content in unfold.items():
            node = project_sheet.file_nodes.get(path)
            if node and content != node.raw_text:
                return ValidationResult(
                    False,
                    [f"Fidelity failed: {path}"],
                    [],
                    False,
                    True,
                    True,
                )
        return ValidationResult(True, [], [], True, True, True)

    def apply(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> FoldRecord:
        """Commit fold."""
        chain = candidate.metadata["chain"]
        recipe = mutation_chain_to_unfold_recipe(chain)
        return FoldRecord(
            operator_id=self.operator_id(),
            shared_representation=chain["base_content"],
            invariants=self.invariants(),
            gain=chain["gain_bytes"],
            utility=candidate.gain.utility_score,
            unfold_recipe=recipe,
            targets=candidate.targets,
            validation_summary="mutation_chain base+mutations",
            dependencies=[],
        )

    def unfold(
        self,
        fold_record: FoldRecord,
        folded_project: Any,
        config: dict[str, Any],
    ) -> dict[Path, str]:
        """Reconstruct files from base + mutations."""
        recipe = fold_record.unfold_recipe
        chain = {
            "base_path": recipe["base_path"],
            "base_content": recipe["base_content"],
            "mutations": recipe["mutations"],
            "paths": recipe["paths"],
        }
        result = reconstruct_from_chain(chain)
        return {Path(p): c for p, c in result.items()}
