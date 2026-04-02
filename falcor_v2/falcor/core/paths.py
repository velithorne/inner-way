"""Path utilities for lab results and runs."""

from __future__ import annotations

from pathlib import Path


def get_lab_results_path(base: Path | str = "lab_results") -> Path:
    """Get lab_results directory path."""
    p = Path(base)
    if not p.is_absolute():
        p = Path.cwd() / p
    return p


def get_run_path(run_id: str, base: Path | str = "lab_results") -> Path:
    """Get path for a specific run directory."""
    return get_lab_results_path(base) / "runs" / run_id


def ensure_run_structure(run_path: Path) -> None:
    """Create run directory structure."""
    (run_path / "plots").mkdir(parents=True, exist_ok=True)
    (run_path / "exports").mkdir(parents=True, exist_ok=True)
