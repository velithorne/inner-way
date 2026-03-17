"""
Infold CLI — entry point for fold operations.
"""

import argparse
import json
import sys
from pathlib import Path

from infold import __version__


def load_config(config_path: Path | None = None) -> dict:
    """Load configuration from JSON file."""
    if config_path is None:
        config_path = Path(__file__).parent / "config.json"
    with open(config_path, encoding="utf-8") as f:
        return json.load(f)


def verify_models() -> bool:
    """Verify core data classes can be imported and instantiated."""
    from infold.models import (
        ProjectSheet,
        FileNode,
        CandidateCrease,
        GainEstimate,
        StressEstimate,
        FoldRecord,
        FoldSimulationResult,
        ValidationResult,
        UnfoldResult,
    )

    # Smoke test: create minimal instances
    gain = GainEstimate(100, 10, 90, 0.8, 0.95)
    stress = StressEstimate(0.1, 0.2, 0.1, 0.0, 0.15)
    sheet = ProjectSheet(source_path=Path("."))
    return True


def verify_intake(config: dict) -> bool:
    """Verify intake layer: scan project and build inventory."""
    from infold.intake import scan_project

    source = Path(config["project"]["source_path"])
    if source == Path("."):
        source = Path(__file__).parent.parent  # workspace root
    excludes = config["project"].get("exclude_patterns", [])
    includes = config["project"].get("include_extensions", [])

    sheet = scan_project(source, exclude_patterns=excludes, include_extensions=includes)
    return sheet


def run_benchmark_suite_cmd(config: dict) -> int:
    """Run second-tier benchmark suite on configured datasets."""
    from infold.reporting import run_benchmark_suite, benchmark_suite_to_text

    suite_config = config.get("benchmark_suite", {})
    if not suite_config.get("enabled", True):
        print("Benchmark suite disabled in config")
        return 0
    datasets_raw = suite_config.get("tier2_datasets", [])
    base = Path(__file__).parent.parent
    datasets = [(base / p, did) for p, did in datasets_raw if p and did]
    results = run_benchmark_suite(datasets, config)
    print(benchmark_suite_to_text(results))
    return 0


def analyze_cmd(args) -> int:
    """Analyze project: fold + concise summary (no archive)."""
    from infold.workflows import analyze_project, analyze_to_text
    config = load_config()
    config["_fold_profile"] = getattr(args, "profile", "auto")
    try:
        data = analyze_project(getattr(args, "source", "."), config)
        if getattr(args, "json", False):
            print(json.dumps(data, indent=2))
        else:
            print(analyze_to_text(data))
        return 0
    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1


def run_benchmark_campaign_cmd(args) -> int:
    """Run full benchmark campaign with pack, profiling, export."""
    from infold.benchmark import (
        run_benchmark_campaign,
        export_campaign_csv,
        export_campaign_json,
        export_campaign_markdown,
        export_campaign_tuning_report,
        get_benchmark_datasets,
    )
    from infold.benchmark.pack import ensure_stress_datasets

    config = load_config()
    profile = getattr(args, "profile", "auto")
    config["_benchmark_profile"] = profile
    base = Path(__file__).parent.parent
    ensure_stress_datasets(base)
    datasets = get_benchmark_datasets(base)
    if not datasets:
        print("No benchmark datasets found")
        return 1
    create_archives = getattr(args, "archives", True)
    results = run_benchmark_campaign(datasets, config, base, create_archives=create_archives)
    out_dir = getattr(args, "output", None)
    if out_dir:
        out = Path(out_dir)
        out.mkdir(parents=True, exist_ok=True)
        export_campaign_csv(results, out / "campaign.csv")
        export_campaign_json(results, out / "campaign.json")
        export_campaign_markdown(results, out / "campaign.md")
        export_campaign_tuning_report(results, out / "tuning_report.md")
        print(f"Exported to {out}/campaign.csv, campaign.json, campaign.md, tuning_report.md")
    for r in results:
        print(f"{r['dataset_id']}: raw={r['raw_bytes']:,} fold={r['fold_count']} recon={r['exact_reconstruction_status']}")
    return 0


