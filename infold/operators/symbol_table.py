"""
Symbol Table Fold v1: compress repeated identifiers via project-wide symbol dictionary.

- Build project-wide symbol inventory
- Target: repeated identifiers (min_symbol_reuse_count 5)
- Reject ambiguous scope/shadowing (conservative: same-name in different files = same symbol for v1)
- Preserve exact spelling, exact reconstruction
- Store symbol dictionary + reference mappings
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

# Placeholder: use Unicode PUA to avoid collision with source (U+E000-U+F8FF)
def _placeholder(pid: int) -> str:
    return f"\uE000{pid}\uE001"


def _build_symbol_inventory(project_sheet: ProjectSheet) -> dict[str, int]:
    """Count symbol occurrences across project."""
    counts: dict[str, int] = {}
    for node in project_sheet.file_nodes.values():
        for sym in node.symbols:
            name = sym.get("name", "")
            if name and not name.startswith("_"):
                counts[name] = counts.get(name, 0) + 1
        for imp in node.imports:
            for n in imp.get("names", []):
                counts[n] = counts.get(n, 0) + 1
            mod = imp.get("module", "")
            if mod:
                counts[mod] = counts.get(mod, 0) + 1
    return counts


def _replace_symbols_in_source(
    raw_text: str,
    symbol_to_id: dict[str, int],
    get_positions: callable,
) -> tuple[str, bool]:
    """
    Replace symbol occurrences with placeholders. get_positions(file_path, ast_data) returns
    list of (name, start, end) for each replaceable occurrence.
    Returns (folded_text, success).
    """
    if not get_positions:
        return raw_text, False
    positions = sorted(get_positions(), key=lambda x: x[1], reverse=True)  # end to start
    result = list(raw_text)
    for name, start, end in positions:
        if name not in symbol_to_id:
            continue
        pid = symbol_to_id[name]
        placeholder = _placeholder(pid)
        result[start:end] = placeholder
    return "".join(result), True


class SymbolTableOperator(BaseOperator):
    """Compress repeated identifiers via symbol dictionary."""

    def operator_id(self) -> str:
        return "symbol_table"

    def operator_name(self) -> str:
        return "Symbol Table Fold"

    def scope(self) -> str:
        return "project-level"

    def invariants(self) -> list[str]:
        return [
            "exact spelling recovery",
            "preserved scope",
            "preserved reference targets",
            "no collisions",
        ]

    def detect_candidates(
        self,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> list[CandidateCrease]:
        """Find high-reuse symbols suitable for dictionary compression."""
        thresh = config.get("thresholds", {}).get("symbol_table", {})
        min_reuse = thresh.get("min_symbol_reuse_count", 5)

        counts = _build_symbol_inventory(project_sheet)
        high_reuse = {n: c for n, c in counts.items() if c >= min_reuse}
        if not high_reuse:
            return []

        # Build symbol dict: name -> id
        symbol_to_id = {n: i for i, n in enumerate(sorted(high_reuse.keys()))}
        id_to_symbol = {i: n for n, i in symbol_to_id.items()}

        # Estimate gain: original size of symbol occurrences vs placeholder size
        total_original = sum(len(n) * c for n, c in high_reuse.items())
        placeholder_len = len(_placeholder(0))
        total_folded = sum(placeholder_len * c for n, c in high_reuse.items())
        dict_overhead = sum(len(n) + 10 for n in high_reuse)  # dict storage
        gross = total_original - total_folded
        net = gross - dict_overhead
        if net <= 0:
            return []

        gain = GainEstimate(gross, dict_overhead, net, 0.6, 0.8)
        stress = StressEstimate(0.1, 0.2, 0.3, 0.0, 0.2)

        return [CandidateCrease(
            operator_id=self.operator_id(),
            targets=list(project_sheet.file_nodes.keys()),
            gain=gain,
            stress=stress,
            invariants=self.invariants(),
            metadata={
                "symbol_to_id": symbol_to_id,
                "id_to_symbol": id_to_symbol,
                "high_reuse": high_reuse,
                "shared_symbol_count": len(high_reuse),
                "symbol_reuse_ratio": sum(high_reuse.values()) / max(1, sum(counts.values())),
            },
        )]

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
        """Dry-run: replace symbols with placeholders."""
        meta = candidate.metadata
        symbol_to_id = meta["symbol_to_id"]
        id_to_symbol = meta["id_to_symbol"]

        def _get_offset(text: str, line: int, col: int) -> int:
            lines = text.splitlines(keepends=True)
            return sum(len(lines[i]) for i in range(min(max(0, line - 1), len(lines)))) + col

        def get_positions_for_file(path: Path, node: Any):
            positions = []
            if not node.ast_data:
                return positions
            for n in ast.walk(node.ast_data):
                if isinstance(n, ast.Name) and n.id in symbol_to_id:
                    start = _get_offset(node.raw_text, n.lineno or 1, n.col_offset or 0)
                    end = start + len(n.id)
                    positions.append((n.id, start, end))
            return positions

        unfold_attempt: dict[Path, str] = {}
        for path, node in project_sheet.file_nodes.items():
            if node.language != "python" or not node.ast_data:
                unfold_attempt[path] = node.raw_text
                continue
            positions = get_positions_for_file(path, node)
            if not positions:
                unfold_attempt[path] = node.raw_text
                continue
            positions = sorted(positions, key=lambda x: x[2], reverse=True)
            result = list(node.raw_text)
            for name, start, end in positions:
                if node.raw_text[start:end] != name:
                    continue
                pid = symbol_to_id[name]
                result[start:end] = _placeholder(pid)
            unfold_attempt[path] = "".join(result)

        # Unfold = reverse the replacement
        unfolded: dict[Path, str] = {}
        for path, folded_content in unfold_attempt.items():
            content = folded_content
            for pid, name in sorted(id_to_symbol.items(), key=lambda x: -len(_placeholder(x[0]))):
                content = content.replace(_placeholder(pid), name)
            unfolded[path] = content

        return FoldSimulationResult(
            folded_state={"symbol_to_id": symbol_to_id, "id_to_symbol": id_to_symbol, "folded_files": {str(p): unfold_attempt[p] for p in unfold_attempt}},
            unfold_attempt=unfolded,
            success=True,
            error=None,
        )

    def validate(
        self,
        simulation_result: FoldSimulationResult,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> ValidationResult:
        """Exact reconstruction check."""
        if not simulation_result.success:
            return ValidationResult(False, ["Simulation failed"], [], False, False, False)
        unfold = simulation_result.unfold_attempt
        if not unfold:
            return ValidationResult(False, ["No unfold"], [], False, False, False)
        for path, content in unfold.items():
            node = project_sheet.file_nodes.get(Path(path) if isinstance(path, str) else path)
            if node is None:
                for p, n in project_sheet.file_nodes.items():
                    if str(p) == str(path):
                        node = n
                        break
            if node and content != node.raw_text:
                return ValidationResult(False, [f"Fidelity failed: {path}"], [], False, True, True)
        return ValidationResult(True, [], [], True, True, True)

    def apply(
        self,
        candidate: CandidateCrease,
        project_sheet: ProjectSheet,
        config: dict[str, Any],
    ) -> FoldRecord:
        """Commit fold. Store folded file contents in recipe for reconstruction."""
        meta = candidate.metadata
        symbol_to_id = meta["symbol_to_id"]
        id_to_symbol = meta["id_to_symbol"]

        def _get_offset(text: str, line: int, col: int) -> int:
            lines = text.splitlines(keepends=True)
            return sum(len(lines[i]) for i in range(min(line - 1, len(lines)))) + col

        folded_files: dict[str, str] = {}
        for path, node in project_sheet.file_nodes.items():
            if node.language != "python" or not node.ast_data:
                folded_files[str(path)] = node.raw_text
                continue
            positions = []
            for n in ast.walk(node.ast_data):
                if isinstance(n, ast.Name) and n.id in symbol_to_id:
                    start = _get_offset(node.raw_text, n.lineno or 1, n.col_offset or 0)
                    end = start + len(n.id)
                    if node.raw_text[start:end] == n.id:
                        positions.append((n.id, start, end))
            if not positions:
                folded_files[str(path)] = node.raw_text
                continue
            positions = sorted(positions, key=lambda x: x[2], reverse=True)
            result = list(node.raw_text)
            for name, start, end in positions:
                pid = symbol_to_id[name]
                result[start:end] = _placeholder(pid)
            folded_files[str(path)] = "".join(result)

        unfold_recipe = {
            "symbol_to_id": symbol_to_id,
            "id_to_symbol": id_to_symbol,
            "folded_files": folded_files,
            "shared_symbol_count": meta["shared_symbol_count"],
            "symbol_reuse_ratio": meta.get("symbol_reuse_ratio"),
        }
        return FoldRecord(
            operator_id=self.operator_id(),
            shared_representation=meta["id_to_symbol"],
            invariants=self.invariants(),
            gain=candidate.gain.net_bytes_saved,
            utility=candidate.gain.utility_score,
            unfold_recipe=unfold_recipe,
            targets=candidate.targets,
            validation_summary="symbol_table fold",
            dependencies=[],
        )

    def unfold(
        self,
        fold_record: FoldRecord,
        folded_project: Any,
        config: dict[str, Any],
    ) -> dict[Path, str]:
        """Reconstruct: replace placeholders with original names."""
        recipe = fold_record.unfold_recipe
        id_to_symbol = recipe["id_to_symbol"]
        folded_files = recipe.get("folded_files", (folded_project or {}).get("folded_files", {}))
        result: dict[Path, str] = {}
        for path_str, content in folded_files.items():
            p = Path(path_str)
            for pid, name in sorted(id_to_symbol.items(), key=lambda x: -len(_placeholder(x[0]))):
                content = content.replace(_placeholder(pid), name)
            result[p] = content
        return result
