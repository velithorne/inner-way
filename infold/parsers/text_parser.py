"""Text fallback parser: sections, headings, repeated blocks."""

import hashlib
import re
from typing import Any

from infold.parsers.base import ParserResult


def _hash_tokens(tokens: list[dict[str, Any]]) -> str:
    """Compute hash of token stream."""
    parts = [f"{t.get('type','')}:{t.get('value','')}" for t in tokens]
    return hashlib.sha256("|".join(parts).encode()).hexdigest()


def parse_text(raw_text: str, *, strict: bool = False) -> ParserResult:
    """
    Parse plain text. Extracts lines, sections (blank-line separated), headings (# prefix).
    """
    tokens: list[dict[str, Any]] = []
    for i, line in enumerate(raw_text.splitlines(), start=1):
        stripped = line.strip()
        if not stripped:
            tokens.append({"type": "blank", "value": "", "line": i, "col": 0})
        elif stripped.startswith("#"):
            # Markdown-style heading
            level = len(re.match(r"^#+", stripped).group()) if re.match(r"^#+", stripped) else 1
            value = stripped.lstrip("#").strip()[:80]
            tokens.append({"type": "heading", "value": value, "line": i, "col": 0, "level": level})
        else:
            tokens.append({"type": "line", "value": stripped[:100], "line": i, "col": 0})
    token_hash = _hash_tokens(tokens) if tokens else None
    return ParserResult(
        tokens=tokens,
        ast_data=None,
        symbols=[],
        imports=[],
        token_hash=token_hash,
        parse_success=True,
        parser_confidence=0.5,  # Text has minimal structure
        diagnostics=[],
    )


def parse_markdown(raw_text: str, *, strict: bool = False) -> ParserResult:
    """
    Parse Markdown. Sections, headings, repeated blocks, template-like regions.
    """
    result = parse_text(raw_text, strict=strict)
    result.parser_confidence = 0.7  # Slightly higher for markdown structure
    return result
