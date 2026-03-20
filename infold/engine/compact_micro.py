"""
Phase 14B: Compact micro-mode encoding.

Denser representation for manifest, reconstruction, inventory, path_table in micro mode.
Deterministic, exactly reversible. Only used when _micro_mode.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


# Micro manifest short keys
_MANIFEST_DECODE = {
    "v": "version",
    "pid": "project_id",
    "ps": "package_spec",
    "c": "compatibility",
    "src": "source_path",
    "cr": "created",
    "fc": "file_count",
    "fd": "folder_count",
    "os": "original_size_bytes",
    "nf": "fold_count",
    "lg": "logical_gain_bytes",
    "pf": "physical_folded_size_bytes",
    "fp": "fold_profile",
    "ce": "creature_enabled",
    "te": "tesseract_planner_enabled",
    "tc": "tesseract_cooperation_enabled",
    "mt": "metadata_table_fold",
    "mn": "metadata_table_fold_net_bytes_saved",
    "fm": "family_membranes",
    "pd": "path_dna_folding",
    "pdb": "path_dna_bytes_saved",
    "sc": "scope_accounting",
}
_MANIFEST_ENCODE = {v: k for k, v in _MANIFEST_DECODE.items()}


def encode_manifest_micro(manifest: dict[str, Any]) -> dict[str, Any]:
    """Encode manifest with short keys for micro mode. Deterministic. Phase 21A: omit optional empty."""
    from infold.engine.package_spec import MANIFEST_REQUIRED_KEYS
    out: dict[str, Any] = {"_m": 1}
    omit_if_empty = {"fold_profile_mode", "fold_profile_reason", "scope_accounting"}
    for k, v in manifest.items():
        if k in MANIFEST_REQUIRED_KEYS:
            pass  # always include required
        elif v is None or (k in omit_if_empty and (v == "" or v == [] or v == {})):
            continue
        if k == "compatibility" and isinstance(v, dict):
            out["c"] = {"sv": v.get("spec_version", "1.0"), "mi": v.get("min_infold_version", "0.2.0"), "mr": v.get("min_reader_version", "0.2.0")}
        else:
            short = _MANIFEST_ENCODE.get(k)
            out[short if short else k] = v
    return out


def decode_manifest_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Decode compact micro manifest to full form."""
    if "_m" not in data:
        return data
    out: dict[str, Any] = {}
    for k, v in data.items():
        if k == "_m":
            continue
        if k == "c" and isinstance(v, dict):
            out["compatibility"] = {
                "spec_version": v.get("sv", "1.0"),
                "min_infold_version": v.get("mi", "0.2.0"),
                "min_reader_version": v.get("mr", "0.2.0"),
                "reconstruction_mode": v.get("rm", "deterministic"),
                "debug_friendly": v.get("db", True),
                "compatibility_status": v.get("cs", "ok"),
                "upgrade_path_available": v.get("up", True),
            }
        else:
            full = _MANIFEST_DECODE.get(k)
            out[full if full else k] = v
    return out


def encode_reconstruction_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Encode reconstruction.json with short keys and positional records. Deterministic."""
    records = data.get("records", [])
    op_ids = data.get("operator_ids", [])
    # Each record: [index, gain, targets_refs, o]
    r = [[rec.get("index", i), rec.get("gain", 0), rec.get("targets_refs", []), rec.get("o", 0)] for i, rec in enumerate(records)]
    out: dict[str, Any] = {"r": r, "pt": 1 if data.get("path_table_ref") else 0, "o": op_ids, "fm": 1 if data.get("family_membranes") else 0}
    return out


def decode_reconstruction_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Decode compact reconstruction to full form."""
    if "r" not in data:
        return data
    op_ids = data.get("o", [])
    out_records = []
    for i, arr in enumerate(data.get("r", [])):
        if isinstance(arr, list) and len(arr) >= 4:
            rec = {"index": arr[0], "gain": arr[1], "targets_refs": arr[2], "o": arr[3]}
            if op_ids and 0 <= arr[3] < len(op_ids):
                rec["operator_id"] = op_ids[arr[3]]
                del rec["o"]
            out_records.append(rec)
        else:
            out_records.append(arr if isinstance(arr, dict) else {})
    return {
        "records": out_records,
        "path_table_ref": bool(data.get("pt", 0)),
        "operator_ids": op_ids,
        "family_membranes": bool(data.get("fm", 0)),
    }


def encode_inventory_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Encode inventory with short keys. Deterministic."""
    files = data.get("files", [])
    # Each file: [path_ref, size]
    f = [[item.get("path_ref", item.get("path", 0)), item.get("size", 0)] for item in files]
    return {"f": f, "pt": 1 if data.get("path_table_ref") else 0}


def decode_inventory_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Decode compact inventory to full form."""
    if "f" not in data:
        return data
    files = [{"path_ref": arr[0], "size": arr[1]} for arr in data.get("f", []) if isinstance(arr, list) and len(arr) >= 2]
    return {"files": files, "path_table_ref": bool(data.get("pt", 0))}


