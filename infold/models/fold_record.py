"""Committed fold record for the ledger."""

from dataclasses import dataclass
from typing import Any


@dataclass
class FoldRecord:
    """Committed fold: operator identity, shared representation, invariants, gain, utility, unfold recipe."""

    operator_id: str
    shared_representation: Any  # canonical region, template, dictionary, etc.
    invariants: list[str]
    gain: int  # net bytes saved
    utility: float
    unfold_recipe: dict[str, Any]  # serializable reconstruction instructions
    targets: list[Any]  # for reporting
    validation_summary: str
    dependencies: list[str]  # prior fold ids this depends on
