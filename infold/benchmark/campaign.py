"""
Benchmark campaign: run full pack, record metrics, export csv/json/markdown.

Records: raw, zip, gzip, Infold physical/logical, exact reconstruction,
archive validate, integrity, fold counts, rejected by reason, times.
"""

import csv
import json
import tempfile
import time
from pathlib import Path
from typing import Any

from infold.reporting.benchmark import _gzip_size, _raw_size, _zip_size
from infold.reporting.report import build_report
from infold.benchmark.tuning_report import build_tuning_report, tuning_report_to_text


def run_benchmark_campaign(
    datasets: list[tuple[Path, str, str]],
    config: dict[str, Any],
    base_path: Path,
    create_archives: bool = True,
) -> list[dict[str, Any]]:
    """
    Run benchmark on each dataset. Returns list of result dicts.
    Each result includes: raw_bytes, zip_bytes, gzip_bytes, infold_physical_folded_size,
    infold_logical_gain, exact_reconstruction_status, archive_validate_status,
    integrity_status, fold_count, fold_count_by_operator, rejected_by_reason,
    archive_creation_time_s, reconstruction_time_s, profile, package_overhead.
    """
    from infold.engine import run_fold, export_package
    from infold.archive import create_archive, validate_archive, reconstruct_archive

    results: list[dict[str, Any]] = []
    excludes = list(config.get("project", {}).get("exclude_patterns", []))
    if "infold_sweep_report" not in excludes:
        excludes.append("infold_sweep_report")

    for path, dataset_id, category in datasets:
        if not path.exists():
            continue
        cfg = {**config, "project": {**config.get("project", {}), "id": dataset_id, "exclude_patterns": excludes}}
        row: dict[str, Any] = {
            "dataset_id": dataset_id,
            "category": category,
            "path": str(path),
        }

        # Run fold with profiling
        cfg["_profile"] = {}
        t0_fold = time.perf_counter()
        result = run_fold(path, cfg)
        row["fold_time_s"] = time.perf_counter() - t0_fold
        row["profile"] = cfg.get("_profile", {})

        sheet = result.project_sheet
        ledger = result.ledger
        raw = _raw_size(sheet)
        row["raw_bytes"] = raw
        row["zip_bytes"] = _zip_size(sheet)
        row["gzip_bytes"] = _gzip_size(sheet)
        row["infold_logical_gain"] = ledger.total_bytes_saved
        row["infold_physical_folded_size"] = raw - ledger.total_bytes_saved
        row["exact_reconstruction_status"] = "ok" if result.exact_reconstruction_ok else "failed"
        row["fold_count"] = ledger.total_folds
        fold_by_op: dict[str, int] = {}
        for r in ledger.fold_records:
            fold_by_op[r.operator_id] = fold_by_op.get(r.operator_id, 0) + 1
        row["fold_count_by_operator"] = fold_by_op

        rejected_by_reason: dict[str, int] = {}
        for rc in result.rejected_candidates:
            reason = rc.get("reason", "unknown")
            planner_dec = rc.get("planner_decision", "")
            key = f"{reason}:{planner_dec}" if planner_dec else reason
            rejected_by_reason[key] = rejected_by_reason.get(key, 0) + 1
        row["rejected_by_reason"] = rejected_by_reason

        row["archive_validate_status"] = "skipped"
        row["integrity_status"] = "skipped"
        row["archive_creation_time_s"] = None
        row["reconstruction_time_s"] = None
        row["package_overhead"] = None
        row["tuning_report"] = build_tuning_report(result, cfg, None)
        report = build_report(result, cfg)
        bfm = report.get("byte_fold_metrics") or {}
        row["byte_fold_metrics"] = bfm if bfm else None

        if create_archives:
            with tempfile.TemporaryDirectory(prefix="infold_campaign_") as tmp:
                archive_path = Path(tmp) / f"{dataset_id}.infold"
                t0_create = time.perf_counter()
                try:
                    create_archive(path, archive_path, cfg)
                    row["archive_creation_time_s"] = time.perf_counter() - t0_create
                    ok, errors = validate_archive(archive_path)
                    row["archive_validate_status"] = "ok" if ok else f"failed:{len(errors)}"
                    row["integrity_status"] = "ok" if ok and not any("integrity" in e.lower() for e in errors) else "failed"
                    # Package overhead (full audit)
                    import zipfile
                    with zipfile.ZipFile(archive_path, "r") as zf:
                        overhead: dict[str, int] = {}
                        total_archive = 0
                        for info in zf.infolist():
                            name = info.filename
                            size = info.file_size
                            total_archive += size
                            if name == "manifest.json":
                                overhead["manifest"] = overhead.get("manifest", 0) + size
                            elif name == "ledger.json":
                                overhead["ledger"] = overhead.get("ledger", 0) + size
                            elif name.startswith("shared/"):
                                overhead["shared"] = overhead.get("shared", 0) + size
                            elif name.startswith("maps/"):
                                overhead["maps"] = overhead.get("maps", 0) + size
                            elif name.startswith("reports/"):
                                overhead["reports"] = overhead.get("reports", 0) + size
                            elif name == "integrity.json":
                                overhead["integrity"] = overhead.get("integrity", 0) + size
                            elif name.startswith("snapshots/"):
                                overhead["snapshots"] = overhead.get("snapshots", 0) + size
                        row["package_overhead"] = overhead
                        row["archive_size_bytes"] = total_archive
                        row["tuning_report"] = build_tuning_report(result, cfg, overhead, total_archive)
                    # Reconstruction time
                    out_dir = Path(tmp) / "restored"
                    t0_recon = time.perf_counter()
                    reconstruct_archive(archive_path, out_dir)
                    row["reconstruction_time_s"] = time.perf_counter() - t0_recon
                except Exception as e:
                    row["archive_validate_status"] = f"error:{type(e).__name__}"
                    row["archive_creation_time_s"] = time.perf_counter() - t0_create

        results.append(row)
    return results


