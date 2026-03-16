"""
Infold Archive v0.1: create, inspect, validate, reconstruct.

Deterministic validation. Preserves exact-mode guarantees.
"""

import json
import zipfile
from pathlib import Path
from typing import Any

from infold.engine.package_spec import (
    MANIFEST_REQUIRED_KEYS,
    REQUIRED_DIRS,
    REQUIRED_FILES,
)


def create_archive(
    source_path: Path | str,
    output_path: Path | str,
    config: dict[str, Any] | None = None,
) -> Path:
    """
    Create an Infold archive: run fold, export package, zip to .infold.
    """
    import shutil
    import tempfile

    from infold.engine import run_fold
    from infold.engine import export_package

    if config is None:
        from infold.cli import load_config
        config = load_config()
    source = Path(source_path).resolve()
    out = Path(output_path).resolve()
    if out.suffix != ".infold":
        out = out.with_suffix(".infold")
    result = run_fold(source, config)
    pkg_dir = Path(tempfile.mkdtemp(prefix="infold_pkg_"))
    try:
        export_package(result, config, pkg_dir)
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
            for f in sorted(pkg_dir.rglob("*")):
                if f.is_file():
                    arcname = f.relative_to(pkg_dir)
                    zf.write(f, arcname)
    finally:
        if pkg_dir.exists():
            shutil.rmtree(pkg_dir)
    return out


def _extract_to_temp(archive_path: Path) -> Path:
    """Extract .infold to temp dir. Caller must clean up."""
    import tempfile
    tmp = Path(tempfile.mkdtemp(prefix="infold_"))
    with zipfile.ZipFile(archive_path, "r") as zf:
        zf.extractall(tmp)
    return tmp


def inspect_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    Inspect archive: read manifest, return summary.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = Path(tmp)
        manifest_path = root / "manifest.json"
        if not manifest_path.exists():
            found = list(root.rglob("manifest.json"))
            manifest_path = found[0] if found else manifest_path
        if not manifest_path.exists():
            raise ValueError("Invalid archive: missing manifest.json")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        return {
            "path": str(archive),
            "manifest": manifest,
            "file_count": manifest.get("file_count", 0),
            "fold_count": manifest.get("fold_count", 0),
            "logical_gain_bytes": manifest.get("logical_gain_bytes", 0),
            "physical_folded_size_bytes": manifest.get("physical_folded_size_bytes", 0),
            "source_path": manifest.get("source_path", ""),
            "created": manifest.get("created", ""),
        }


def validate_archive(archive_path: Path | str) -> tuple[bool, list[str]]:
    """
    Validate archive against package spec v1. Deterministic checks.
    Returns (ok, list of error messages).
    """
    errors: list[str] = []
    archive = Path(archive_path).resolve()
    if not archive.exists():
        return False, [f"Archive not found: {archive}"]
    try:
        with zipfile.ZipFile(archive, "r") as zf:
            names = set(zf.namelist())
    except zipfile.BadZipFile:
        return False, ["Invalid zip file"]
    root_prefix = ""
    for n in names:
        if "/" in n:
            root_prefix = n.split("/")[0] + "/"
            break

    def has_file(p: str) -> bool:
        return p in names or (root_prefix and (root_prefix + p) in names)

    def has_dir(d: str) -> bool:
        return any(n == d or n.startswith(d + "/") or n == root_prefix + d or n.startswith(root_prefix + d + "/") for n in names)

    for f in REQUIRED_FILES:
        if not has_file(f):
            errors.append(f"Missing required file: {f}")
    for d in REQUIRED_DIRS:
        if not has_dir(d):
            errors.append(f"Missing required directory: {d}/")
    if errors:
        return False, errors
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = Path(tmp)
        manifest_path = root / "manifest.json"
        if not manifest_path.exists():
            found = list(root.rglob("manifest.json"))
            manifest_path = found[0] if found else manifest_path
        if not manifest_path.exists():
            return False, errors + ["Could not find manifest.json after extract"]
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        for key in MANIFEST_REQUIRED_KEYS:
            if key not in manifest:
                errors.append(f"Manifest missing required key: {key}")
        compat = manifest.get("compatibility", {})
        if compat.get("reconstruction_mode") != "deterministic":
            errors.append("Compatibility: reconstruction_mode must be 'deterministic'")
    return len(errors) == 0, errors


