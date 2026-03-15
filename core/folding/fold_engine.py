"""Fold engine: orchestrates operators and maintains fold ledger."""

from core.types import ProjectSheet
from core.interfaces.base_operator import BaseOperator


class FoldEngine:
    """Orchestrates fold operators in runtime order."""

    RUNTIME_ORDER = [
        "exact_repetition",
        "symbol_table",
        "template_skeleton",
        "hierarchy_mirror",
        "dependency_motif",
    ]

    def __init__(self, operators: list[BaseOperator] | None = None) -> None:
        self._operators = {op.operator_id(): op for op in (operators or [])}

    def register(self, operator: BaseOperator) -> None:
        """Register an operator."""
        self._operators[operator.operator_id()] = operator

    def run(self, sheet: ProjectSheet, config: dict | None = None) -> tuple[ProjectSheet, list[dict]]:
        """
        Run all enabled operators in runtime order.
        Returns (modified_sheet, fold_records).
        Stub: returns sheet unchanged and empty records.
        """
        config = config or {}
        records: list[dict] = []
        current = sheet

        for op_id in self.RUNTIME_ORDER:
            op = self._operators.get(op_id)
            if not op:
                continue
            op_config = config.get("operators", {}).get(op_id, {})
            if not op_config.get("enabled", True):
                continue
            candidates = op.detect_candidates(current)
            for c in candidates:
                if op.validate(current, c):
                    current, record = op.apply(current, c)
                    records.append(record)

        return current, records
