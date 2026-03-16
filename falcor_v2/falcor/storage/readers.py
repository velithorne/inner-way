"""Readers: parquet, JSON."""

from __future__ import annotations

from pathlib import Path
from typing import Any

import pandas as pd
import pyarrow.parquet as pq


def read_parquet(path: Path | str) -> pd.DataFrame:
    """Read parquet to DataFrame."""
    return pq.read_table(path).to_pandas()


def read_json(path: Path | str) -> dict[str, Any]:
    """Read JSON to dict."""
    import json

    with open(path, encoding="utf-8") as f:
        return json.load(f)
