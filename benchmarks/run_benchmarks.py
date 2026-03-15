"""Run benchmarks comparing Origami folding vs ZIP/gzip/zstd."""

import json
import subprocess
import sys
from pathlib import Path


def run_benchmarks(project_path: str | Path, config: dict | None = None) -> dict:
    """
    Run benchmark comparison.
    Stub: returns placeholder structure. Full impl would:
    - measure raw project size
    - run origami fold
    - measure folded size
    - run zip/gzip/zstd on original
    - compare ratios
    """
    project_path = Path(project_path)
    config = config or {}
    bench_config = config.get("benchmarks", {})

    results = {
        "project_path": str(project_path),
        "original_bytes": 0,
        "folded_bytes": 0,
        "zip_bytes": 0,
        "gzip_bytes": 0,
        "zstd_bytes": 0,
        "ratios": {},
    }

    if not project_path.exists():
        return results

    # Count original size (sum of file sizes)
    total = 0
    for f in project_path.rglob("*"):
        if f.is_file():
            total += f.stat().st_size
    results["original_bytes"] = total

    # Stub: no actual folding/compression yet
    return results


def main() -> None:
    path = sys.argv[1] if len(sys.argv) > 1 else "."
    config_path = Path(__file__).parent.parent / "config" / "default_config.json"
    config = json.loads(config_path.read_text()) if config_path.exists() else {}
    results = run_benchmarks(path, config)
    print(json.dumps(results, indent=2))


if __name__ == "__main__":
    main()