def reconstruct_archive(
    archive_path: Path | str,
    output_path: Path | str,
) -> dict[str, str]:
    """
    Reconstruct project from archive. Writes files to output_path.
    Returns {path: content} for reconstructed files.
    """
    archive = Path(archive_path).resolve()
    out_root = Path(output_path).resolve()
    out_root.mkdir(parents=True, exist_ok=True)
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = Path(tmp)
        manifest_candidates = list(root.rglob("manifest.json"))
        if manifest_candidates:
            root = manifest_candidates[0].parent
        maps_path = root / "maps" / "reconstruction.json"
        if not maps_path.exists():
            raise ValueError("Invalid archive: missing maps/reconstruction.json")
        maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
        records = maps_data.get("records", [])
        shared_dir = root / "shared"
        result: dict[str, str] = {}
        for rec in records:
            idx = rec.get("index", 0)
            op_id = rec.get("operator_id", "")
            targets = rec.get("targets", [])
            if op_id == "exact_repetition":
                content_path = shared_dir / f"exact_{idx}.txt"
                if content_path.exists():
                    content = content_path.read_text(encoding="utf-8")
                    for t in targets:
                        p = out_root / t
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[t] = content
            elif op_id == "template_skeleton":
                tmpl_path = shared_dir / f"template_{idx}.json"
                if tmpl_path.exists():
                    data = json.loads(tmpl_path.read_text(encoding="utf-8"))
                    const_blocks = data.get("const_blocks", [])
                    slot_groups = data.get("slot_groups", [])
                    paths = data.get("paths", targets)
                    for j, path_str in enumerate(paths):
                        out_parts = []
                        for bi, const in enumerate(const_blocks):
                            out_parts.append(const)
                            if bi < len(slot_groups):
                                sg = slot_groups[bi]
                                if sg and isinstance(sg[0], list):
                                    out_parts.append("".join(sg[k][j] for k in range(len(sg))))
                                else:
                                    out_parts.append(sg[j] if j < len(sg) else "")
                        content = "".join(out_parts)
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
            elif op_id == "symbol_table":
                sym_path = shared_dir / f"symbols_{idx}.json"
                if sym_path.exists():
                    data = json.loads(sym_path.read_text(encoding="utf-8"))
                    id_to_symbol = data.get("id_to_symbol", {})
                    folded_files = data.get("folded_files", {})
                    def _ph(pid): return f"\uE000{pid}\uE001"
                    for path_str, folded in folded_files.items():
                        content = folded
                        for pid, name in sorted(id_to_symbol.items(), key=lambda x: -len(_ph(x[0]))):
                            content = content.replace(_ph(pid), name)
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
            elif op_id == "hierarchy_mirror":
                hier_path = shared_dir / f"hierarchy_{idx}.json"
                if hier_path.exists():
                    data = json.loads(hier_path.read_text(encoding="utf-8"))
                    file_contents = data.get("file_contents", {})
                    for path_str, content in file_contents.items():
                        if path_str in result:
                            continue
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
            elif op_id == "dependency_motif":
                dep_path = shared_dir / f"dependency_motif_{idx}.json"
                if dep_path.exists():
                    data = json.loads(dep_path.read_text(encoding="utf-8"))
                    file_contents = data.get("file_contents", {})
                    for path_str, content in file_contents.items():
                        if path_str in result:
                            continue
                        p = out_root / path_str
                        p.parent.mkdir(parents=True, exist_ok=True)
                        p.write_text(content, encoding="utf-8")
                        result[path_str] = content
        passthrough_path = root / "snapshots" / "passthrough.json"
        if passthrough_path.exists():
            passthrough = json.loads(passthrough_path.read_text(encoding="utf-8"))
            for path_str, content in passthrough.items():
                if path_str not in result:
                    p = out_root / path_str
                    p.parent.mkdir(parents=True, exist_ok=True)
                    p.write_text(content, encoding="utf-8")
                    result[path_str] = content
        return result
