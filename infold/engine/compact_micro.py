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
}
_MANIFEST_ENCODE = {v: k for k, v in _MANIFEST_DECODE.items()}


def encode_manifest_micro(manifest: dict[str, Any]) -> dict[str, Any]:
    """Encode manifest with short keys for micro mode. Deterministic."""
    out: dict[str, Any] = {"_m": 1}
    for k, v in manifest.items():
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


def apply_compact_micro(pkg_dir: Path, config: dict[str, Any]) -> None:
    """
    Rewrite manifest, reconstruction, inventory, path_table with compact encoding.
    Call after metadata_table_fold and apply_family_membranes. Only when micro.
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