def export_campaign_csv(results: list[dict[str, Any]], out_path: Path) -> None:
    """Export campaign results to CSV (flattened)."""
    if not results:
        return
    rows = []
    for r in results:
        flat: dict[str, Any] = {
            "dataset_id": r.get("dataset_id", ""),
            "category": r.get("category", ""),
            "raw_bytes": r.get("raw_bytes", 0),
            "zip_bytes": r.get("zip_bytes", 0),
            "gzip_bytes": r.get("gzip_bytes", 0),
            "infold_physical_folded_size": r.get("infold_physical_folded_size", 0),
            "infold_logical_gain": r.get("infold_logical_gain", 0),
            "exact_reconstruction_status": r.get("exact_reconstruction_status", ""),
            "archive_validate_status": r.get("archive_validate_status", ""),
            "integrity_status": r.get("integrity_status", ""),
            "fold_count": r.get("fold_count", 0),
            "fold_time_s": r.get("fold_time_s"),
            "archive_creation_time_s": r.get("archive_creation_time_s"),
            "reconstruction_time_s": r.get("reconstruction_time_s"),
        }
        for op, cnt in (r.get("fold_count_by_operator") or {}).items():
            flat[f"fold_{op}"] = cnt
        for k, v in (r.get("rejected_by_reason") or {}).items():
            flat[f"rejected_{k.replace(':', '_')}"] = v
        rows.append(flat)
    keys = sorted(set().union(*(set(row.keys()) for row in rows)))
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=keys, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)


def export_campaign_json(results: list[dict[str, Any]], out_path: Path) -> None:
    """Export campaign results to JSON."""
    # Serialize for JSON (Path, etc.)
    out = []
    for r in results:
        c = {k: v for k, v in r.items() if k != "profile"}
        c["profile"] = r.get("profile", {})
        out.append(c)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)


