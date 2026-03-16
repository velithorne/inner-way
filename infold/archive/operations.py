"""
Infold Archive v0.1: create, inspect, validate, reconstruct, list, stats, compare.

Deterministic validation. Integrity hashing. Preserves exact-mode guarantees.
"""

import hashlib
import json
import zipfile
from pathlib import Path
from typing import Any

from infold.engine.package_spec import (
    MANIFEST_REQUIRED_KEYS,
    REQUIRED_DIRS,
    REQUIRED_FILES,
)
from infold.engine.package_schema import (
    check_logical_vs_physical,
    check_manifest_ledger_consistency,
)


def _sha256_file(path: Path) -> str:
    """Compute SHA256 of file. Deterministic."""
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


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
        _write_integrity_checksums(pkg_dir)
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zf:
            for f in sorted(pkg_dir.rglob("*")):
                if f.is_file():
                    arcname = f.relative_to(pkg_dir)
                    zf.write(f, arcname)
    finally:
        if pkg_dir.exists():
            shutil.rmtree(pkg_dir)
    return out


def _write_integrity_checksums(pkg_dir: Path) -> None:
    """Write integrity.json with SHA256 checksums for key files."""
    checksums: dict[str, str] = {}
    for name in ["manifest.json", "ledger.json"]:
        p = pkg_dir / name
        if p.exists():
            checksums[name] = _sha256_file(p)
    shared_dir = pkg_dir / "shared"
    if shared_dir.exists():
        for f in sorted(shared_dir.iterdir()):
            if f.is_file():
                checksums[f"shared/{f.name}"] = _sha256_file(f)
    maps_dir = pkg_dir / "maps"
    if maps_dir.exists():
        for f in sorted(maps_dir.iterdir()):
            if f.is_file():
                checksums[f"maps/{f.name}"] = _sha256_file(f)
    (pkg_dir / "integrity.json").write_text(
        json.dumps({"checksums": checksums}, indent=2),
        encoding="utf-8",
    )


def _extract_root(tmp: Path) -> Path:
    """Find package root (where manifest.json lives)."""
    manifest = tmp / "manifest.json"
    if manifest.exists():
        return tmp
    found = list(tmp.rglob("manifest.json"))
    return found[0].parent if found else tmp


