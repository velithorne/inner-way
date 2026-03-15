"""Registry of parsers by language."""

from core.parsing.parser_base import ParserBase
from core.parsing.python_parser import PythonParser
from core.parsing.text_parser import TextParser


class ParserRegistry:
    """Registry of parsers by language."""

    def __init__(self) -> None:
        self._parsers: dict[str, ParserBase] = {}
        self._register_defaults()

    def _register_defaults(self) -> None:
        self.register(PythonParser())
        self.register(TextParser())

    def register(self, parser: ParserBase) -> None:
        """Register a parser for its language."""
        self._parsers[parser.language()] = parser

    def get(self, language: str) -> ParserBase | None:
        """Get parser for language, or None."""
        return self._parsers.get(language)

    def parse(self, language: str, raw_text: str, path: str = "") -> dict:
        """Parse using the appropriate parser. Falls back to text parser if unknown."""
        parser = self.get(language) or self.get("text")
        if parser:
            return parser.parse(raw_text, path)
        return {"raw_text": raw_text, "parse_success": False, "diagnostics": ["No parser available"]}


_registry: ParserRegistry | None = None


def get_registry() -> ParserRegistry:
    """Singleton parser registry."""
    global _registry
    if _registry is None:
        _registry = ParserRegistry()
    return _registry
