"""
Anchor Files v0.1/v0.2: detect files that define or stabilize structure of nearby files.

Conservative, deterministic. No content folding - metadata/annotation only.
v0.2: Shared anchor context utilities, context compression, operator guidance.
"""

from pathlib import Path
from typing import Any

from infold.models.project_sheet import ProjectSheet


# --- Shared anchor context utilities (Phase 18B) ---


def build_path_to_anchor_scopes(anchors: list[dict[str, Any]]) -> dict[str, set[str]]:
    """
    Map each path to set of anchor path identifiers that scope it.
    Reusable by Fold Echo, Mutation Chain, package, report.
    """
    path_to_scopes: dict[str, set[str]] = {}
    for anchor in anchors:
        anchor_id = anchor.get("path", "")
        for p in anchor.get("anchored_paths", []):
            path_to_scopes.setdefault(p, set()).add(anchor_id)
    return path_to_scopes


def paths_share_anchor(
    paths: list[str] | list[Path],
    path_to_scopes: dict[str, set[str]],
) -> str | None:
    """
    Return anchor id if all paths share at least one anchor scope, else None.
    Deterministic: returns min(common) when multiple anchors match.
    """
    if not paths or not path_to_scopes:
        return None
    norm = lambda x: str(x).replace("\\", "/")
    common = path_to_scopes.get(norm(paths[0]), set()).copy()
    for p in paths[1:]:
        common &= path_to_scopes.get(norm(p), set())
    return min(common) if common else None


def path_in_anchor_scope(path: str | Path, anchor_id: str, path_to_scopes: dict[str, set[str]]) -> bool:
    """True if path is in the given anchor's scope."""
    norm = str(path).replace("\\", "/")
    return anchor_id in path_to_scopes.get(norm, set())


def get_nearest_anchor_for_path(
    path: str | Path,
    path_to_scopes: dict[str, set[str]],
    anchors: list[dict[str, Any]],
) -> dict[str, Any] | None:
    """
    Return the anchor dict for the path's scope, or None.
    When multiple anchors scope the path, return the one with shortest path (most local).
    """
    norm = str(path).replace("\\", "/")
    scopes = path_to_scopes.get(norm, set())
    if not scopes:
        return None
    by_id = {a.get("path", ""): a for a in anchors if a.get("path", "") in scopes}
    if not by_id:
        return None
    return by_id[min(scopes)]


def anchor_relative_path(path: str | Path, anchor_dir: Path) -> str:
    """
    Return path relative to anchor directory for compact representation.
    Exactly reversible: anchor_dir / rel == path.
    """
    p = Path(str(path).replace("\\", "/"))
    try:
        rel = p.relative_to(Path(str(anchor_dir).replace("\\", "/")))
        return str(rel).replace("\\", "/")
    except ValueError:
        return str(p).replace("\\", "/")


def expand_anchor_relative_path(rel: str, anchor_path: str) -> str:
    """Expand anchor-relative path back to full path. Exactly reversible."""
    anchor_dir = Path(str(anchor_path).replace("\\", "/")).parent
    full = anchor_dir / rel
    return str(full).replace("\\", "/")


# Canonical anchor filenames (deterministic, project-style)
ANCHOR_FILENAMES: frozenset[str] = frozenset({
    "package.json",
    "manifest.json",
    "pyproject.toml",
    "setup.py",
    "setup.cfg",
    "Cargo.toml",
    "go.mod",
    "requirements.txt",
    "Pipfile",
    "tsconfig.json",
    "webpack.config.js",
    "Makefile",
    "README.md",
    "index.html",
    "config.json",
    "settings.json",
})

# Anchor types for reporting
ANCHOR_TYPES: dict[str, str] = {
    "package.json": "package_root",
    "manifest.json": "manifest",
    "pyproject.toml": "project_metadata",
    "setup.py": "project_metadata",
    "setup.cfg": "project_metadata",
    "Cargo.toml": "dependency_descriptor",
    "go.mod": "dependency_descriptor",
    "requirements.txt": "dependency_descriptor",
    "Pipfile": "dependency_descriptor",
    "tsconfig.json": "config_root",
    "webpack.config.js": "config_root",
    "Makefile": "registry_index",
    "README.md": "project_metadata",
    "index.html": "manifest",
    "config.json": "config_root",
    "settings.json": "config_root",
}


def _path_depth(p: Path) -> int:
    """Number of path components (lower = more central)."""
    return len(p.parts)


def _is_anchor_filename(name: str) -> bool:
    """Check if filename is a known anchor."""
    return name in ANCHOR_FILENAMES


def find_anchor_files(
    project_sheet: ProjectSheet,
    source_path: Path,
    max_scope_depth: int = 3,
) -> list[dict[str, Any]]:
    """
    Detect anchor files in the project.
    Returns list of {path, anchor_type, scope, anchored_count, metadata_reduction_estimate}.
    """
    source = Path(source_path).resolve()
    anchors: list[dict[str, Any]] = []

    for path, node in project_sheet.file_nodes.items():
        if node.diagnostics:
            continue
        name = path.name
        if not _is_anchor_filename(name):
            continue
        path_str = str(path).replace("\\", "/")
        anchor_dir = path.parent

        # Count "anchored" members: files in same dir or subdirs within scope
        anchored: list[str] = []
        for other_path, other_node in project_sheet.file_nodes.items():
            if other_path == path:
                continue
            if other_node.diagnostics:
                continue
            if anchor_dir == Path("."):
                # Root anchor: include all project files
                anchored.append(str(other_path).replace("\\", "/"))
            else:
                try:
                    rel = other_path.relative_to(anchor_dir)
                    if len(rel.parts) <= max_scope_depth:
                        anchored.append(str(other_path).replace("\\", "/"))
                except ValueError:
                    pass

        anchor_type = ANCHOR_TYPES.get(name, "manifest")
        # Metadata reduction: rough estimate - each anchored path could save ~len(path) * 0.1 if we used anchor-relative refs
        metadata_reduction = min(500, len(anchored) * 8)  # conservative cap

        anchors.append({
            "path": path_str,
            "anchor_type": anchor_type,
            "scope": "directory",
            "anchored_count": len(anchored),
            "anchored_paths": anchored[:50],  # cap for storage
            "metadata_reduction_estimate": metadata_reduction,
        })

    # Sort by path for determinism
    anchors.sort(key=lambda a: a["path"])
    return anchors