def explain_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    Explain archive: summarize package contents, fold counts by operator,
    biggest gain contributors, rejected candidates, reconstruction guarantees.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
        ledger = json.loads((root / "ledger.json").read_text(encoding="utf-8"))
        report = {}
        if (root / "reports" / "report.json").exists():
            report = json.loads((root / "reports" / "report.json").read_text(encoding="utf-8"))
        maps_data = json.loads((root / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        records = maps_data.get("records", [])

        fold_by_op: dict[str, int] = {}
        gain_by_op: dict[str, int] = {}
        for rec in records:
            op = rec.get("operator_id", "unknown")
            fold_by_op[op] = fold_by_op.get(op, 0) + 1
            gain_by_op[op] = gain_by_op.get(op, 0) + rec.get("gain", 0)

        gain_contributors = sorted(
            [{"operator_id": op, "gain": g} for op, g in gain_by_op.items()],
            key=lambda x: -x["gain"],
        )[:10]

        compat = manifest.get("compatibility", {})
        reconstruction_guarantees = [
            f"mode={compat.get('reconstruction_mode', '?')}",
            f"min_infold={compat.get('min_infold_version', '?')}",
            f"debug_friendly={compat.get('debug_friendly', False)}",
        ]

        return {
            "path": str(archive),
            "manifest": manifest,
            "package_summary": {
                "file_count": manifest.get("file_count", 0),
                "fold_count": manifest.get("fold_count", 0),
                "logical_gain_bytes": manifest.get("logical_gain_bytes", 0),
                "physical_folded_size_bytes": manifest.get("physical_folded_size_bytes", 0),
                "original_size_bytes": manifest.get("original_size_bytes", 0),
                "source_path": manifest.get("source_path", ""),
                "created": manifest.get("created", ""),
            },
            "fold_counts_by_operator": fold_by_op,
            "gain_by_operator": gain_by_op,
            "biggest_gain_contributors": gain_contributors,
            "rejected_candidates_summary": report.get("rejected_candidates_summary", []),
            "reconstruction_guarantees": reconstruction_guarantees,
            "template_families": report.get("template_families", []),
            "duplicate_families": report.get("duplicate_families", []),
            "hierarchy_templates": report.get("hierarchy_templates", []),
            "dependency_motifs": report.get("dependency_motifs", []),
        }


def list_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    List shared artifacts and fold families by operator.
    Deterministic, readable output.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        shared_dir = root / "shared"
        maps_data = json.loads((root / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        records = maps_data.get("records", [])

        shared_artifacts: list[dict[str, Any]] = []
        if shared_dir.exists():
            for f in sorted(shared_dir.iterdir()):
                if f.is_file():
                    shared_artifacts.append({"path": f"shared/{f.name}", "size": f.stat().st_size})

        families_by_op: dict[str, list[dict[str, Any]]] = {}
        for i, rec in enumerate(records):
            op = rec.get("operator_id", "unknown")
            targets = rec.get("targets", [])
            gain = rec.get("gain", 0)
            if op not in families_by_op:
                families_by_op[op] = []
            fam: dict[str, Any] = {"index": i, "gain": gain, "target_count": len(targets)}
            if op == "exact_repetition":
                fam["paths"] = targets[:5]
                if len(targets) > 5:
                    fam["paths"].append(f"... +{len(targets) - 5} more")
            elif op == "template_skeleton":
                tmpl = root / "shared" / f"template_{i}.json"
                if tmpl.exists():
                    data = json.loads(tmpl.read_text(encoding="utf-8"))
                    fam["paths"] = data.get("paths", targets)[:5]
                    if len(targets) > 5:
                        fam["paths"].append(f"... +{len(targets) - 5} more")
                else:
                    fam["paths"] = targets[:5]
            elif op in ("hierarchy_mirror", "dependency_motif"):
                hj = root / "shared" / (f"hierarchy_{i}.json" if op == "hierarchy_mirror" else f"dependency_motif_{i}.json")
                if hj.exists():
                    data = json.loads(hj.read_text(encoding="utf-8"))
                    roots = data.get("roots", data.get("paths", []))
                    fam["roots_or_paths"] = roots[:5] if isinstance(roots, list) else list(roots)[:5]
                else:
                    fam["paths"] = targets[:5]
            else:
                fam["paths"] = targets[:5]
            families_by_op[op].append(fam)

        return {
            "path": str(archive),
            "shared_artifacts": shared_artifacts,
            "families_by_operator": families_by_op,
        }


def stats_archive(archive_path: Path | str) -> dict[str, Any]:
    """
    Detailed archive metrics: logical gain, physical folded size, per-operator contributions,
    rejection counts, template purity/slot ratios, hierarchy/dependency family metrics.
    """
    archive = Path(archive_path).resolve()
    if not archive.exists():
        raise FileNotFoundError(f"Archive not found: {archive}")
    info = explain_archive(archive)
    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))
        report = {}
        if (root / "reports" / "report.json").exists():
            report = json.loads((root / "reports" / "report.json").read_text(encoding="utf-8"))

    stats: dict[str, Any] = {
        "path": str(archive),
        "logical_gain_bytes": info["package_summary"]["logical_gain_bytes"],
        "physical_folded_size_bytes": info["package_summary"]["physical_folded_size_bytes"],
        "original_size_bytes": info["package_summary"]["original_size_bytes"],
        "per_operator_contributions": info["gain_by_operator"],
        "per_operator_gain_share": {},
        "rejection_count": len(info.get("rejected_candidates_summary", [])),
    }
    total = info["package_summary"]["logical_gain_bytes"]
    for op, g in info["gain_by_operator"].items():
        stats["per_operator_gain_share"][op] = round(g / total, 4) if total else 0

    tm = report.get("template_skeleton_metrics") or {}
    stats["template_metrics"] = {
        "families_found": tm.get("families_found", 0),
        "family_purity": tm.get("family_purity", []),
        "slot_ambiguity": tm.get("slot_ambiguity", []),
        "avg_slot_size": tm.get("avg_slot_size", []),
    } if tm else None

    hm = report.get("hierarchy_metrics") or {}
    stats["hierarchy_metrics"] = {
        "templates_found": hm.get("hierarchy_templates_found", 0),
        "instances_per_template": hm.get("instances_per_template", []),
        "structural_reuse_ratio": hm.get("structural_reuse_ratio", []),
    } if hm else None

    dm = report.get("dependency_metrics") or {}
    stats["dependency_metrics"] = {
        "motifs_found": dm.get("dependency_motifs_found", 0),
        "instances_per_family": dm.get("motif_instances_per_family", []),
        "average_motif_size": dm.get("average_motif_size", []),
    } if dm else None

    return stats


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


VALIDATION_MODES = ("basic", "strict", "integrity-only", "schema-only")


def validate_archive(
    archive_path: Path | str,
    mode: str = "strict",
) -> tuple[bool, list[str]]:
    """
    Validate archive. Modes:
    - basic: required files/dirs, manifest required keys
    - strict: basic + ledger consistency, maps, report, manifest-ledger, integrity
    - integrity-only: only integrity checksums
    - schema-only: manifest/ledger/integrity schema structure, no content checks
    Returns (ok, list of error messages). Deterministic and readable.
    """
    if mode not in VALIDATION_MODES:
        return False, [f"Invalid validation mode: {mode}"]
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

    if mode in ("basic", "strict", "schema-only"):
        for f in REQUIRED_FILES:
            if not has_file(f):
                errors.append(f"Missing required file: {f}")
        for d in REQUIRED_DIRS:
            if not has_dir(d):
                errors.append(f"Missing required directory: {d}/")
    if errors and mode != "integrity-only":
        return False, errors

    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(archive, "r") as zf:
            zf.extractall(tmp)
        root = _extract_root(Path(tmp))

        manifest_path = root / "manifest.json"
        if not manifest_path.exists():
            if mode != "integrity-only":
                return False, errors + ["Could not find manifest.json"]
            manifest = {}
        else:
            try:
                manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as e:
                errors.append(f"Manifest invalid JSON: {e}")
                manifest = {}
        if mode in ("basic", "strict", "schema-only") and manifest:
            for key in MANIFEST_REQUIRED_KEYS:
                if key not in manifest:
                    errors.append(f"Manifest missing required key: {key}")
            compat = manifest.get("compatibility", {})
            if compat.get("reconstruction_mode") != "deterministic":
                errors.append("Compatibility: reconstruction_mode must be 'deterministic'")
            if mode == "schema-only":
                if not isinstance(manifest.get("fold_count"), (int, type(None))):
                    errors.append("Manifest: fold_count must be int")
                if not isinstance(manifest.get("logical_gain_bytes"), (int, type(None))):
                    errors.append("Manifest: logical_gain_bytes must be int")

        ledger_path = root / "ledger.json"
        if ledger_path.exists() and mode in ("strict", "schema-only"):
            try:
                ledger = json.loads(ledger_path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as e:
                errors.append(f"Ledger invalid JSON: {e}")
                ledger = {}
            lr = ledger.get("fold_records", [])
            ltotal = ledger.get("total_folds", 0)
            lbytes = ledger.get("total_bytes_saved", 0)
            if mode == "strict":
                if len(lr) != ltotal:
                    errors.append(f"Ledger inconsistency: fold_records count {len(lr)} != total_folds {ltotal}")
                computed = sum(r.get("gain", 0) for r in lr)
                if computed != lbytes:
                    errors.append(f"Ledger inconsistency: sum(gain) {computed} != total_bytes_saved {lbytes}")
                consistency_errors = check_manifest_ledger_consistency(manifest, ledger)
                errors.extend(consistency_errors)
            logical_physical_errors = check_logical_vs_physical(manifest)
            errors.extend(logical_physical_errors)

        maps_path = root / "maps" / "reconstruction.json"
        if maps_path.exists() and mode == "strict":
            maps_data = json.loads(maps_path.read_text(encoding="utf-8"))
            records = maps_data.get("records", [])
            manifest_fold = manifest.get("fold_count", 0)
            if len(records) != manifest_fold:
                errors.append(f"Maps/manifest inconsistency: maps records {len(records)} != manifest fold_count {manifest_fold}")
            shared_dir = root / "shared"
            for i, rec in enumerate(records):
                op_id = rec.get("operator_id", "")
                if op_id == "exact_repetition":
                    if not (shared_dir / f"exact_{i}.txt").exists():
                        errors.append(f"Missing shared artifact: shared/exact_{i}.txt for record {i}")
                elif op_id == "template_skeleton":
                    if not (shared_dir / f"template_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/template_{i}.json for record {i}")
                elif op_id == "symbol_table":
                    if not (shared_dir / f"symbols_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/symbols_{i}.json for record {i}")
                elif op_id == "hierarchy_mirror":
                    if not (shared_dir / f"hierarchy_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/hierarchy_{i}.json for record {i}")
                elif op_id == "dependency_motif":
                    if not (shared_dir / f"dependency_motif_{i}.json").exists():
                        errors.append(f"Missing shared artifact: shared/dependency_motif_{i}.json for record {i}")
            if records:
                indices = [r.get("index", -1) for r in records]
                for j, idx in enumerate(indices):
                    if idx != j:
                        errors.append(f"Maps integrity: record at position {j} has index {idx}, expected {j}")

        report_path = root / "reports" / "report.json"
        if report_path.exists() and mode == "strict":
            report = json.loads(report_path.read_text(encoding="utf-8"))
            rfold = report.get("fold_count", -1)
            mfold = manifest.get("fold_count", -1)
            if rfold != mfold:
                errors.append(f"Manifest/report inconsistency: report fold_count {rfold} != manifest {mfold}")
            rgain = report.get("logical_gain_bytes", report.get("total_bytes_saved", -1))
            mgain = manifest.get("logical_gain_bytes", -1)
            if rgain != mgain:
                errors.append(f"Manifest/report inconsistency: report gain {rgain} != manifest {mgain}")

        integrity_path = root / "integrity.json"
        if integrity_path.exists() and mode in ("strict", "integrity-only"):
            integrity = json.loads(integrity_path.read_text(encoding="utf-8"))
            stored = integrity.get("checksums", {})
            for rel_path, expected in stored.items():
                fp = root / rel_path
                if not fp.exists():
                    errors.append(f"Integrity: checksum file missing: {rel_path}")
                else:
                    actual = _sha256_file(fp)
                    if actual != expected:
                        errors.append(f"Integrity mismatch: {rel_path} (expected {expected[:16]}..., got {actual[:16]}...)")

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


def _family_signatures(root: Path, records: list) -> dict[str, dict[str, set[str]]]:
    """Extract family signatures for template, hierarchy, dependency. Returns {op: {sig: set of paths/roots}}."""
    sigs: dict[str, dict[str, set[str]]] = {
        "template_skeleton": {},
        "hierarchy_mirror": {},
        "dependency_motif": {},
    }
    shared = root / "shared"
    for i, rec in enumerate(records):
        op = rec.get("operator_id", "")
        if op not in sigs:
            continue
        targets = rec.get("targets", [])
        if op == "template_skeleton":
            tmpl = shared / f"template_{i}.json"
            if tmpl.exists():
                data = json.loads(tmpl.read_text(encoding="utf-8"))
                paths = tuple(sorted(data.get("paths", targets)))
            else:
                paths = tuple(sorted(str(t) for t in targets))
            sig = hashlib.sha256(json.dumps(paths).encode()).hexdigest()[:16]
            sigs[op][sig] = set(paths)
        elif op == "hierarchy_mirror":
            hj = shared / f"hierarchy_{i}.json"
            if hj.exists():
                data = json.loads(hj.read_text(encoding="utf-8"))
                roots = tuple(sorted(str(r) for r in data.get("roots", [])))
                sig = data.get("structure_sig", "") or hashlib.sha256(json.dumps(roots).encode()).hexdigest()[:16]
                sigs[op][sig] = set(roots)
        elif op == "dependency_motif":
            dj = shared / f"dependency_motif_{i}.json"
            if dj.exists():
                data = json.loads(dj.read_text(encoding="utf-8"))
                paths = tuple(sorted(str(p) for p in data.get("paths", targets)))
                sig = data.get("signature", "") or hashlib.sha256(json.dumps(paths).encode()).hexdigest()[:16]
                sigs[op][sig] = set(paths)
    return sigs


def compare_archives(
    archive_a_path: Path | str,
    archive_b_path: Path | str,
) -> dict[str, Any]:
    """
    Compare two .infold archives. Returns diff summary:
    - logical_gain, physical_folded_size, operator fold counts
    - which template families, hierarchy templates, dependency motifs changed (added/removed)
    - compatibility metadata
    """
    a = Path(archive_a_path).resolve()
    b = Path(archive_b_path).resolve()
    if not a.exists():
        raise FileNotFoundError(f"Archive not found: {a}")
    if not b.exists():
        raise FileNotFoundError(f"Archive not found: {b}")
    info_a = explain_archive(a)
    info_b = explain_archive(b)
    m_a = info_a["manifest"]
    m_b = info_b["manifest"]
    pkg_a = info_a["package_summary"]
    pkg_b = info_b["package_summary"]
    diff = {
        "archive_a": str(a),
        "archive_b": str(b),
        "logical_gain": {
            "a": pkg_a["logical_gain_bytes"],
            "b": pkg_b["logical_gain_bytes"],
            "diff": pkg_b["logical_gain_bytes"] - pkg_a["logical_gain_bytes"],
        },
        "physical_folded_size": {
            "a": pkg_a["physical_folded_size_bytes"],
            "b": pkg_b["physical_folded_size_bytes"],
            "diff": pkg_b["physical_folded_size_bytes"] - pkg_a["physical_folded_size_bytes"],
        },
        "fold_counts_by_operator": {
            "a": info_a["fold_counts_by_operator"],
            "b": info_b["fold_counts_by_operator"],
            "diff": {},
        },
        "gain_by_operator": {
            "a": info_a["gain_by_operator"],
            "b": info_b["gain_by_operator"],
            "diff": {},
        },
        "template_families": {"a_count": len(info_a["template_families"]), "b_count": len(info_b["template_families"])},
        "duplicate_families": {"a_count": len(info_a["duplicate_families"]), "b_count": len(info_b["duplicate_families"])},
        "hierarchy_templates": {"a_count": len(info_a["hierarchy_templates"]), "b_count": len(info_b["hierarchy_templates"])},
        "dependency_motifs": {"a_count": len(info_a["dependency_motifs"]), "b_count": len(info_b["dependency_motifs"])},
        "template_families_changed": {"added": [], "removed": []},
        "hierarchy_templates_changed": {"added": [], "removed": []},
        "dependency_motifs_changed": {"added": [], "removed": []},
        "compatibility": {
            "a": m_a.get("compatibility", {}),
            "b": m_b.get("compatibility", {}),
        },
    }
    all_ops = set(info_a["fold_counts_by_operator"]) | set(info_b["fold_counts_by_operator"])
    for op in all_ops:
        diff["fold_counts_by_operator"]["diff"][op] = info_b["fold_counts_by_operator"].get(op, 0) - info_a["fold_counts_by_operator"].get(op, 0)
    for op in all_ops:
        diff["gain_by_operator"]["diff"][op] = info_b["gain_by_operator"].get(op, 0) - info_a["gain_by_operator"].get(op, 0)

    import tempfile
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(a, "r") as zf:
            zf.extractall(tmp)
        root_a = _extract_root(Path(tmp))
        maps_a = json.loads((root_a / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        recs_a = maps_a.get("records", [])
        sigs_a = _family_signatures(root_a, recs_a)
    with tempfile.TemporaryDirectory(prefix="infold_") as tmp:
        with zipfile.ZipFile(b, "r") as zf:
            zf.extractall(tmp)
        root_b = _extract_root(Path(tmp))
        maps_b = json.loads((root_b / "maps" / "reconstruction.json").read_text(encoding="utf-8"))
        recs_b = maps_b.get("records", [])
        sigs_b = _family_signatures(root_b, recs_b)

    for op in ["template_skeleton", "hierarchy_mirror", "dependency_motif"]:
        sa = sigs_a.get(op, {})
        sb = sigs_b.get(op, {})
        added = [sig for sig in sb if sig not in sa]
        removed = [sig for sig in sa if sig not in sb]
        if op == "template_skeleton":
            diff["template_families_changed"]["added"] = added
            diff["template_families_changed"]["removed"] = removed
        elif op == "hierarchy_mirror":
            diff["hierarchy_templates_changed"]["added"] = added
            diff["hierarchy_templates_changed"]["removed"] = removed
        elif op == "dependency_motif":
            diff["dependency_motifs_changed"]["added"] = added
            diff["dependency_motifs_changed"]["removed"] = removed

    return diff


def explain_to_text(info: dict[str, Any]) -> str:
    """Format explain output for human reading."""
    lines = [
        "Archive Explain",
        "===============",
        "",
        f"Path: {info.get('path', '?')}",
        "",
        "Package summary:",
    ]
    pkg = info.get("package_summary", {})
    for k, v in pkg.items():
        if isinstance(v, int) and "bytes" in str(k):
            lines.append(f"  {k}: {v:,}")
        else:
            lines.append(f"  {k}: {v}")
    lines.append("")
    lines.append("Fold counts by operator:")
    for op, cnt in info.get("fold_counts_by_operator", {}).items():
        lines.append(f"  {op}: {cnt}")
    lines.append("")
    lines.append("Biggest gain contributors:")
    for c in info.get("biggest_gain_contributors", [])[:5]:
        lines.append(f"  {c.get('operator_id', '?')}: {c.get('gain', 0):,} bytes")
    rejected = info.get("rejected_candidates_summary", [])
    if rejected:
        lines.append("")
        lines.append("Rejected candidates:")
        for r in rejected[:10]:
            lines.append(f"  {r.get('operator_id', '?')}: {r.get('reason', '?')} - {r.get('detail', '')[:50]}")
        if len(rejected) > 10:
            lines.append(f"  ... and {len(rejected) - 10} more")
    lines.append("")
    lines.append("Reconstruction guarantees:")
    for g in info.get("reconstruction_guarantees", []):
        lines.append(f"  {g}")
    return "\n".join(lines)


def compare_to_text(diff: dict[str, Any]) -> str:
    """Format compare output for human reading."""
    lines = [
        "Archive Compare",
        "===============",
        "",
        f"A: {diff.get('archive_a', '?')}",
        f"B: {diff.get('archive_b', '?')}",
        "",
        "Logical gain:",
        f"  A: {diff.get('logical_gain', {}).get('a', 0):,} bytes",
        f"  B: {diff.get('logical_gain', {}).get('b', 0):,} bytes",
        f"  diff (B-A): {diff.get('logical_gain', {}).get('diff', 0):+,} bytes",
        "",
        "Physical folded size:",
        f"  A: {diff.get('physical_folded_size', {}).get('a', 0):,} bytes",
        f"  B: {diff.get('physical_folded_size', {}).get('b', 0):,} bytes",
        f"  diff (B-A): {diff.get('physical_folded_size', {}).get('diff', 0):+,} bytes",
        "",
        "Fold counts by operator (diff B-A):",
    ]
    for op, d in diff.get("fold_counts_by_operator", {}).get("diff", {}).items():
        lines.append(f"  {op}: {d:+d}")
    lines.append("")
    lines.append("Families:")
    tf = diff.get("template_families", {})
    df = diff.get("duplicate_families", {})
    hf = diff.get("hierarchy_templates", {})
    mf = diff.get("dependency_motifs", {})
    lines.append(f"  template: A={tf.get('a_count', 0)} B={tf.get('b_count', 0)}")
    lines.append(f"  duplicate: A={df.get('a_count', 0)} B={df.get('b_count', 0)}")
    lines.append(f"  hierarchy: A={hf.get('a_count', 0)} B={hf.get('b_count', 0)}")
    lines.append(f"  dependency: A={mf.get('a_count', 0)} B={mf.get('b_count', 0)}")
    tfc = diff.get("template_families_changed", {})
    htc = diff.get("hierarchy_templates_changed", {})
    dmc = diff.get("dependency_motifs_changed", {})
    if tfc.get("added") or tfc.get("removed"):
        lines.append("")
        lines.append("Template families changed:")
        if tfc.get("added"):
            lines.append(f"  added: {tfc['added']}")
        if tfc.get("removed"):
            lines.append(f"  removed: {tfc['removed']}")
    if htc.get("added") or htc.get("removed"):
        lines.append("")
        lines.append("Hierarchy templates changed:")
        if htc.get("added"):
            lines.append(f"  added: {htc['added']}")
        if htc.get("removed"):
            lines.append(f"  removed: {htc['removed']}")
    if dmc.get("added") or dmc.get("removed"):
        lines.append("")
        lines.append("Dependency motifs changed:")
        if dmc.get("added"):
            lines.append(f"  added: {dmc['added']}")
        if dmc.get("removed"):
            lines.append(f"  removed: {dmc['removed']}")
    return "\n".join(lines)


def list_to_text(info: dict[str, Any]) -> str:
    """Format list output for human reading."""
    lines = [
        "Archive List",
        "============",
        "",
        f"Path: {info.get('path', '?')}",
        "",
        "Shared artifacts:",
    ]
    for a in info.get("shared_artifacts", []):
        lines.append(f"  {a.get('path', '?')} ({a.get('size', 0):,} bytes)")
    lines.append("")
    lines.append("Fold families by operator:")
    for op, fams in info.get("families_by_operator", {}).items():
        lines.append(f"  {op}:")
        for f in fams:
            paths = f.get("paths", f.get("roots_or_paths", []))
            lines.append(f"    [{f.get('index', '?')}] gain={f.get('gain', 0):,} targets={f.get('target_count', 0)}")
            if paths:
                for p in paths[:3]:
                    lines.append(f"      - {p}")
                if len(paths) > 3:
                    lines.append(f"      - ...")
    return "\n".join(lines)


def stats_to_text(stats: dict[str, Any]) -> str:
    """Format stats output for human reading."""
    lines = [
        "Archive Stats",
        "=============",
        "",
        f"Path: {stats.get('path', '?')}",
        "",
        "Metrics:",
        f"  logical_gain_bytes: {stats.get('logical_gain_bytes', 0):,}",
        f"  physical_folded_size_bytes: {stats.get('physical_folded_size_bytes', 0):,}",
        f"  original_size_bytes: {stats.get('original_size_bytes', 0):,}",
        f"  rejection_count: {stats.get('rejection_count', 0)}",
        "",
        "Per-operator contributions:",
    ]
    for op, g in stats.get("per_operator_contributions", {}).items():
        share = stats.get("per_operator_gain_share", {}).get(op, 0)
        lines.append(f"  {op}: {g:,} bytes ({share:.1%})")
    tm = stats.get("template_metrics")
    if tm and tm.get("families_found", 0) > 0:
        lines.append("")
        lines.append("Template metrics:")
        lines.append(f"  families_found: {tm.get('families_found', 0)}")
        if tm.get("family_purity"):
            lines.append(f"  family_purity: {tm['family_purity']}")
        if tm.get("slot_ambiguity"):
            lines.append(f"  slot_ambiguity: {tm['slot_ambiguity']}")
    hm = stats.get("hierarchy_metrics")
    if hm and hm.get("templates_found", 0) > 0:
        lines.append("")
        lines.append("Hierarchy metrics:")
        lines.append(f"  templates_found: {hm.get('templates_found', 0)}")
        if hm.get("instances_per_template"):
            lines.append(f"  instances_per_template: {hm['instances_per_template']}")
    dm = stats.get("dependency_metrics")
    if dm and dm.get("motifs_found", 0) > 0:
        lines.append("")
        lines.append("Dependency metrics:")
        lines.append(f"  motifs_found: {dm.get('motifs_found', 0)}")
        if dm.get("average_motif_size"):
            lines.append(f"  average_motif_size: {dm['average_motif_size']}")
    return "\n".join(lines)
