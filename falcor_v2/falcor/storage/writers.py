"""Writers: parquet, JSON."""

from __future__ import annotations

from pathlib import Path
from typing import Any

import pandas as pd
import pyarrow as pa
import pyarrow.parquet as pq


def write_parquet(path: Path | str, df: pd.DataFrame) -> None:
    """Write DataFrame to parquet."""
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    table = pa.Table.from_pandas(df)
    pq.write_table(table, path)


def write_json(path: Path | str, data: dict[str, Any]) -> None:
    """Write dict to JSON."""
    import json

    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
