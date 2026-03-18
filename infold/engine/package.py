"""
Physical folded package export: manifest, ledger, shared, maps, reports, snapshots.

Package spec v1: required manifest.json, ledger.json; required dirs shared/, maps/, reports/, snapshots/.
Versioning and compatibility metadata in manifest.
Supports compact mode for reduced footprint.
"""

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from infold.engine.ledger import FoldLedger
from infold.engine.orchestrator import FoldResult
from infold.engine.package_spec import (
    COMPATIBILITY_METADATA,
    PACKAGE_SPEC_VERSION,
)
from infold.models.project_sheet import ProjectSheet


def _json_dump(obj: Any, compact: bool) -> str:
    """Serialize to JSON. Compact = no indent."""
    return json.dumps(obj, indent=None if compact else 2, separators=(",", ":") if compact else (", ", ": "))


def export_package(
    result: FoldResult,
    config: dict[str, Any],
    output_path: Path | str,
) -> Path:
    """
    Export a physical folded package layout:
      manifest.json
      ledger.json
      shared/
      maps/
      reports/
      snapshots/
    """
    out = Path(output_path).resolve()
    out.mkdir(parents=True, exist_ok=True)

    pkg_cfg = config.get("package_export", {})
    compact = pkg_cfg.get("compact", False)
    report_text = pkg_cfg.get("report_text", True)
    inventory_minimal = pkg_cfg.get("inventory_minimal", False)

    sheet = result.project_sheet
    ledger = result.ledger

    # manifest.json (package spec v1)
    raw_size = sheet.metrics.get("original_size_bytes", 0)
    physical_folded = raw_size - ledger.total_bytes_saved
    pfi = config.get("_fold_profile_info") or {}
    manifest = {
        "version": PACKAGE_SPEC_VERSION,
        "package_spec": "1.0",
        "compatibility": COMPATIBILITY_METADATA,
        "project_id": config.get("project", {}).get("id"),
        "source_path": str(sheet.source_path),
        "created": datetime.now(timezone.utc).isoformat(),
        "file_count": sheet.metrics.get("file_count", 0),
        "folder_count": sheet.metrics.get("folder_count", 0),
        "original_size_bytes": raw_size,
        "fold_count": ledger.total_folds,
        "logical_gain_bytes": ledger.total_bytes_saved,
        "physical_folded_size_bytes": physical_folded,
        "required_files": ["manifest.json", "ledger.json"],
        "required_dirs": ["shared", "maps", "reports", "snapshots"],
        "fold_profile": pfi.get("fold_profile"),
        "fold_profile_mode": pfi.get("fold_profile_mode"),
        "fold_profile_reason": pfi.get("fold_profile_reason"),
    }
    ci = config.get("_fold_creature_info")
    if ci:
        manifest["fold_species"] = ci.get("fold_species")
        manifest["fold_species_mode"] = ci.get("fold_species_mode")
        manifest["fold_species_reason"] = ci.get("fold_species_reason")
        manifest["fold_creature_traits_final"] = ci.get("fold_creature_traits_final")
        manifest["fold_creature_adapt_reasons"] = ci.get("fold_creature_adapt_reasons")
    tpi = config.get("_tesseract_planner_info")
    if tpi:
        manifest["tesseract_planner_route"] = tpi.get("route")
        manifest["tesseract_planner_route_reason"] = tpi.get("route_reason")
        manifest["tesseract_planner_dominant"] = tpi.get("dominant_dimension")
        manifest["tesseract_planner_operator_priority"] = tpi.get("operator_family_priority")
    ep = config.get("_tesseract_execution_plan")
    if ep:
        manifest["tesseract_execution_steps"] = ep.get("execution_steps", [])
        manifest["tesseract_cooperation_mode"] = ep.get("cooperation_mode")
        manifest["tesseract_plan_reason"] = ep.get("plan_reason")
    (out / "manifest.json").write_text(_json_dump(manifest, compact), encoding="utf-8")

    # ledger.json
    (out / "ledger.json").write_text(_json_dump(ledger.to_dict(), compact), encoding="utf-8")

    # shared/ - canonical content from fold records (always create, even when empty)
    shared_dir = out / "shared"
    shared_dir.mkdir(exist_ok=True)
    chunk_reconstruction_records: list[dict[str, Any]] = []
    for i, record in enumerate(ledger.fold_records):
        if record.operator_id == "exact_repetition":
            content = record.shared_representation
            if isinstance(content, str):
                (shared_dir / f"exact_{i}.txt").write_text(content, encoding="utf-8")
        elif record.operator_id == "template_skeleton":
            recipe = record.unfold_recipe
            (shared_dir / f"template_{i}.json").write_text(
                _json_dump({
                    "const_blocks": recipe.get("const_blocks"),
                    "slot_groups": recipe.get("slot_groups", []),
                    "paths": recipe.get("paths", [str(t) for t in record.targets]),
                }, compact),
                encoding="utf-8",
            )
        elif record.operator_id == "symbol_table":
            recipe = record.unfold_recipe
            (shared_dir / f"symbols_{i}.json").write_text(
                _json_dump({
                    "id_to_symbol": recipe.get("id_to_symbol"),
                    "folded_files": recipe.get("folded_files", {}),
                }, compact),
                encoding="utf-8",
            )
        elif record.operator_id == "hierarchy_mirror":
            recipe = record.unfold_recipe
            (shared_dir / f"hierarchy_{i}.json").write_text(
                _json_dump({
                    "structure_sig": recipe.get("structure_sig"),
                    "roots": recipe.get("roots"),
                    "instance_count": recipe.get("instance_count"),
                    "file_contents": recipe.get("file_contents", {}),
                }, compact),
                encoding="utf-8",
            )
        elif record.operator_id == "dependency_motif":
            recipe = record.unfold_recipe
            (shared_dir / f"dependency_motif_{i}.json").write_text(
                _json_dump({
                    "signature": recipe.get("signature"),
                    "canonical_imports": recipe.get("canonical_imports"),
                    "paths": recipe.get("paths"),
                    "instance_count": recipe.get("instance_count"),
                    "motif_size": recipe.get("motif_size"),
                    "file_contents": recipe.get("file_contents", {}),
                }, compact),
                encoding="utf-8",
            )
        elif record.operator_id == "byte_fold":
            import base64
            recipe = record.unfold_recipe
            chunk_dict_b64 = recipe.get("chunk_dict_b64", {})
            reconstruction = recipe.get("reconstruction", {})
            chunks_dir = shared_dir / "chunks"
            chunks_dir.mkdir(exist_ok=True)
            chunk_ids = list(chunk_dict_b64.keys())
            id_to_idx = {cid: idx for idx, cid in enumerate(chunk_ids)}
            for ch_id, b64 in chunk_dict_b64.items():
                (chunks_dir / f"{ch_id}.bin").write_bytes(base64.b64decode(b64))
            (shared_dir / "chunk_index.json").write_text(
                _json_dump({"ids": chunk_ids, "record_index": i}, compact),
                encoding="utf-8",
            )
            paths = list(reconstruction.keys())
            seqs = [[id_to_idx[cid] for cid in reconstruction[p]] for p in paths]
            while len(chunk_reconstruction_records) <= i:
                chunk_reconstruction_records.append({})
            chunk_reconstruction_records[i] = {"paths": paths, "seqs": seqs}

    # Ensure shared/ has at least one file when empty (0 folds) so ZIP/validation sees the dir
    if not any(shared_dir.iterdir()):
        (shared_dir / ".gitkeep").write_text("", encoding="utf-8")

    # maps/ - reconstruction mappings
    maps_dir = out / "maps"
    maps_dir.mkdir(exist_ok=True)
    maps_data: dict[str, Any] = {"records": []}
    for i, record in enumerate(ledger.fold_records):
        maps_data["records"].append({
            "index": i,
            "operator_id": record.operator_id,
            "targets": [str(t) for t in record.targets],
            "gain": record.gain,
        })
    (maps_dir / "reconstruction.json").write_text(_json_dump(maps_data, compact), encoding="utf-8")
    if chunk_reconstruction_records:
        (maps_dir / "chunk_reconstruction.json").write_text(
            _json_dump({"records": chunk_reconstruction_records}, compact),
            encoding="utf-8",
        )

    # reports/
    reports_dir = out / "reports"
    reports_dir.mkdir(exist_ok=True)
    from infold.reporting.report import build_report, report_to_json, report_to_text
    report_dict = build_report(result, config)
    (reports_dir / "report.json").write_text(
        _json_dump(report_dict, compact) if compact else report_to_json(result, config),
        encoding="utf-8",
    )
    if report_text:
        (reports_dir / "report.txt").write_text(report_to_text(result, config), encoding="utf-8")

    # snapshots/ - file inventory; passthrough = files not in any fold (for full reconstruction)
    snapshots_dir = out / "snapshots"
    snapshots_dir.mkdir(exist_ok=True)
    folded_paths = set()
    for r in ledger.fold_records:
        for t in r.targets:
            folded_paths.add(str(t).replace("\\", "/"))
    for r in ledger.fold_records:
        recipe = getattr(r, "unfold_recipe", {}) or {}
        for p in recipe.get("file_contents", {}).keys():
            folded_paths.add(str(p).replace("\\", "/"))
        for p in recipe.get("folded_files", {}).keys():
            folded_paths.add(str(p).replace("\\", "/"))
        for p in recipe.get("reconstruction", {}).keys():
            folded_paths.add(str(p).replace("\\", "/"))
    inventory = {
        "files": [
            {"path": str(p), "size": len(n.raw_text.encode("utf-8")), **({} if inventory_minimal else {"language": n.language})}
            for p, n in sheet.file_nodes.items()
        ],
    }
    (snapshots_dir / "inventory.json").write_text(_json_dump(inventory, compact), encoding="utf-8")
    passthrough = {str(p): n.raw_text for p, n in sheet.file_nodes.items() if str(p).replace("\\", "/") not in folded_paths}
    (snapshots_dir / "passthrough.json").write_text(_json_dump(passthrough, compact), encoding="utf-8")

    return out
