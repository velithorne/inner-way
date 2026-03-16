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
from infold.parsers import parse_project
from infold.validation import validate_candidate


@dataclass
class FoldResult:
    """Result of a fold run."""

    project_sheet: ProjectSheet
    ledger: FoldLedger
    folded_state: dict[str, Any] = field(default_factory=dict)  # canonical regions, etc.
    errors: list[str] = field(default_factory=list)


def _get_enabled_operators(config: dict[str, Any]) -> list[BaseOperator]:
    """Return operators that are enabled in config, in runtime order."""
    ops_config = config.get("operators", {})
    order = [
        ("exact_repetition", ExactRepetitionOperator),
        # Future: Symbol Table, Template Skeleton, Hierarchy Mirror, Dependency Motif
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

    for op in _get_enabled_operators(config):
        candidates = op.detect_candidates(sheet, config)
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

    return FoldResult(
        project_sheet=sheet,
        ledger=ledger,
        folded_state=folded_state,
        errors=errors,
    )
