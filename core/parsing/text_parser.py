"""Generic text parser for unsupported or plain text files."""

from core.parsing.parser_base import ParserBase


class TextParser(ParserBase):
    """Fallback parser for text, markdown, and unknown formats."""

    def language(self) -> str:
        return "text"

    def parse(self, raw_text: str, path: str = "") -> dict:
        """Minimal parse: pass through raw text."""
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
