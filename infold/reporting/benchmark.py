"""
Benchmark: compare folded size against raw, ZIP, gzip baselines.
"""

import gzip
import io
import zipfile
from pathlib import Path
from typing import Any

from infold.engine.orchestrator import FoldResult
from infold.models.project_sheet import ProjectSheet


def _raw_size(sheet: ProjectSheet) -> int:
    """Raw size = sum of file contents."""
    return sheet.metrics.get("original_size_bytes", 0)


def _zip_size(sheet: ProjectSheet) -> int:
    """ZIP archive size (in-memory)."""
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as zf:
        for path, node in sheet.file_nodes.items():
            arcname = str(path).replace("\\", "/")
            zf.writestr(arcname, node.raw_text.encode("utf-8"))
    return len(buf.getvalue())


def _gzip_size(sheet: ProjectSheet) -> int:
    """Gzip of concatenated content (single stream)."""
    content = b""
    for node in sheet.file_nodes.values():
        content += node.raw_text.encode("utf-8")
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb") as gz:
        gz.write(content)
    return len(buf.getvalue())


def run_benchmark(result: FoldResult, config: dict[str, Any]) -> dict[str, Any]:
    """
    Run baseline comparisons. Returns dict with raw, zip, gzip sizes and folded size.
    logical_gain = bytes saved by folding. physical_folded_size = raw - logical_gain.
    """
    sheet = result.project_sheet
    ledger = result.ledger

    baselines = config.get("benchmark", {}).get("baselines", ["raw", "zip", "gzip"])
    raw = _raw_size(sheet)
    logical_gain = ledger.total_bytes_saved
    physical_folded_size = raw - logical_gain

    out: dict[str, Any] = {
        "raw_bytes": raw,
        "folded_bytes": physical_folded_size,
        "bytes_saved": logical_gain,
        "logical_gain_bytes": logical_gain,
        "physical_folded_size_bytes": physical_folded_size,
        "baselines": {},
    }

    if "zip" in baselines:
        out["baselines"]["zip"] = _zip_size(sheet)
    if "gzip" in baselines:
        out["baselines"]["gzip"] = _gzip_size(sheet)

    return out


def benchmark_to_text(benchmark: dict[str, Any]) -> str:
    """Human-readable benchmark summary."""
    lines = [
        "Benchmark",
        "=========",
        "",
        f"Raw size:              {benchmark['raw_bytes']:,} bytes",
        f"Physical folded size:  {benchmark.get('physical_folded_size_bytes', benchmark['folded_bytes']):,} bytes",
        f"Logical gain:          {benchmark['bytes_saved']:,} bytes",
        "",
        "Baselines:",
    ]
    for name, size in benchmark.get("baselines", {}).items():
        lines.append(f"  {name}: {size:,} bytes")
    return "\n".join(lines)
