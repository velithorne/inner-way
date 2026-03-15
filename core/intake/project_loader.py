"""Load and scan project folders into ProjectSheet."""

import hashlib
import uuid
from pathlib import Path

from core.types import FileSheet, ProjectSheet


def load_project(
    path: str | Path,
    *,
    include_hidden: bool = False,
    include_vendor: bool = False,
) -> ProjectSheet:
    """
    Scan a project folder and build a ProjectSheet.
    Stub: returns minimal sheet with file paths; parsing delegated to parsers.
    """
    path = Path(path)
    if not path.is_dir():
        raise ValueError(f"Not a directory: {path}")

    project_id = str(uuid.uuid4())
    file_nodes: list[FileSheet] = []
    folder_nodes: list[str] = []

    for item in path.rglob("*"):
        if item.is_dir():
            if not include_hidden and item.name.startswith("."):
                continue
            if not include_vendor and item.name in ("vendor", "node_modules", "__pycache__", ".git"):
                continue
            folder_nodes.append(str(item.relative_to(path)))
        elif item.is_file():
            if not include_hidden and item.name.startswith("."):
                continue
            if not include_vendor:
                parts = item.parts
                if "vendor" in parts or "node_modules" in parts or "__pycache__" in parts or ".git" in parts:
                    continue
            rel = str(item.relative_to(path))
            lang = _guess_language(item)
            raw = item.read_text(errors="replace")
            file_nodes.append(
                FileSheet(
                    path=rel,
                    language=lang,
                    raw_text=raw,
                    parse_success=False,
                )
            )

    return ProjectSheet(
        project_id=project_id,
        file_nodes=file_nodes,
        folder_nodes=sorted(folder_nodes),
    )


def _guess_language(path: Path) -> str:
    """Guess language from extension."""
    ext = path.suffix.lower()
    mapping = {
        ".py": "python",
        ".js": "javascript",
        ".ts": "typescript",
        ".json": "json",
        ".yaml": "yaml",
        ".yml": "yaml",
        ".md": "markdown",
        ".txt": "text",
    }
    return mapping.get(ext, "text")
