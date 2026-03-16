"""
Fold engine orchestration: run operators in order, validate, commit, ledger.

Runtime order: Exact Repetition, Symbol Table, Template Skeleton, Hierarchy Mirror, Dependency Motif.
Phase 1: Only Exact Repetition is implemented.
"""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from infold.engine.ledger import FoldLedger
from infold.intake import scan_project
from infold.models.project_sheet import ProjectSheet
from infold.operators.base import BaseOperator
from infold.operators.exact_repetition import ExactRepetitionOperator
from infold.operators.symbol_table import SymbolTableOperator
from infold.operators.template_skeleton import TemplateSkeletonOperator
from infold.parsers import parse_project
from infold.validation import validate_candidate


@dataclass
class FoldResult:
    """Result of a fold run."""

    project_sheet: ProjectSheet
    ledger: FoldLedger
    folded_state: dict[str, Any] = field(default_factory=dict)  # canonical regions, etc.
    errors: list[str] = field(default_factory=list)
    candidate_counts: dict[str, int] = field(default_factory=dict)  # operator_id -> count detected
    exact_reconstruction_ok: bool = True  # verified byte-for-byte recovery


def _get_enabled_operators(config: dict[str, Any]) -> list[BaseOperator]:
    """Return operators that are enabled in config, in runtime order."""
    ops_config = config.get("operators", {})
    order = [
        ("exact_repetition", ExactRepetitionOperator),
        ("symbol_table", SymbolTableOperator),
        ("template_skeleton", TemplateSkeletonOperator),
    ]
    result: list[BaseOperator] = []
    for op_id, op_class in order:
        if ops_config.get(op_id, {}).get("enabled", True):
            result.append(op_class())
    return result


def run_fold(
    source_path: Path | str,
    config: dict[str, Any],
) -> FoldResult:
    """
    Run the full fold pipeline: scan, parse, run operators, validate, commit, ledger.

    Returns FoldResult with project sheet, ledger, and folded state.
    """
    source_path = Path(source_path).resolve()
    sheet = scan_project(
        source_path,
        exclude_patterns=config.get("project", {}).get("exclude_patterns"),
        include_extensions=config.get("project", {}).get("include_extensions"),
    )
    parse_project(sheet)

    ledger = FoldLedger(
        project_id=config.get("project", {}).get("id"),
        config_snapshot=config,
    )
    folded_state: dict[str, Any] = {"canonicals": [], "references": []}
    errors: list[str] = []
    candidate_counts: dict[str, int] = {}

    for op in _get_enabled_operators(config):
        candidates = op.detect_candidates(sheet, config)
        candidate_counts[op.operator_id()] = len(candidates)
        for c in candidates:
            vr = validate_candidate(op, c, sheet, config)
            if not vr.accepted:
                errors.append(f"{op.operator_id()}: candidate rejected: {vr.hard_failures}")
                continue
            try:
                record = op.apply(c, sheet, config)
                ledger.append(record)
                folded_state["canonicals"].append(
                    {"operator": op.operator_id(), "targets": len(record.targets), "gain": record.gain}
                )
            except Exception as e:
                errors.append(f"{op.operator_id()}: apply failed: {e}")

    # Verify exact reconstruction for committed folds
    exact_reconstruction_ok = True
    ops_by_id = {op.operator_id(): op for op in _get_enabled_operators(config)}
    for record in ledger.fold_records:
        op = ops_by_id.get(record.operator_id)
        if not op:
            continue
        try:
            unfolded = op.unfold(record, folded_state, config)
            if unfolded is None:
                continue
            for path, content in (unfolded.items() if isinstance(unfolded, dict) else []):
                p = path if isinstance(path, Path) else Path(path)
                orig_node = sheet.file_nodes.get(p)
                if orig_node is None:
                    for k, n in sheet.file_nodes.items():
                        if str(k) == str(p) or k == p:
                            orig_node = n
                            break
                if orig_node and content != orig_node.raw_text:
                    exact_reconstruction_ok = False
                    errors.append(f"Exact reconstruction failed: {p}")
        except Exception:
            exact_reconstruction_ok = False

    return FoldResult(
        project_sheet=sheet,
        ledger=ledger,
        folded_state=folded_state,
        errors=errors,
        candidate_counts=candidate_counts,
        exact_reconstruction_ok=exact_reconstruction_ok,
    )
