"""Dataset abstraction for runs."""

from __future__ import annotations

from pathlib import Path
from typing import Any

import pandas as pd

from falcor.storage.readers import read_parquet, read_json


class RunDataset:
    """Dataset for a single run."""

    def __init__(self, run_path: Path):
        self.run_path = Path(run_path)
        self._raw: pd.DataFrame | None = None
        self._derived: pd.DataFrame | None = None
        self._manifest: dict[str, Any] | None = None

    @property
    def raw(self) -> pd.DataFrame:
        if self._raw is None:
            p = self.run_path / "raw_timeseries.parquet"
            self._raw = read_parquet(p) if p.exists() else pd.DataFrame()
        return self._raw

    @property
    def derived(self) -> pd.DataFrame:
        if self._derived is None:
            p = self.run_path / "derived_metrics.parquet"
            self._derived = read_parquet(p) if p.exists() else pd.DataFrame()
        return self._derived

    @property
    def manifest(self) -> dict[str, Any]:
        if self._manifest is None:
            p = self.run_path / "manifest.json"
            self._manifest = read_json(p) if p.exists() else {}
        return self._manifest