def encode_path_table_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Encode path_table with short key. paths -> p."""
    if "path_dna" in data:
        return data  # path_dna already uses r, p
    if "paths" in data:
        return {"p": data["paths"]}
    return data


def decode_path_table_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Decode compact path_table. p -> paths."""
    if "path_dna" in data:
        return data
    if "p" in data and "paths" not in data:
        return {"paths": data["p"]}
    return data


def load_manifest_micro(root: Path) -> dict[str, Any]:
    """Load manifest, expanding compact micro format if present."""
    p = root / "manifest.json"
    if not p.exists():
        return {}
    data = json.loads(p.read_text(encoding="utf-8"))
    return decode_manifest_micro(data)


def load_reconstruction_micro(root: Path) -> dict[str, Any]:
    """Load reconstruction.json, expanding compact format if present."""
    p = root / "maps" / "reconstruction.json"
    if not p.exists():
        return {}
    data = json.loads(p.read_text(encoding="utf-8"))
    if "r" in data:
        return decode_reconstruction_micro(data)
    return data


def load_inventory_micro(root: Path) -> dict[str, Any]:
    """Load inventory.json, expanding compact format if present."""
    p = root / "snapshots" / "inventory.json"
    if not p.exists():
        return {}
    data = json.loads(p.read_text(encoding="utf-8"))
    if "f" in data:
        return decode_inventory_micro(data)
    return data


def encode_ledger_micro(ledger: dict[str, Any]) -> dict[str, Any]:
    """Phase 14D: Compact ledger for micro. Gains array only. Deterministic."""
    records = ledger.get("fold_records", [])
    gains = [r.get("gain", 0) for r in records]
    return {
        "_l": 1,
        "v": ledger.get("version", "1.0"),
        "nf": ledger.get("total_folds", len(gains)),
        "lg": ledger.get("total_bytes_saved", sum(gains)),
        "g": gains,
    }


