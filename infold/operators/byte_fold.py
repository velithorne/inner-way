"""
Byte Fold: chunk-based folding for mixed, weakly structured, and binary-like content.

v0.1: content-defined chunking, chunk dictionary, per-file reconstruction.
Runs after structural operators; targets files not yet committed.
"""

from __future__ import annotations

import base64
from pathlib import Path
from typing import Any

from infold.chunking.roller import ChunkResult, chunk_bytes
from infold.engine.file_routing import get_chunk_eligible_paths
from infold.models.candidate import CandidateCrease
from infold.models.estimates import GainEstimate, StressEstimate
from infold.models.fold_record import FoldRecord
from infold.models.project_sheet import ProjectSheet
from infold.models.simulation import FoldSimulationResult
from infold.models.validation_result import ValidationResult
from infold.operators.base import BaseOperator


# Metadata overhead per chunk reference (path + chunk_id) - rough estimate
CHUNK_REF_OVERHEAD = 24

# Overhead for chunk index entry
CHUNK_INDEX_OVERHEAD = 40


class ByteFoldOperator(BaseOperator):
    """Chunk-based folding for repeated byte patterns across files."""

    def operator_id(self) -> str:
        return "byte_fold"

    def operator_name(self) -> str:
        return "Byte Fold"

    def scope(self) -> str:
        return "file-level"

    def invariants(self) -> list[str]:
        return [
            "exact byte recovery",
            "preserved source ordering",
            "content-defined chunk boundaries",
            "deterministic reconstruction",
        ]

    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> list[CandidateCrease]:
        """Find chunk-fold candidates among eligible (non-committed) paths."""
        committed = config.get("_committed_paths") or set()
        cfg = config.get("thresholds", {}).get("byte_fold", {})
        min_chunk = cfg.get("min_chunk_bytes", 256)
        max_chunk = cfg.get("max_chunk_bytes", 8192)
        avg_chunk = cfg.get("avg_chunk_bytes", 1024)
        min_net_gain = cfg.get("min_net_gain", 64)

        eligible = get_chunk_eligible_paths(
            project_sheet.file_nodes, committed, config
        )
        if not eligible:
            return []

        # Chunk each eligible file
        path_to_result: dict[Path, ChunkResult] = {}
        for path in eligible:
            node = project_sheet.file_nodes.get(path)
            if not node or not node.raw_text:
                continue
            data = node.raw_text.encode("utf-8")
            path_to_result[path] = chunk_bytes(
                data, min_chunk=min_chunk, max_chunk=max_chunk, avg_chunk=avg_chunk
            )

        # Build global chunk dict and occurrence counts
        chunk_to_content: dict[str, bytes] = {}
        chunk_to_count: dict[str, int] = {}
        path_to_chunk_ids: dict[Path, list[str]] = {}

        for path, result in path_to_result.items():
            path_to_chunk_ids[path] = result.chunk_ids
            data = project_sheet.file_nodes[path].raw_text.encode("utf-8")
            for i, ch_id in enumerate(result.chunk_ids):
                start, end = result.boundaries[i]
                blob = data[start:end]
                if ch_id not in chunk_to_content:
                    chunk_to_content[ch_id] = blob
                chunk_to_count[ch_id] = chunk_to_count.get(ch_id, 0) + 1

        # Gross reused bytes: for each chunk, (count-1)*size
        gross = 0
        for ch_id, count in chunk_to_count.items():
            if count > 1:
                gross += (count - 1) * len(chunk_to_content[ch_id])

        # Metadata: chunk dict size + reconstruction map
        chunk_dict_bytes = sum(len(b) for b in chunk_to_content.values())
        total_refs = sum(len(ids) for ids in path_to_chunk_ids.values())
        meta_cost = chunk_dict_bytes + total_refs * CHUNK_REF_OVERHEAD
        net = gross - meta_cost

        if net < min_net_gain:
            return []

        gain = GainEstimate(gross, meta_cost, net, 0.85, 1.0)
        stress = StressEstimate(0.0, 0.05, 0.0, 0.0, 0.02)

        # Serialize chunk dict for metadata (base64 for JSON)
        chunk_dict_b64 = {
            ch_id: base64.b64encode(b).decode("ascii")
            for ch_id, b in chunk_to_content.items()
        }
        reconstruction = {
            str(p).replace("\\", "/"): ids for p, ids in path_to_chunk_ids.items()
        }

        return [
            CandidateCrease(
                operator_id=self.operator_id(),
                targets=list(path_to_chunk_ids.keys()),
                gain=gain,
                stress=stress,
                invariants=self.invariants(),
                metadata={
                    "chunk_dict": chunk_to_content,
                    "chunk_dict_b64": chunk_dict_b64,
                    "reconstruction": reconstruction,
                    "unique_chunk_count": len(chunk_to_content),
                    "reused_chunk_count": sum(1 for c in chunk_to_count.values() if c > 1),
                    "chunk_reused_bytes": gross,
                },
            )
        ]

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
        """Dry-run: reconstruct each file from chunks."""
        meta = candidate.metadata
        chunk_dict = meta["chunk_dict"]
        reconstruction = meta["reconstruction"]
        unfold_attempt: dict[Path, str] = {}
        for path_str, chunk_ids in reconstruction.items():
            p = Path(path_str)
            parts: list[bytes] = []
            for ch_id in chunk_ids:
                parts.append(chunk_dict[ch_id])
            reconstructed = b"".join(parts).decode("utf-8")
            unfold_attempt[p] = reconstructed

        folded_state = {
            "chunk_dict_b64": meta["chunk_dict_b64"],
            "reconstruction": reconstruction,
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
        if not unfold:
            return ValidationResult(False, ["No unfold attempt"], [], False, False, False)
        for path, restored in unfold.items():
            p = Path(path) if not isinstance(path, Path) else path
            orig_node = project_sheet.file_nodes.get(p)
            if orig_node is None:
                for k, node in project_sheet.file_nodes.items():
                    if str(k).replace("\\", "/") == str(p).replace("\\", "/"):
                        orig_node = node
                        break
            if orig_node is None:
                return ValidationResult(
                    accepted=False,
                    hard_failures=[f"Target not found: {p}"],
                    warnings=[],
                    fidelity_ok=False,
                    stress_ok=False,
                    utility_ok=False,
                )
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
        unfold_recipe = {
            "chunk_dict_b64": meta["chunk_dict_b64"],
            "reconstruction": meta["reconstruction"],
        }
        return FoldRecord(
            operator_id=self.operator_id(),
            shared_representation=unfold_recipe,
            invariants=self.invariants(),
            gain=candidate.gain.net_bytes_saved,
            utility=candidate.gain.utility_score,
            unfold_recipe=unfold_recipe,
            targets=candidate.targets,
            validation_summary="byte_fold chunk-based fold",
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
        chunk_dict_b64 = recipe["chunk_dict_b64"]
        reconstruction = recipe["reconstruction"]
        chunk_dict = {
            ch_id: base64.b64decode(b64)
            for ch_id, b64 in chunk_dict_b64.items()
        }
        result: dict[Path, str] = {}
        for path_str, chunk_ids in reconstruction.items():
            p = Path(path_str)
            parts = [chunk_dict[ch_id] for ch_id in chunk_ids]
            result[p] = b"".join(parts).decode("utf-8")
        return result
