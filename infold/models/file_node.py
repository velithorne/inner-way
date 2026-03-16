"""Single loaded file in the project."""

from dataclasses import dataclass
from pathlib import Path
from typing import Any


@dataclass
class FileNode:
    """One loaded file: path, language, raw text, tokens, AST data, symbols, imports, hashes, parser confidence, diagnostics."""

    path: Path
    language: str
    raw_text: str
    tokens: list[Any]  # token objects from parser
    ast_data: Any | None  # AST where supported
    symbols: list[Any]  # symbol references
    imports: list[Any]  # import/dependency references
    raw_hash: str  # hash of raw bytes
    token_hash: str | None  # hash of token stream
    parser_confidence: float  # 0.0–1.0
    diagnostics: list[dict[str, Any]]  # parse warnings/errors
