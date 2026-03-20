"""
Exact Repetition Fold: collapse exact repeated regions into one shared representation plus references.

Scope: token-level, file-level, cross-file repeated blocks.
v1: whole-file duplicate detection using raw hash.
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


# Metadata overhead per reference (path + ref id) - rough estimate in bytes
REFERENCE_OVERHEAD = 50


class ExactRepetitionOperator(BaseOperator):
    """Collapse exact repeated file/regions into one canonical + references."""

    def operator_id(self) -> str:
        return "exact_repetition"

    def operator_name(self) -> str:
        return "Exact Repetition Fold"

    def scope(self) -> str:
        return "file-level"

    def invariants(self) -> list[str]:
        return [
            "exact byte recovery",
            "preserved source ordering",
            "preserved boundaries",
            "no encoding mismatch",
        ]

    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> list[CandidateCrease]:
        """Find whole-file duplicates by raw_hash. Require min_occurrences."""
        thresh = config.get("thresholds", {}).get("exact_repetition", {})
        min_occurrences = thresh.get("min_occurrences", 2)
        min_region_bytes = thresh.get("min_region_length_bytes", 64)

        # Group by raw_hash
        hash_to_files: dict[str, list[tuple[Path, str, int]]] = {}
        for path, node in project_sheet.file_nodes.items():
            if not node.raw_text or node.diagnostics:
                continue
            size = len(node.raw_text.encode("utf-8"))
            if size < min_region_bytes:
                continue
            h = node.raw_hash
            if h not in hash_to_files:
                hash_to_files[h] = []
            hash_to_files[h].append((path, node.raw_text, size))

        candidates: list[CandidateCrease] = []
        for _hash, files in hash_to_files.items():
            if len(files) < min_occurrences:
                continue
            # Pick first as canonical
            canonical_path, content, size = files[0]
            occurrences = [{"path": p, "content": c, "size": s, "is_canonical": i == 0} for i, (p, c, s) in enumerate(files)]
            targets = [p for p, _, _ in files]
            gross = (len(files) - 1) * size
            meta_cost = (len(files) - 1) * REFERENCE_OVERHEAD
            net = gross - meta_cost
            if net <= 0:
                continue
            gain = GainEstimate(gross, meta_cost, net, 0.8, 1.0)
            stress = StressEstimate(0.0, 0.1, 0.0, 0.0, 0.05)
            candidates.append(CandidateCrease(
                operator_id=self.operator_id(),
                targets=targets,
                gain=gain,
                stress=stress,
                invariants=self.invariants(),
                metadata={
                    "canonical_path": canonical_path,
                    "content": content,
                    "occurrences": occurrences,
                    "content_size": size,
                    "raw_hash": _hash,
                },
            ))
        return candidates

    def estimate_gain(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> GainEstimate:
        return candidate.gain

    def estimate_stress(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> StressEstimate:
        return candidate.stress

    def simulate(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> FoldSimulationResult:
        """Dry-run: folded state = canonical + refs; unfold = copy canonical to each ref."""
        meta = candidate.metadata
        content = meta["content"]
        occurrences = meta["occurrences"]
        unfold_attempt: dict[Path, str] = {}
        for occ in occurrences:
            unfold_attempt[Path(occ["path"]) if isinstance(occ["path"], str) else occ["path"]] = content
        folded_state = {
            "canonical_content": content,
            "occurrences": [{"path": str(o["path"]), "is_canonical": o["is_canonical"]} for o in occurrences],
        }
        return FoldSimulationResult(
            folded_state=folded_state,
            unfold_attempt=unfold_attempt,
            success=True,
            error=None,
        )

    def validate(
        self,
        simulation_result: FoldSimulationResult,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> ValidationResult:
        """Check unfold matches original for each target."""
        if not simulation_result.success:
            return ValidationResult(
                accepted=False,
                hard_failures=["Simulation failed"],
                warnings=[],
                fidelity_ok=False,
                stress_ok=False,
                utility_ok=False,
            )
        unfold = simulation_result.unfold_attempt
        if unfold is None:
            return ValidationResult(False, ["No unfold attempt"], [], False, False, False)
        for path, restored in unfold.items():
            p = Path(path) if not isinstance(path, Path) else path
            orig_node = project_sheet.file_nodes.get(p)
            if orig_node is None:
                for k, node in project_sheet.file_nodes.items():
                    if str(k) == str(p):
                        orig_node = node
                        break
            if orig_node is None:
                continue
            if restored != orig_node.raw_text:
                return ValidationResult(
                    accepted=False,
                    hard_failures=[f"Fidelity failed: {p} reconstruction differs"],
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
        """Commit: produce FoldRecord with unfold recipe."""
        meta = candidate.metadata
        content = meta["content"]
        occurrences = meta["occurrences"]
        unfold_recipe = {
            "canonical_content": content,
            "occurrences": [{"path": str(o["path"]), "is_canonical": o["is_canonical"]} for o in occurrences],
        }
        return FoldRecord(
            operator_id=self.operator_id(),
            shared_representation=content,
            invariants=self.invariants(),
            gain=candidate.gain.net_bytes_saved,
            utility=candidate.gain.utility_score,
            unfold_recipe=unfold_recipe,
            targets=candidate.targets,
            validation_summary="exact_repetition whole-file fold",
            dependencies=[],
        )

    def unfold(
        self,
        fold_record: FoldRecord,
        folded_project: Any,
        config: dict[str, Any],
    ) -> dict[Path, str]:
        """Reconstruct: return dict of path -> content."""
        recipe = fold_record.unfold_recipe
        content = recipe["canonical_content"]
        result: dict[Path, str] = {}
        for occ in recipe["occurrences"]:
            p = Path(occ["path"])
            result[p] = content
        return result
