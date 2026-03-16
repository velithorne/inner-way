"""
Intake layer: scans folders, classifies files, loads text, builds project inventory.
"""

import fnmatch
import hashlib
from pathlib import Path

from infold.models.file_node import FileNode
from infold.models.project_sheet import ProjectSheet

# Extension -> language mapping
EXTENSION_TO_LANGUAGE: dict[str, str] = {
    ".py": "python",
    ".js": "javascript",
    ".ts": "typescript",
    ".json": "json",
    ".yaml": "yaml",
    ".yml": "yaml",
    ".md": "markdown",
    ".txt": "text",
}


def _should_exclude(path: Path, exclude_patterns: list[str]) -> bool:
    """Return True if path should be excluded."""
    parts = path.parts
    for pattern in exclude_patterns:
        # Glob pattern (e.g. *.pyc)
        if "*" in pattern:
            if fnmatch.fnmatch(path.name, pattern):
                return True
        # Exact segment match (e.g. .git, __pycache__, node_modules)
        elif pattern in parts:
            return True
    return False


def _should_include(path: Path, include_extensions: list[str]) -> bool:
    """Return True if file has an included extension."""
    return path.suffix.lower() in include_extensions


def _load_text(path: Path) -> tuple[str, str | None]:
    """Load file as text. Returns (content, error). error is None on success."""
    try:
        content = path.read_text(encoding="utf-8", errors="replace")
        return content, None
    except Exception as e:
        return "", str(e)


def _raw_hash(content: str) -> str:
    """Compute hash of raw bytes (UTF-8)."""
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def scan_project(
    source_path: Path | str,
    *,
    include_extensions: list[str] | None = None,
    exclude_patterns: list[str] | None = None,
) -> ProjectSheet:
    """
    Scan a project folder and build the initial project inventory.

    Returns a ProjectSheet with FileNodes (raw text loaded, tokens/AST empty until parsing).
    """
    source_path = Path(source_path).resolve()
    if not source_path.is_dir():
        raise ValueError(f"Source path is not a directory: {source_path}")

    if include_extensions is None:
        include_extensions = [".py", ".js", ".ts", ".json", ".yaml", ".yml", ".md", ".txt"]
    if exclude_patterns is None:
        exclude_patterns = [".git", "__pycache__", "*.pyc", ".venv", "venv", "node_modules", ".tox", "dist", "build"]

    file_nodes: dict[Path, FileNode] = {}
    folder_nodes: list[Path] = []

    for item in source_path.rglob("*"):
        rel = item.relative_to(source_path)
        if _should_exclude(rel, exclude_patterns):
            continue
        if item.is_dir():
            folder_nodes.append(rel)
            continue
        if not item.is_file() or not _should_include(item, include_extensions):
            continue
        content, load_error = _load_text(item)
        if load_error:
            # Create FileNode with empty content and diagnostic
            file_nodes[rel] = FileNode(
                path=rel,
                language=EXTENSION_TO_LANGUAGE.get(item.suffix.lower(), "text"),
                raw_text="",
                tokens=[],
                ast_data=None,
                symbols=[],
                imports=[],
                raw_hash="",
                token_hash=None,
                parser_confidence=0.0,
                diagnostics=[{"type": "load_error", "message": load_error}],
            )
        else:
            file_nodes[rel] = FileNode(
                path=rel,
                language=EXTENSION_TO_LANGUAGE.get(item.suffix.lower(), "text"),
                raw_text=content,
                tokens=[],
                ast_data=None,
                symbols=[],
                imports=[],
                raw_hash=_raw_hash(content),
                token_hash=None,
                parser_confidence=0.0,
                diagnostics=[],
            )

    return ProjectSheet(
        source_path=source_path,
        file_nodes=file_nodes,
        folder_nodes=sorted(set(folder_nodes)),
        metrics={
            "file_count": len(file_nodes),
            "folder_count": len(folder_nodes),
            "original_size_bytes": sum(len(n.raw_text.encode("utf-8")) for n in file_nodes.values()),
        },
    )