def run_profile_compare_cmd(args) -> int:
    """Run profile comparison benchmark: each dataset across all profiles."""
    from infold.benchmark import (
        run_profile_comparison,
        build_comparison_summary,
        export_comparison_csv,
        export_comparison_markdown,
        export_auto_vs_best_csv,
        get_benchmark_datasets,
    )
    from infold.benchmark.pack import ensure_stress_datasets

    config = load_config()
    base = Path(__file__).parent.parent
    ensure_stress_datasets(base)
    datasets = get_benchmark_datasets(base)
    if not datasets:
        print("No benchmark datasets found")
        return 1
    create_archives = getattr(args, "archives", True)
    comparison = run_profile_comparison(datasets, config, base, create_archives=create_archives)
    summary = build_comparison_summary(comparison)
    out = Path(getattr(args, "output", "results/profile_compare"))
    out.mkdir(parents=True, exist_ok=True)
    export_comparison_csv(summary, out / "profile_comparison.csv")
    export_auto_vs_best_csv(summary, out / "auto_vs_best.csv")
    export_comparison_markdown(summary, out / "profile_comparison.md")
    with open(out / "profile_comparison.json", "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)
    print(f"Exported to {out}/")
    s = summary.get("summary", {})
    print(f"Auto matched best physical: {s.get('auto_matched_best_physical_count', 0)}/{s.get('total_datasets', 0)} ({s.get('auto_matched_physical_pct', 0)}%)")
    print(f"Auto matched best gain: {s.get('auto_matched_best_gain_count', 0)}/{s.get('total_datasets', 0)} ({s.get('auto_matched_gain_pct', 0)}%)")
    return 0


def run_creature_compare_cmd(args) -> int:
    """Run creature comparison: static vs adaptive (fox profile)."""
    from infold.benchmark.creature_comparison import (
        run_creature_comparison,
        creature_comparison_to_markdown,
    )

    config = load_config()
    base = Path(__file__).parent.parent
    create_archives = getattr(args, "archives", True)
    data = run_creature_comparison(config, base, create_archives=create_archives)
    out = Path(getattr(args, "output", "results/creature_compare"))
    out.mkdir(parents=True, exist_ok=True)
    with open(out / "creature_comparison.json", "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    (out / "creature_comparison.md").write_text(creature_comparison_to_markdown(data), encoding="utf-8")
    print(f"Exported to {out}/creature_comparison.json, creature_comparison.md")
    s = data.get("summary", {})
    print(f"Adaptive wins physical: {s.get('adaptive_wins_physical', 0)}")
    print(f"Static wins physical: {s.get('static_wins_physical', 0)}")
    print(f"Both reconstruction OK: {s.get('both_reconstruction_ok', 0)}")
    return 0


def archive_showcase_cmd(args) -> int:
    """Full workflow: create, validate, reconstruct, save results."""
    from infold.workflows import real_project_showcase
    config = load_config()
    try:
        r = real_project_showcase(
            getattr(args, "source", "."),
            getattr(args, "output_dir", "showcase_out"),
            config=config,
            profile=getattr(args, "profile", "auto"),
        )
        print(f"Showcase saved to {r.get('output_dir', '?')}")
        print(f"  archive: {r.get('archive_path', '?')}")
        print(f"  restored: {r.get('output_dir', '?')}/restored/")
        print(f"  summary: {r.get('output_dir', '?')}/summary.md")
        print(f"  Validation: {'OK' if r.get('validation_ok') else 'FAILED'}")
        print(f"  Exact reconstruction: {'OK' if r.get('exact_reconstruction_ok') else 'FAILED'}")
        return 0 if r.get("validation_ok") and r.get("exact_reconstruction_ok") else 1
    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1


def archive_workflow_cmd(args) -> int:
    """Create + validate + summary in one command."""
    from infold.workflows import archive_workflow
    config = load_config()
    try:
        r = archive_workflow(
            getattr(args, "source", "."),
            getattr(args, "output", "out.infold"),
            config=config,
            profile=getattr(args, "profile", "auto"),
        )
        if getattr(args, "json", False):
            out = {k: v for k, v in r.items() if k != "summary"}
            out["validation_ok"] = r.get("validation_ok")
            out["explain"] = r.get("explain")
            print(json.dumps(out, indent=2))
        else:
            print(f"Created: {r.get('archive_path', '?')}")
            print(f"Validation: {'OK' if r.get('validation_ok') else 'FAILED'}")
            if r.get("validation_errors"):
                for e in r["validation_errors"]:
                    print(f"  Error: {e}")
            print("")
            print(r.get("summary", ""))
        return 0 if r.get("validation_ok") else 1
    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1


def archive_create(args) -> int:
    """Create Infold archive."""
    from infold.archive import create_archive
    config = load_config()
    if getattr(args, "compact", False):
        config = {**config}
        config["package_export"] = {
            **config.get("package_export", {}),
            "compact": True,
            "report_text": False,
            "inventory_minimal": True,
        }
    profile = getattr(args, "profile", "auto")
    config["_creature_adaptive"] = getattr(args, "creature", True)
    out = create_archive(args.source, args.output, config, profile=profile)
    print(f"Created: {out}")
    return 0


def archive_inspect(args) -> int:
    """Inspect archive."""
    from infold.archive import inspect_archive
    info = inspect_archive(args.archive)
    if getattr(args, "json", False):
        import json
        print(json.dumps(info, indent=2))
    else:
        print(f"Archive: {info['path']}")
        print(f"Source: {info['source_path']}")
        print(f"Files: {info['file_count']}, Folds: {info['fold_count']}")
        fp = info.get("manifest", {}).get("fold_profile")
        if fp:
            print(f"Fold profile: {fp}")
        print(f"Logical gain: {info['logical_gain_bytes']:,} bytes")
        print(f"Physical folded: {info['physical_folded_size_bytes']:,} bytes")
    return 0


def archive_validate(args) -> int:
    """Validate archive."""
    from infold.archive import validate_archive
    mode = getattr(args, "mode", "strict")
    ok, errors = validate_archive(args.archive, mode=mode)
    if ok:
        print("Valid")
        return 0
    print("Invalid:")
    for e in errors:
        print(f"  - {e}")
    return 1


def archive_reconstruct(args) -> int:
    """Reconstruct from archive."""
    from infold.archive import reconstruct_archive
    result = reconstruct_archive(args.archive, args.output)
    print(f"Reconstructed {len(result)} files to {args.output}")
    return 0


def archive_explain(args) -> int:
    """Explain archive contents."""
    from infold.archive import explain_archive
    from infold.archive.operations import explain_to_text
    info = explain_archive(args.archive)
    if getattr(args, "json", False):
        import json
        print(json.dumps(info, indent=2))
    else:
        print(explain_to_text(info))
    return 0


def archive_compare(args) -> int:
    """Compare two archives."""
    from infold.archive import compare_archives
    from infold.archive.operations import compare_to_text
    diff = compare_archives(args.archive_a, args.archive_b)
    if getattr(args, "json", False):
        import json
        print(json.dumps(diff, indent=2))
    else:
        print(compare_to_text(diff))
    return 0


def archive_list(args) -> int:
    """List shared artifacts and fold families."""
    from infold.archive import list_archive
    from infold.archive.operations import list_to_text
    info = list_archive(args.archive)
    if getattr(args, "json", False):
        import json
        print(json.dumps(info, indent=2))
    else:
        print(list_to_text(info))
    return 0


def archive_stats(args) -> int:
    """Show detailed archive stats."""
    from infold.archive import stats_archive
    from infold.archive.operations import stats_to_text
    stats = stats_archive(args.archive)
    if getattr(args, "json", False):
        import json
        print(json.dumps(stats, indent=2))
    else:
        print(stats_to_text(stats))
    return 0


def sync_capture_cmd(args) -> int:
    """Add snapshot + timeline summary in one command."""
    from infold.workflows import sync_capture
    config = load_config()
    try:
        r = sync_capture(
            getattr(args, "sync_dir", ".infold-sync"),
            getattr(args, "source", "."),
            config=config,
        )
        snap = r.get("snapshot", {})
        if getattr(args, "json", False):
            print(json.dumps({"snapshot": snap, "timeline": r.get("timeline", {})}, indent=2))
        else:
            print(f"Added snapshot {snap.get('id', '?')}: {snap.get('path', '?')}")
            print(f"  gain={snap.get('logical_gain_bytes', 0)} folds={snap.get('fold_count', 0)}")
            print("")
            print(r.get("timeline_text", ""))
        return 0
    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1


def sync_init_cmd(args) -> int:
    """Initialize sync directory."""
    from infold.sync import init_sync
    r = init_sync(
        getattr(args, "sync_dir", ".infold-sync"),
        source_path=Path(args.source),
    )
    print(f"Initialized: {r['sync_dir']}")
    return 0


def sync_add_cmd(args) -> int:
    """Create snapshot and add to lineage."""
    from infold.sync import create_and_add_snapshot
    config = load_config()
    r = create_and_add_snapshot(
        getattr(args, "sync_dir", ".infold-sync"),
        args.source,
        snapshot_id=getattr(args, "snapshot_id", None),
        config=config,
    )
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        s = r["snapshot"]
        print(f"Added snapshot {s['id']}: {s['path']}")
        print(f"  gain={s['logical_gain_bytes']} folds={s['fold_count']}")
    return 0


def sync_list_cmd(args) -> int:
    """List snapshots."""
    from infold.sync import list_snapshots, sync_list_to_text
    r = list_snapshots(getattr(args, "sync_dir", ".infold-sync"))
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(sync_list_to_text(r))
    return 0


def _resolve_sync_archive(ref: str, sync_dir: str) -> str:
    """Resolve archive ref (path, 'latest', 'previous') to path."""
    from infold.sync.operations import resolve_snapshot_ref
    if (ref or "").lower() in ("latest", "previous"):
        resolved = resolve_snapshot_ref(sync_dir, ref)
        if not resolved or not resolved.exists():
            raise FileNotFoundError(f"No {ref} snapshot in lineage")
        return str(resolved)
    return ref


def sync_compare_cmd(args) -> int:
    """Compare two snapshots."""
    from infold.archive.operations import compare_archives, compare_to_text
    sync_dir = getattr(args, "sync_dir", ".infold-sync")
    try:
        a = _resolve_sync_archive(args.archive_a, sync_dir)
        b = _resolve_sync_archive(args.archive_b, sync_dir)
    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    r = compare_archives(a, b)
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(compare_to_text(r))
    return 0


def sync_report_cmd(args) -> int:
    """Report added/removed/changed fold families."""
    from infold.sync import sync_report, sync_report_to_text
    sync_dir = getattr(args, "sync_dir", ".infold-sync")
    try:
        a = _resolve_sync_archive(args.archive_a, sync_dir)
        b = _resolve_sync_archive(args.archive_b, sync_dir)
    except FileNotFoundError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    r = sync_report(a, b)
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(sync_report_to_text(r))
    return 0


def sync_reconstruct_cmd(args) -> int:
    """Reconstruct snapshot (delegates to archive reconstruct). Supports 'latest'."""
    from infold.archive import reconstruct_archive
    from infold.sync.operations import resolve_snapshot_ref
    sync_dir = getattr(args, "sync_dir", ".infold-sync")
    archive_ref = args.archive
    if (archive_ref or "").lower() in ("latest", "previous"):
        resolved = resolve_snapshot_ref(sync_dir, archive_ref)
        if not resolved or not resolved.exists():
            print(f"Error: no {archive_ref} snapshot in lineage", file=sys.stderr)
            return 1
        archive_ref = str(resolved)
    reconstruct_archive(archive_ref, args.output)
    print(f"Reconstructed to {args.output}")
    return 0


def sync_validate_cmd(args) -> int:
    """Validate sync lineage."""
    from infold.sync import validate_sync
    r = validate_sync(getattr(args, "sync_dir", ".infold-sync"))
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        if r["valid"]:
            print("Valid")
        else:
            print("Invalid")
            for e in r.get("errors", []):
                print(f"  Error: {e}")
        for w in r.get("warnings", []):
            print(f"  Warning: {w}")
    return 0 if r["valid"] else 1


def sync_search_cmd(args) -> int:
    """Search across lineage snapshots."""
    from infold.sync import sync_search
    from infold.archive.operations import search_to_text
    r = sync_search(
        getattr(args, "sync_dir", ".infold-sync"),
        snapshot_id=getattr(args, "snapshot_id", None),
        snapshot_path=getattr(args, "snapshot_path", None),
        created_after=getattr(args, "created_after", None),
        created_before=getattr(args, "created_before", None),
        with_lineage=getattr(args, "with_lineage", False),
        operator=getattr(args, "operator", None),
        path=getattr(args, "path", None),
        family=getattr(args, "family", None),
    )
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(search_to_text(r))
    return 0


def sync_summary_cmd(args) -> int:
    """Show lineage summary."""
    from infold.sync import sync_summary, sync_summary_to_text
    r = sync_summary(getattr(args, "sync_dir", ".infold-sync"))
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(sync_summary_to_text(r))
    return 0


def sync_timeline_cmd(args) -> int:
    """Lineage timeline: gain/size/fold over time."""
    from infold.sync.operations import sync_timeline, sync_timeline_to_text
    r = sync_timeline(getattr(args, "sync_dir", ".infold-sync"))
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(sync_timeline_to_text(r))
    return 0


def sync_trace_cmd(args) -> int:
    """Trace family lifecycle: first_seen, last_seen, present_in_latest."""
    from infold.sync.operations import sync_trace, sync_trace_to_text
    r = sync_trace(
        getattr(args, "sync_dir", ".infold-sync"),
        family=getattr(args, "family", None),
        operator=getattr(args, "operator", None),
    )
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(sync_trace_to_text(r))
    return 0


def sync_lineage_report_cmd(args) -> int:
    """Change-focused lineage report: added/removed by interval."""
    from infold.sync.operations import sync_lineage_report, sync_lineage_report_to_text
    r = sync_lineage_report(getattr(args, "sync_dir", ".infold-sync"))
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(sync_lineage_report_to_text(r))
    return 0


def sync_diff_cmd(args) -> int:
    """Compare latest vs previous snapshot."""
    from infold.sync.operations import _get_latest_and_previous
    from infold.archive.operations import compare_archives, compare_to_text
    from pathlib import Path
    sync_dir = Path(getattr(args, "sync_dir", ".infold-sync"))
    latest, previous = _get_latest_and_previous(sync_dir)
    if not latest:
        print("Error: no snapshots in lineage", file=sys.stderr)
        return 1
    if not previous:
        print("Error: only one snapshot; need at least two for diff", file=sys.stderr)
        return 1
    r = compare_archives(previous["path"], latest["path"])
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(compare_to_text(r))
    return 0


def sync_report_diff_cmd(args) -> int:
    """Report added/removed/changed (latest vs previous)."""
    from infold.sync import sync_report, sync_report_to_text
    from infold.sync.operations import _get_latest_and_previous
    from pathlib import Path
    sync_dir = Path(getattr(args, "sync_dir", ".infold-sync"))
    latest, previous = _get_latest_and_previous(sync_dir)
    if not latest or not previous:
        print("Error: need at least two snapshots for report-diff", file=sys.stderr)
        return 1
    r = sync_report(previous["path"], latest["path"])
    if getattr(args, "json", False):
        print(json.dumps(r, indent=2))
    else:
        print(sync_report_to_text(r))
    return 0


def archive_search(args) -> int:
    """Search within archive(s). Path can be .infold file or directory of .infold archives."""
    from infold.archive import search_archive, search_archives
    from infold.archive.operations import search_to_text, search_to_csv, search_to_markdown, write_search_results

    target = Path(args.archive)
    if not target.exists():
        print(f"Error: path not found: {target}", file=sys.stderr)
        return 1

    search_kw = dict(
        operator=getattr(args, "operator", None),
        path=getattr(args, "path", None),
        family=getattr(args, "family", None),
        family_id=getattr(args, "family_id", None),
        artifact_id=getattr(args, "artifact_id", None),
        min_gain=getattr(args, "min_gain", None),
        max_gain=getattr(args, "max_gain", None),
        min_target_count=getattr(args, "min_targets", None),
        max_target_count=getattr(args, "max_targets", None),
        min_chunk_reused_bytes=getattr(args, "min_chunk_reused_bytes", None),
        max_chunk_reused_bytes=getattr(args, "max_chunk_reused_bytes", None),
        min_chunk_reuse_ratio=getattr(args, "min_chunk_reuse_ratio", None),
        max_chunk_reuse_ratio=getattr(args, "max_chunk_reuse_ratio", None),
        min_files_chunk_folded=getattr(args, "min_files_chunk_folded", None),
        max_files_chunk_folded=getattr(args, "max_files_chunk_folded", None),
        sort_by=getattr(args, "sort_by", None),
        rejected=getattr(args, "rejected", False),
        blocked=getattr(args, "blocked", False),
        superseded=getattr(args, "superseded", False),
        planner_decision=getattr(args, "planner_decision", None),
        diagnostics_only=getattr(args, "diagnostics_only", False),
        group_by=getattr(args, "group_by", None),
        explain=getattr(args, "explain", False),
    )
    _df = getattr(args, "debug_friendly", None)
    _df_val = None if _df is None else (_df == "true")
    archives_extra = dict(
        spec_version=getattr(args, "spec_version", None),
        reconstruction_mode=getattr(args, "reconstruction_mode", None),
        source_path=getattr(args, "source_path", None),
        debug_friendly=_df_val,
        created_after=getattr(args, "created_after", None),
        created_before=getattr(args, "created_before", None),
        min_logical_gain=getattr(args, "min_logical_gain", None),
        max_logical_gain=getattr(args, "max_logical_gain", None),
        min_physical_size=getattr(args, "min_physical_size", None),
        max_physical_size=getattr(args, "max_physical_size", None),
        min_rejection_count=getattr(args, "min_rejection_count", None),
        max_rejection_count=getattr(args, "max_rejection_count", None),
        min_fold_count=getattr(args, "min_fold_count", None),
        max_fold_count=getattr(args, "max_fold_count", None),
    )

    if target.is_file():
        result = search_archive(target, **search_kw)
    else:
        archives = sorted(target.glob("*.infold"))
        if not archives:
            print(f"No .infold archives found in {target}", file=sys.stderr)
            return 1
        result = search_archives(
            archives,
            archive_filter=getattr(args, "archive_filter", None),
            **search_kw,
            **archives_extra,
        )

    output_path = getattr(args, "output", None)
    export = getattr(args, "export", None)
    use_json = export == "json" or getattr(args, "json", False)

    if output_path:
        fmt = "json"
        if export == "csv":
            fmt = "csv"
        elif export == "markdown":
            fmt = "markdown"
        elif use_json or export == "json":
            fmt = "json"
        written = write_search_results(result, output_path, format=fmt)
        print(f"Wrote: {written}")
    elif export == "csv":
        print(search_to_csv(result))
    elif export == "markdown":
        print(search_to_markdown(result))
    elif use_json:
        print(json.dumps(result, indent=2))
    else:
        print(search_to_text(result))
    return 0


def main() -> int:
    """Main CLI entry point."""
    parser = argparse.ArgumentParser(description="Infold Core — structure-aware folding engine")
    parser.add_argument("--benchmark-suite", action="store_true", help="Run second-tier benchmark suite")
    parser.add_argument("--benchmark-campaign", action="store_true", help="Run full benchmark campaign")
    parser.add_argument("--campaign-output", dest="campaign_output", help="Output dir for campaign csv/json/md")
    parser.add_argument("--no-archives", dest="archives", action="store_false", default=True, help="Skip archive create/validate (with --benchmark-campaign)")
    parser.add_argument("--profile-compare", action="store_true", help="Run profile comparison benchmark (all profiles per dataset)")
    parser.add_argument("--profile-compare-output", dest="profile_compare_output", help="Output dir for profile comparison results")
    parser.add_argument("--creature-compare", action="store_true", help="Run creature comparison (static vs adaptive)")
    parser.add_argument("--creature-compare-output", dest="creature_compare_output", help="Output dir for creature comparison results")
    parser.add_argument("--profile", default="auto", help="Fold profile for create/campaign: auto, sparrow, fox, dragon, golem, serpent")
    subparsers = parser.add_subparsers(dest="command", help="Commands")
    analyze_p = subparsers.add_parser("analyze", help="Analyze project: fold + concise summary (no archive)")
    analyze_p.add_argument("--source", required=True, help="Source project path")
    analyze_p.add_argument("--profile", default="auto", help="Fold profile")
    analyze_p.add_argument("--json", action="store_true", help="JSON output")
    analyze_p.set_defaults(func=analyze_cmd)
    archive_parser = subparsers.add_parser("archive", help="Infold Archive commands")
    archive_parser.add_argument("--json", action="store_true", help="Machine-readable JSON output")
    archive_sub = archive_parser.add_subparsers(dest="archive_cmd")
    create_p = archive_sub.add_parser("create", help="Create archive")
    create_p.add_argument("--source", required=True, help="Source path")
    create_p.add_argument("--output", required=True, help="Output .infold path")
    create_p.add_argument("--profile", default="auto", dest="profile", help="Fold profile: auto, sparrow, fox, dragon, golem, serpent")
    create_p.add_argument("--compact", action="store_true", help="Use compact package format (smaller archive)")
    create_p.add_argument("--no-creature", dest="creature", action="store_false", default=True, help="Disable creature adaptation (static profile only)")
    create_p.set_defaults(func=archive_create)
    workflow_p = archive_sub.add_parser("workflow", help="Create + validate + summary in one command")
    workflow_p.add_argument("--source", required=True, help="Source path")
    workflow_p.add_argument("--output", required=True, help="Output .infold path")
    workflow_p.add_argument("--profile", default="auto", help="Fold profile")
    workflow_p.add_argument("--json", action="store_true", help="JSON output")
    workflow_p.set_defaults(func=archive_workflow_cmd)
    showcase_p = archive_sub.add_parser("showcase", help="Full workflow: create, validate, reconstruct, save results")
    showcase_p.add_argument("--source", required=True, help="Source path")
    showcase_p.add_argument("--output-dir", dest="output_dir", required=True, help="Output directory for archive, restored, summary")
    showcase_p.add_argument("--profile", default="auto", help="Fold profile")
    showcase_p.set_defaults(func=archive_showcase_cmd)
    inspect_p = archive_sub.add_parser("inspect", help="Inspect archive")
    inspect_p.add_argument("archive", help="Archive path")
    inspect_p.add_argument("--json", action="store_true", help="JSON output")
    inspect_p.set_defaults(func=archive_inspect)
    validate_p = archive_sub.add_parser("validate", help="Validate archive")
    validate_p.add_argument("archive", help="Archive path")
    validate_p.add_argument("--mode", choices=["basic", "strict", "integrity-only", "schema-only"], default="strict", help="Validation mode")
    validate_p.set_defaults(func=archive_validate)
    reconstruct_p = archive_sub.add_parser("reconstruct", help="Reconstruct from archive")
    reconstruct_p.add_argument("archive", help="Archive path")
    reconstruct_p.add_argument("--output", required=True, help="Output directory")
    reconstruct_p.set_defaults(func=archive_reconstruct)
    explain_p = archive_sub.add_parser("explain", help="Explain archive contents")
    explain_p.add_argument("archive", help="Archive path")
    explain_p.add_argument("--json", action="store_true", help="JSON output")
    explain_p.set_defaults(func=archive_explain)
    list_p = archive_sub.add_parser("list", help="List shared artifacts and fold families")
    list_p.add_argument("archive", help="Archive path")
    list_p.add_argument("--json", action="store_true", help="JSON output")
    list_p.set_defaults(func=archive_list)
    stats_p = archive_sub.add_parser("stats", help="Detailed archive stats")
    stats_p.add_argument("archive", help="Archive path")
    stats_p.add_argument("--json", action="store_true", help="JSON output")
    stats_p.set_defaults(func=archive_stats)
    compare_p = archive_sub.add_parser("compare", help="Compare two archives")
    compare_p.add_argument("archive_a", help="First archive path")
    compare_p.add_argument("archive_b", help="Second archive path")
    compare_p.add_argument("--json", action="store_true", help="JSON output")
    compare_p.set_defaults(func=archive_compare)
    search_p = archive_sub.add_parser("search", help="Search within archive(s). Path: .infold file or directory of archives.")
    search_p.add_argument("archive", help="Archive path or directory containing .infold files")
    search_p.add_argument("--operator", help="Filter by operator (exact_repetition, template_skeleton, etc.)")
    search_p.add_argument("--path", help="Filter by path substring (contains match)")
    search_p.add_argument("--family", help="Filter by family type (duplicate, template, symbol, hierarchy, dependency, byte_fold)")
    search_p.add_argument("--family-id", type=int, dest="family_id", help="Filter by family index")
    search_p.add_argument("--artifact-id", type=int, dest="artifact_id", help="Filter by artifact index")
    search_p.add_argument("--min-gain", type=int, dest="min_gain", help="Minimum gain (bytes)")
    search_p.add_argument("--max-gain", type=int, dest="max_gain", help="Maximum gain (bytes)")
    search_p.add_argument("--min-targets", type=int, dest="min_targets", help="Minimum target count")
    search_p.add_argument("--max-targets", type=int, dest="max_targets", help="Maximum target count")
    search_p.add_argument("--min-chunk-reused-bytes", type=int, dest="min_chunk_reused_bytes", help="Byte Fold: min chunk_reused_bytes")
    search_p.add_argument("--max-chunk-reused-bytes", type=int, dest="max_chunk_reused_bytes", help="Byte Fold: max chunk_reused_bytes")
    search_p.add_argument("--min-chunk-reuse-ratio", type=float, dest="min_chunk_reuse_ratio", help="Byte Fold: min chunk_reuse_ratio")
    search_p.add_argument("--max-chunk-reuse-ratio", type=float, dest="max_chunk_reuse_ratio", help="Byte Fold: max chunk_reuse_ratio")
    search_p.add_argument("--min-files-chunk-folded", type=int, dest="min_files_chunk_folded", help="Byte Fold: min files_chunk_folded")
    search_p.add_argument("--max-files-chunk-folded", type=int, dest="max_files_chunk_folded", help="Byte Fold: max files_chunk_folded")
    search_p.add_argument("--archive-filter", dest="archive_filter", help="Filter archives by path substring (multi-archive)")
    search_p.add_argument("--sort-by", dest="sort_by", choices=["gain", "operator", "archive", "target_count"], help="Sort results")
    search_p.add_argument("--rejected", action="store_true", help="Include rejected candidates")
    search_p.add_argument("--blocked", action="store_true", help="Include blocked folds")
    search_p.add_argument("--superseded", action="store_true", help="Include superseded folds")
    search_p.add_argument("--planner-decision", dest="planner_decision", help="Filter by planner decision (e.g. reject_conflict)")
    search_p.add_argument("--diagnostics-only", dest="diagnostics_only", action="store_true", help="Search only diagnostics (no folds)")
    search_p.add_argument("--group-by", dest="group_by", choices=["archive", "operator", "family"], help="Group results")
    search_p.add_argument("--explain", action="store_true", help="Add match explanation to each result")
    search_p.add_argument("--spec-version", dest="spec_version", help="Filter archives by spec_version (multi-archive)")
    search_p.add_argument("--reconstruction-mode", dest="reconstruction_mode", help="Filter by reconstruction_mode")
    search_p.add_argument("--source-path", dest="source_path", help="Filter by source_path substring")
    search_p.add_argument("--debug-friendly", dest="debug_friendly", choices=["true", "false"], help="Filter archives by debug_friendly (multi-archive)")
    search_p.add_argument("--created-after", dest="created_after", help="Filter archives created after ISO timestamp (multi-archive)")
    search_p.add_argument("--created-before", dest="created_before", help="Filter archives created before ISO timestamp (multi-archive)")
    search_p.add_argument("--min-logical-gain", type=int, dest="min_logical_gain", help="Filter archives by min logical gain")
    search_p.add_argument("--max-logical-gain", type=int, dest="max_logical_gain", help="Filter archives by max logical gain")
    search_p.add_argument("--min-physical-size", type=int, dest="min_physical_size", help="Filter archives by min physical size")
    search_p.add_argument("--max-physical-size", type=int, dest="max_physical_size", help="Filter archives by max physical size")
    search_p.add_argument("--min-rejection-count", type=int, dest="min_rejection_count", help="Filter archives by min rejection count")
    search_p.add_argument("--max-rejection-count", type=int, dest="max_rejection_count", help="Filter archives by max rejection count")
    search_p.add_argument("--min-fold-count", type=int, dest="min_fold_count", help="Filter archives by min fold count")
    search_p.add_argument("--max-fold-count", type=int, dest="max_fold_count", help="Filter archives by max fold count")
    search_p.add_argument("--output", "-o", dest="output", help="Write results to file (json/csv/markdown by --export)")
    search_p.add_argument("--export", choices=["json", "csv", "markdown"], help="Export format (overrides --json)")
    search_p.add_argument("--json", action="store_true", help="JSON output")
    search_p.set_defaults(func=archive_search)
    sync_p = archive_sub.add_parser("sync", help="Infold Sync: versioned snapshots, lineage, compare, report")
    sync_sub = sync_p.add_subparsers(dest="sync_cmd")
    sync_capture_p = sync_sub.add_parser("capture", help="Add snapshot + timeline summary in one command")
    sync_capture_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_capture_p.add_argument("--source", required=True, help="Source path")
    sync_capture_p.add_argument("--json", action="store_true", help="JSON output")
    sync_capture_p.set_defaults(func=sync_capture_cmd)
    sync_init_p = sync_sub.add_parser("init", help="Initialize sync directory with lineage manifest")
    sync_init_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory (default: .infold-sync)")
    sync_init_p.add_argument("--source", required=True, help="Source project path")
    sync_init_p.set_defaults(func=sync_init_cmd)
    sync_add_p = sync_sub.add_parser("add", help="Create snapshot from source and add to lineage")
    sync_add_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_add_p.add_argument("--source", required=True, help="Source project path")
    sync_add_p.add_argument("--id", dest="snapshot_id", help="Snapshot ID (default: v1, v2, ...)")
    sync_add_p.add_argument("--json", action="store_true", help="JSON output")
    sync_add_p.set_defaults(func=sync_add_cmd)
    sync_list_p = sync_sub.add_parser("list", help="List snapshots in lineage")
    sync_list_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_list_p.add_argument("--json", action="store_true", help="JSON output")
    sync_list_p.set_defaults(func=sync_list_cmd)
    sync_compare_p = sync_sub.add_parser("compare", help="Compare two snapshots structurally")
    sync_compare_p.add_argument("archive_a", help="First snapshot (path or 'latest'/'previous')")
    sync_compare_p.add_argument("archive_b", help="Second snapshot (path or 'latest'/'previous')")
    sync_compare_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory (for latest/previous)")
    sync_compare_p.add_argument("--json", action="store_true", help="JSON output")
    sync_compare_p.set_defaults(func=sync_compare_cmd)
    sync_report_p = sync_sub.add_parser("report", help="Report added/removed/changed fold families")
    sync_report_p.add_argument("archive_a", help="First snapshot (path or 'latest'/'previous')")
    sync_report_p.add_argument("archive_b", help="Second snapshot (path or 'latest'/'previous')")
    sync_report_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory (for latest/previous)")
    sync_report_p.add_argument("--json", action="store_true", help="JSON output")
    sync_report_p.set_defaults(func=sync_report_cmd)
    sync_reconstruct_p = sync_sub.add_parser("reconstruct", help="Reconstruct chosen snapshot (archive path or 'latest')")
    sync_reconstruct_p.add_argument("archive", help="Snapshot path or 'latest'")
    sync_reconstruct_p.add_argument("--output", "-o", required=True, help="Output directory")
    sync_reconstruct_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory (for 'latest')")
    sync_reconstruct_p.set_defaults(func=sync_reconstruct_cmd)
    sync_validate_p = sync_sub.add_parser("validate", help="Validate lineage structure and snapshot archives")
    sync_validate_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_validate_p.add_argument("--json", action="store_true", help="JSON output")
    sync_validate_p.set_defaults(func=sync_validate_cmd)
    sync_search_p = sync_sub.add_parser("search", help="Search across all snapshots in lineage")
    sync_search_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_search_p.add_argument("--snapshot-id", dest="snapshot_id", help="Filter by snapshot ID")
    sync_search_p.add_argument("--snapshot-path", dest="snapshot_path", help="Filter by path substring")
    sync_search_p.add_argument("--created-after", dest="created_after", help="Filter snapshots created after ISO timestamp")
    sync_search_p.add_argument("--created-before", dest="created_before", help="Filter snapshots created before ISO timestamp")
    sync_search_p.add_argument("--operator", help="Search filter: operator")
    sync_search_p.add_argument("--path", help="Search filter: path substring")
    sync_search_p.add_argument("--family", help="Search filter: family type")
    sync_search_p.add_argument("--with-lineage", action="store_true", help="Add first_seen, last_seen, lineage_tracking to results")
    sync_search_p.add_argument("--json", action="store_true", help="JSON output")
    sync_search_p.set_defaults(func=sync_search_cmd)
    sync_summary_p = sync_sub.add_parser("summary", help="Show lineage summary")
    sync_summary_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_summary_p.add_argument("--json", action="store_true", help="JSON output")
    sync_summary_p.set_defaults(func=sync_summary_cmd)
    sync_diff_p = sync_sub.add_parser("diff", help="Compare latest vs previous snapshot")
    sync_diff_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_diff_p.add_argument("--json", action="store_true", help="JSON output")
    sync_diff_p.set_defaults(func=sync_diff_cmd)
    sync_report_diff_p = sync_sub.add_parser("report-diff", help="Report added/removed/changed (latest vs previous)")
    sync_report_diff_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_report_diff_p.add_argument("--json", action="store_true", help="JSON output")
    sync_report_diff_p.set_defaults(func=sync_report_diff_cmd)
    sync_timeline_p = sync_sub.add_parser("timeline", help="Lineage timeline: gain/size/fold over time")
    sync_timeline_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_timeline_p.add_argument("--json", action="store_true", help="JSON output")
    sync_timeline_p.set_defaults(func=sync_timeline_cmd)
    sync_trace_p = sync_sub.add_parser("trace", help="Trace family lifecycle: first_seen, last_seen, present_in_latest")
    sync_trace_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_trace_p.add_argument("--family", help="Filter by family: duplicate, template, hierarchy, dependency, byte_fold")
    sync_trace_p.add_argument("--operator", help="Filter by operator")
    sync_trace_p.add_argument("--json", action="store_true", help="JSON output")
    sync_trace_p.set_defaults(func=sync_trace_cmd)
    sync_lineage_report_p = sync_sub.add_parser("lineage-report", help="Change-focused report: added/removed families by interval")
    sync_lineage_report_p.add_argument("--dir", dest="sync_dir", default=".infold-sync", help="Sync directory")
    sync_lineage_report_p.add_argument("--json", action="store_true", help="JSON output")
    sync_lineage_report_p.set_defaults(func=sync_lineage_report_cmd)
    args = parser.parse_args()

    if args.benchmark_suite:
        config = load_config()
        return run_benchmark_suite_cmd(config)
    if args.benchmark_campaign:
        class CampaignArgs:
            pass
        ca = CampaignArgs()
        ca.output = getattr(args, "campaign_output", None)
        ca.archives = getattr(args, "archives", True)
        ca.profile = getattr(args, "profile", "auto")
        return run_benchmark_campaign_cmd(ca)
    if getattr(args, "profile_compare", False):
        class ProfileCompareArgs:
            pass
        pca = ProfileCompareArgs()
        pca.output = getattr(args, "profile_compare_output", None) or "results/profile_compare"
        pca.archives = getattr(args, "archives", True)
        return run_profile_compare_cmd(pca)
    if getattr(args, "creature_compare", False):
        class CreatureCompareArgs:
            pass
        cca = CreatureCompareArgs()
        cca.output = getattr(args, "creature_compare_output", None) or "results/creature_compare"
        cca.archives = getattr(args, "archives", True)
        return run_creature_compare_cmd(cca)
    if args.command == "archive":
        if hasattr(args, "func") and args.func is not None:
            return args.func(args)
        archive_parser.print_help()
        return 1
    if args.command == "analyze":
        if hasattr(args, "func") and args.func is not None:
            return args.func(args)
        return 1

    print(f"Infold Core v{__version__}")
    print("Structure-aware folding engine for code projects and structured text")
    print()
    try:
        config = load_config()
        print("✓ Config loaded successfully")
        print(f"  - Project include extensions: {config['project']['include_extensions']}")
        print(f"  - Operators enabled: {[k for k, v in config['operators'].items() if v.get('enabled')]}")
        print()
        verify_models()
        print("✓ Core data models OK (ProjectSheet, FileNode, CandidateCrease, etc.)")
        print()
        sheet = verify_intake(config)
        print("✓ Intake layer OK — project scanned")
        print(f"  - Files: {sheet.metrics.get('file_count', 0)}")
        print(f"  - Folders: {sheet.metrics.get('folder_count', 0)}")
        print(f"  - Original size: {sheet.metrics.get('original_size_bytes', 0):,} bytes")
        print()
        from infold.parsers import parse_project

        parse_project(sheet)
        py_files = [n for n in sheet.file_nodes.values() if n.language == "python"]
        total_tokens = sum(len(n.tokens) for n in sheet.file_nodes.values())
        total_symbols = sum(len(n.symbols) for n in sheet.file_nodes.values())
        total_imports = sum(len(n.imports) for n in sheet.file_nodes.values())
        print("✓ Parsers OK — Python + text fallback")
        print(f"  - Python files: {len(py_files)}")
        print(f"  - Total tokens: {total_tokens}")
        print(f"  - Total symbols: {total_symbols}")
        print(f"  - Total imports: {total_imports}")
        print()
        from infold.operators import NoOpOperator

        op = NoOpOperator()
        candidates = op.detect_candidates(sheet, config)
        assert op.operator_id() == "noop"
        assert op.scope() == "none"
        assert len(candidates) == 0
        print("✓ Base operator interface OK")
        print(f"  - NoOpOperator: {op.operator_name()}, scope={op.scope()}")
        print(f"  - detect_candidates returned {len(candidates)} (expected 0)")
        print()
        from infold.validation import validate_candidate
        from infold.models import CandidateCrease, GainEstimate, StressEstimate

        synthetic = CandidateCrease(
            operator_id="noop",
            targets=["test"],
            gain=GainEstimate(0, 0, 0, 0.0, 0.0),
            stress=StressEstimate(0.0, 0.0, 0.0, 0.0, 0.0),
            invariants=[],
            metadata={},
        )
        vr = validate_candidate(op, synthetic, sheet, config)
        assert vr.accepted
        print("✓ Validation pipeline OK")
        print(f"  - validate_candidate(NoOp, synthetic) -> accepted={vr.accepted}")
        print()
        from infold.operators import ExactRepetitionOperator
        from infold.models import FileNode, ProjectSheet
        import hashlib

        # Exact Repetition: create sheet with 2 identical files (min 64 bytes)
        dup_content = ("x = 1\ny = 2\nz = x + y\n" * 5)  # ~90 bytes
        dup_hash = hashlib.sha256(dup_content.encode("utf-8")).hexdigest()
        base = Path(__file__).parent.parent
        dup_sheet = ProjectSheet(source_path=base)
        for name in ["dup_a.py", "dup_b.py"]:
            dup_sheet.file_nodes[Path(name)] = FileNode(
                path=Path(name),
                language="python",
                raw_text=dup_content,
                tokens=[],
                ast_data=None,
                symbols=[],
                imports=[],
                raw_hash=dup_hash,
                token_hash=None,
                parser_confidence=1.0,
                diagnostics=[],
            )
        ex_op = ExactRepetitionOperator()
        ex_candidates = ex_op.detect_candidates(dup_sheet, config)
        assert len(ex_candidates) == 1
        c = ex_candidates[0]
        vr2 = validate_candidate(ex_op, c, dup_sheet, config)
        assert vr2.accepted
        record = ex_op.apply(c, dup_sheet, config)
        unfolded = ex_op.unfold(record, None, config)
        assert len(unfolded) == 2
        assert unfolded[Path("dup_a.py")] == dup_content
        assert unfolded[Path("dup_b.py")] == dup_content
        print("✓ Exact Repetition Fold OK")
        print(f"  - Found {len(ex_candidates)} candidate(s), gain={record.gain} bytes")
        print(f"  - Unfold restored {len(unfolded)} files correctly")
        print()
        from infold.engine import run_fold

        source = Path(__file__).parent.parent
        result = run_fold(source, config)
        print("✓ Fold engine orchestration OK")
        print(f"  - Ledger: {result.ledger.total_folds} folds, {result.ledger.total_bytes_saved} bytes saved")
        print(f"  - Errors: {len(result.errors)}")
        print()
        from infold.reporting import export_report, run_benchmark, benchmark_to_text

        json_path, text_path = export_report(result, config)
        print("✓ Reporting OK")
        print(f"  - Exported: {json_path}, {text_path}")
        bench = run_benchmark(result, config)
        print()
        print("✓ Benchmark OK")
        print(benchmark_to_text(bench))
        from infold.engine import export_package

        pkg_path = export_package(result, config, "infold_package")
        print()
        print("✓ Package export OK")
        print(f"  - {pkg_path}/")
        print(f"  - manifest.json, ledger.json, shared/, maps/, reports/, snapshots/")
        return 0
    except Exception as e:
        print(f"✗ Error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
