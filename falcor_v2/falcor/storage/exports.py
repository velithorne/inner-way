"""Export to CSV and other formats."""

from __future__ import annotations

from pathlib import Path

import pandas as pd

from falcor.storage.readers import read_parquet


def export_run_csv(run_path: Path | str, output_path: Path | str | None = None) -> Path:
    """Export run data to CSV. Returns path to exported file."""
    run_path = Path(run_path)
    out_dir = run_path / "exports"
    out_dir.mkdir(parents=True, exist_ok=True)

    raw_p = run_path / "raw_timeseries.parquet"
    derived_p = run_path / "derived_metrics.parquet"
    primary = out_dir / "export.csv"
    if output_path:
        primary = Path(output_path)
        primary.parent.mkdir(parents=True, exist_ok=True)

    if derived_p.exists():
        read_parquet(derived_p).to_csv(primary, index=False)
    elif raw_p.exists():
        read_parquet(raw_p).to_csv(primary, index=False)
    return primary
