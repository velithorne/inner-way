"""Recovery result from unfold operation."""

from dataclasses import dataclass
from typing import Any


@dataclass
class UnfoldResult:
    """Recovery result: exact match, syntax validity, restored targets, timing."""

    exact_match: bool
    syntax_valid: bool
    restored_targets: list[Any]
    timing_ms: float
    error: str | None  # if recovery failed
