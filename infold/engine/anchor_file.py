"""
Anchor Files v0.1: detect files that define or stabilize structure of nearby files.

Conservative, deterministic. No content folding - metadata/annotation only.
"""

from pathlib import Path
from typing import Any

from infold.models.project_sheet import ProjectSheet


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
