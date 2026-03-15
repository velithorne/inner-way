"""Python file parser. Stub implementation."""

from core.parsing.parser_base import ParserBase


class PythonParser(ParserBase):
    """Parser for Python source files."""

    def language(self) -> str:
        return "python"

    def parse(self, raw_text: str, path: str = "") -> dict:
        """Stub: return minimal structure. Full implementation would use ast module."""
        return {
            "raw_text": raw_text,
            "tokens": [],
            "parse_success": True,
            "ast": None,
            "symbols": {},
            "import_refs": [],
            "fingerprints": {},
            "diagnostics": [],
        }
