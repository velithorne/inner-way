"""
Template Skeleton Fold v1: token-based extraction of shared scaffold + variable slots.

- Same file type only
- Min 3 files in family
- High structural similarity (min_scaffold_similarity 0.80)
- Max slot ratio 0.35
- Line-based alignment for exact reconstruction (conservative v1)
- Rejects: different logic (low similarity), too many slots, <3 files, syntax-invalid reconstruction
"""

import ast
from pathlib import Path
from typing import Any

from infold.models.candidate import CandidateCrease
from infold.models.estimates import GainEstimate, StressEstimate
from infold.models.fold_record import FoldRecord
from infold.models.project_sheet import ProjectSheet
from infold.models.simulation import FoldSimulationResult
from infold.models.validation_result import ValidationResult
from infold.operators.base import BaseOperator


def _find_template_families(
    project_sheet: ProjectSheet,
    min_family: int,
    min_similarity: float,
    max_slot_ratio: float,
    diagnostics: list[dict[str, Any]] | None = None,
) -> list[tuple[list[Path], list[str], list[list[str]], float, float]]:
    """
    Find families of files with same line count, high line-level similarity.
    Returns accepted families: (paths, const_blocks, slot_groups, similarity, slot_ratio).
    If diagnostics list provided, appends rejected-family diagnostics to it.
    """
    MIN_LINES = 5
    by_lang: dict[str, list[tuple[Path, list[str]]]] = {}
    skipped_few_lines = 0
    for path, node in project_sheet.file_nodes.items():
        if node.diagnostics:
            continue
        lines = node.raw_text.splitlines(keepends=True)
        if len(lines) < MIN_LINES:
            skipped_few_lines += 1
            continue
        by_lang.setdefault(node.language, []).append((path, lines))

    families: list[tuple[list[Path], list[str], list[list[str]], float, float]] = []
    diag = diagnostics if diagnostics is not None else []
    if skipped_few_lines and diag is not None:
        diag.append({
            "file_count": skipped_few_lines,
            "scaffold_similarity": None,
            "slot_ratio": None,
            "family_purity": None,
            "reject_reason": f"files skipped: {skipped_few_lines} with < {MIN_LINES} lines (min for template analysis)",
        })

    for lang, files in by_lang.items():
        by_line_count: dict[int, list[tuple[Path, list[str]]]] = {}
        for path, lines in files:
            n = len(lines)
            by_line_count.setdefault(n, []).append((path, lines))

        for n_lines, group in by_line_count.items():
            if len(group) < min_family:
                diag.append({
                    "file_count": len(group),
                    "scaffold_similarity": None,
                    "slot_ratio": None,
                    "family_purity": None,
                    "reject_reason": f"file_count {len(group)} < min_family_size {min_family}",
                })
                continue
            paths = [p for p, _ in group]
            lines_list = [lines for _, lines in group]
            const_blocks: list[str] = []
            slot_groups: list[list[str]] = []
            current_const: list[str] = []
            current_slots: list[list[str]] = []
            for i in range(n_lines):
                line_vals = [ll[i] for ll in lines_list]
                if len(set(line_vals)) == 1:
                    if current_slots:
                        slot_groups.append(current_slots)
                        current_slots = []
                    current_const.append(line_vals[0])
                else:
                    if current_const:
                        const_blocks.append("".join(current_const))
                        current_const = []
                    current_slots.append(line_vals)
            if current_const:
                const_blocks.append("".join(current_const))
            if current_slots:
                slot_groups.append(current_slots)

            # slot_ratio = fraction of lines that vary (slot lines / total lines)
            total_slot_lines = sum(len(sg) for sg in slot_groups)
            slot_ratio = total_slot_lines / n_lines if n_lines else 0
            similarity = 1.0 - slot_ratio
            if similarity < min_similarity or slot_ratio > max_slot_ratio:
                reason = []
                if similarity < min_similarity:
                    reason.append(f"scaffold_similarity {similarity:.3f} < {min_similarity}")
                if slot_ratio > max_slot_ratio:
                    reason.append(f"slot_ratio {slot_ratio:.3f} > {max_slot_ratio}")
                diag.append({
                    "file_count": len(paths),
                    "scaffold_similarity": round(similarity, 4),
                    "slot_ratio": round(slot_ratio, 4),
                    "family_purity": round(similarity, 4),
                    "reject_reason": "; ".join(reason),
                })
                continue
            if slot_ratio == 0:
                diag.append({
                    "file_count": len(paths),
                    "scaffold_similarity": 1.0,
                    "slot_ratio": 0.0,
                    "family_purity": 1.0,
                    "reject_reason": "exact_duplicates (leave to Exact Repetition)",
                })
                continue

            def reconstruct(j: int) -> str:
                out: list[str] = []
                for bi, const in enumerate(const_blocks):
                    out.append(const)
                    if bi < len(slot_groups):
                        slot_block = slot_groups[bi]
                        if isinstance(slot_block[0], list):
                            out.append("".join(slot_block[k][j] for k in range(len(slot_block))))
                        else:
                            out.append(slot_block[j])
                return "".join(out)

            for j in range(len(paths)):
                if reconstruct(j) != project_sheet.file_nodes[paths[j]].raw_text:
                    diag.append({
                        "file_count": len(paths),
                        "scaffold_similarity": round(similarity, 4),
                        "slot_ratio": round(slot_ratio, 4),
                        "family_purity": round(similarity, 4),
                        "reject_reason": "reconstruction_fidelity_failed",
                    })
                    break
            else:
                families.append((paths, const_blocks, slot_groups, similarity, slot_ratio))

    return families


