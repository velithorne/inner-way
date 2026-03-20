"""Parser contract and result types."""

from dataclasses import dataclass
from typing import Any, Protocol


@dataclass
class ParserResult:
    """Output from a parser: tokens, AST, symbols, imports, fingerprints, diagnostics."""

    tokens: list[dict[str, Any]]  # {"type": str, "value": str, "line": int, "col": int}
    ast_data: Any | None
    symbols: list[dict[str, Any]]  # {"name": str, "kind": str, "line": int, ...}
    imports: list[dict[str, Any]]  # {"module": str, "names": list, "line": int, ...}
    token_hash: str | None
    parse_success: bool
    parser_confidence: float  # 0.0–1.0
    diagnostics: list[dict[str, Any]]


class Parser(Protocol):
    """Parser contract: parse(raw_text, language, strict) -> ParserResult."""

    def parse(
        self,
        raw_text: str,
        language: str,
        *,
        strict: bool = False,
    ) -> ParserResult:
        ...
