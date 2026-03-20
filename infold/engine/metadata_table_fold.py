"""
Metadata Table Fold: second byte operator. Reduces package overhead by folding
repeated metadata (paths, strings) into compact shared tables.

Runs after package export, before integrity. Metadata-level only; does not
modify source content. Preserves exact reconstruction.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

PATH_TABLE_FILENAME = "path_table.json"
METADATA_TABLES_DIR = "metadata_tables"
MIN_NET_GAIN_BYTES = 32


def _apply_shared_artifact_path_refs(shared_dir: Path, path_to_ref: dict[str, int]) -> None:
    """
    Phase 21A: Rewrite shared operator artifacts to use path_refs instead of paths.
    Saves bytes when paths are long and repeated. Deterministic.
    """
    for f in shared_dir.glob("template_*.json"):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            paths = data.get("paths", [])
            if paths and all(str(p) in path_to_ref for p in paths):
                data["path_refs"] = [path_to_ref[str(p)] for p in paths]
                del data["paths"]
                data["path_table_ref"] = True
                f.write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
        except Exception:
            pass
    for f in shared_dir.glob("mutation_chain_*.json"):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            paths = data.get("paths", [])
            if paths and all(str(p) in path_to_ref for p in paths):
                data["path_refs"] = [path_to_ref[str(p)] for p in paths]
                del data["paths"]
                data["path_table_ref"] = True
                f.write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
        except Exception:
            pass


def load_path_table(root: Path) -> list[str] | None:
    """Load path table from package if present. Returns None if not using metadata table fold.
    Supports Path DNA format (Phase 14A): when path_dna key exists, expands to full paths."""
    pt_path = root / "shared" / METADATA_TABLES_DIR / PATH_TABLE_FILENAME
    if not pt_path.exists():
        return None
    data = json.loads(pt_path.read_text(encoding="utf-8"))
    if "path_dna" in data:
        from infold.engine.path_dna import expand_path_dna
        return expand_path_dna(data["path_dna"])
    return data.get("p", data.get("paths", []))


def _collect_paths_from_package(pkg_dir: Path) -> set[str]:
    """Collect path strings from targeted package files (maps, snapshots). Deterministic."""
    paths: set[str] = set()
    maps_dir = pkg_dir / "maps"
    snapshots_dir = pkg_dir / "snapshots"

    if (maps_dir / "reconstruction.json").exists():
        data = json.loads((maps_dir / "reconstruction.json").read_text(encoding="utf-8"))
        for rec in data.get("records", []):
            for t in rec.get("targets", []):
                if isinstance(t, str):
                    paths.add(t)

    if (maps_dir / "chunk_reconstruction.json").exists():
        data = json.loads((maps_dir / "chunk_reconstruction.json").read_text(encoding="utf-8"))
        for rec in data.get("records", []):
            for p in rec.get("paths", []):
                if isinstance(p, str):
                    paths.add(p)

    if (snapshots_dir / "inventory.json").exists():
        data = json.loads((snapshots_dir / "inventory.json").read_text(encoding="utf-8"))
        for item in data.get("files", []):
            p = item.get("path")
            if isinstance(p, str):
                paths.add(p)

    if (snapshots_dir / "passthrough.json").exists():
        data = json.loads((snapshots_dir / "passthrough.json").read_text(encoding="utf-8"))
        for k in data.keys():
            if isinstance(k, str):
                paths.add(k)

    return paths


def _build_path_table(paths: set[str]) -> list[str]:
    """Build deterministic ordered path table."""
    return sorted(paths)


def _estimate_gain(
    paths: set[str],
    path_table: list[str],
    path_to_ref: dict[str, int],
) -> tuple[int, int, int]:
    """
    Estimate bytes: (gross_saved, table_overhead, net_saved).
    gross_saved = sum(len(p) for each occurrence) - sum(ref size in JSON)
    ref in JSON: integer like 0, 1, 2 -> ~1-4 chars. Path ~20-80 chars.
    """
    path_to_count: dict[str, int] = {}
    maps_dir = Path("/dummy")
    # We need occurrence counts. Re-collect from files.
    return 0, 0, 0  # Placeholder; we'll compute in apply


def _compute_occurrences(pkg_dir: Path) -> dict[str, int]:
    """Count how many times each path string appears in targeted package files."""
    counts: dict[str, int] = {}
    maps_dir = pkg_dir / "maps"
    snapshots_dir = pkg_dir / "snapshots"

    def add(p: str) -> None:
        if isinstance(p, str):
            counts[p] = counts.get(p, 0) + 1

    if (maps_dir / "reconstruction.json").exists():
        data = json.loads((maps_dir / "reconstruction.json").read_text(encoding="utf-8"))
        for rec in data.get("records", []):
            for t in rec.get("targets", []):
                add(t)

    if (maps_dir / "chunk_reconstruction.json").exists():
        data = json.loads((maps_dir / "chunk_reconstruction.json").read_text(encoding="utf-8"))
        for rec in data.get("records", []):
            for p in rec.get("paths", []):
                add(p)

    if (snapshots_dir / "inventory.json").exists():
        data = json.loads((snapshots_dir / "inventory.json").read_text(encoding="utf-8"))
        for item in data.get("files", []):
            add(item.get("path", ""))

    if (snapshots_dir / "passthrough.json").exists():
        data = json.loads((snapshots_dir / "passthrough.json").read_text(encoding="utf-8"))
        for k in data.keys():
            add(k)

    return counts


def apply_family_membranes(
    pkg_dir: Path,
    config: dict[str, Any],
    metadata_table_applied: bool = False,
) -> dict[str, Any] | None:
    """
    Phase 14A: Compact operator_id in reconstruction.json when micro mode.
    When metadata_table_applied, Family Membranes are already applied.
    When not, apply membranes-only compaction.
    """
    if not config.get("_micro_mode", False) or metadata_table_applied:
        return None
    maps_dir = pkg_dir / "maps"
    recon_path = maps_dir / "reconstruction.json"
    if not recon_path.exists():
        return None
    data = json.loads(recon_path.read_text(encoding="utf-8"))
    records = data.get("records", [])
    if not records or data.get("family_membranes"):
        return None
    op_to_idx: dict[str, int] = {}
    op_ids: list[str] = []
    for rec in records:
        op = rec.get("operator_id", "")
        if op not in op_to_idx:
            op_to_idx[op] = len(op_ids)
            op_ids.append(op)
        rec["o"] = op_to_idx[op]
        del rec["operator_id"]
    data["operator_ids"] = op_ids
    data["family_membranes"] = True
    recon_path.write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
    manifest_path = pkg_dir / "manifest.json"
    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["family_membranes"] = True
        compact = config.get("package_export", {}).get("compact", False)
        manifest_path.write_text(
            json.dumps(manifest, indent=None if compact else 2, separators=(",", ":") if compact else (", ", ": ")),
            encoding="utf-8",
        )
    report_path = pkg_dir / "reports" / "report.json"
    if report_path.exists():
        report = json.loads(report_path.read_text(encoding="utf-8"))
        report["phase14a_tiny_archive"] = {"path_dna_folding": False, "family_membranes": True}
        compact = config.get("package_export", {}).get("compact", False)
        report_path.write_text(
            json.dumps(report, indent=None if compact else 2, separators=(",", ":") if compact else (", ", ": ")),
            encoding="utf-8",
        )
    return {"family_membranes": True}


def apply_metadata_table_fold(
    pkg_dir: Path,
    config: dict[str, Any],
) -> dict[str, Any] | None:
    """
    Apply Metadata Table Fold to package. Rewrites maps/snapshots with path refs.
    Returns metrics dict if applied, None if skipped (low gain).
    Phase 10C: When Tesseract cooperation allows metadata follow-up, use lower min_net threshold.
    """
    cfg = config.get("thresholds", {}).get("metadata_table_fold", {})
    min_net = cfg.get("min_net_gain_bytes", MIN_NET_GAIN_BYTES)

    paths = _collect_paths_from_package(pkg_dir)
    if not paths:
        return None

    path_table = _build_path_table(paths)
    # Phase 15: Large-project metadata efficiency - lower threshold when many paths
    if len(path_table) >= 150:
        min_net = min(min_net, 24)

    if config.get("_tesseract_cooperation", True):
        plan = config.get("_tesseract_execution_plan")
        if plan:
            from infold.tesseract.execution_plan import execution_plan_allows_metadata_followup
            lower_by = config.get("_tesseract_metadata_threshold_lower_by", 8)
            if execution_plan_allows_metadata_followup(plan):
                min_net = min(min_net, max(16, min_net - lower_by))
    enabled = config.get("operators", {}).get("metadata_table_fold", {}).get("enabled", True)
    if not enabled:
        return None

    # paths/path_table already computed above for scale-aware min_net
    path_to_ref = {p: i for i, p in enumerate(path_table)}
    occurrences = _compute_occurrences(pkg_dir)

    gross_saved = 0
    for p, cnt in occurrences.items():
        if p in path_to_ref:
            orig_bytes = len(p.encode("utf-8")) * cnt
            ref_bytes = len(str(path_to_ref[p]).encode("utf-8")) * cnt
            gross_saved += orig_bytes - ref_bytes

    # Phase 14A: Path DNA - use compact path encoding when micro mode and it saves bytes
    # Phase 14C: Ruthless micro - skip path_dna if savings below threshold
    table_json_normal = json.dumps({"paths": path_table}, separators=(",", ":"))
    table_overhead_normal = len(table_json_normal.encode("utf-8"))
    path_dna = None
    table_json = table_json_normal
    if config.get("_micro_mode", False):
        from infold.engine.path_dna import build_path_dna
        path_dna = build_path_dna(path_table)
        if path_dna is not None:
            table_json_dna = json.dumps({"path_dna": path_dna}, separators=(",", ":"))
            dna_overhead = len(table_json_dna.encode("utf-8"))
            dna_saved = table_overhead_normal - dna_overhead
            min_dna_saved = config.get("_micro_path_dna_min_bytes_saved", 0)
            if dna_saved >= min_dna_saved and dna_overhead < table_overhead_normal:
                table_json = table_json_dna
            else:
                path_dna = None
                if dna_saved > 0 and min_dna_saved > 0:
                    config.setdefault("_micro_skip_diagnostics", []).append({
                        "reason": "path_dna",
                        "detail": f"dna_saved {dna_saved} < min {min_dna_saved}",
                        "dna_saved": dna_saved,
                        "min_required": min_dna_saved,
                    })

    table_overhead = len(table_json.encode("utf-8"))
    net_saved = gross_saved - table_overhead

    if net_saved < min_net:
        if config.get("_micro_mode"):
            config.setdefault("_micro_skip_diagnostics", []).append({
                "reason": "metadata_table_fold",
                "detail": f"net_saved {net_saved} < min_net {min_net}",
                "gross_saved": gross_saved,
                "table_overhead": table_overhead,
                "net_saved": net_saved,
            })
        return None

    mt_dir = pkg_dir / "shared" / METADATA_TABLES_DIR
    mt_dir.mkdir(parents=True, exist_ok=True)
    (mt_dir / PATH_TABLE_FILENAME).write_text(table_json, encoding="utf-8")

    maps_dir = pkg_dir / "maps"
    shared_dir = pkg_dir / "shared"
    snapshots_dir = pkg_dir / "snapshots"

    if (maps_dir / "reconstruction.json").exists():
        data = json.loads((maps_dir / "reconstruction.json").read_text(encoding="utf-8"))
        records = data.get("records", [])
        for rec in records:
            targets = rec.get("targets", [])
            rec["targets_refs"] = [path_to_ref[str(t)] for t in targets if str(t) in path_to_ref]
            del rec["targets"]
        data["path_table_ref"] = True

        # Phase 14A: Family Membranes - compact operator_id when micro mode
        if config.get("_micro_mode", False) and records:
            op_to_idx: dict[str, int] = {}
            op_ids: list[str] = []
            for rec in records:
                op = rec.get("operator_id", "")
                if op not in op_to_idx:
                    op_to_idx[op] = len(op_ids)
                    op_ids.append(op)
                rec["o"] = op_to_idx[op]
                del rec["operator_id"]
            data["operator_ids"] = op_ids
            data["family_membranes"] = True

        (maps_dir / "reconstruction.json").write_text(
            json.dumps(data, separators=(",", ":")),
            encoding="utf-8",
        )

    if (maps_dir / "chunk_reconstruction.json").exists():
        data = json.loads((maps_dir / "chunk_reconstruction.json").read_text(encoding="utf-8"))
        for rec in data.get("records", []):
            paths_list = rec.get("paths", [])
            rec["path_refs"] = [path_to_ref[p] for p in paths_list if p in path_to_ref]
            if "paths" in rec:
                del rec["paths"]
        data["path_table_ref"] = True
        (maps_dir / "chunk_reconstruction.json").write_text(
            json.dumps(data, separators=(",", ":")),
            encoding="utf-8",
        )

    if (snapshots_dir / "inventory.json").exists():
        data = json.loads((snapshots_dir / "inventory.json").read_text(encoding="utf-8"))
        new_files = []
        for item in data.get("files", []):
            p = item.get("path", "")
            if p in path_to_ref:
                new_item = {k: v for k, v in item.items() if k != "path"}
                new_item["path_ref"] = path_to_ref[p]
                new_files.append(new_item)
            else:
                new_files.append(item)
        data["files"] = new_files
        data["path_table_ref"] = True
        (snapshots_dir / "inventory.json").write_text(
            json.dumps(data, separators=(",", ":")),
            encoding="utf-8",
        )

    if (snapshots_dir / "passthrough.json").exists():
        data = json.loads((snapshots_dir / "passthrough.json").read_text(encoding="utf-8"))
        new_passthrough = {}
        for k, v in data.items():
            if k in path_to_ref:
                new_passthrough[path_to_ref[k]] = v
            else:
                new_passthrough[k] = v
        (snapshots_dir / "passthrough.json").write_text(
            json.dumps(new_passthrough, separators=(",", ":")),
            encoding="utf-8",
        )

    # Phase 21A: Shared artifact path ref compaction (template, mutation_chain)
    _apply_shared_artifact_path_refs(shared_dir, path_to_ref)

    manifest_path = pkg_dir / "manifest.json"
    if manifest_path.exists():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["metadata_table_fold"] = True
        manifest["metadata_table_fold_net_bytes_saved"] = net_saved
        if path_dna is not None and "path_dna" in table_json:
            manifest["path_dna_folding"] = True
            manifest["path_dna_bytes_saved"] = table_overhead_normal - table_overhead
        if config.get("_micro_mode", False):
            manifest["family_membranes"] = True
        compact = config.get("package_export", {}).get("compact", False)
        manifest_path.write_text(
            json.dumps(manifest, indent=None if compact else 2, separators=(",", ":") if compact else (", ", ": ")),
            encoding="utf-8",
        )

    report_path = pkg_dir / "reports" / "report.json"
    if report_path.exists():
        report = json.loads(report_path.read_text(encoding="utf-8"))
        report["metadata_table_fold_metrics"] = {
            "metadata_table_fold_count": 1,
            "metadata_table_unique_paths": len(path_table),
            "metadata_table_reused_refs": sum(occurrences.get(p, 0) for p in path_table) - len(path_table),
            "path_dna_folding": path_dna is not None and "path_dna" in table_json,
            "path_dna_bytes_saved": (table_overhead_normal - table_overhead) if path_dna and "path_dna" in table_json else 0,
            "family_membranes": config.get("_micro_mode", False),
            "metadata_table_table_size_bytes": table_overhead,
            "metadata_table_gross_saved_bytes": gross_saved,
            "metadata_table_net_bytes_saved": net_saved,
        }
        compact = config.get("package_export", {}).get("compact", False)
        report_path.write_text(
            json.dumps(report, indent=None if compact else 2, separators=(",", ":") if compact else (", ", ": ")),
            encoding="utf-8",
        )

    return {
        "metadata_table_fold_count": 1,
        "metadata_table_unique_paths": len(path_table),
        "metadata_table_reused_refs": sum(occurrences.get(p, 0) for p in path_table) - len(path_table),
        "metadata_table_table_size_bytes": table_overhead,
        "metadata_table_gross_saved_bytes": gross_saved,
        "metadata_table_net_bytes_saved": net_saved,
    }