class TemplateSkeletonOperator(BaseOperator):
    """Extract shared scaffold + variable slots from similar files."""

    def operator_id(self) -> str:
        return "template_skeleton"

    def operator_name(self) -> str:
        return "Template Skeleton Fold"

    def scope(self) -> str:
        return "file-level"

    def invariants(self) -> list[str]:
        return [
            "syntax-valid reconstruction",
            "exact scaffold preservation",
            "slot order preservation",
            "file identity preservation",
        ]

    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> list[CandidateCrease]:
        """Find template families."""
        thresh = config.get("thresholds", {}).get("template_skeleton", {})
        min_family = thresh.get("min_family_size", 3)
        min_similarity = thresh.get("min_scaffold_similarity", 0.80)
        max_slot_ratio = thresh.get("max_slot_ratio", 0.35)

        diag_list = config.get("_run_diagnostics", {}).get("template_rejected", [])
        families = _find_template_families(
            project_sheet, min_family, min_similarity, max_slot_ratio, diagnostics=diag_list
        )
        candidates: list[CandidateCrease] = []
        for paths, const_blocks, slot_groups, sim, slot_ratio in families:
            total_raw = sum(len(project_sheet.file_nodes[p].raw_text.encode("utf-8")) for p in paths)
            scaffold_size = sum(len(b) for b in const_blocks)
            slot_storage = sum(sum(len(s) for s in sg) for sg in slot_groups)
            gross = total_raw - (scaffold_size + slot_storage)
            meta_cost = 80 + len(paths) * 30
            net = max(0, gross - meta_cost)
            if net <= 0:
                diag_list.append({
                    "file_count": len(paths),
                    "scaffold_similarity": round(sim, 4),
                    "slot_ratio": round(slot_ratio, 4),
                    "family_purity": round(sim, 4),
                    "reject_reason": f"net_gain {net} <= 0 (gross={gross}, meta_cost={meta_cost})",
                })
                continue
            gain = GainEstimate(gross, meta_cost, net, 0.7, 0.85)
            stress = StressEstimate(0.2, 0.3, 0.2, 0.1, 0.25)
            # Family purity: scaffold similarity (higher = purer)
            family_purity = sim
            # Slot ambiguity: variance in slot lengths (0 = no ambiguity)
            slot_lengths = [sum(len(s) for s in sg) for sg in slot_groups]
            avg_slot = sum(slot_lengths) / len(slot_lengths) if slot_lengths else 0
            slot_ambiguity = (sum((x - avg_slot) ** 2 for x in slot_lengths) / len(slot_lengths)) ** 0.5 if slot_lengths else 0

            candidates.append(CandidateCrease(
                operator_id=self.operator_id(),
                targets=paths,
                gain=gain,
                stress=stress,
                invariants=self.invariants(),
                metadata={
                    "const_blocks": const_blocks,
                    "slot_groups": slot_groups,
                    "paths": [str(p) for p in paths],
                    "scaffold_similarity": sim,
                    "slot_ratio": slot_ratio,
                    "scaffold_length": scaffold_size,
                    "slot_count": len(slot_groups),
                    "family_purity": family_purity,
                    "slot_ambiguity": round(slot_ambiguity, 2),
                    "avg_slot_size": int(avg_slot),
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
        const_blocks = meta["const_blocks"]
        slot_groups = meta["slot_groups"]
        paths = [Path(p) for p in meta["paths"]]
        unfold_attempt: dict[Path, str] = {}
        for j, p in enumerate(paths):
            out: list[str] = []
            for bi, const in enumerate(const_blocks):
                out.append(const)
                if bi < len(slot_groups):
                    slot_block = slot_groups[bi]
                    if slot_block and isinstance(slot_block[0], list):
                        out.append("".join(slot_block[k][j] for k in range(len(slot_block))))
                    else:
                        out.append(slot_block[j] if j < len(slot_block) else "")
            unfold_attempt[p] = "".join(out)
        return FoldSimulationResult(
            folded_state={"const_blocks": const_blocks, "slot_groups": slot_groups, "paths": meta["paths"]},
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
        """Byte-for-byte reconstruction + syntax validation where supported."""
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
            # Syntax validation for Python
            if node and node.language == "python" and content:
                try:
                    ast.parse(content)
                except SyntaxError as e:
                    return ValidationResult(
                        False,
                        [f"Syntax-invalid reconstruction: {path}: {e.msg}"],
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
        meta = candidate.metadata
        unfold_recipe = {
            "const_blocks": meta["const_blocks"],
            "slot_groups": meta["slot_groups"],
            "paths": meta["paths"],
            "family_purity": meta.get("family_purity"),
            "slot_ambiguity": meta.get("slot_ambiguity"),
            "avg_slot_size": meta.get("avg_slot_size"),
            "file_count": len(candidate.targets),
            "scaffold_similarity": meta.get("scaffold_similarity"),
            "slot_ratio": meta.get("slot_ratio"),
        }
        return FoldRecord(
            operator_id=self.operator_id(),
            shared_representation=meta["const_blocks"],
            invariants=self.invariants(),
            gain=candidate.gain.net_bytes_saved,
            utility=candidate.gain.utility_score,
            unfold_recipe=unfold_recipe,
            targets=candidate.targets,
            validation_summary="template_skeleton line-based fold",
            dependencies=[],
        )

    def unfold(
        self,
        fold_record: FoldRecord,
        folded_project: Any,
        config: dict[str, Any],
    ) -> dict[Path, str]:
        """Reconstruct files."""
        recipe = fold_record.unfold_recipe
        const_blocks = recipe["const_blocks"]
        slot_groups = recipe["slot_groups"]
        paths = recipe["paths"]
        result: dict[Path, str] = {}
        for j, p in enumerate(paths):
            out: list[str] = []
            for bi, const in enumerate(const_blocks):
                out.append(const)
                if bi < len(slot_groups):
                    slot_block = slot_groups[bi]
                    if slot_block and isinstance(slot_block[0], list):
                        out.append("".join(slot_block[k][j] for k in range(len(slot_block))))
                    else:
                        out.append(slot_block[j] if j < len(slot_block) else "")
            result[Path(p)] = "".join(out)
        return result