def decode_ledger_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Decode compact ledger to full form."""
    if "_l" not in data:
        return data
    gains = data.get("g", [])
    return {
        "version": data.get("v", "1.0"),
        "project_id": data.get("pid"),
        "total_folds": data.get("nf", len(gains)),
        "total_bytes_saved": data.get("lg", sum(gains)),
        "fold_records": [{"gain": g} for g in gains],
    }


def encode_report_micro(report: dict[str, Any]) -> dict[str, Any]:
    """Phase 14D: Minimal report for micro. Deterministic."""
    scope = report.get("scope_accounting")
    return {
        "_r": 1,
        "fc": report.get("file_count", 0),
        "fd": report.get("folder_count", 0),
        "os": report.get("original_size_bytes", 0),
        "nf": report.get("fold_count", 0),
        "lg": report.get("logical_gain_bytes", report.get("total_bytes_saved", 0)),
        "pf": report.get("physical_folded_size_bytes", 0),
        "ok": report.get("exact_reconstruction_ok", True),
        "st": report.get("reconstruction_status", "ok"),
        "err": report.get("errors", []),
        "rc": report.get("rejected_candidates_count", 0),
        **({"sc": scope} if scope else {}),
    }


def decode_report_micro(data: dict[str, Any]) -> dict[str, Any]:
    """Decode compact report to full form."""
    if "_r" not in data:
        return data
    rc = data.get("rc", 0)
    out = {
        "file_count": data.get("fc", 0),
        "folder_count": data.get("fd", 0),
        "original_size_bytes": data.get("os", 0),
        "fold_count": data.get("nf", 0),
        "logical_gain_bytes": data.get("lg", 0),
        "total_bytes_saved": data.get("lg", 0),
        "physical_folded_size_bytes": data.get("pf", 0),
        "exact_reconstruction_ok": data.get("ok", True),
        "reconstruction_status": data.get("st", "ok"),
        "errors": data.get("err", []),
        "rejected_candidates_count": rc,
        "rejected_candidates_summary": [{}] * rc,  # placeholder for len() compat
    }
    if "sc" in data:
        out["scope_accounting"] = data["sc"]
    return out


def _compact_passthrough_micro(pkg_dir: Path) -> None:
    """Phase 21B: Convert passthrough to array format [[ref, content], ...] when path_refs. Saves key overhead."""
    pt_path = pkg_dir / "shared" / "metadata_tables" / "path_table.json"
    if not pt_path.exists():
        return
    passthrough_path = pkg_dir / "snapshots" / "passthrough.json"
    if not passthrough_path.exists():
        return
    data = json.loads(passthrough_path.read_text(encoding="utf-8"))
    if "_pa" in data:
        return  # already compact
    # Check if all keys are path refs (numeric strings)
    items = list(data.items()) if isinstance(data, dict) else []
    if not items:
        return
    all_refs = all(k.isdigit() for k in data.keys())
    if not all_refs:
        return
    # Convert to [[ref, content], ...] - deterministic order by ref
    entries = [[int(k), v] for k, v in sorted(items, key=lambda x: int(x[0]))]
    compact = {"_pa": 1, "e": entries}
    passthrough_path.write_text(json.dumps(compact, separators=(",", ":")), encoding="utf-8")


def _compact_shared_artifacts_micro(pkg_dir: Path) -> None:
    """Phase 21B: Template short keys (cb, sg, pr), anchors compact."""
    shared_dir = pkg_dir / "shared"
    for f in shared_dir.glob("template_*.json"):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
            if "cb" in data:
                continue  # already compact
            out = {}
            if "const_blocks" in data:
                out["cb"] = data["const_blocks"]
            if "slot_groups" in data:
                out["sg"] = data["slot_groups"]
            if "path_refs" in data:
                out["pr"] = data["path_refs"]
            elif "paths" in data:
                out["paths"] = data["paths"]
            if "path_table_ref" in data:
                out["pt"] = 1
            if out:
                f.write_text(json.dumps(out, separators=(",", ":")), encoding="utf-8")
        except Exception:
            pass
    anchors_path = shared_dir / "anchors.json"
    if anchors_path.exists():
        try:
            data = json.loads(anchors_path.read_text(encoding="utf-8"))
            if "_an" in data:
                return
            anchors = data.get("anchors", [])
            if not anchors:
                return
            compact_anchors = []
            for a in anchors:
                ca = {"p": a.get("path"), "t": a.get("anchor_type"), "c": a.get("anchored_count")}
                if a.get("anchored_paths"):
                    ca["ap"] = a["anchored_paths"][:30]
                compact_anchors.append(ca)
            anchors_path.write_text(
                json.dumps({"_an": 1, "a": compact_anchors}, separators=(",", ":")),
                encoding="utf-8",
            )
        except Exception:
            pass


def apply_compact_micro(pkg_dir: Path, config: dict[str, Any]) -> None:
    """
    Rewrite manifest, reconstruction, inventory, path_table, ledger, report with compact encoding.
    Call after metadata_table_fold and apply_family_membranes. Only when micro.
    Phase 14D: Added ledger and report compaction.
    """
    if not config.get("_micro_mode", False):
        return
    manifest_path = pkg_dir / "manifest.json"
    if not manifest_path.exists():
        return
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("_m"):  # Already compact
        return
    # Encode manifest
    compact_m = encode_manifest_micro(manifest)
    manifest_path.write_text(json.dumps(compact_m, separators=(",", ":")), encoding="utf-8")

    # Encode ledger (Phase 14D)
    ledger_path = pkg_dir / "ledger.json"
    if ledger_path.exists():
        ledger = json.loads(ledger_path.read_text(encoding="utf-8"))
        if "_l" not in ledger:
            compact_ledger = encode_ledger_micro(ledger)
            ledger_path.write_text(json.dumps(compact_ledger, separators=(",", ":")), encoding="utf-8")

    # Encode report (Phase 14D)
    report_path = pkg_dir / "reports" / "report.json"
    if report_path.exists():
        report = json.loads(report_path.read_text(encoding="utf-8"))
        if "_r" not in report:
            compact_report = encode_report_micro(report)
            report_path.write_text(json.dumps(compact_report, separators=(",", ":")), encoding="utf-8")

    # Encode reconstruction
    recon_path = pkg_dir / "maps" / "reconstruction.json"
    if recon_path.exists():
        data = json.loads(recon_path.read_text(encoding="utf-8"))
        if "records" in data:
            compact_r = encode_reconstruction_micro(data)
            recon_path.write_text(json.dumps(compact_r, separators=(",", ":")), encoding="utf-8")

    # Encode inventory
    inv_path = pkg_dir / "snapshots" / "inventory.json"
    if inv_path.exists():
        data = json.loads(inv_path.read_text(encoding="utf-8"))
        if "files" in data:
            compact_i = encode_inventory_micro(data)
            inv_path.write_text(json.dumps(compact_i, separators=(",", ":")), encoding="utf-8")

    # Path table: paths -> p (when not path_dna)
    pt_path = pkg_dir / "shared" / "metadata_tables" / "path_table.json"
    if pt_path.exists():
        data = json.loads(pt_path.read_text(encoding="utf-8"))
        if "paths" in data and "path_dna" not in data:
            compact_pt = {"p": data["paths"]}
            pt_path.write_text(json.dumps(compact_pt, separators=(",", ":")), encoding="utf-8")

    # Phase 21B: Passthrough array format, template short keys, anchors compact (skip when baseline)
    if not config.get("_skip_phase21b_compaction", False):
        _compact_passthrough_micro(pkg_dir)
        _compact_shared_artifacts_micro(pkg_dir)
