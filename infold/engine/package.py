"""
Physical folded package export: manifest, ledger, shared, maps, reports, snapshots.
"""

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from infold.engine.ledger import FoldLedger
from infold.engine.orchestrator import FoldResult
from infold.models.project_sheet import ProjectSheet


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

    sheet = result.project_sheet
    ledger = result.ledger

    # manifest.json
    manifest = {
        "version": "1.0",
        "project_id": config.get("project", {}).get("id"),
        "source_path": str(sheet.source_path),
        "created": datetime.now(timezone.utc).isoformat(),
        "file_count": sheet.metrics.get("file_count", 0),
        "folder_count": sheet.metrics.get("folder_count", 0),
        "original_size_bytes": sheet.metrics.get("original_size_bytes", 0),
        "fold_count": ledger.total_folds,
        "logical_gain_bytes": ledger.total_bytes_saved,
        "physical_folded_size_bytes": sheet.metrics.get("original_size_bytes", 0) - ledger.total_bytes_saved,
    }
    (out / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")

    # ledger.json
    (out / "ledger.json").write_text(json.dumps(ledger.to_dict(), indent=2), encoding="utf-8")

    # shared/ - canonical content from fold records
    shared_dir = out / "shared"
    shared_dir.mkdir(exist_ok=True)
    for i, record in enumerate(ledger.fold_records):
        if record.operator_id == "exact_repetition":
            content = record.shared_representation
            if isinstance(content, str):
                (shared_dir / f"exact_{i}.txt").write_text(content, encoding="utf-8")
        elif record.operator_id == "template_skeleton":
            recipe = record.unfold_recipe
            (shared_dir / f"template_{i}.json").write_text(
                json.dumps({"const_blocks": recipe.get("const_blocks"), "slot_count": len(recipe.get("slot_groups", []))}, indent=2),
                encoding="utf-8",
            )
        elif record.operator_id == "symbol_table":
            recipe = record.unfold_recipe
            (shared_dir / f"symbols_{i}.json").write_text(
                json.dumps({"id_to_symbol": recipe.get("id_to_symbol")}, indent=2),
                encoding="utf-8",
            )
        elif record.operator_id == "hierarchy_mirror":
            recipe = record.unfold_recipe
            (shared_dir / f"hierarchy_{i}.json").write_text(
                json.dumps({
                    "structure_sig": recipe.get("structure_sig"),
                    "roots": recipe.get("roots"),
                    "instance_count": recipe.get("instance_count"),
                }, indent=2),
                encoding="utf-8",
            )
        elif record.operator_id == "dependency_motif":
            recipe = record.unfold_recipe
            (shared_dir / f"dependency_motif_{i}.json").write_text(
                json.dumps({
                    "signature": recipe.get("signature"),
                    "canonical_imports": recipe.get("canonical_imports"),
                    "paths": recipe.get("paths"),
                    "instance_count": recipe.get("instance_count"),
                    "motif_size": recipe.get("motif_size"),
                }, indent=2),
                encoding="utf-8",
            )

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
    (maps_dir / "reconstruction.json").write_text(json.dumps(maps_data, indent=2), encoding="utf-8")

    # reports/
    reports_dir = out / "reports"
    reports_dir.mkdir(exist_ok=True)
    from infold.reporting.report import report_to_json, report_to_text
    (reports_dir / "report.json").write_text(report_to_json(result, config), encoding="utf-8")
    (reports_dir / "report.txt").write_text(report_to_text(result, config), encoding="utf-8")

    # snapshots/ - file inventory snapshot
    snapshots_dir = out / "snapshots"
    snapshots_dir.mkdir(exist_ok=True)
    inventory = {
        "files": [{"path": str(p), "size": len(n.raw_text.encode("utf-8")), "language": n.language} for p, n in sheet.file_nodes.items()],
    }
    (snapshots_dir / "inventory.json").write_text(json.dumps(inventory, indent=2), encoding="utf-8")

    return out