def export_campaign_tuning_report(results: list[dict[str, Any]], out_path: Path) -> None:
    """Export tuning reports for each dataset."""
    lines = ["# Campaign Tuning Report", ""]
    for r in results:
        tr = r.get("tuning_report", {})
        if not tr:
            continue
        lines.append(tuning_report_to_text(tr, r.get("dataset_id", "")))
        lines.append("")
        lines.append("---")
        lines.append("")
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def export_campaign_markdown(results: list[dict[str, Any]], out_path: Path) -> None:
    """Export campaign results to Markdown table."""
    lines = [
        "# Infold Benchmark Campaign",
        "",
        "| Dataset | Category | Raw | ZIP | Gzip | Infold Phys | Infold Gain | Fold | Recon | Validate | Integrity |",
        "|---------|----------|-----|-----|------|-------------|-------------|------|-------|----------|-----------|",
    ]
    for r in results:
        did = r.get("dataset_id", "")
        cat = r.get("category", "")
        raw = r.get("raw_bytes", 0)
        zip_b = r.get("zip_bytes", 0)
        gz = r.get("gzip_bytes", 0)
        phys = r.get("infold_physical_folded_size", 0)
        gain = r.get("infold_logical_gain", 0)
        fold = r.get("fold_count", 0)
        recon = r.get("exact_reconstruction_status", "")
        val = r.get("archive_validate_status", "")
        integ = r.get("integrity_status", "")
        lines.append(f"| {did} | {cat} | {raw:,} | {zip_b:,} | {gz:,} | {phys:,} | {gain:,} | {fold} | {recon} | {val} | {integ} |")
    lines.extend(["", "## Times (s)", ""])
    for r in results:
        ft = r.get("fold_time_s")
        ct = r.get("archive_creation_time_s")
        rt = r.get("reconstruction_time_s")
        ft_s = f"{ft:.2f}" if ft is not None else "-"
        ct_s = f"{ct:.2f}" if ct is not None else "-"
        rt_s = f"{rt:.2f}" if rt is not None else "-"
        lines.append(f"- **{r.get('dataset_id', '')}**: fold={ft_s}, create={ct_s}, recon={rt_s}")

    bf_results = [r for r in results if r.get("byte_fold_metrics")]
    if bf_results:
        lines.extend([
            "",
            "## Byte Fold Metrics",
            "",
            "| Dataset | Category | files_chunk_folded | unique_chunk_count | reused_chunk_count | chunk_reused_bytes | chunk_reuse_ratio | chunk_dict_bytes |",
            "|---------|----------|-------------------|-------------------|--------------------|--------------------|-------------------|------------------|",
        ])
        for r in bf_results:
            bfm = r.get("byte_fold_metrics") or {}
            fcf = bfm.get("files_chunk_folded", 0)
            ucc = bfm.get("unique_chunk_count", 0)
            rcc = bfm.get("reused_chunk_count", 0)
            crb = bfm.get("chunk_reused_bytes", 0)
            crr = bfm.get("chunk_reuse_ratio")
            crr_s = f"{sum(crr)/len(crr):.2f}" if isinstance(crr, list) and crr else (f"{crr:.2f}" if isinstance(crr, (int, float)) else "-")
            cds = bfm.get("chunk_dictionary_size_bytes", 0)
            lines.append(f"| {r.get('dataset_id', '')} | {r.get('category', '')} | {fcf} | {ucc} | {rcc} | {crb:,} | {crr_s} | {cds:,} |")
        top_bf = sorted(bf_results, key=lambda x: (x.get("byte_fold_metrics") or {}).get("chunk_reused_bytes", 0), reverse=True)[:5]
        lines.extend(["", "Top Byte Fold contributing datasets (by chunk_reused_bytes):"])
        for r in top_bf:
            crb = (r.get("byte_fold_metrics") or {}).get("chunk_reused_bytes", 0)
            lines.append(f"  - {r.get('dataset_id', '')}: {crb:,} bytes")

    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
