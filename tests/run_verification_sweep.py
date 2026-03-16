#!/usr/bin/env python3
"""
Step 10 verification sweep: run fold engine on test datasets and record metrics.

Datasets:
1. duplicate-heavy Python project
2. template-heavy project
3. config-heavy repo
4. mixed small project (Python + markdown + JSON + text)

Benchmark modes: raw, infold_exact_only, infold_exact_plus_template, zip, gzip
"""

import json
import sys
from pathlib import Path

# Add project root
sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold
from infold.reporting import export_report, run_benchmark
from infold.reporting.benchmark import _raw_size, _zip_size, _gzip_size


def load_config() -> dict:
    config_path = Path(__file__).parent.parent / "infold" / "config.json"
    with open(config_path, encoding="utf-8") as f:
        return json.load(f)


def run_sweep(dataset_path: Path, dataset_id: str, config: dict) -> dict:
    """Run fold on dataset, return sweep record."""
    config = config.copy()
    config["project"] = config.get("project", {}).copy()
    config["project"]["id"] = dataset_id
    config["project"]["source_path"] = str(dataset_path)
    config["project"] = {**config["project"], "exclude_patterns": config["project"].get("exclude_patterns", []) + ["infold_sweep_report"]}

    result = run_fold(dataset_path, config)
    bench = run_benchmark(result, config)

    # Export report to dataset-specific dir
    out_dir = dataset_path / "infold_sweep_report"
    export_report(result, config, output_dir=out_dir)

    raw = result.project_sheet.metrics.get("original_size_bytes", 0)
    logical_gain = result.ledger.total_bytes_saved
    physical_folded = raw - logical_gain
    total_candidates = sum(result.candidate_counts.values())
    committed = result.ledger.total_folds

    # Benchmark suite: raw, infold_exact_only, infold_exact_plus_template, zip, gzip
    sheet = result.project_sheet
    benchmark_suite = {
        "raw": raw,
        "infold_full": physical_folded,
        "logical_gain": logical_gain,
        "zip": _zip_size(sheet),
        "gzip": _gzip_size(sheet),
    }
    # Run infold_exact_only and infold_exact_plus_template by re-running with different configs
    cfg_exact_only = {**config, "operators": {**config.get("operators", {}), "exact_repetition": {"enabled": True}, "template_skeleton": {"enabled": False}}}
    cfg_both = {**config, "operators": {**config.get("operators", {}), "exact_repetition": {"enabled": True}, "template_skeleton": {"enabled": True}}}
    res_exact = run_fold(dataset_path, cfg_exact_only)
    res_both = run_fold(dataset_path, cfg_both)
    benchmark_suite["infold_exact_only"] = raw - res_exact.ledger.total_bytes_saved
    benchmark_suite["infold_exact_plus_template"] = raw - res_both.ledger.total_bytes_saved

    return {
        "project_id": dataset_id,
        "file_count": result.project_sheet.metrics.get("file_count", 0),
        "folder_count": result.project_sheet.metrics.get("folder_count", 0),
        "raw_size_bytes": raw,
        "folded_size_bytes": physical_folded,
        "logical_bytes_saved": logical_gain,
        "candidate_count": total_candidates,
        "committed_fold_count": committed,
        "exact_reconstruction_status": "ok" if result.exact_reconstruction_ok else "failed",
        "report_export_success": (out_dir / "report.json").exists(),
        "benchmark": bench,
        "benchmark_suite": benchmark_suite,
        "candidate_counts_by_operator": result.candidate_counts,
        "errors": result.errors,
    }


def main() -> int:
    fixtures = Path(__file__).parent / "fixtures"
    config = load_config()

    datasets = [
        (fixtures / "duplicate_python", "duplicate-heavy-python"),
        (fixtures / "template_heavy", "template-heavy"),
        (fixtures / "config_heavy", "config-heavy"),
        (fixtures / "mixed_project", "mixed-small"),
        (fixtures / "hierarchy_mirror", "hierarchy-mirror"),
    ]

    results = []
    for path, dataset_id in datasets:
        if not path.exists():
            print(f"SKIP {dataset_id}: path not found {path}")
            continue
        rec = run_sweep(path, dataset_id, config)
        results.append(rec)
        print(f"\n{'='*60}")
        print(f"Dataset: {dataset_id}")
        print(f"  file_count: {rec['file_count']}")
        print(f"  folder_count: {rec['folder_count']}")
        print(f"  raw_size: {rec['raw_size_bytes']:,} bytes")
        print(f"  folded_size: {rec['folded_size_bytes']:,} bytes")
        print(f"  logical_bytes_saved: {rec['logical_bytes_saved']:,}")
        print(f"  candidate_count: {rec['candidate_count']}")
        print(f"  committed_fold_count: {rec['committed_fold_count']}")
        print(f"  exact_reconstruction: {rec['exact_reconstruction_status']}")
        print(f"  report_export: {'ok' if rec['report_export_success'] else 'failed'}")
        print(f"  benchmark zip: {rec['benchmark']['baselines'].get('zip', 'N/A'):,}")
        print(f"  benchmark gzip: {rec['benchmark']['baselines'].get('gzip', 'N/A'):,}")
        print(f"  benchmark_suite: raw={rec.get('benchmark_suite',{}).get('raw','N/A'):,} "
              f"exact_only={rec.get('benchmark_suite',{}).get('infold_exact_only','N/A'):,} "
              f"exact+template={rec.get('benchmark_suite',{}).get('infold_exact_plus_template','N/A'):,} "
              f"zip={rec.get('benchmark_suite',{}).get('zip','N/A'):,} "
              f"gzip={rec.get('benchmark_suite',{}).get('gzip','N/A'):,}")

    # Invariant checks
    print(f"\n{'='*60}")
    print("Engine invariant checks:")
    for rec in results:
        ok = True
        if rec["committed_fold_count"] > 0:
            if rec["candidate_count"] < rec["committed_fold_count"]:
                ok = False  # can't commit more than candidates (unless we count per-target)
            if rec["exact_reconstruction_status"] != "ok":
                ok = False
        if not rec["report_export_success"]:
            ok = False
        status = "PASS" if ok else "FAIL"
        print(f"  {rec['project_id']}: {status}")

    # Write summary
    summary_path = fixtures / "sweep_summary.json"
    with open(summary_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2)
    print(f"\nSummary written to {summary_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
