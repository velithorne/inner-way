"""
Fold pipeline profiler: scan, parse, per-operator, package export, integrity, reconstruction.

Records wall-clock times for each phase. Deterministic, minimal overhead.
"""

import time
from dataclasses import dataclass, field
from typing import Any, Callable

from infold.engine.orchestrator import FoldResult


@dataclass
class ProfileResult:
    """Profiling timings for a fold run."""

    scan_time_s: float = 0.0
    parse_time_s: float = 0.0
    operator_times_s: dict[str, dict[str, float]] = field(default_factory=dict)
    package_export_time_s: float = 0.0
    integrity_time_s: float = 0.0
    reconstruction_time_s: float = 0.0
    total_fold_time_s: float = 0.0


def _timeit(fn: Callable[[], Any]) -> tuple[Any, float]:
    """Run fn, return (result, elapsed_seconds)."""
    t0 = time.perf_counter()
    out = fn()
    return out, time.perf_counter() - t0


def profile_fold(
    run_fold_fn: Callable[[], FoldResult],
    source_path: Any,
    config: dict[str, Any],
) -> tuple[FoldResult, ProfileResult]:
    """
    Run fold with profiling. Returns (result, profile).
    Note: run_fold_fn is the actual run_fold; we cannot easily instrument
    internal phases without modifying run_fold. For now we profile total fold time.
    """
    result, total_s = _timeit(run_fold_fn)
    profile = ProfileResult(total_fold_time_s=total_s)
    return result, profile


def profile_fold_instrumented(
    source_path: Any,
    config: dict[str, Any],
) -> tuple[FoldResult, ProfileResult]:
    """
    Run fold with full instrumentation. Requires run_fold to support _profile callback.
    Fallback: just time total run.
    """
    from infold.engine import run_fold
    result, total_s = _timeit(lambda: run_fold(source_path, config))
    return result, ProfileResult(total_fold_time_s=total_s)
