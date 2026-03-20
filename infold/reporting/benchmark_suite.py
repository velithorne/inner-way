"""
Real benchmark suite: second tier with multiple datasets.

Reports: raw, zip, gzip, Infold logical gain, Infold physical folded size,
fold count by operator, exact reconstruction status.
"""

import json
from pathlib import Path
from typing import Any

from infold.engine import run_fold
from infold.reporting.benchmark import _gzip_size, _raw_size, _zip_size


def run_benchmark_suite(
    datasets: list[tuple[Path, str]],
    config: dict[str, Any],
) -> list[dict[str, Any]]:
    """
    Run fold + benchmark on each dataset. Returns list of benchmark records.
    """
    results: list[dict[str, Any]] = []
    for path, dataset_id in datasets:
        if not path.exists():
            continue
        cfg = {**config, "project": {**config.get("project", {}), "id": dataset_id}}
        excludes = list(cfg["project"].get("exclude_patterns", []))
        if "infold_sweep_report" not in excludes:
            excludes.append("infold_sweep_report")
        cfg["project"]["exclude_patterns"] = excludes
        result = run_fold(path, cfg)
        sheet = result.project_sheet
        ledger = result.ledger
        raw = _raw_size(sheet)
        zip_size = _zip_size(sheet)
        gzip_size = _gzip_size(sheet)
        logical_gain = ledger.total_bytes_saved
        physical_folded = raw - logical_gain
        fold_count_by_operator: dict[str, int] = {}
        for r in ledger.fold_records:
            fold_count_by_operator[r.operator_id] = fold_count_by_operator.get(r.operator_id, 0) + 1
        results.append({
            "dataset_id": dataset_id,
            "file_count": sheet.metrics.get("file_count", 0),
            "raw_bytes": raw,
            "zip_bytes": zip_size,
            "gzip_bytes": gzip_size,
            "infold_logical_gain": logical_gain,
            "infold_physical_folded_size": physical_folded,
            "fold_count": ledger.total_folds,
            "fold_count_by_operator": fold_count_by_operator,
            "exact_reconstruction_status": "ok" if result.exact_reconstruction_ok else "failed",
        })
    return results


def benchmark_suite_to_text(results: list[dict[str, Any]]) -> str:
    """Human-readable benchmark suite report."""
    lines = [
        "Infold Benchmark Suite",
        "=====================",
        "",
    ]
    for r in results:
        lines.append(f"Dataset: {r['dataset_id']}")
        lines.append(f"  Files: {r['file_count']}")
        lines.append(f"  Raw: {r['raw_bytes']:,} bytes")
        lines.append(f"  ZIP: {r['zip_bytes']:,} bytes")
        lines.append(f"  Gzip: {r['gzip_bytes']:,} bytes")
        lines.append(f"  Infold logical gain: {r['infold_logical_gain']:,} bytes")
        lines.append(f"  Infold physical folded: {r['infold_physical_folded_size']:,} bytes")
        lines.append(f"  Fold count: {r['fold_count']}")
        lines.append(f"  By operator: {r['fold_count_by_operator']}")
        lines.append(f"  Exact reconstruction: {r['exact_reconstruction_status']}")
        lines.append("")
    return "\n".join(lines)
