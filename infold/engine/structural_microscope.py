"""
Structural Microscope v0.1: magnify weak structure in tiny files for better fold matching.

Analysis-only, temporary. Does not store magnified form.
"""

import json
import re
from pathlib import Path
from typing import Any

from infold.models.project_sheet import ProjectSheet


def _magnify_json(content: str) -> list[str]:
    """Expand JSON to one key-value per line for structure visibility."""
    try:
        obj = json.loads(content)
        if isinstance(obj, dict):
            lines = []
            for k, v in sorted(obj.items()):
                lines.append(f'  "{k}": {json.dumps(v)}\n')
            return ["{\n"] + lines + ["}\n"] if lines else [content]
        if isinstance(obj, list):
            lines = ["[\n"]
            for i, v in enumerate(obj):
                lines.append(f"  {json.dumps(v)}\n")
            lines.append("]\n")
            return lines
    except (json.JSONDecodeError, TypeError):
        pass
    return content.splitlines(keepends=True) if content else []


def _magnify_ini(content: str) -> list[str]:
    """Normalize INI-like to one key=value per line."""
    lines = []
    for line in content.splitlines(keepends=True):
        line = line.rstrip("\n") + "\n" if line.endswith("\n") else line + "\n"
        m = re.match(r"^\s*([#;].*)$", line)
        if m:
            lines.append(line)
            continue
        m = re.match(r"^\s*(\[[^\]]*\])\s*$", line)
        if m:
            lines.append(line)
            continue
        m = re.match(r"^\s*([^=#;\s]+)\s*=\s*(.*)$", line)
        if m:
            lines.append(f"{m.group(1).strip()}={m.group(2).strip()}\n")
            continue
        lines.append(line)
    return lines


def _magnify_key_value(content: str) -> list[str]:
    """Generic key-value: one pair per line."""
    lines = []
    for line in content.splitlines(keepends=True):
        if "=" in line and not line.strip().startswith(("#", ";", "[")):
            k, _, v = line.partition("=")
            lines.append(f"{k.strip()}={v.strip()}\n")
        else:
            lines.append(line if line.endswith("\n") else line + "\n")
    return lines


def magnify_content(content: str, path: Path) -> list[str]:
    """
    Create magnified structural view. Deterministic.
    Returns list of lines (with keepends).
    """
    ext = path.suffix.lower()
    if ext == ".json":
        return _magnify_json(content)
    if ext in (".yaml", ".yml"):
        return content.splitlines(keepends=True)  # keep as-is for now
    if ext == ".toml":
        return content.splitlines(keepends=True)
    if ext in (".ini", ".cfg", ".conf"):
        return _magnify_ini(content)
    if ext in (".py", ".js", ".ts"):
        return content.splitlines(keepends=True)
    if ext in (".txt", ".md"):
        return _magnify_key_value(content) if "=" in content else content.splitlines(keepends=True)
    return content.splitlines(keepends=True)


def _structural_signature(lines: list[str]) -> str:
    """Deterministic signature: const/slot pattern."""
    parts = []
    for line in lines:
        s = line.strip()
        if not s or s.startswith("#") or s.startswith(";"):
            parts.append("_")
        elif "=" in s:
            parts.append("K")
        elif s in ("{", "}", "[", "]", ","):
            parts.append("P")
        elif s.startswith('"') or s.startswith("'"):
            parts.append("V")
        else:
            parts.append("T")
    return "".join(parts)


def find_eligible_tiny_files(
    project_sheet: ProjectSheet,
    max_bytes: int = 512,
    max_lines: int = 6,
    text_extensions: frozenset[str] | None = None,
) -> list[tuple[Path, str, list[str]]]:
    """
    Find files eligible for microscope analysis.
    Returns [(path, raw_content, magnified_lines), ...].
    """
    ext_allow = text_extensions or frozenset({".json", ".yaml", ".yml", ".toml", ".ini", ".cfg", ".py", ".js", ".ts", ".txt", ".md"})
    eligible: list[tuple[Path, str, list[str]]] = []
    for path, node in project_sheet.file_nodes.items():
        if node.diagnostics:
            continue
        if path.suffix.lower() not in ext_allow:
            continue
        raw = node.raw_text
        if len(raw.encode("utf-8")) > max_bytes:
            continue
        lines = raw.splitlines(keepends=True)
        if len(lines) > max_lines:
            continue
        if len(lines) < 2:
            continue
        magnified = magnify_content(raw, path)
        if len(magnified) < 2:
            continue
        eligible.append((path, raw, magnified))
    return eligible


def find_microscope_assisted_groups(
    project_sheet: ProjectSheet,
    max_bytes: int = 512,
    max_lines: int = 6,
    min_family: int = 3,
    min_magnified_similarity: float = 0.75,
) -> list[tuple[list[Path], list[list[str]]]]:
    """
    Group eligible tiny files by magnified structure.
    Returns [(paths, lines_list), ...] where lines_list uses ORIGINAL content.
    Only returns groups that could form templates (same original line count).
    """
    eligible = find_eligible_tiny_files(project_sheet, max_bytes, max_lines)
    if len(eligible) < min_family:
        return []

    by_magnified_sig: dict[str, list[tuple[Path, str, list[str]]]] = {}
    for path, raw, magnified in eligible:
        sig = _structural_signature(magnified)
        by_magnified_sig.setdefault(sig, []).append((path, raw, magnified))

    groups: list[tuple[list[Path], list[list[str]]]] = []
    for sig, items in by_magnified_sig.items():
        if len(items) < min_family:
            continue
        by_orig_line_count: dict[int, list[tuple[Path, str, list[str]]]] = {}
        for path, raw, magnified in items:
            orig_lines = raw.splitlines(keepends=True)
            n = len(orig_lines)
            by_orig_line_count.setdefault(n, []).append((path, raw, magnified))
        for n, sub in by_orig_line_count.items():
            if len(sub) < min_family:
                continue
            paths = [p for p, _, _ in sub]
            lines_list = [raw.splitlines(keepends=True) for _, raw, _ in sub]
            groups.append((paths, lines_list))
    return groups
